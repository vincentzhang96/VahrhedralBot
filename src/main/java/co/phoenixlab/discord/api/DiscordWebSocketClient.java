package co.phoenixlab.discord.api;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class DiscordWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    private final DiscordApiClient apiClient;
    private final JSONParser parser;

    public DiscordWebSocketClient(DiscordApiClient apiClient, URI serverUri) {
        super(serverUri);
        this.apiClient = apiClient;
        this.parser = new JSONParser();
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
                //  TODO
            }
        } catch (ParseException e) {
            LOGGER.warn("Unable to parse message", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("Closing WebSocket {}: {} {}", code, reason, remote ? "remote" : "local");
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.warn("WebSocket error", ex);
    }
}
