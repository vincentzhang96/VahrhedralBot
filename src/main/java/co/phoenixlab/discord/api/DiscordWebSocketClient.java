package co.phoenixlab.discord.api;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class DiscordWebSocketClient extends WebSocketClient {

    private final DiscordApiClient apiClient;

    public DiscordWebSocketClient(DiscordApiClient apiClient, URI serverUri) {
        super(serverUri);
        this.apiClient = apiClient;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}
