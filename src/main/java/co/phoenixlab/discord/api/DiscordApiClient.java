package co.phoenixlab.discord.api;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.LogInEvent;
import co.phoenixlab.discord.api.event.UserUpdateEvent;
import co.phoenixlab.discord.api.event.WebSocketCloseEvent;
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
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DiscordApiClient {

    public static final Server NO_SERVER = new Server("NO_SERVER");
    public static final Channel NO_CHANNEL = new Channel("", "NO_CHANNEL");
    public static final User NO_USER = new User("NO_USER", "NO_USER", "NO_USER", null);
    public static final Member NO_MEMBER = new Member(NO_USER, Collections.emptyList(), "");
    public static final Role NO_ROLE = new Role(0, 0, "NO_ROLE", false, "", false, 0);
    public static final String[] EMPTY_STR_ARRAY = new String[0];
    public static final Pattern USER_ID_REGEX = Pattern.compile("[0-9]+");
    private static final Logger LOGGER = LoggerFactory.getLogger("DiscordApiClient");

    static {
        NO_CHANNEL.setParent(NO_SERVER);
    }

    private final ScheduledExecutorService executorService;
    private final AtomicReference<String> sessionId;
    private final AtomicReference<User> clientUser;
    private final List<Server> servers;
    private final Map<String, Server> serverMap;
    private final EventBus eventBus;
    private final Gson gson;
    private final Map<String, Channel> privateChannels;
    private final Map<User, Channel> privateChannelsByUser;
    private final AtomicBoolean active;
    private final Statistics statistics;
    private final Map<String, Presence> userPresences;
    private final Map<String, String> userGames;
    private String email;
    private String password;
    private String token;
    private DiscordWebSocketClient webSocketClient;

    public DiscordApiClient() {
        statistics = new Statistics();
        sessionId = new AtomicReference<>();
        clientUser = new AtomicReference<>();
        executorService = Executors.newScheduledThreadPool(4);
        servers = new ArrayList<>();
        serverMap = new HashMap<>();
        privateChannels = new HashMap<>();
        privateChannelsByUser = new HashMap<>();
        userPresences = new HashMap<>();
        userGames = new HashMap<>();
        active = new AtomicBoolean();
        eventBus = new EventBus((e, c) -> {
            LOGGER.warn("Error while handling event {} when calling {}",
                    c.getEvent(), c.getSubscriberMethod().toGenericString());
            LOGGER.warn("EventBus dispatch exception", e);
            statistics.eventDispatchErrorCount.increment();
        });
        gson = new GsonBuilder().serializeNulls().create();
        eventBus.register(this);
    }

    @Subscribe
    public void countEvent(Object object) {
        statistics.eventCount.increment();
    }

    public void logIn(String token) throws IOException {
        this.token = token;
        LOGGER.info("Using provided token {}", token);
        openWebSocket();
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
            statistics.restErrorCount.increment();
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
            statistics.restErrorCount.increment();
            throw new IOException("Unable to log in: HTTP " + response.getStatus() + ": " + response.getStatusText());
        }
        token = response.getBody().getObject().getString("token");
        LOGGER.info("Successfully logged in, token is {}", token);
        openWebSocket();
    }

    private void openWebSocket() throws IOException {
        try {
            int retryTimeSec = 0;
            do {
                try {
                    final String gateway = getWebSocketGateway();
                    final URI gatewayUri = new URI(gateway);
                    webSocketClient = new DiscordWebSocketClient(this, gatewayUri);
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, null, null);
                    webSocketClient.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(context));
                } catch (URISyntaxException e) {
                    LOGGER.warn("Bad gateway", e);
                    throw new IOException(e);
                } catch (Exception e) {
                    LOGGER.warn("Unexpected error", e);
                    throw new IOException(e);
                }
                statistics.connectAttemptCount.increment();
                active.set(webSocketClient.connectBlocking());
                if (!active.get()) {
                    LOGGER.warn("Unable to connect, retrying in {}s...", retryTimeSec);
                    Thread.sleep(MILLISECONDS.convert(retryTimeSec, SECONDS));
                    retryTimeSec = Math.min(retryTimeSec + 2, 30);  //  Cap at 30 interval
                }
            } while (!active.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getWebSocketGateway() throws IOException {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.get(ApiConst.WEBSOCKET_GATEWAY).
                    header("authorization", token).
                    asJson();
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            throw new IOException("Unable to retrieve websocket gateway", e);
        }
        int status = response.getStatus();
        if (status != HttpURLConnection.HTTP_OK) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to retrieve websocket gateway: HTTP {}: {}", status, response.getStatusText());
            throw new IOException("Unable to retrieve websocket : HTTP " + status + ": " + response.getStatusText());
        }
        String gateway = response.getBody().getObject().getString("url");
        LOGGER.info("Found WebSocket gateway at {}", gateway);
        return gateway;
    }

    void onLogInEvent(LogInEvent event) {
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
        //  Update presences
        Arrays.stream(readyMessage.getServers()).
                filter(Server::isAvailable).
                map(ReadyServer::getPresences).
                flatMap(Arrays::stream).
                forEach(p -> {
                    String id = p.getUser().getId();
                    userPresences.put(id, p.getStatus());
                    userGames.put(id, SafeNav.of(p.getGame()).next(Game::getName).orElse("[misc.nothing]"));
                });
        //  Request to populate the large servers
        servers.stream().filter(Server::isLarge).
                forEach(this::requestLargerServerUsers);

        LOGGER.info("Holding {} private conversations", readyMessage.getPrivateChannels().length);
        for (Channel privateChannel : readyMessage.getPrivateChannels()) {
            privateChannels.put(privateChannel.getId(), privateChannel);
            privateChannelsByUser.put(privateChannel.getRecipient(), privateChannel);
        }
    }

    private void requestLargerServerUsers(Server server) {
        org.json.JSONObject object = new org.json.JSONObject();
        object.put("op", 8);
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("guild_id", server.getId());
        requestBody.put("query", "");
        requestBody.put("limit", 0);
        object.put("d", requestBody);
        webSocketClient.send(object.toString());
    }

    @Subscribe
    public void onUserUpdate(UserUpdateEvent event) {
        UserUpdate update = event.getUpdate();
        User oldUser = clientUser.get();
        if (!oldUser.getId().equals(update.getId())) {
            throw new IllegalStateException("User ID should not be able to be updated!");
        }
        String oldEmail = email;
        email = update.getEmail();
        User newUser = new User(update.getUsername(), update.getId(),
                update.getDiscriminator(), update.getAvatar());
        clientUser.set(newUser);
        if (!oldUser.getUsername().equals(update.getUsername())) {
            LOGGER.info("Username changed from '{}' to '{}'", oldUser.getUsername(), update.getUsername());
        }
        if (!oldUser.getDiscriminator().equals(update.getDiscriminator())) {
            LOGGER.info("Discriminator changed from '{}' to '{}'", oldUser.getDiscriminator(), update.getDiscriminator());
        }
        if (!oldUser.getAvatar().equals(update.getAvatar())) {
            LOGGER.info("Avatar changed from '{}' to '{}'", oldUser.getAvatar(), update.getAvatar());
        }
        if (!oldEmail.equals(email)) {
            LOGGER.info("Email changed from '{}' to '{}'", oldEmail, email);
        }
        //  Fire off a change to ourself across all servers as well
        for (Server server : servers) {
            server.getMembers().stream().
                    map(Member::getUser).
                    filter(u -> u.equals(newUser)).
                    findFirst().ifPresent(u -> {
                u.setAvatar(update.getAvatar());
                u.setUsername(update.getUsername());
                u.setDiscriminator(update.getDiscriminator());
            });
        }
    }

    public Future<Message> sendMessage(String body, String channelId) {
        return sendMessage(body, channelId, EMPTY_STR_ARRAY, true);
    }

    public Future<Message> sendMessage(String body, Channel channel) {
        return sendMessage(body, channel, EMPTY_STR_ARRAY, true);
    }

    public Future<Message> sendMessage(String body, String channelId, boolean async) {
        return sendMessage(body, channelId, EMPTY_STR_ARRAY, async);
    }

    public Future<Message> sendMessage(String body, Channel channel, boolean async) {
        return sendMessage(body, channel, EMPTY_STR_ARRAY, async);
    }

    public Future<Message> sendMessage(String body, Channel channel, String[] mentions) {
        return sendMessage(body, channel.getId(), mentions);
    }

    public Future<Message> sendMessage(String body, String channelId, String[] mentions) {
        return sendMessage(body, channelId, mentions, true);
    }

    public Future<Message> sendMessage(String body, Channel channel, String[] mentions, boolean async) {
        return sendMessage(body, channel.getId(), mentions, async);
    }

    public Future<Message> sendMessage(String body, String channelId, String[] mentions, boolean async) {
        if (body == null || channelId == null || mentions == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }
        Future<Message> future = executorService.submit(() -> sendMessageInternal(body, channelId, mentions));
        if (!async) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.warn("Exception while waiting for sendMessage result", e);
            }
        }
        return future;
    }

    Message sendMessageInternal(String body, String channelId, String[] mentions) {
        Gson g = new GsonBuilder().serializeNulls().create();
        //  April fools.
//        String[] splitBody = body.split("\n");
//        StringJoiner joiner = new StringJoiner("\n");
//        for (String s : splitBody) {
//            joiner.add(reverseLine(s));
//        }
//        body = joiner.toString();
        OutboundMessage outboundMessage = new OutboundMessage(body, false, mentions);
        String content = g.toJson(outboundMessage);

        HttpResponse<String> response;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.AUTHORIZATION, token);
        try {
            response = Unirest.post(ApiConst.CHANNELS_ENDPOINT + channelId + "/messages").
                    headers(headers).
                    body(content).
                    asString();
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to send message", e);
            return null;
        }
        int status = response.getStatus();
        if (status != 200) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to send message: HTTP {}: {}", status, response.getStatusText());
            return null;
        }
        return g.fromJson(response.getBody(), Message.class);
    }

    private String reverseLine(String s) {
        if (s.startsWith("NOFLIP")) {
            return s.substring("NOFLIP".length());
        }
        String[] tokens = s.split(" ");
        StringJoiner joiner = new StringJoiner(" ");
        String[] outArray = new String[tokens.length];
        for (int i = 0, tokensLength = tokens.length; i < tokensLength; i++) {
            String token = tokens[i];
            if (!token.startsWith("http://") && !token.startsWith("https://")) {
                token = reverse(token);
            }
            outArray[outArray.length - i - 1] = token;
        }
        for(String s1 : outArray) {
            joiner.add(s1);
        }
        return joiner.toString();
    }

    private String reverse(String s) {
        char[] chars = s.toCharArray();
        char c;
        int length = chars.length;
        for (int i = 0; i < length / 2; i++) {
            c = chars[length - i - 1];
            chars[length - i - 1] = flip(chars[i]);
            chars[i] = flip(c);
        }
        return new String(chars);
    }

    private char flip(char c) {
        switch (c) {
            case ')':
                return '(';
            case '(':
                return ')';
            case '[':
                return ']';
            case ']':
                return '[';
            case '{':
                return '}';
            case '}':
                return '{';
            default:
                return c;
        }
    }

    public void deleteMessage(String channelId, String messageId) {
        deleteMessage(messageId, channelId, true);
    }

    public void deleteMessage(String channelId, String messageId, boolean async) {
        if (channelId == null || messageId == null) {
            return;
        }

        if (async) {
            executorService.submit(() -> deleteMessage(messageId, channelId, false));
            return;
        }

        HttpResponse<String> response;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, token);
        try {
            response = Unirest.delete(ApiConst.CHANNELS_ENDPOINT + channelId + "/messages/" + messageId).
                    headers(headers).
                    asString();
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to delete message", e);
            return;
        }
        int status = response.getStatus();
        if (status != 204) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to delete message: HTTP {}: {}", status, response.getStatusText());
            return;
        }
    }

    public void editMessage(String channelId, String messageId, String content) {
        editMessage(channelId, messageId, content, null);
    }

    public void editMessage(String channelId, String messageId, String content, boolean async) {
        editMessage(channelId, messageId, content, null, async);
    }

    public void editMessage(String channelId, String messageId, String content, String[] mentions) {
        editMessage(channelId, messageId, content, mentions, true);
    }

    public void editMessage(String channelId, String messageId, String content, String[] mentions, boolean async) {
        if (channelId == null || messageId == null || content == null) {
            return;
        }
        if (async) {
            executorService.submit(() -> editMessage(channelId, messageId, content, mentions, false));
            return;
        }
        //  TODO Add support for mention changes
        Map<String, Object> ret = new HashMap<>();
        ret.put("content", content);
        JSONObject object = new JSONObject(ret);
        HttpResponse<JsonNode> response;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.AUTHORIZATION, token);
        try {
            response = Unirest.patch(ApiConst.CHANNELS_ENDPOINT + channelId + "/messages/" + messageId).
                    headers(headers).
                    body(object.toJSONString()).
                    asJson();
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to edit message", e);
            return;
        }
        int status = response.getStatus();
        if (status != 200) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to edit message: HTTP {}: {}", status, response.getStatusText());
            return;
        }
    }

