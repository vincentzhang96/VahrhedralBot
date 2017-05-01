package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.MessageReactionUpdate;
import co.phoenixlab.discord.api.entities.Server;

public class MessageReactionChangeEvent {

    public enum ReactionChange {
        ADDED,
        DELETED
    }

    private final MessageReactionUpdate update;
    private final ReactionChange type;
    private final Server server;
    private final Channel channel;
    private final DiscordApiClient apiClient;

    public MessageReactionChangeEvent(MessageReactionUpdate update, ReactionChange type, Server server,
                                      Channel channel, DiscordApiClient apiClient) {
        this.update = update;
        this.type = type;
        this.server = server;
        this.channel = channel;
        this.apiClient = apiClient;
    }

    public MessageReactionUpdate getUpdate() {
        return update;
    }

    public ReactionChange getType() {
        return type;
    }

    public Server getServer() {
        return server;
    }

    public Channel getChannel() {
        return channel;
    }

    public DiscordApiClient getApiClient() {
        return apiClient;
    }
}
