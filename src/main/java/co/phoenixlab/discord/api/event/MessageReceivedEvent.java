package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Message;

public class MessageReceivedEvent {

    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("%s[msg='%s',from=%s,channelId=%s]",
                getClass().getSimpleName(),
                message.getContent(),
                message.getAuthor().getUsername(),
                message.getChannelId());
    }
}