//    public Future<Message[]> getChannelHistory(String channelId, String before, int limit) {
//
//    }

//    Message[] getChannelHistoryInternal(String channelId, String before, int limit) {
//        try {
//            Map<String, String> headers = new HashMap<>();
//            headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
//            headers.put(HttpHeaders.AUTHORIZATION, token);
//            Map<String, Object> queryParams = new HashMap<>();
//            queryParams.put("limit", Math.max(1, Math.min(limit, 50)));
//            if (before != null) {
//                queryParams.put("before", before);
//            }
//            HttpResponse<JsonNode> response = Unirest.get(ApiConst.CHANNELS_ENDPOINT +
//                    channelId + "/messages").
//                    headers(headers).
//                    queryString(queryParams).
//                    asJson();
//            int status = response.getStatus();
//            if (status != 200) {
//                statistics.restErrorCount.increment();
//                LOGGER.warn("Unable to retrieve message: HTTP {}: {}", status, response.getStatusText());
//                return null;
//            }
//            JSONArray ret = response.getBody().getArray();
//
//
//        } catch (UnirestException e) {
//            statistics.restErrorCount.increment();
//            LOGGER.warn("Unable to retrieve message");
//            return null;
//        }
//    }

    public void updateNowPlaying(String message) {
        webSocketClient.sendNowPlayingUpdate(message);
    }

    public void updateRolesByObj(User user, Server server, Collection<Role> roles) {
        updateRolesByObj(user, server, roles, true);
    }

    public void updateRolesByObj(User user, Server server, Collection<Role> roles, boolean async) {
        List<String> r = roles.stream().map(Role::getId).collect(Collectors.toList());
        updateRoles(user, server, r, async);
    }

    public void updateRoles(User user, Server server, Collection<String> roles) {
        updateRoles(user, server, roles, true);
    }

    public void updateRoles(User user, Server server, Collection<String> roles, boolean async) {
        if (async) {
            executorService.submit(() -> updateRoles(user, server, roles, false));
            return;
        }
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            headers.put(HttpHeaders.AUTHORIZATION, token);
            org.json.JSONObject object = new org.json.JSONObject();
            object.put("roles", roles);
            HttpResponse<JsonNode> r = Unirest.patch(ApiConst.SERVERS_ENDPOINT + server.getId() + "/members/" +
                    user.getId()).
                    headers(headers).
                    body(object.toString()).
                    asJson();
            int status = r.getStatus();
            if (status < 200 || status >= 300) {
                statistics.restErrorCount.increment();
                LOGGER.warn("Unable to update member {} ({}) roles in {} ({}): HTTP {}: {}",
                        user.getUsername(), user.getId(),
                        server.getName(), server.getId(),
                        status, r.getStatusText());
                return;
            }
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            LOGGER.warn("Unable to update member {} ({}) roles in {} ({})",
                    user.getUsername(), user.getId(),
                    server.getName(), server.getId());
            LOGGER.warn("Unable to update member roles", e);
            return;
        }
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

    public Map<String, Channel> getPrivateChannels() {
        return privateChannels;
    }

    public Map<User, Channel> getPrivateChannelsByUserMap() {
        return privateChannelsByUser;
    }

    public Channel getPrivateChannelById(String id) {
        return privateChannels.get(id);
    }

    public Channel getPrivateChannelByUser(User user) {
        return privateChannelsByUser.get(user);
    }


    public void remapServers() {
        serverMap.clear();
        serverMap.putAll(servers.stream().collect(Collectors.toMap(Server::getId, Function.identity())));
        servers.stream().
                filter(Server::isAvailable).
                forEach(server -> server.getChannels().forEach(channel -> channel.setParent(server)));
    }

    public Channel getChannelById(String id) {
        if (id == null) {
            return NO_CHANNEL;
        }
        return servers.stream().
                filter(Server::isAvailable).
                map(Server::getChannels).
                flatMap(Set::stream).
                filter(c -> id.equals(c.getId())).
                findFirst().orElse(getPrivateChannelByIdAsChannel(id));
    }

    public Channel getPrivateChannelByIdAsChannel(String id) {
        Channel privateChannel = getPrivateChannelById(id);
        if (privateChannel != null) {
            return privateChannel;
        }
        return NO_CHANNEL;
    }

    public Channel getChannelById(String id, Server server) {
        if (id == null) {
            return NO_CHANNEL;
        }
        if (server == null || server == NO_SERVER) {
            return getChannelById(id);
        }
        return server.getChannels().stream().
                filter(c -> id.equals(c.getId())).
                findFirst().orElse(NO_CHANNEL);
    }

    public User findUser(String username) {
        if (username == null) {
            return NO_USER;
        }
        username = username.toLowerCase();
        for (Server server : servers) {
            User user = findUser(username, server);
            if (user != NO_USER) {
                return user;
            }
        }
        return NO_USER;
    }

    public User findUser(String username, Server server) {
        if (username == null) {
            return NO_USER;
        }
        if (server == null || server == NO_SERVER) {
            return findUser(username);
        }
        username = username.toLowerCase();
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (username.equalsIgnoreCase(user.getUsername())) {
                return user;
            }
        }
        User temp = null;
        //  No match? Try matching start
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getUsername().toLowerCase().startsWith(username)) {
                if (temp == null || user.getUsername().length() <= temp.getUsername().length()) {
                    temp = user;
                }
            }
        }
        if (temp != null) {
            return temp;
        }
        //  ID match
        if (USER_ID_REGEX.matcher(username).matches()) {
            temp = getUserById(username, server);
            if (temp != null) {
                return temp;
            }
        }

        //  Still no match? Try fuzzy match
        //  TODO

        return NO_USER;
    }

    public User getUserById(String userId) {
        if (userId == null) {
            return NO_USER;
        }
        for (Server server : servers) {
            User user = getUserById(userId, server);
            if (user != NO_USER) {
                return user;
            }
        }
        return NO_USER;
    }

    public User getUserById(String userId, Server server) {
        if (userId == null) {
            return NO_USER;
        }
        if (server == null || server == NO_SERVER) {
            return getUserById(userId);
        }
        if (server.getMembers() == null) {
            LOGGER.warn("Server {} {} has null member list", server.getId(), server.getName());
            return NO_USER;
        }
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        //  Try API endpoint
        try {
            Member member = getMemberHttp(server.getId(), userId);
            if (member != NO_MEMBER) {
                server.getMembers().add(member);
                return member.getUser();
            } else {
                LOGGER.warn("User {} not found via HTTP", userId);
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get user " + userId + " via HTTP", e);
        }
        return NO_USER;
    }

    public Member getUserMember(User user, Server server) {
        if (user == null) {
            return NO_MEMBER;
        }
        return getUserMember(user.getId(), server);
    }

    public Member getUserMember(String userId, Server server) {
        if (userId == null || server == null || server == NO_SERVER) {
            return NO_MEMBER;
        }
        for (Member member : server.getMembers()) {
            User user = member.getUser();
            if (user.getId().equals(userId)) {
                return member;
            }
        }
        //  Try API endpoint
        try {
            Member member = getMemberHttp(server.getId(), userId);
            if (member != NO_MEMBER) {
                server.getMembers().add(member);
                return member;
            } else {
                LOGGER.warn("User {} not found via HTTP", userId);
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get user " + userId + " via HTTP", e);
        }
        return NO_MEMBER;
    }

    public Role getRole(String roleId, Server server) {
        if (roleId == null || server == null || server == NO_SERVER) {
            return NO_ROLE;
        }
        for (Role role : server.getRoles()) {
            if (roleId.equals(role.getId())) {
                return role;
            }
        }
        return NO_ROLE;
    }

    public Role findRole(String roleName, Server server) {
        if (roleName == null || server == null || server == NO_SERVER) {
            return NO_ROLE;
        }
        //  Exact (case insensitive) match
        for (Role role : server.getRoles()) {
            if (roleName.equalsIgnoreCase(role.getName())) {
                return role;
            }
        }
        //  No match? Try matching start
        Role temp = null;
        for (Role role : server.getRoles()) {
            if (role.getName().toLowerCase().startsWith(roleName)) {
                if (temp == null || temp.getName().length() <= temp.getName().length()) {
                    temp = role;
                }
            }
        }
        if (temp != null) {
            return temp;
        }
        //  Fuzzy match
        //  TODO

        //  ID match
        temp = getRole(roleName, server);
        if (temp != null) {
            return temp;
        }
        return NO_ROLE;
    }

    public Set<Role> getMemberRoles(Member member, Server server) {
        if (member == null || server == null || server == NO_SERVER) {
            return Collections.emptySet();
        }
        Set<Role> definedRoles = member.getRoles().stream().
                map(r -> getRole(r, server)).
                collect(Collectors.toCollection(LinkedHashSet::new));
        Role everyone = findRole("@everyone", server);
        LinkedHashSet<Role> ret = new LinkedHashSet<>(definedRoles.size() + 1);
        if (everyone != null) {
            ret.add(everyone);
        }
        ret.addAll(definedRoles);
        return ret;
    }

    public Member getMemberHttp(String serverId, String userId) throws UnirestException {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            headers.put(HttpHeaders.AUTHORIZATION, token);
            HttpResponse<String> response = Unirest.
                    get("https://discordapp.com/api/guilds/" + serverId + "/members/" + userId).
                    headers(headers).
                    asString();
            int status = response.getStatus();
            if (status == 500) {
                //  Actually a not found error
                return NO_MEMBER;
            }
            if (status != 200) {
                statistics.restErrorCount.increment();
                throw new UnirestException("HTTP " + response.getStatus() + ": " + response.getStatusText());
            }
            Member member = gson.fromJson(response.getBody(), Member.class);
            if (member == null) {
                throw new UnirestException("Invalid entity: null");
            }
            return member;
        } catch (UnirestException e) {
            statistics.restErrorCount.increment();
            return NO_MEMBER;
        }
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

    public Statistics getStatistics() {
        return statistics;
    }

    public boolean isActive() {
        return active.get();
    }

    public Map<String, Presence> getUserPresences() {
        return userPresences;
    }

    public Map<String, String> getUserGames() {
        return userGames;
    }

    @Subscribe
    public void onWebSocketClose(WebSocketCloseEvent event) {
        if (!active.get()) {
            //  Ignore
            return;
        }
        //  Reconnect
        active.set(false);
        try {
            openWebSocket();
        } catch (IOException e) {
            LOGGER.warn("Unable to reopen WebSocket", e);
        }
    }

    public void stop() {
        active.set(false);
        executorService.shutdown();
        eventBus.unregister(this);
        webSocketClient.close();
    }

    public static class Statistics {
        public final LongAdder eventCount = new LongAdder();
        public final LongAdder eventDispatchErrorCount = new LongAdder();
        public final LongAdder connectAttemptCount = new LongAdder();
        public final LongAdder restErrorCount = new LongAdder();
    }
}
