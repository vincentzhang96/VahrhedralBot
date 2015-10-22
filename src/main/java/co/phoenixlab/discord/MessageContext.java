package co.phoenixlab.discord;

import sx.blah.discord.DiscordClient;
import sx.blah.discord.handle.obj.Message;

public class MessageContext {

    private final DiscordClient discord;
    private final Message message;
    private final VahrhedralBot bot;

    public MessageContext(DiscordClient discord, Message message, VahrhedralBot bot) {
        this.discord = discord;
        this.message = message;
        this.bot = bot;
    }

    public DiscordClient getDiscord() {
        return discord;
    }

    public Message getMessage() {
        return message;
    }

    public VahrhedralBot getBot() {
        return bot;
    }
}
