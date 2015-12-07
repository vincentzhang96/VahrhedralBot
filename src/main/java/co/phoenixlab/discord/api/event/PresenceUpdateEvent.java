package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.PresenceUpdate;
import co.phoenixlab.discord.api.entities.Server;

public class PresenceUpdateEvent {

    private final PresenceUpdate presenceUpdate;

    private final Server server;

    public PresenceUpdateEvent(PresenceUpdate presenceUpdate, Server server) {
        this.presenceUpdate = presenceUpdate;
        this.server = server;
    }

    public PresenceUpdate getPresenceUpdate() {
        return presenceUpdate;
    }

    public Server getServer() {
        return server;
    }
}
