package co.phoenixlab.discord.ncs.cfg;

import co.phoenixlab.discord.ncs.Enableable;
import co.phoenixlab.discord.ncs.cdtimer.ChargedCooldownTimer;
import co.phoenixlab.discord.ncs.cdtimer.CooldownTimer;
import co.phoenixlab.discord.ncs.cdtimer.SingleChargeCooldownTimer;
import co.phoenixlab.discord.ncs.cdtimer.ZeroCooldownTimer;
import co.phoenixlab.discord.ncs.permissions.PermissionChecker;

import java.util.*;

public class CommandChannelConfig implements Enableable {

    private String channelId;

    private CooldownTimer cooldownTimer;
    protected boolean overridesParent;

    private final List<PermissionChecker> permissionCheckers;

    private boolean enabled;

    private CommandServerConfig parent;

    public CommandChannelConfig() {
        permissionCheckers = new ArrayList<>();
        enabled = true;
        overridesParent = false;
    }

    public CooldownTimer getCooldownTimer() {
        return cooldownTimer;
    }

    public void setCooldownTimer(CooldownTimer cooldownTimer) {
        this.cooldownTimer = Objects.requireNonNull(cooldownTimer);
        overridesParent = true;
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
        return enabled && ((parent == null) || parent.isEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannelId() {
        return channelId;
    }

    public CommandServerConfig getParent() {
        return parent;
    }

    public void setParent(CommandServerConfig parent) {
        this.parent = parent;
    }

    protected void onParentDefaultCooldownChanged(OptionalLong seconds) {

    }

    protected void onParentDefaultCooldownChargesChanged(OptionalInt charges) {

    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private final CommandChannelConfig cfg;

        private Builder() {
            cfg = new CommandChannelConfig();
        }

        public Builder forChannel(String channelId) {
            cfg.channelId = channelId;
            return this;
        }

        public Builder withCooldownTimer(long cooldownSecs) {
            if (cooldownSecs == 0) {
                return withNoCooldown();
            }
            cfg.setCooldownTimer(new SingleChargeCooldownTimer(cooldownSecs));
            cfg.overridesParent = true;
            return this;
        }

        public Builder withCooldownTimer(long cooldownSecs, int charges) {
            if (cooldownSecs == 0) {
                return withNoCooldown();
            }
            if (charges == 1) {
                return withCooldownTimer(cooldownSecs);
            }
            cfg.setCooldownTimer(new ChargedCooldownTimer(cooldownSecs, charges));
            cfg.overridesParent = true;
            return this;
        }

        public Builder withNoCooldown() {
            cfg.setCooldownTimer(new ZeroCooldownTimer());
            cfg.overridesParent = true;
            return this;
        }

        public Builder withCooldownTimer(CooldownTimer timer) {
            cfg.setCooldownTimer(timer);
            cfg.overridesParent = true;
            return this;
        }

        public Builder withParentDefaultCooldownTimer() {
            withCooldownTimer(
                    cfg.parent.getDefaultCooldown().orElse(cfg.parent.getParent().getDefaultCooldown()),
                    cfg.parent.getDefaultCooldownCharges().orElse(cfg.parent.getParent().getDefaultCooldownCharges()));
            //  Un-override
            cfg.overridesParent = false;
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

        public Builder disabled() {
            cfg.disable();
            return this;
        }

        public Builder withParent(CommandServerConfig parent) {
            cfg.parent = parent;
            return this;
        }

        public CommandChannelConfig build() {
            if (cfg.channelId == null) {
                throw new IllegalStateException("ChannelId must be set with forChannel(String chid)");
            }
            if (cfg.cooldownTimer == null) {
                throw new IllegalStateException("CooldownTimer must be set with withCooldownTimer(...) or" +
                        " withNoCooldown() or withParentDefaultCooldownTimer");
            }
            if (cfg.parent == null) {
                throw new IllegalStateException("Parent must be set with withParent(CommandServerConfig parent)");
            }
            return cfg;
        }
    }

}
