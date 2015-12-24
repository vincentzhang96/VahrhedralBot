package co.phoenixlab.discord.api.entities.voice;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import com.google.gson.annotations.SerializedName;

import static co.phoenixlab.discord.api.DiscordApiClient.*;

public class VoiceStateUpdate {

    @SerializedName("self_deaf")
    private boolean selfDeaf;

    @SerializedName("user_id")
    private String userId;

    private transient User user = NO_USER;

    @SerializedName("guild_id")
    private String serverId;

    private transient Server server = NO_SERVER;

    private boolean deaf;

    @SerializedName("session_id")
    private String sessionId;

    private boolean mute;

    private boolean suppress;

    private boolean selfMute;

    @SerializedName("channel_id")
    private String channelId;

    private transient Channel channel = NO_CHANNEL;

    public VoiceStateUpdate(boolean selfDeaf, String userId, String serverId, boolean deaf, String sessionId,
                            boolean mute, boolean suppress, boolean selfMute, String channelId) {
        this.selfDeaf = selfDeaf;
        this.userId = userId;
        this.serverId = serverId;
        this.deaf = deaf;
        this.sessionId = sessionId;
        this.mute = mute;
        this.suppress = suppress;
        this.selfMute = selfMute;
        this.channelId = channelId;
    }

    public VoiceStateUpdate() {
        this(false, null, null, false, null, false, false, false, null);
    }

    public boolean isSelfDeaf() {
        return selfDeaf;
    }

    public String getUserId() {
        return userId;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isDeaf() {
        return deaf;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isMute() {
        return mute;
    }

    public boolean isSuppress() {
        return suppress;
    }

    public boolean isSelfMute() {
        return selfMute;
    }

    public String getChannelId() {
        return channelId;
    }

    public void fix(DiscordApiClient apiClient) {
        if (serverId != null) {
            server = apiClient.getServerByID(serverId);
        } else {
            serverId = "";
        }
        if (userId != null) {
            user = apiClient.getUserById(userId, server);
        } else {
            userId = "";
        }
        if (channel != null) {
            channel = apiClient.getChannelById(channelId, server);
        } else {
            channelId = "";
        }
    }

    public User getUser() {
        return user;
    }

    public Server getServer() {
        return server;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "VoiceStateUpdate{" +
                "selfDeaf=" + selfDeaf +
                ", userId='" + userId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", deaf=" + deaf +
                ", sessionId='" + sessionId + '\'' +
                ", mute=" + mute +
                ", suppress=" + suppress +
                ", selfMute=" + selfMute +
                ", channelId='" + channelId + '\'' +
                '}';
    }
}
