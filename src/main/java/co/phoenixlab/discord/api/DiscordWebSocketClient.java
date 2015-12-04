package co.phoenixlab.discord.api;

import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.ReadyMessage;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.event.LogInEvent;
import co.phoenixlab.discord.api.event.MessageReceivedEvent;
import co.phoenixlab.discord.api.event.ServerJoinLeaveEvent;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;

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
                        //  TODO
                        break;
                    case "GUILD_MEMBER_REMOVE":
                        //  TODO
                        break;
                    case "CHANNEL_CREATE":
                        //  TODO
                        break;
                    case "CHANNEL_DELETE":
                        //  TODO
                        break;
                    case "PRESENCE_UPDATE":
                        //  TODO
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
