package co.phoenixlab.discord;


import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;

public class MessageContext {

    private final Message message;
    private final VahrhedralBot bot;
    private final CommandDispatcher dispatcher;

    public MessageContext(Message message, VahrhedralBot bot, CommandDispatcher dispatcher) {
        this.message = message;
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    public Message getMessage() {
        return message;
    }

    public VahrhedralBot getBot() {
        return bot;
    }

    public DiscordApiClient getApiClient() {
        return bot.getApiClient();
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }
}
