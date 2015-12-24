package co.phoenixlab.discord.api.voice;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.DiscordWebSocketClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.ChannelType;
import co.phoenixlab.discord.api.entities.Server;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceClient implements Runnable {

    private static final AtomicBoolean connected = new AtomicBoolean(false);

    public static Logger LOGGER = LoggerFactory.getLogger("VoiceClient");

    private final DiscordApiClient apiClient;
    private final DiscordWebSocketClient webSocketClient;
    private final Channel channel;
    private final Server server;

    public VoiceClient(Channel channel, DiscordApiClient apiClient) {
        this.apiClient = Objects.requireNonNull(apiClient);
        this.webSocketClient = apiClient.getWebSocketClient();
        this.channel = Objects.requireNonNull(channel);
        this.server = channel.getParent();
        if (channel.getType() != ChannelType.VOICE) {
            throw new IllegalArgumentException(channel.getName() + " is not a voice channel");
        }
        apiClient.getEventBus().register(this);
    }

    @Override
    public void run() {
        join();
    }

    private void join() {
        LOGGER.info("Attempting to join voice channel {} ({}) in {} ({})",
                channel.getName(), channel.getId(),
                server.getName(), server.getId());
        if (!connected.compareAndSet(false, true)) {
            throw new IllegalStateException("Another VoiceClient is already connected");
        }
        JSONObject outer = new JSONObject();
        outer.put("op", 4);
        JSONObject payload = new JSONObject();
        payload.put("guild_id", server.getId());
        payload.put("channel_id", channel.getId());
        payload.put("self_mute", false);
        payload.put("self_deaf", false);
        outer.put("d", payload);
        webSocketClient.send(outer.toString());
        LOGGER.info("Sent payload");
    }

    public void disconnect() {
        LOGGER.info("Disconnecting from channel {} ({}) in {} ({})",
                channel.getName(), channel.getId(),
                server.getName(), server.getId());
        JSONObject outer = new JSONObject();
        outer.put("op", 4);
        JSONObject payload = new JSONObject();
        payload.put("guild_id", JSONObject.NULL);
        payload.put("channel_id", JSONObject.NULL);
        payload.put("self_mute", false);
        payload.put("self_deaf", false);
        outer.put("d", payload);
        webSocketClient.send(outer.toString());
        LOGGER.info("Sent disconnect packet");

        apiClient.getEventBus().unregister(this);
        connected.set(false);
    }
}
