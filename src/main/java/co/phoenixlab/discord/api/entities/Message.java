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

    private final String content;

    private final String id;

    private final Mention[] mentions;

    private final String time;

    public Message(User author, String channelId, String content, String id, Mention[] mentions, String time) {
        this.author = author;
        this.channelId = channelId;
        this.content = content;
        this.id = id;
        this.mentions = mentions;
        this.time = time;
    }

    public Message() {
        this(null, null, null, null, null, null);
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

    public Mention[] getMentions() {
        return mentions;
    }

    public String getTime() {
        return time;
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
