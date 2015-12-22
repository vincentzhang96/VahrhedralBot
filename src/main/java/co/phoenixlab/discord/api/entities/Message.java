package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Represents a text Message sent in a Channel on a Server
 */
public class Message {

    private final User author;

    @SerializedName("channel_id")
    private final String channelId;

    private final String nonce;

    private final String[] attachments;

    private final String[] embeds;

    @SerializedName("mention_everyone")
    private final boolean mentionEveryone;

    private final String content;

    private final String id;

    private final User[] mentions;

    private final String timestamp;

    @SerializedName("edited_timestamp")
    private final String editedTimestamp;

    public Message(User author, String channelId, String nonce, String[] attachments, String[] embeds,
                   boolean mentionEveryone, String content, String id, User[] mentions, String timestamp,
                   String editedTimestamp) {
        this.author = author;
        this.channelId = channelId;
        this.nonce = nonce;
        this.attachments = attachments;
        this.embeds = embeds;
        this.mentionEveryone = mentionEveryone;
        this.content = content;
        this.id = id;
        this.mentions = mentions;
        this.timestamp = timestamp;
        this.editedTimestamp = editedTimestamp;
    }

    public Message(User author, String channelId, String content, String id, User[] mentions, String timestamp) {
        this(author, channelId, null, null, null, false, content, id, mentions, timestamp, null);
    }

    public Message() {
        this(null, null, null, null, null, false, null, null, null, null, null);
    }

    public User getAuthor() {
        return author;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public User[] getMentions() {
        return mentions;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public String[] getAttachments() {
        return attachments;
    }

    public String[] getEmbeds() {
        return embeds;
    }

    public boolean isMentionEveryone() {
        return mentionEveryone;
    }

    public String getEditedTimestamp() {
        return editedTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return Objects.equals(channelId, message.channelId) &&
                Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, id);
    }
}
