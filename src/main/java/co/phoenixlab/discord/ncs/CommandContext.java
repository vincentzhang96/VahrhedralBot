package co.phoenixlab.discord.ncs;

import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;

public class CommandContext {

    private final VahrhedralBot bot;
    private final DiscordApiClient apiClient;
    private final CommandGroup ownerGroup;
    private final Server server;
    private final Channel channel;
    private final Member author;

    public CommandContext(VahrhedralBot bot, DiscordApiClient apiClient, CommandGroup ownerGroup,
                          Server server, Channel channel, Member author) {
        this.bot = bot;
        this.apiClient = apiClient;
        this.ownerGroup = ownerGroup;
        this.server = server;
        this.channel = channel;
        this.author = author;
    }

    public VahrhedralBot getBot() {
        return bot;
    }

    public DiscordApiClient getApiClient() {
        return apiClient;
    }

    public CommandGroup getOwnerGroup() {
        return ownerGroup;
    }

    public Server getServer() {
        return server;
    }

    public Channel getChannel() {
        return channel;
    }

    public Member getAuthor() {
        return author;
    }
}
