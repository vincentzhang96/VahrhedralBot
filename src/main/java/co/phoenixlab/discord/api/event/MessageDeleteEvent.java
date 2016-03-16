package co.phoenixlab.discord.api.event;

public class MessageDeleteEvent {

    private String messageId;
    private String channelId;

    public MessageDeleteEvent(String messageId, String channelId) {
        this.messageId = messageId;
        this.channelId = channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getChannelId() {
        return channelId;
    }
}
