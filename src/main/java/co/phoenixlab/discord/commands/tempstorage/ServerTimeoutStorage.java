package co.phoenixlab.discord.commands.tempstorage;

import java.util.HashMap;
import java.util.Map;

public class ServerTimeoutStorage {

    private String timeoutRoleId;
    private final Map<String, ServerTimeout> timeouts;

    public ServerTimeoutStorage() {
        timeouts = new HashMap<>();
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
