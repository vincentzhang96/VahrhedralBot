package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class ReadyMessage {

    @SerializedName("v")
    private int version;

    private User user;
    @SerializedName("session_id")
    private String sessionId;
    @SerializedName("read_state")
    private ReadState[] readState;
    @SerializedName("private_channels")
    private Channel[] privateChannels;
    @SerializedName("heartbeat_interval")
    private long heartbeatInterval;
    @SerializedName("guilds")
    private ReadyServer[] servers;

    public ReadyMessage() {
    }

    public int getVersion() {
        return version;
    }

    public User getUser() {
        return user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ReadState[] getReadState() {
        return readState;
    }

    public Channel[] getPrivateChannels() {
        return privateChannels;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public ReadyServer[] getServers() {
        return servers;
    }

    class ReadState {
        @SerializedName("mention_count")
        private int mentionCount;
        @SerializedName("last_message_id")
        private String lastMessageId;

        private String id;

        public int getMentionCount() {
            return mentionCount;
        }

        public String getLastMessageId() {
            return lastMessageId;
        }

        public String getId() {
            return id;
        }
    }
}
