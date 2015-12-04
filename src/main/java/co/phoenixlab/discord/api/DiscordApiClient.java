package co.phoenixlab.discord.api;

import co.phoenixlab.discord.api.entities.*;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscordApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    private final ScheduledExecutorService executorService;

    private String email;
    private String password;

    private String token;

    private AtomicReference<String> sessionId;

    private AtomicReference<User> clientUser;

    private DiscordWebSocketClient webSocketClient;

    private List<Server> servers;
    private Map<String, Server> serverMap;

    private final EventBus eventBus;

    private final Gson gson;

    private Map<String, PrivateChannel> privateChannels;
    private Map<User, PrivateChannel> privateChannelsByUser;

    public DiscordApiClient() {
        sessionId = new AtomicReference<>();
        clientUser = new AtomicReference<>();
        executorService = Executors.newScheduledThreadPool(4);
        servers = new ArrayList<>();
        serverMap = new HashMap<>();
        privateChannels = new HashMap<>();
        eventBus = new EventBus((e, c) -> {
            LOGGER.warn("Error while handling event {} when calling {}",
                    c.getEvent(), c.getSubscriberMethod().toGenericString());
            LOGGER.warn("EventBus dispatch exception", e);
        });
        gson = new GsonBuilder().serializeNulls().create();
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

    public void sendMessage(String body, String channelId) {
        sendMessage(body, channelId, new String[0]);
    }

    public void sendMessage(String body, String channelId, String[] mentions) {
        OutboundMessage outboundMessage = new OutboundMessage(body, false, mentions);
        String content = gson.toJson(outboundMessage);

        HttpResponse<JsonNode> response;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.AUTHORIZATION, token);
        try {
            response = Unirest.post(ApiConst.CHANNELS_ENDPOINT + channelId + "/messages").
                    headers(headers).
                    body(content).
                    asJson();
        } catch (UnirestException e) {
            LOGGER.warn("Unable to send message", e);
            return;
        }
        int status = response.getStatus();
        if (status != 200) {
            LOGGER.warn("Unable to send message: HTTP {}: {}", status, response.getStatusText());
            return;
        }
    }

    public void deleteMessage(String messageId, String channelId) throws IOException {
        //  TODO
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

    public User getClientUser() {
        return clientUser.get();
    }

    public void setClientUser(User user) {
        clientUser.set(user);
    }

    public List<Server> getServers() {
        return servers;
    }

    public Map<String, Server> getServerMap() {
        return serverMap;
    }

    public Server getServerByID(String id) {
        return serverMap.get(id);
    }

    public Map<String, PrivateChannel> getPrivateChannels() {
        return privateChannels;
    }

    public Map<User, PrivateChannel> getPrivateChannelsByUserMap() {
        return privateChannelsByUser;
    }

    public PrivateChannel getPrivateChannelById(String id) {
        return privateChannels.get(id);
    }

    public PrivateChannel getPrivateChannelByUser(User user) {
        return privateChannelsByUser.get(user);
    }


    public void remapServers() {
        serverMap.clear();
        serverMap.putAll(servers.stream().collect(Collectors.toMap(Server::getId, Function.identity())));
        servers.forEach(server -> server.getChannels().forEach(channel -> channel.setParent(server)));
    }

    public Channel getChannelById(String id) {
        return servers.stream().
                map(Server::getChannels).
                flatMap(Set::stream).
                filter(c -> id.equals(c.getId())).
                findFirst().orElse(null);
    }

    public User findUser(String username) {
        username = username.toLowerCase();
        for (Server server : servers) {
            User user = findUser(username, server);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    public User findUser(String username, Server server) {
        username = username.toLowerCase();
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        //  No match? Try matching start
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getUsername().toLowerCase().startsWith(username)) {
                return user;
            }
        }
        //  Still no match? Try fuzzy match
        //  TODO

        return null;
    }

    public User getUserById(String userId) {
        for (Server server : servers) {
            User user = getUserById(userId, server);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    public User getUserById(String userId, Server server) {
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public DiscordWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
}
