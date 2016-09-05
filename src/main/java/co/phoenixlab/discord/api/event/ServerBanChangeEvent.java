package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;

public class ServerBanChangeEvent {

    private final User user;
    private final Server server;
    private final BanChange change;

    public ServerBanChangeEvent(User user, Server server, BanChange change) {
        this.user = user;
        this.server = server;
        this.change = change;
    }

    public User getUser() {
        return user;
    }

    public Server getServer() {
        return server;
    }

    public BanChange getChange() {
        return change;
    }

    public enum BanChange {
        ADDED,
        DELETED
    }
}
