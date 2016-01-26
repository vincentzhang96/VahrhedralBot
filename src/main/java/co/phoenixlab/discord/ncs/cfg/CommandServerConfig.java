package co.phoenixlab.discord.ncs.cfg;

import co.phoenixlab.discord.ncs.Enableable;
import co.phoenixlab.discord.ncs.permissions.PermissionChecker;

import java.util.*;

public class CommandServerConfig implements Enableable {

    private String serverId;

    private final List<PermissionChecker> permissionCheckers;

    private boolean enabled;

    private final Map<String, CommandChannelConfig> channelConfigs;

    private OptionalLong defaultCooldown;

    private OptionalInt defaultCooldownCharges;

    private CommandGlobalConfig parent;

    public CommandServerConfig() {
        permissionCheckers = new ArrayList<>();
        enabled = true;
        channelConfigs = new HashMap<>();
        defaultCooldown = OptionalLong.empty();
        defaultCooldownCharges = OptionalInt.of(1);
    }

    public String getServerId() {
        return serverId;
    }

    public List<PermissionChecker> getPermissionCheckers() {
        return Collections.unmodifiableList(permissionCheckers);
    }

    public void addPermissionChecker(PermissionChecker checker) {
        permissionCheckers.add(Objects.requireNonNull(checker));
    }

    public void removePermissionChecker(PermissionChecker checker) {
        permissionCheckers.remove(checker);
    }

    public void removePermissionChecker(int index) {
        permissionCheckers.remove(index);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, CommandChannelConfig> getChannelConfigs() {
        return channelConfigs;
    }

    public OptionalInt getDefaultCooldownCharges() {
        return defaultCooldownCharges;
    }

    public OptionalLong getDefaultCooldown() {
        return defaultCooldown;
    }

    public CommandGlobalConfig getParent() {
        return parent;
    }

    public void addChannel(String channelId) {
        CommandChannelConfig cfg = CommandChannelConfig.builder().
                forChannel(channelId).
                withParent(this).
                withParentDefaultCooldownTimer().
                build();
        channelConfigs.put(channelId, cfg);
    }

    public CommandChannelConfig getChannel(String channelId) {
        return channelConfigs.get(channelId);
    }

    protected void onParentDefaultCooldownChanged(long seconds) {
        //  Only notify if we didn't already have a server-wide CD
        if (!defaultCooldown.isPresent()) {
            OptionalLong o = OptionalLong.of(seconds);
            channelConfigs.values().forEach(c -> c.onParentDefaultCooldownChanged(o));
        }
    }

    protected void onParentDefaultCooldownChargesChanged(int charges) {
        //  Only notify if we didn't already have a server-wide charge count
        if (!defaultCooldownCharges.isPresent()) {
            OptionalInt o = OptionalInt.of(charges);
            channelConfigs.values().forEach(c -> c.onParentDefaultCooldownChargesChanged(o));
        }
    }

    public void setDefaultCooldown(long defaultCooldown) {
        if (defaultCooldown < 0) {
            throw new IllegalArgumentException("Cooldown must be at least 0");
        }
        this.defaultCooldown = OptionalLong.of(defaultCooldown);
        //  Notify children of changes
        channelConfigs.values().forEach(s -> s.onParentDefaultCooldownChanged(this.defaultCooldown));
    }

    public void clearDefaultCooldown() {
        defaultCooldown = OptionalLong.empty();
        //  Notify children of changes
        channelConfigs.values().forEach(s -> s.onParentDefaultCooldownChanged(this.defaultCooldown));
    }

    public void setDefaultCooldownCharges(int defaultCooldownCharges) {
        if (defaultCooldownCharges < 1) {
            throw new IllegalArgumentException("Charges must be at least 1");
        }
        this.defaultCooldownCharges = OptionalInt.of(defaultCooldownCharges);
        //  Notify children of changes
        channelConfigs.values().forEach(s -> s.onParentDefaultCooldownChargesChanged(this.defaultCooldownCharges));
    }

    public void clearDefaultCooldownCharges() {
        defaultCooldownCharges = OptionalInt.empty();
        //  Notify children of changes
        channelConfigs.values().forEach(s -> s.onParentDefaultCooldownChargesChanged(this.defaultCooldownCharges));
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private final CommandServerConfig cfg;

        private Builder() {
            cfg = new CommandServerConfig();
        }

        public Builder forServer(String serverId) {
            cfg.serverId = serverId;
            return this;
        }

        public Builder withPermissionChecker(PermissionChecker checker) {
            cfg.addPermissionChecker(checker);
            return this;
        }

        public Builder withPermissionCheckers(PermissionChecker... checkers) {
            Collections.addAll(cfg.permissionCheckers, checkers);
            return this;
        }

        public Builder withDefaultCooldown(long seconds) {
            cfg.defaultCooldown = OptionalLong.of(seconds);
            return this;
        }

        public Builder withDefaultCooldownCharges(int charges) {
            cfg.defaultCooldownCharges = OptionalInt.of(charges);
            return this;
        }

        public Builder disabled() {
            cfg.disable();
            return this;
        }

        public Builder withParent(CommandGlobalConfig parent) {
            cfg.parent = parent;
            return this;
        }

        public CommandServerConfig build() {
            if (cfg.serverId == null) {
                throw new IllegalStateException("ServerId must be set with forServer(String sid)");
            }
            if (cfg.parent == null) {
                throw new IllegalStateException("Parent must be set with withParent(CommandServerConfig parent)");
            }
            return cfg;
        }
    }
}
