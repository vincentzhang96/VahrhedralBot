package co.phoenixlab.discord.api;

import co.phoenixlab.discord.VahrhedralBot;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;

public class DiscordApiClient {

    private static final Logger LOGGER = VahrhedralBot.LOGGER;

    private String email;
    private String password;

    private String token;

    public DiscordApiClient() {

    }

    public void logIn(String email, String password) throws IOException {
        //  TODO
        LOGGER.info("Attempting to log in as {}...", email);
        this.email = email;
        this.password = password;

        JSONObject auth = new JSONObject();
        auth.put("email", email);
        auth.put("password", password);

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

    private void openWebSocket() {
        //  TODO
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




}
