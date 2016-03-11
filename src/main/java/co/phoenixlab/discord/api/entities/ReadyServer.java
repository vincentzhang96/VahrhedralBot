package co.phoenixlab.discord.api.entities;

public class ReadyServer extends Server {

    private PresenceUpdate[] presences;

    public ReadyServer() {
    }

    public ReadyServer(String id) {
        super(id);
    }

    public ReadyServer(String id, String name) {
        super(id, name);
    }

    public PresenceUpdate[] getPresences() {
        return presences;
    }
}
