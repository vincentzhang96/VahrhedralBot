package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Server;

public class ServerJoinLeaveEvent {

    private final Server server;
    private final boolean join;

    public ServerJoinLeaveEvent(Server server, boolean join) {
        this.server = server;
        this.join = join;
    }

    public Server getServer() {
        return server;
    }

    public boolean isJoin() {
        return join;
    }
}
