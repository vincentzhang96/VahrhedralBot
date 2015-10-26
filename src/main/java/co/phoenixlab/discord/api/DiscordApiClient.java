package co.phoenixlab.discord.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    private String email;
    private String password;

    private String token;

    private AtomicReference<String> sessionId;

    private DiscordWebSocketClient webSocketClient;

    public DiscordApiClient() {

    }

    public void logIn(String email, String password) throws IOException {
        LOGGER.info("Attempting to log in as {}...", email);
        this.email = email;
        this.password = password;

        Map<String, String> authObj = new HashMap<>();
        authObj.put("email", email);
        authObj.put("password", password);
        JSONObject auth = new JSONObject(authObj);
        HttpResponse<JsonNode> response;
        try {
             response = Unirest.post(ApiConst.LOGIN_ENDPOINT).
                    header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()).
                    body(auth.toJSONString()).
                    asJson();
        } catch (UnirestException e) {
            throw new IOException("Unable to log in", e);
        }
        int status = response.getStatus();

        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                //  Servers return FORBIDDEN for bad credentials
                LOGGER.warn("Unable to log in with given credentials: ", response.getStatusText());
            } else {
                LOGGER.warn("Unable to log in, Discord may be having issues");
            }
            throw new IOException("Unable to log in: HTTP " + response.getStatus() + ": " + response.getStatusText());
        }
        token = response.getBody().getObject().getString("token");
        LOGGER.info("Successfully logged in, token is {}", token);
        openWebSocket();
    }

    private void openWebSocket() throws IOException {
        final String gateway = getWebSocketGateway();
        try {
            webSocketClient = new DiscordWebSocketClient(this, new URI(gateway));
        } catch (URISyntaxException e) {
            LOGGER.warn("Bad gateway", e);
            throw new IOException(e);
        }
        webSocketClient.connect();

    }

    private String getWebSocketGateway() throws IOException {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.get(ApiConst.WEBSOCKET_GATEWAY).
                    header("authorization", token).
                    asJson();
        } catch (UnirestException e) {
            throw new IOException("Unable to retrieve websocket gateway", e);
        }
        int status = response.getStatus();
        if (status != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Unable to retrieve websocket gateway: HTTP {}: {}", status, response.getStatusText());
            throw new IOException("Unable to retrieve websocket : HTTP " + status + ": " + response.getStatusText());
        }
        String gateway = response.getBody().getObject().getString("url");
        gateway = "w" + gateway.substring(2);
        LOGGER.info("Found WebSocket gateway at {}", gateway);
        return gateway;
    }

    public void sendMessage(String body, String channelId) throws IOException {
        //  TODO
    }

    public void deleteMessage(String messageId, String channelId) throws IOException {
        //  TODO
    }

    public boolean isOpen() {
        //  TODO
        return false;
    }

    public String getToken() {
        return token;
    }

    public String getSessionId() {
        return sessionId.get();
    }

    public void setSessionId(String sessionId) {
        this.sessionId.set(sessionId);
    }
}
