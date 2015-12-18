package co.phoenixlab.discord;


import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;

public class MessageContext {

    private final Message message;
    private final Channel channel;
    private final Server server;
    private final User author;
    private final VahrhedralBot bot;
    private final DiscordApiClient apiClient;
    private final CommandDispatcher dispatcher;

    public MessageContext(Message message, VahrhedralBot bot, CommandDispatcher dispatcher) {
        this.message = message;
        this.bot = bot;
        this.dispatcher = dispatcher;
        this.apiClient = bot.getApiClient();
        this.channel = apiClient.getChannelById(message.getChannelId());
        this.server = channel.getParent();
        this.author = message.getAuthor();
    }

    public Message getMessage() {
        return message;
    }

    public VahrhedralBot getBot() {
        return bot;
    }

    public DiscordApiClient getApiClient() {
        return apiClient;
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    public Channel getChannel() {
        return channel;
    }

    public Server getServer() {
        return server;
    }

    public User getAuthor() {
        return author;
    }
}
