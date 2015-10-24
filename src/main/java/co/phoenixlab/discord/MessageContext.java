package co.phoenixlab.discord;


import co.phoenixlab.discord.api.entities.Message;

public class MessageContext {

    private final Message message;
    private final VahrhedralBot bot;

    public MessageContext(Message message, VahrhedralBot bot) {
        this.message = message;
        this.bot = bot;
    }

    public Message getMessage() {
        return message;
    }

    public VahrhedralBot getBot() {
        return bot;
    }
}
