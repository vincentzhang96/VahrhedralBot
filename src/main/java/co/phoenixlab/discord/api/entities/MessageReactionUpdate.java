package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class MessageReactionUpdate {

    private ReactionEmoji emoji;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("message_id")
    private String messageId;
    @SerializedName("channel_id")
    private String channelId;

    public MessageReactionUpdate() {
    }

    public MessageReactionUpdate(ReactionEmoji emoji, String userId, String messageId, String channelId) {
        this.emoji = emoji;
        this.userId = userId;
        this.messageId = messageId;
        this.channelId = channelId;
    }

    public ReactionEmoji getEmoji() {
        return emoji;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getChannelId() {
        return channelId;
    }
}
