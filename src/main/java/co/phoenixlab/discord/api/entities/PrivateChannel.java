package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class PrivateChannel {

    private User recipient;
    @SerializedName("last_message_id")
    private String lastMessageId;
    @SerializedName("is_private")
    private boolean isPrivate;
    private String id;

    public PrivateChannel() {
    }

    public User getRecipient() {
        return recipient;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getId() {
        return id;
    }
}
