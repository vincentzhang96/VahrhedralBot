package co.phoenixlab.discord.api;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.entities.voice.VoiceServerUpdate;
import co.phoenixlab.discord.api.entities.voice.VoiceStateUpdate;
import co.phoenixlab.discord.api.event.*;
import co.phoenixlab.discord.api.event.MessageReactionChangeEvent.ReactionChange;
import co.phoenixlab.discord.api.event.ServerBanChangeEvent.BanChange;
import co.phoenixlab.discord.api.event.voice.VoiceServerUpdateEvent;
import co.phoenixlab.discord.api.event.voice.VoiceStateUpdateEvent;
import co.phoenixlab.discord.cfg.DiscordApiClientConfig;
import co.phoenixlab.discord.cfg.InfluxDbConfig;
import co.phoenixlab.discord.stats.RunningAverage;
import co.phoenixlab.discord.util.TryingScheduledExecutor;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.gson.Gson;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.api.measurements.KeyValueMetricMeasurementTransformer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static java.util.concurrent.TimeUnit.*;

public class DiscordWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiWebSocketClient");
    private static final Map<String, String> header;

    static {
        header = new HashMap<>();
        header.put("Accept-Encoding", "gzip");
    }

    private final DiscordApiClient apiClient;
    private final JSONParser parser;
    private final Gson gson;
    private final Statistics statistics;
    private ScheduledFuture keepAliveFuture;

    private MetricRegistry metricRegistry;
    private ScheduledReporter metricReporter;
    private ScheduledExecutorService metricExecutorService;

    public DiscordWebSocketClient(DiscordApiClient apiClient, URI serverUri, DiscordApiClientConfig config) {
        super(serverUri, new Draft_10(), header, 0);
        this.apiClient = apiClient;
        this.parser = new JSONParser();
        this.gson = new Gson();
        statistics = new Statistics();

        metricRegistry = new MetricRegistry();
        metricRegistry.register("events.count.ws_event", (Gauge<Long>) statistics.messageReceiveCount::longValue);
        metricExecutorService = new TryingScheduledExecutor(Executors.newScheduledThreadPool(1), LOGGER);
        if (config.isEnableMetrics() && config.getReportingIntervalMsec() > 0) {
            InfluxDbConfig idbc = config.getApiClientInfluxConfig();
            HttpInfluxdbProtocol protocol = idbc.toInfluxDbProtocolConfig();
            LOGGER.info("Will be connecting to InfluxDB at {}", gson.toJson(protocol));
            metricReporter = InfluxdbReporter.forRegistry(metricRegistry)
                .protocol(protocol)
                .convertDurationsTo(MILLISECONDS)
                .convertRatesTo(SECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(true)
                .transformer(new KeyValueMetricMeasurementTransformer())
                .withScheduler(metricExecutorService)
                .build();
            metricReporter.start(config.getReportingIntervalMsec(), MILLISECONDS);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Thread.currentThread().setName("WebSocketClient");
        LOGGER.info("WebSocket connection opened");
        org.json.JSONObject connectObj = new org.json.JSONObject();
        connectObj.put("op", 2);
        org.json.JSONObject dataObj = new org.json.JSONObject();
        dataObj.put("token", apiClient.getToken());
        dataObj.put("v", 3);
        dataObj.put("large_threshold", 250);
        dataObj.put("compress", false);
        org.json.JSONObject properties = new org.json.JSONObject();
        properties.put("$os", "Linux");
        properties.put("$browser", "Java");
        properties.put("$device", "Java");
        properties.put("$referrer", "");
        properties.put("$referring_domain", "");
        dataObj.put("properties", properties);
        connectObj.put("d", dataObj);

        send(connectObj.toString());
    }

    @Override
    public void onMessage(String message) {
        long start = System.nanoTime();
        try {
            LOGGER.trace("Recieved message: {}", message);
            statistics.messageReceiveCount.increment();
            try {
                JSONObject msg = (JSONObject) parser.parse(message);
                String errorMessage = (String) msg.get("message");
                if (errorMessage != null) {
                    if (errorMessage.isEmpty()) {
                        LOGGER.warn("[0] '': Discord returned an unknown error");
                    } else {
                        LOGGER.warn("[0] '': Discord returned error: {}", errorMessage);
                    }
                    statistics.errorCount.increment();
                    return;
                }
                int opCode = ((Number) msg.get("op")).intValue();
                if (opCode != 0) {
                    LOGGER.warn("Unknown opcode {} received: {}", opCode,
                            message);
                    return;
                }
                String type = (String) msg.get("t");
                JSONObject data = (JSONObject) msg.get("d");
                switch (type) {
                    case "READY":
                        handleReadyMessage(data);
                        break;
                    case "GUILD_MEMBERS_CHUNK":
                        handleGuildMembersChunk(data);
                        break;
                    case "USER_UPDATE":
                        handleUserUpdate(data);
                        break;
                    case "USER_SETTINGS_UPDATE":
                        //  Don't care
                        break;
                    case "MESSAGE_CREATE":
                        handleMessageCreate(data);
                        break;
                    case "MESSAGE_UPDATE":
                        handleMessageUpdate(data);
                        break;
                    case "MESSAGE_DELETE":
                        handleMessageDelete(data);
                        break;
                    case "BULK_MESSAGE_DELETE":
                        handleBulkMessageDelete(data);
                        break;
                    case "TYPING_START":
                        //  Don't care
                        break;
                    case "GUILD_CREATE":
                        handleGuildCreate(data);
                        break;
                    case "GUILD_DELETE":
                        handleGuildDelete(data);
                        break;
                    case "GUILD_UPDATE":
                        handleGuildUpdate(data);
                        break;
                    case "GUILD_MEMBER_ADD":
                        handleGuildMemberAdd(data);
                        break;
                    case "GUILD_MEMBER_REMOVE":
                        handleGuildMemberRemove(data);
                        break;
                    case "GUILD_MEMBER_UPDATE":
                        handleGuildMemberUpdate(data);
                        break;
                    case "GUILD_ROLE_CREATE":
                        handleGuildRoleCreate(data);
                        break;
                    case "GUILD_ROLE_DELETE":
                        handleGuildRoleDelete(data);
                        break;
                    case "GUILD_ROLE_UPDATE":
                        handleGuildRoleUpdate(data);
                        break;
                    case "CHANNEL_CREATE":
                        handleChannelCreate(data);
                        break;
                    case "CHANNEL_DELETE":
                        handleChannelDelete(data);
                        break;
                    case "CHANNEL_UPDATE":
                        handleChannelUpdate(data);
                        break;
                    case "MESSAGE_REACTION_ADD":
                        handleMessageReactionAdd(data);
                        break;
                    case "MESSAGE_REACTION_REMOVE":
                        handleMessageReactionRemove(data);
                        break;
                    case "PRESENCE_UPDATE":
                        handlePresenceUpdate(data, message);
                        break;
                    case "VOICE_STATE_UPDATE":
                        handleVoiceStateUpdate(data);
                        break;
                    case "VOICE_SERVER_UPDATE":
                        handleVoiceServerUpdate(data);
                        break;
                    case "GUILD_BAN_ADD":
                        handleGuildBanAdd(data);
                        break;
                    case "GUILD_BAN_DELETE":
                        handleGuildBanDelete(data);
                        break;
                    case "GUILD_EMOJIS_UPDATE":
                        //  Ignored
                        break;
                    case "MESSAGE_ACK":
                        //  Ignored
                        break;
                    //  TODO
                    default:
                        LOGGER.warn("[0] '': Unknown message type {}:\n{}", type, data.toJSONString());
                }
            } catch (Exception e) {
                LOGGER.warn("[0] '': Unable to parse message", e);
            }
        } finally {
            statistics.avgMessageHandleTime.add(MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS));
        }
    }

    private void handleMessageReactionAdd(JSONObject data) {
        MessageReactionUpdate update = jsonObjectToObject(data, MessageReactionUpdate.class);
        Channel channel = apiClient.getChannelById(update.getChannelId());
        Server server = NO_SERVER;
        if (channel != NO_CHANNEL) {
            server = Optional.ofNullable(channel.getParent()).orElse(NO_SERVER);
        }
        apiClient.getEventBus().post(new MessageReactionChangeEvent(update, ReactionChange.ADDED,
            server, channel, apiClient));
    }

    private void handleMessageReactionRemove(JSONObject data) {
        MessageReactionUpdate update = jsonObjectToObject(data, MessageReactionUpdate.class);
        Channel channel = apiClient.getChannelById(update.getChannelId());
        Server server = NO_SERVER;
        if (channel != NO_CHANNEL) {
            server = Optional.ofNullable(channel.getParent()).orElse(NO_SERVER);
        }
        apiClient.getEventBus().post(new MessageReactionChangeEvent(update, ReactionChange.DELETED,
            server, channel, apiClient));
    }

    private void handleGuildUpdate(JSONObject data) {
        Server server = jsonObjectToObject(data, Server.class);
        Server localServer = apiClient.getServerByID(server.getId());
        SafeNav.of(server.getIcon()).ifPresent(localServer::setIcon);
        SafeNav.of(server.getOwnerId()).ifPresent(localServer::setOwnerId);
        SafeNav.of(server.getRegion()).ifPresent(localServer::setRegion);
    }

    private void handleGuildBanDelete(JSONObject data) {
        String serverId = (String) data.get("guild_id");
        JSONObject userJSON = (JSONObject) data.get("user");
        User user = jsonObjectToObject(userJSON, User.class);
        Server server = apiClient.getServerByID(serverId);
        apiClient.getEventBus().post(new ServerBanChangeEvent(user, server, BanChange.DELETED));
    }

    private void handleGuildBanAdd(JSONObject data) {
        String serverId = (String) data.get("guild_id");
        JSONObject userJSON = (JSONObject) data.get("user");
        User user = jsonObjectToObject(userJSON, User.class);
        Server server = apiClient.getServerByID(serverId);
        apiClient.getEventBus().post(new ServerBanChangeEvent(user, server, BanChange.ADDED));
    }

    private void handleMessageDelete(JSONObject data) {
        String messageId = (String) data.get("id");
        String channelId = (String) data.get("channel_id");
        LOGGER.debug("Message {} deleted from {}", messageId, channelId);
        apiClient.getEventBus().post(new MessageDeleteEvent(messageId, channelId));
    }

    private void handleBulkMessageDelete(JSONObject data) {
        Object[] idsObjs = (Object[]) data.get("ids");
        List<String> collect = Arrays.stream(idsObjs)
            .map(o -> (o instanceof String ? (String) o : o.toString()))
            .collect(Collectors.toList());
        String channelId = (String) data.get("channel_id");
        LOGGER.debug("{} messages deleted from {}", collect.size(), channelId);
        collect.stream()
            .map(id -> new MessageDeleteEvent(id, channelId))
            .forEach(apiClient.getEventBus()::post);
    }

    private void handleMessageUpdate(JSONObject data) {
        LOGGER.debug("Message edited: {}", data.toJSONString());
        Message message = jsonObjectToObject(data, Message.class);
        Channel channel = apiClient.getChannelById(message.getChannelId());
        if (channel == null || channel == NO_CHANNEL || channel.isPrivate()) {
            message.setPrivateMessage(true);
        } else {
            message.setPrivateMessage(false);
        }
        apiClient.getEventBus().post(new MessageEditEvent(message));
    }

    private void handleGuildMembersChunk(JSONObject data) {
        String serverId = (String) data.get("guild_id");
        JSONArray array = (JSONArray) data.get("members");
        Member[] members = new Member[array.size()];
        for (int i = 0; i < array.size(); i++) {
            Object object = array.get(i);
            Member m = jsonObjectToObject((JSONObject) object, Member.class);
            members[i] = m;
        }
        Server server = apiClient.getServerByID(serverId);
        Collections.addAll(server.getMembers(), members);
        LOGGER.debug("[{}] '{}': Received guild member chunk size {}",
                server.getId(), server.getName(),
                members.length);
    }

    private void handleUserUpdate(JSONObject data) {
        UserUpdate update = jsonObjectToObject(data, UserUpdate.class);
        LOGGER.debug("[0] '': Received user account update: username={} id={} avatar={} email={}",
                update.getUsername(), update.getId(), update.getAvatar(), update.getEmail());
        apiClient.getEventBus().post(new UserUpdateEvent(update));
    }

    private void handleVoiceStateUpdate(JSONObject data) {
        VoiceStateUpdate update = jsonObjectToObject(data, VoiceStateUpdate.class);
        update.fix(apiClient);
        Server server = update.getServer();
        LOGGER.debug("[{}] '{}': Received voice status update for {} ({})",
                server.getId(), server.getName(),
                update.getUser().getUsername(), update.getUser().getId());
        apiClient.getEventBus().post(new VoiceStateUpdateEvent(update));
    }

    private void handleVoiceServerUpdate(JSONObject data) {
        VoiceServerUpdate update = jsonObjectToObject(data, VoiceServerUpdate.class);
        update.fix(apiClient);
        Server server = update.getServer();
        LOGGER.debug("[{}] '{}': Received voice server update: endpoint={} token={}",
                server.getId(), server.getName(),
                update.getEndpoint(), update.getToken());
        apiClient.getEventBus().post(new VoiceServerUpdateEvent(update));
    }

    private void handleGuildRoleUpdate(JSONObject data) {
        Role role = jsonObjectToObject((JSONObject) data.get("role"), Role.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            //  Literally just shove it in because Set
            server.getRoles().remove(role);
            server.getRoles().add(role);
            LOGGER.debug("[{}] '{}': Updated role {} ({})",
                    server.getId(), server.getName(),
                    role.getName(), role.getId());
            apiClient.getEventBus().post(new RoleChangeEvent(role, server,
                    RoleChangeEvent.RoleChange.UPDATED));
        } else {
            LOGGER.warn("[{}] '': Orphan role update received, ignored (roleid={} rolename={})",
                    serverId,
                    role.getId(), role.getName());
        }
    }

    private void handleGuildRoleDelete(JSONObject data) {
        String roleId = (String) data.get("role_id");
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            Role removed = null;
            for (Iterator<Role> iterator = server.getRoles().iterator(); iterator.hasNext(); ) {
                Role role = iterator.next();
                if (role.getId().equals(roleId)) {
                    removed = role;
                    iterator.remove();
                    break;
                }
            }
            if (removed != null) {
                LOGGER.debug("[{}] '{}': Deleted role {} ({})",
                        server.getId(), server.getName(),
                        removed.getName(), removed.getId());
                apiClient.getEventBus().post(new RoleChangeEvent(removed, server,
                        RoleChangeEvent.RoleChange.DELETED));
            } else {
                LOGGER.warn("[{}] '{}': No such role to delete (roleid={})",
                        server.getId(), server.getName(),
                        roleId);
            }
        } else {
            LOGGER.warn("[{}] '': Orphan role delete received, ignored (roleid={})",
                    serverId,
                    roleId);
        }
    }

    private void handleGuildRoleCreate(JSONObject data) {
        Role role = jsonObjectToObject((JSONObject) data.get("role"), Role.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            server.getRoles().add(role);
            LOGGER.debug("[{}] '{}': Added new role {} ({})",
                    server.getId(), server.getName(),
                    role.getName(), role.getId());
            apiClient.getEventBus().post(new RoleChangeEvent(role, server,
                    RoleChangeEvent.RoleChange.CREATED));
        } else {
            LOGGER.warn("[{}] '': Orphan role create received, ignored (roleid={} rolename={})",
                    serverId,
                    role.getId(), role.getName());
        }
    }

    private void handlePresenceUpdate(JSONObject data, String raw) {
        PresenceUpdate update = jsonObjectToObject(data, PresenceUpdate.class);
        Server server = apiClient.getServerByID(update.getServerId());
        if (server != NO_SERVER) {
            User updateUser = update.getUser();
            User user = apiClient.getUserById(updateUser.getId(), server);
            String oldUsername = user.getUsername();
            SafeNav.of(updateUser.getAvatar()).ifPresent(user::setAvatar);
            SafeNav.of(updateUser.getUsername()).ifPresent(user::setUsername);
            Member member = apiClient.getUserMember(user, server);
            if (member != NO_MEMBER && member.getUser().equals(user)) {
                member.getRoles().clear();
                member.getRoles().addAll(update.getRoles());
                LOGGER.debug("[{}] '{}': {}'s ({}) presence changed",
                        server.getId(), server.getName(),
                        user.getUsername(), user.getId());
                apiClient.getUserGames().put(user.getId(), update.getGame());
                apiClient.getUserPresences().put(user.getId(), update.getStatus());

                //  Rewrite the updateUser to have the right info
                updateUser.setUsername(user.getUsername());
                updateUser.setDiscriminator(user.getDiscriminator());
                updateUser.setAvatar(user.getAvatar());

                apiClient.getEventBus().post(new PresenceUpdateEvent(oldUsername, update, server));
            } else {
//                LOGGER.warn("[{}] '{}': Orphan presence update received, ignored (userid={} username={}): Not found",
//                        server.getId(), server.getName(),
//                        update.getUser().getId(), update.getUser().getUsername());
            }
        } else {
//            LOGGER.warn("[{}] '{}': Orphan presence update received, ignored (userid={} username={})",
//                    update.getServerId(), (server == null ? "" : server.getName()),
//                    update.getUser().getId(), update.getUser().getUsername());
        }
    }

    private void handleGuildMemberUpdate(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            Member oldMember = apiClient.getUserMember(member.getUser(), server);
            String oldNickname = null;
            if (oldMember != null) {
                oldNickname = oldMember.getNick();
                if (member.getJoinedAt() == null) {
                    member.setJoinedAt(oldMember.getJoinedAt());
                }
            }
            server.getMembers().remove(member);
            server.getMembers().add(member);
            LOGGER.debug("[{}] '{}': Updated {}'s ({}) membership",
                    server.getId(), server.getName(),
                    member.getUser().getUsername(), member.getUser().getId());
            apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                    MemberChangeEvent.MemberChange.UPDATED, oldNickname));
        } else {
            LOGGER.warn("[{}] '': Orphan member update received, ignored (userid={} username={})",
                    serverId,
                    member.getUser().getId(), member.getUser().getUsername());
        }
    }

    private void handleGuildMemberRemove(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            if (server.getMembers().remove(member)) {
                LOGGER.debug("[{}] '{}': Removed {}'s ({}) membership",
                        server.getId(), server.getName(),
                        member.getUser().getUsername(), member.getUser().getId());
                apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                        MemberChangeEvent.MemberChange.DELETED));
            } else {
                LOGGER.warn("[{}] '{}': Member {} ({}) could not be removed: Not found",
                        server.getId(), server.getName(),
                        member.getUser().getId(), member.getUser().getUsername());
            }
        } else {
            LOGGER.warn("[{}] '': Orphan member remove received, ignored (userid={} username={})",
                    serverId,
                    member.getUser().getId(), member.getUser().getUsername());
        }
    }

    private void handleGuildMemberAdd(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            //  Fix missing join date time
            if (member.getJoinedAt() == null) {
                member = new Member(member.getUser(), member.getRoles(), ZonedDateTime.now().toString(),
                        member.getNick());
            }
            server.getMembers().add(member);
            LOGGER.debug("[{}] '{}': Added {}'s ({}) membership",
                    server.getId(), server.getName(),
                    member.getUser().getUsername(),
                    member.getUser().getId());
            apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                    MemberChangeEvent.MemberChange.ADDED));
        } else {
            LOGGER.warn("[{}] '': Orphan member add received, ignored (userid={} username={})",
                    serverId,
                    member.getUser().getId(), member.getUser().getUsername());
        }
    }

    private void handleChannelUpdate(JSONObject data) {
        Channel channel = jsonObjectToObject(data, Channel.class);
        if (channel.isPrivate()) {
            //  TODO
            LOGGER.debug("[0] '': Updated private channel with {}", channel.getRecipient().getUsername());

            return;
        }
        String parentServerId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(parentServerId);
        if (server != NO_SERVER) {
            channel.setParent(server);
            server.getChannels().add(channel);
            LOGGER.debug("[{}] '{}': Channel {} ({}) updated",
                    server.getId(), server.getName(),
                    channel.getName(), channel.getId());
            apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                    ChannelChangeEvent.ChannelChange.UPDATED));
        } else {
            LOGGER.warn("[{}] '': Orphan update channel received, ignored (id={}, name={})",
                    parentServerId,
                    channel.getId(), channel.getName());
        }
    }

    private void handleChannelDelete(JSONObject data) {
        Channel channel = jsonObjectToObject(data, Channel.class);
        if (channel.isPrivate()) {
            apiClient.getPrivateChannels().remove(channel.getId());
            apiClient.getPrivateChannelsByUserMap().remove(channel.getRecipient());
            LOGGER.debug("[0] '': Deleted private channel with {}", channel.getRecipient().getUsername());
            return;
        }
        String parentServerId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(parentServerId);
        if (server != NO_SERVER) {
            channel.setParent(server);
            if (server.getChannels().remove(channel)) {
                LOGGER.debug("[{}] '{}': Channel {} ({}) deleted",
                        server.getId(), server.getName(),
                        channel.getName(), channel.getId());
                apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                        ChannelChangeEvent.ChannelChange.DELETED));
            } else {
                LOGGER.warn("[{}] '{}': Channel {} ({}) could not be deleted (not found)",
                        server.getId(), server.getName(),
                        channel.getName(), channel.getId());
            }
        } else {
            LOGGER.warn("[{}] '': Orphan delete channel received, ignored (id={}, name={})",
                    parentServerId,
                    channel.getId(), channel.getName());
        }
    }

    private void handleChannelCreate(JSONObject data) {
        Channel channel = jsonObjectToObject(data, Channel.class);
        if (channel.isPrivate()) {
            LOGGER.debug("[0] '': New private channel with {}", channel.getRecipient().getUsername());
            apiClient.getPrivateChannels().put(channel.getId(), channel);
            apiClient.getPrivateChannelsByUserMap().put(channel.getRecipient(), channel);
            return;
        }
        String parentServerId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(parentServerId);
        if (server != NO_SERVER) {
            channel.setParent(server);
            server.getChannels().add(channel);
            LOGGER.debug("[{}] '{}': New channel {} ({})",
                    server.getId(), server.getName(),
                    channel.getName(), channel.getId());
            apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                    ChannelChangeEvent.ChannelChange.ADDED));
        } else {
            LOGGER.warn("[{}] '': Orphan create channel received, ignored (id={}, name={})",
                    parentServerId,
                    channel.getId(), channel.getName());
        }
    }

    private void handleReadyMessage(JSONObject data) {
        ReadyMessage readyMessage = jsonObjectToObject(data, ReadyMessage.class);
        startKeepAlive(readyMessage.getHeartbeatInterval());
        LOGGER.info("[0] '': Sending keepAlive every {} ms", readyMessage.getHeartbeatInterval());
        LogInEvent event = new LogInEvent(readyMessage);
        apiClient.onLogInEvent(event);
        apiClient.getEventBus().post(event);
    }

    private void handleGuildCreate(JSONObject data) {
        ReadyServer server = jsonObjectToObject(data, ReadyServer
                .class);
        server.getChannels().forEach(channel -> channel.setParent(server));
        //  Update presences
        if (server.getPresences() != null) {
            Arrays.stream(server.getPresences()).forEach(p -> {
                String id = p.getUser().getId();
                apiClient.getUserPresences().put(id, p.getStatus());
                apiClient.getUserGames().put(id, p.getGame());
            });
        } else {
            LOGGER.warn("No presences received on new server");
        }
        //  need to delete the null named versions
        apiClient.getServers().removeIf(server1 -> server1.getId().equals(server.getId()));
        apiClient.getServers().add(server);
        apiClient.getServerMap().put(server.getId(), server);
        apiClient.requestLargerServerUsers(server);
        LOGGER.info("[{}] '{}': Joined server",
                server.getId(), server.getName());
        apiClient.getEventBus().post(new ServerJoinLeaveEvent(server, true, apiClient));
    }

    private void handleGuildDelete(JSONObject data) {
        Server server = jsonObjectToObject(data, Server.class);
        apiClient.getServers().remove(server);
        Server removed = apiClient.getServerMap().remove(server.getId());
        LOGGER.info("[{}] '{}': Left server",
                removed.getId(), removed.getName());
        apiClient.getEventBus().post(new ServerJoinLeaveEvent(server, false, apiClient));
    }

    @SuppressWarnings("unchecked")
    private void startKeepAlive(long keepAliveInterval) {
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
        keepAliveFuture = apiClient.getExecutorService().scheduleAtFixedRate(() -> {
            JSONObject keepAlive = new JSONObject();
            keepAlive.put("op", 1);
            keepAlive.put("d", System.currentTimeMillis());
            LOGGER.debug("[0] '': Sending keepAlive");
            send(keepAlive.toJSONString());
            statistics.keepAliveCount.increment();
        }, 0, keepAliveInterval, MILLISECONDS);
    }

    private void handleMessageCreate(JSONObject data) {
        Message message = jsonObjectToObject(data, Message.class);
        Channel channel = apiClient.getChannelById(message.getChannelId());
        if (channel == null || channel == NO_CHANNEL || channel.isPrivate()) {
            LOGGER.debug("[0] '': Recieved direct message from {}: {}",
                    message.getAuthor().getUsername(),
                    message.getContent());
            message.setPrivateMessage(true);
        } else {
            LOGGER.debug("[{}] '{}': Recieved message from {} in #{}: {}",
                    channel.getParent().getId(), channel.getParent().getName(),
                    message.getAuthor().getUsername(),
                    channel.getName(),
                    message.getContent());
            message.setPrivateMessage(false);
        }
        apiClient.getEventBus().post(new MessageReceivedEvent(message));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("[0] '': Closing WebSocket {}: '{}' {}", code, reason, remote ? "remote" : "local");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
        statistics.deathCount.increment();
        apiClient.getEventBus().post(new WebSocketCloseEvent(code, reason, remote));
        if (metricReporter != null) {
            metricReporter.stop();
        }
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.warn("[0] '': WebSocket error", ex);
        statistics.errorCount.increment();
    }

    void sendNowPlayingUpdate(String message) {
        org.json.JSONObject data = new org.json.JSONObject();
        if (message == null) {
            data.put("game", org.json.JSONObject.NULL);
        } else {
            org.json.JSONObject game = new org.json.JSONObject();
            game.put("name", message);
            data.put("game", game);
        }
        data.put("idle_since", org.json.JSONObject.NULL);
        org.json.JSONObject outer = new org.json.JSONObject();
        outer.put("op", 3);
        outer.put("d", data);
        String out = outer.toString();
        send(out);
    }

    private <T> T jsonObjectToObject(JSONObject object, Class<T> clazz) {
        //  Because this doesnt come too often and to simplify matters
        //  we'll serialize the object to string and have Gson parse out the object
        //  Eventually come up with a solution that allows for direct creation
        String j = object.toJSONString();
        return gson.fromJson(j, clazz);
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public static class Statistics {
        public final RunningAverage avgMessageHandleTime = new RunningAverage();
        public final LongAdder messageReceiveCount = new LongAdder();
        public final LongAdder keepAliveCount = new LongAdder();
        public final LongAdder errorCount = new LongAdder();
        public final LongAdder deathCount = new LongAdder();
    }
}
