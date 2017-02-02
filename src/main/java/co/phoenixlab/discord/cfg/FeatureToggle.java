package co.phoenixlab.discord.cfg;

import java.util.HashMap;
import java.util.Map;

public class FeatureToggle {

    public enum Override {
        ENABLED,
        DISABLED,
        NOT_SET
    }

    private boolean globalDisable;
    private boolean enabled;
    private Map<String, Override> serverOverrides;
    private Map<String, Override> channelOverrides;

    public FeatureToggle() {
        serverOverrides = new HashMap<>();
        channelOverrides = new HashMap<>();
        globalDisable = false;
        enabled = false;
    }

    public boolean isGloballyDisabled() {
        return globalDisable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Override getServerOverride(String serverId) {
        return serverOverrides.getOrDefault(serverId, Override.NOT_SET);
    }

    public Override getChannelOverride(String channelId) {
        return channelOverrides.getOrDefault(channelId, Override.NOT_SET);
    }

    public boolean use() {
        return enabled && !globalDisable;
    }

    public boolean use(String serverId) {
        if (serverId == null) {
            return use();
        }
        Override serverOverride = getServerOverride(serverId);
        if (serverOverride == Override.NOT_SET) {
            return use();
        }
        return !globalDisable && serverOverride == Override.ENABLED;
    }

    public boolean use(String serverId, String channelId) {
        if(channelId == null) {
            return use(serverId);
        }
        Override serverOverride = getServerOverride(serverId);
        Override channelOverride = getChannelOverride(channelId);
        if (channelOverride == Override.NOT_SET && serverOverride == Override.NOT_SET) {
            return use();
        }
        if (channelOverride != Override.NOT_SET && serverOverride == Override.NOT_SET) {
            return !globalDisable && channelOverride == Override.ENABLED;
        } else {
            return use(serverId);
        }
    }

    public void setGloballyDisabled(boolean disabled) {
        globalDisable = disabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setServerOverride(String serverId, Override override) {
        if (override == Override.NOT_SET || override == null) {
            serverOverrides.remove(serverId);
        } else {
            serverOverrides.put(serverId, override);
        }
    }

    public void setChannelOverride(String channelId, Override override) {
        if (override == Override.NOT_SET || override == null) {
            channelOverrides.remove(channelId);
        } else {
            channelOverrides.put(channelId, override);
        }
    }
}
