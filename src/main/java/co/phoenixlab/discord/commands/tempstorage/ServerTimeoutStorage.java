package co.phoenixlab.discord.commands.tempstorage;

import java.util.HashMap;
import java.util.Map;

public class ServerTimeoutStorage {

    private final String serverId;
    private String timeoutRoleId;
    private final Map<String, ServerTimeout> timeouts;

    public ServerTimeoutStorage(String serverId) {
        this.serverId = serverId;
        timeouts = new HashMap<>();
    }

    public String getServerId() {
        return serverId;
    }

    public Map<String, ServerTimeout> getTimeouts() {
        return timeouts;
    }

    public String getTimeoutRoleId() {
        return timeoutRoleId;
    }

    public void setTimeoutRoleId(String timeoutRoleId) {
        this.timeoutRoleId = timeoutRoleId;
    }
}
