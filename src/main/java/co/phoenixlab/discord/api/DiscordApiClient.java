package co.phoenixlab.discord.api;

import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.LogInEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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

    public static final Server NO_SERVER = new Server("NO_SERVER");
    public static final Channel NO_CHANNEL = new Channel("", "NO_CHANNEL");
    public static final User NO_USER = new User("NO_USER", "NO_USER", "NO_USER", null);

    static {
        NO_CHANNEL.setParent(NO_SERVER);
    }

    private final ScheduledExecutorService executorService;

    private String email;
    private String password;

    private String token;

    private final AtomicReference<String> sessionId;

    private final AtomicReference<User> clientUser;

    private DiscordWebSocketClient webSocketClient;

    private final List<Server> servers;
    private final Map<String, Server> serverMap;

    private final EventBus eventBus;

    private final Gson gson;

    private final Map<String, PrivateChannel> privateChannels;
    private final Map<User, PrivateChannel> privateChannelsByUser;

    public DiscordApiClient() {
        sessionId = new AtomicReference<>();
        clientUser = new AtomicReference<>();
        executorService = Executors.newScheduledThreadPool(4);
        servers = new ArrayList<>();
        serverMap = new HashMap<>();
        privateChannels = new HashMap<>();
        privateChannelsByUser = new HashMap<>();
        eventBus = new EventBus((e, c) -> {
            LOGGER.warn("Error while handling event {} when calling {}",
                    c.getEvent(), c.getSubscriberMethod().toGenericString());
            LOGGER.warn("EventBus dispatch exception", e);
        });
        gson = new GsonBuilder().serializeNulls().create();
        eventBus.register(this);
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

    @Subscribe
    public void onLogInEvent(LogInEvent event) {
        ReadyMessage readyMessage = event.getReadyMessage();
        setSessionId(readyMessage.getSessionId());
        LOGGER.info("Using sessionId {}", getSessionId());
        User user = readyMessage.getUser();
        setClientUser(user);
        LOGGER.info("Logged in as {}#{} ID {}", user.getUsername(), user.getDiscriminator(), user.getId());
        LOGGER.info("Connected to {} servers", readyMessage.getServers().length);
        //  We don't bother populating channel messages since we only care about new messages coming in
        servers.clear();
        Collections.addAll(servers, readyMessage.getServers());
        remapServers();

        LOGGER.info("Holding {} private conversations", readyMessage.getPrivateChannels().length);
        for (PrivateChannel privateChannel : readyMessage.getPrivateChannels()) {
            privateChannels.put(privateChannel.getId(), privateChannel);
            privateChannelsByUser.put(privateChannel.getRecipient(), privateChannel);
        }
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
        Server server = serverMap.get(id);
        if (server == null) {
            server = NO_SERVER;
        }
        return server;
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
                findFirst().orElse(NO_CHANNEL);
    }

    public User findUser(String username) {
        username = username.toLowerCase();
        for (Server server : servers) {
            User user = findUser(username, server);
            if (user != null) {
                return user;
            }
        }
        return NO_USER;
    }

    public User findUser(String username, Server server) {
        if (server == null) {
            return findUser(username);
        }
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

        return NO_USER;
    }

    public User getUserById(String userId) {
        for (Server server : servers) {
            User user = getUserById(userId, server);
            if (user != null) {
                return user;
            }
        }
        return NO_USER;
    }

    public User getUserById(String userId, Server server) {
        if (server == null) {
            getUserById(userId);
        }
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        return NO_USER;
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
