package co.phoenixlab.discord.ncs.cfg;

import co.phoenixlab.discord.ncs.Enableable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandGlobalConfig implements Enableable {

    private boolean enabled;

    private final Map<String, CommandServerConfig> servers;

    private long defaultCooldown;

    private int defaultCooldownCharges;

    public CommandGlobalConfig() {
        enabled = true;
        servers = new HashMap<>();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, CommandServerConfig> getServers() {
        return Collections.unmodifiableMap(servers);
    }

    public long getDefaultCooldown() {
        return defaultCooldown;
    }

    public void setDefaultCooldown(long defaultCooldown) {
        if (defaultCooldown < 0) {
            throw new IllegalArgumentException("Cooldown must be at least 0");
        }
        this.defaultCooldown = defaultCooldown;
        //  Notify children of changes
        servers.values().forEach(s -> s.onParentDefaultCooldownChanged(defaultCooldown));
    }

    public int getDefaultCooldownCharges() {
        return defaultCooldownCharges;
    }

    public void setDefaultCooldownCharges(int defaultCooldownCharges) {
        if (defaultCooldownCharges < 1) {
            throw new IllegalArgumentException("Charges must be at least 1");
        }
        this.defaultCooldownCharges = defaultCooldownCharges;
        //  Notify children of changes
        servers.values().forEach(s -> s.onParentDefaultCooldownChargesChanged(defaultCooldownCharges));
    }

    public void addServer(String id) {

    }
}
