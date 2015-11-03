package co.phoenixlab.discord.api;

import co.phoenixlab.discord.api.entities.ReadyMessage;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DiscordWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    private final DiscordApiClient apiClient;
    private final JSONParser parser;
    private final Gson gson;
    private ScheduledFuture keepAliveFuture;

    public DiscordWebSocketClient(DiscordApiClient apiClient, URI serverUri) {
        super(serverUri);
        this.apiClient = apiClient;
        this.parser = new JSONParser();
        this.gson = new Gson();
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
        LOGGER.debug("Recieved message: {}", message);
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
                //  TODO

                default:
                    LOGGER.warn("Unknown message type {}:\n{}", type, data.toJSONString());
            }
        } catch (ParseException e) {
            LOGGER.warn("Unable to parse message", e);
        }
    }

    private void handleReadyMessage(JSONObject data) {
        ReadyMessage readyMessage = jsonObjectToObject(data, ReadyMessage.class);
        apiClient.setSessionId(readyMessage.getSessionId());
        LOGGER.info("Using sessionId {}", apiClient.getSessionId());
        User user = readyMessage.getUser();
        apiClient.setClientUser(user);
        LOGGER.info("Logged in as {}#{} ID {}", user.getUsername(), user.getDiscriminator(), user.getId());
        startKeepAlive(readyMessage.getHeartbeatInterval());
        LOGGER.info("Sending keepAlive every {} ms", readyMessage.getHeartbeatInterval());
        LOGGER.info("Connected to {} servers", readyMessage.getServers().length);
        LOGGER.info("Holding {} private conversations", readyMessage.getPrivateChannels().length);
        //  We don't bother populating channel messages since we only care about new messages coming in
        List<Server> servers = apiClient.getServers();
        servers.clear();
        Collections.addAll(servers, readyMessage.getServers());
        apiClient.remapServers();
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
        }, 0, keepAliveInterval, TimeUnit.MILLISECONDS);
    }

    private void handleMessageCreate(JSONObject data) {

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
    }

    private <T> T jsonObjectToObject(JSONObject object, Class<T> clazz) {
        //  Because this doesnt come too often and to simplify matters
        //  we'll serialize the object to string and have Gson parse out the object
        //  Eventually come up with a solution that allows for direct creation
        String j = object.toJSONString();
        return gson.fromJson(j, clazz);
    }
}
