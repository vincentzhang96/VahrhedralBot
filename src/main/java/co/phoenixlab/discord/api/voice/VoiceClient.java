package co.phoenixlab.discord.api.voice;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.DiscordWebSocketClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.ChannelType;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.voice.VoiceServerUpdate;
import co.phoenixlab.discord.api.entities.voice.VoiceStateUpdate;
import co.phoenixlab.discord.api.event.voice.VoiceServerUpdateEvent;
import co.phoenixlab.discord.api.event.voice.VoiceStateUpdateEvent;
import com.google.common.eventbus.Subscribe;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceClient implements Runnable {

    private static final AtomicBoolean active = new AtomicBoolean(false);

    public static final Logger LOGGER = LoggerFactory.getLogger("VoiceClient");

    private final DiscordApiClient apiClient;
    private final DiscordWebSocketClient mainWebSocketClient;
    private final Channel channel;
    private final Server server;
    private final AtomicBoolean connected;
    private final AtomicBoolean keepRunning;

    private URI endpoint;
    private String token;
    private String sessionId;

    private CountDownLatch initializationLatch;
    private VoiceWebSocketClient voiceWebSocketClient;

    private Thread runningThread;

    public VoiceClient(Channel channel, DiscordApiClient apiClient) {
        this.apiClient = Objects.requireNonNull(apiClient);
        this.mainWebSocketClient = apiClient.getWebSocketClient();
        this.channel = Objects.requireNonNull(channel);
        this.server = channel.getParent();
        this.keepRunning = new AtomicBoolean(false);
        this.connected = new AtomicBoolean(false);
        if (channel.getType() != ChannelType.VOICE) {
            throw new IllegalArgumentException(channel.getName() + " is not a voice channel");
        }
        apiClient.getEventBus().register(this);
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        join();
        LOGGER.info("Awaiting data...");
        try {
            initializationLatch.await();
        } catch (InterruptedException e) {
            if (!keepRunning.get()) {
                LOGGER.info("Interrupted by stop request");
                disconnect();
                return;
            }
        }
        LOGGER.info("Got endpoint and auth information");
        voiceWebSocketClient = new VoiceWebSocketClient(endpoint);
        try {
            if (!voiceWebSocketClient.connectBlocking()) {
                LOGGER.warn("Unable to connect to voice endpoint");
            }
        } catch (InterruptedException e) {
            if (!keepRunning.get()) {
                LOGGER.info("Interrupted by stop request");
                disconnect();
                return;
            }
        }
        connected.set(true);
        LOGGER.info("Connected to voice endpoint");

    }

    @Subscribe
    public void onVoiceServerUpdate(VoiceServerUpdateEvent event) {
        try {
            if (connected.get()) {
                LOGGER.warn("Ignoring voice server update while already connected");
                return;
            }
            VoiceServerUpdate update = event.getServerUpdate();
            endpoint = new URI("ws://" + update.getEndpoint().replace(":80", ""));
            token = update.getToken();
            LOGGER.info("Received server info: endpoint={} token={}", endpoint.toString(), token);
            initializationLatch.countDown();
        } catch (Exception e) {
            LOGGER.warn("Exception while handling voice state update", e);
            stop();
        }
    }

    @Subscribe
    public void onVoiceStateUpdate(VoiceStateUpdateEvent event) {
        try {
            VoiceStateUpdate update = event.getStateUpdate();
            //  VoiceStateUpdates come from other users as well - we only care about our own
            if (!apiClient.getClientUser().equals(update.getUser())) {
                return;
            }
            //  Also don't care if we're already connected
            if (connected.get()) {
                LOGGER.warn("Ignoring voice state update while already connected");
                return;
            }
            LOGGER.info("Received sessionId {}", update.getSessionId());
            sessionId = update.getSessionId();
            initializationLatch.countDown();
        } catch (Exception e) {
            LOGGER.warn("Exception while handling voice state update", e);
            stop();
        }
    }

    private void join() {
        LOGGER.info("Attempting to join voice channel {} ({}) in {} ({})",
                channel.getName(), channel.getId(),
                server.getName(), server.getId());
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("Another VoiceClient is already active");
        }
        keepRunning.set(true);
        initializationLatch = new CountDownLatch(2);
        JSONObject outer = new JSONObject();
        outer.put("op", 4);
        JSONObject payload = new JSONObject();
        payload.put("guild_id", server.getId());
        payload.put("channel_id", channel.getId());
        payload.put("self_mute", false);
        payload.put("self_deaf", false);
        outer.put("d", payload);
        mainWebSocketClient.send(outer.toString());
        LOGGER.info("Sent payload");
    }

    private void disconnect() {
        LOGGER.info("Disconnecting from channel {} ({}) in {} ({})",
                channel.getName(), channel.getId(),
                server.getName(), server.getId());
        connected.set(false);
        JSONObject outer = new JSONObject();
        outer.put("op", 4);
        JSONObject payload = new JSONObject();
        payload.put("guild_id", JSONObject.NULL);
        payload.put("channel_id", JSONObject.NULL);
        payload.put("self_mute", false);
        payload.put("self_deaf", false);
        outer.put("d", payload);
        mainWebSocketClient.send(outer.toString());
        LOGGER.info("Sent disconnect packet");

        apiClient.getEventBus().unregister(this);
        active.set(false);
    }

    private void sendKeepAlive() {

    }

    public void stop() {
        keepRunning.set(false);
        runningThread.interrupt();
    }
}

class VoiceWebSocketClient extends WebSocketClient {

    public VoiceWebSocketClient(URI endpoint) {
        super(endpoint);
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
