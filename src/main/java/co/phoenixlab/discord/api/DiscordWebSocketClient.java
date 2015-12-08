package co.phoenixlab.discord.api;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.*;
import co.phoenixlab.discord.stats.RunningAverage;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;

import static co.phoenixlab.discord.api.DiscordApiClient.NO_MEMBER;
import static co.phoenixlab.discord.api.DiscordApiClient.NO_SERVER;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class DiscordWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    private final DiscordApiClient apiClient;
    private final JSONParser parser;
    private final Gson gson;
    private ScheduledFuture keepAliveFuture;
    private final Statistics statistics;

    public DiscordWebSocketClient(DiscordApiClient apiClient, URI serverUri) {
        super(serverUri);
        this.apiClient = apiClient;
        this.parser = new JSONParser();
        this.gson = new Gson();
        statistics = new Statistics();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Thread.currentThread().setName("WebSocketClient");
        LOGGER.info("WebSocket connection opened");
        send("{\"op\":2,\"d\":{\"token\":\"" + apiClient.getToken() + "\",\"properties\":{\"$os\":\"Linux\",\"" +
                "$browser\":\"DesuBot\",\"$device\":\"DesuBot\",\"$referrer\":\"\",\"$referring_domain\"" +
                ":\"\"},\"v\":2}}");
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
                        LOGGER.warn("Discord returned an unknown error");
                    } else {
                        LOGGER.warn("Discord returned error: {}", errorMessage);
                    }
                    return;
                }
                String type = (String) msg.get("t");
                JSONObject data = (JSONObject) msg.get("d");
                switch (type) {
                    case "READY":
                        handleReadyMessage(data);
                        break;
                    case "MESSAGE_CREATE":
                        handleMessageCreate(data);
                        break;
                    case "MESSAGE_UPDATE":
                        //  Don't care
                        break;
                    case "MESSAGE_DELETE":
                        //  Don't care
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
                    case "PRESENCE_UPDATE":
                        handlePresenceUpdate(data);
                        break;
                    case "VOICE_STATE_UPDATE":
                        //  TODO
                        break;
                    //  TODO
                    default:
                        LOGGER.warn("Unknown message type {}:\n{}", type, data.toJSONString());
                }
            } catch (ParseException e) {
                LOGGER.warn("Unable to parse message", e);
            }
        } finally {
            statistics.avgMessageHandleTime.add(MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS));
        }
    }

    private void handleGuildRoleUpdate(JSONObject data) {
        Role role = jsonObjectToObject((JSONObject) data.get("role"), Role.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            //  Literally just shove it in because Set
            server.getRoles().remove(role);
            server.getRoles().add(role);
            LOGGER.debug("Updated role {} ({}) to {} ({})",
                    role.getName(), role.getId(), server.getName(), server.getId());
            apiClient.getEventBus().post(new RoleChangeEvent(role, server,
                    RoleChangeEvent.RoleChange.UPDATED));
        } else {
            LOGGER.warn("Orphan role update received, ignored (roleid={} rolename={} serverid={}",
                    role.getId(), role.getName(), serverId);
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
                LOGGER.debug("Deleted role {} ({}) to {} ({})",
                        removed.getName(), removed.getId(), server.getName(), server.getId());
                apiClient.getEventBus().post(new RoleChangeEvent(removed, server,
                        RoleChangeEvent.RoleChange.DELETED));
            } else {
                LOGGER.warn("No such role to delete (roleid={} serverid={})",
                        roleId, serverId);
            }
        } else {
            LOGGER.warn("Orphan role delete received, ignored (roleid={} serverid={})",
                    roleId, serverId);
        }
    }

    private void handleGuildRoleCreate(JSONObject data) {
        Role role = jsonObjectToObject((JSONObject) data.get("role"), Role.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            server.getRoles().add(role);
            LOGGER.debug("Added new role {} ({}) to {} ({})",
                    role.getName(), role.getId(), server.getName(), server.getId());
            apiClient.getEventBus().post(new RoleChangeEvent(role, server,
                    RoleChangeEvent.RoleChange.CREATED));
        } else {
            LOGGER.warn("Orphan role create received, ignored (roleid={} rolename={} serverid={}",
                    role.getId(), role.getName(), serverId);
        }
    }

    private void handlePresenceUpdate(JSONObject data) {
        PresenceUpdate update = jsonObjectToObject(data, PresenceUpdate.class);
        Server server = apiClient.getServerByID(update.getServerId());
        if (server != NO_SERVER) {
            User updateUser = update.getUser();
            User user = apiClient.getUserById(updateUser.getId(), server);
            SafeNav.of(updateUser.getAvatar()).ifPresent(user::setAvatar);
            SafeNav.of(updateUser.getUsername()).ifPresent(user::setUsername);
            Member member = apiClient.getUserMember(user, server);
            if (member != NO_MEMBER && member.getUser().equals(user)) {
                member.getRoles().clear();
                member.getRoles().addAll(update.getRoles());
                LOGGER.debug("{}'s ({}) presence changed in {} ({})",
                        user.getUsername(), user.getId(),
                        server.getName(), server.getId());
                apiClient.getEventBus().post(new PresenceUpdateEvent(update, server));
            } else {
                LOGGER.info("Orphan presence update received, ignored (userid={} username={} serverid={}): Not found",
                        update.getUser().getId(), update.getUser().getUsername(), update.getServerId());
            }
        } else {
            LOGGER.info("Orphan presence update received, ignored (userid={} username={} serverid={})",
                    update.getUser().getId(), update.getUser().getUsername(), update.getServerId());
        }
    }

    private void handleGuildMemberUpdate(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            server.getMembers().remove(member);
            server.getMembers().add(member);
            LOGGER.debug("Updated {}'s ({}) membership in {} ({})",
                    member.getUser().getUsername(), member.getUser().getId(),
                    server.getName(), server.getId());
            apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                    MemberChangeEvent.MemberChange.UPDATED));
        } else {
            LOGGER.info("Orphan member update received, ignored (userid={} username={} serverid={}",
                    member.getUser().getId(), member.getUser().getUsername(), serverId);
        }
    }

    private void handleGuildMemberRemove(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            if (server.getMembers().remove(member)) {
                LOGGER.debug("Removed {}'s ({}) membership in {} ({})",
                        member.getUser().getUsername(), member.getUser().getId(),
                        server.getName(), server.getId());
                apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                        MemberChangeEvent.MemberChange.DELETED));
            } else {
                LOGGER.warn("Member {} ({}) could not be removed from {} ({}): Not found",
                        member.getUser().getId(), member.getUser().getUsername(),
                        server.getName(), server.getId());
            }
        } else {
            LOGGER.warn("Orphan member remove received, ignored (userid={} username={} serverid={}",
                    member.getUser().getId(), member.getUser().getUsername(), serverId);
        }
    }

    private void handleGuildMemberAdd(JSONObject data) {
        Member member = jsonObjectToObject(data, Member.class);
        String serverId = (String) data.get("guild_id");
        Server server = apiClient.getServerByID(serverId);
        if (server != NO_SERVER) {
            server.getMembers().add(member);
            LOGGER.info("Added {}'s ({}) membership in {} ({})",
                    member.getUser().getUsername(), member.getUser().getId(),
                    server.getName(), server.getId());
            apiClient.getEventBus().post(new MemberChangeEvent(member, server,
                    MemberChangeEvent.MemberChange.ADDED));
        } else {
            LOGGER.warn("Orphan member add received, ignored (userid={} username={} serverid={}",
                    member.getUser().getId(), member.getUser().getUsername(), serverId);
        }
    }

    private void handleChannelUpdate(JSONObject data) {
        if (Boolean.TRUE.equals(data.get("is_private"))) {
            PrivateChannel channel = jsonObjectToObject(data, PrivateChannel.class);
            //  TODO
            LOGGER.info("Updated private channel with {}", channel.getRecipient().getUsername());
        } else {
            Channel channel = jsonObjectToObject(data, Channel.class);
            String parentServerId = (String) data.get("guild_id");
            Server server = apiClient.getServerByID(parentServerId);
            if (server != NO_SERVER) {
                channel.setParent(server);
                server.getChannels().add(channel);
                LOGGER.debug("Channel {} ({}) in server {} ({}) updated",
                        channel.getName(), channel.getId(), server.getName(), server.getId());
                apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                        ChannelChangeEvent.ChannelChange.UPDATED));
            } else {
                LOGGER.warn("Orphan update channel received, ignored (id={}, name={}, parentServerId={}",
                        channel.getId(), channel.getName(), parentServerId);
            }
        }
    }

    private void handleChannelDelete(JSONObject data) {
        if (Boolean.TRUE.equals(data.get("is_private"))) {
            PrivateChannel channel = jsonObjectToObject(data, PrivateChannel.class);
            //  TODO
            LOGGER.info("Delete private channel with {}", channel.getRecipient().getUsername());
        } else {
            Channel channel = jsonObjectToObject(data, Channel.class);
            String parentServerId = (String) data.get("guild_id");
            Server server = apiClient.getServerByID(parentServerId);
            if (server != NO_SERVER) {
                channel.setParent(server);
                if (server.getChannels().remove(channel)) {
                    LOGGER.debug("Channel {} ({}) in server {} ({}) deleted",
                            channel.getName(), channel.getId(), server.getName(), server.getId());
                    apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                            ChannelChangeEvent.ChannelChange.DELETED));
                } else {
                    LOGGER.warn("Channel {} ({}) in server {} ({}) could not be deleted (not found)",
                            channel.getName(), channel.getId(), server.getName(), server.getId());
                }
            } else {
                LOGGER.warn("Orphan delete channel received, ignored (id={}, name={}, parentServerId={}",
                        channel.getId(), channel.getName(), parentServerId);
            }
        }
    }

    private void handleChannelCreate(JSONObject data) {
        if (Boolean.TRUE.equals(data.get("is_private"))) {
            PrivateChannel channel = jsonObjectToObject(data, PrivateChannel.class);
            //  TODO
            LOGGER.info("New private channel with {}", channel.getRecipient().getUsername());
        } else {
            Channel channel = jsonObjectToObject(data, Channel.class);
            String parentServerId = (String) data.get("guild_id");
            Server server = apiClient.getServerByID(parentServerId);
            if (server != NO_SERVER) {
                channel.setParent(server);
                server.getChannels().add(channel);
                LOGGER.debug("New channel {} ({}) in server {} ({}) added",
                        channel.getName(), channel.getId(), server.getName(), server.getId());
                apiClient.getEventBus().post(new ChannelChangeEvent(channel,
                        ChannelChangeEvent.ChannelChange.ADDED));
            } else {
                LOGGER.warn("Orphan create channel received, ignored (id={}, name={}, parentServerId={}",
                        channel.getId(), channel.getName(), parentServerId);
            }
        }
    }

    private void handleReadyMessage(JSONObject data) {
        ReadyMessage readyMessage = jsonObjectToObject(data, ReadyMessage.class);
        startKeepAlive(readyMessage.getHeartbeatInterval());
        LOGGER.info("Sending keepAlive every {} ms", readyMessage.getHeartbeatInterval());
        apiClient.getEventBus().post(new LogInEvent(readyMessage));
    }

    private void handleGuildCreate(JSONObject data) {
        Server server = jsonObjectToObject(data, Server.class);
        server.getChannels().forEach(channel -> channel.setParent(server));
        apiClient.getServers().add(server);
        apiClient.getServerMap().put(server.getId(), server);
        LOGGER.info("Added to server {}", server.getName());
        apiClient.getEventBus().post(new ServerJoinLeaveEvent(server, true));
    }

    private void handleGuildDelete(JSONObject data) {
        Server server = jsonObjectToObject(data, Server.class);
        apiClient.getServers().remove(server);
        apiClient.getServerMap().remove(server.getId());
        LOGGER.info("Left server {}", server.getName());
        apiClient.getEventBus().post(new ServerJoinLeaveEvent(server, false));
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
            LOGGER.debug("Sending keepAlive");
            send(keepAlive.toJSONString());
            statistics.keepAliveCount.increment();
        }, 0, keepAliveInterval, MILLISECONDS);
    }

    private void handleMessageCreate(JSONObject data) {
        Message message = jsonObjectToObject(data, Message.class);
        //  Ignore messages from self
        if (!message.getAuthor().equals(apiClient.getClientUser())) {
            Channel channel = apiClient.getChannelById(message.getChannelId());
            if (channel == null) {
                LOGGER.debug("Recieved direct message from {}: {}",
                        message.getAuthor().getUsername(),
                        message.getContent());
            } else {
                LOGGER.debug("Recieved message from {} in #{}: {}",
                        message.getAuthor().getUsername(),
                        channel.getName(),
                        message.getContent());
            }
            apiClient.getEventBus().post(new MessageReceivedEvent(message));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Closing WebSocket {}: {} {}", code, reason, remote ? "remote" : "local");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.warn("WebSocket error", ex);
        statistics.errorCount.increment();
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
    }
}
