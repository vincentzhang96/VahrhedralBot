package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Message;

public class MessageEditEvent {

    private Message message;

    public MessageEditEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
