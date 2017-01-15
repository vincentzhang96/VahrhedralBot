package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Server;

public class ServerJoinLeaveEvent {

    private final Server server;
    private final boolean join;
    private final DiscordApiClient apiClient;

    public ServerJoinLeaveEvent(Server server, boolean join, DiscordApiClient apiClient) {
        this.server = server;
        this.join = join;
        this.apiClient = apiClient;
    }

    public Server getServer() {
        return server;
    }

    public boolean isJoin() {
        return join;
    }

    public DiscordApiClient getApiClient() {
        return apiClient;
    }
}
