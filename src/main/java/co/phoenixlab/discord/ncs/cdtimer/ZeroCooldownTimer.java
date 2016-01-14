package co.phoenixlab.discord.ncs.cdtimer;

public class ZeroCooldownTimer implements CooldownTimer {
    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public int getCharges() {
        return 1;
    }

    @Override
    public int getMaxCharges() {
        return 1;
    }

    @Override
    public boolean isOnCooldown() {
        return false;
    }

    @Override
    public boolean check() {
        return false;
    }

    @Override
    public void reset() {
        //  Do nothing
    }

    @Override
    public boolean checkAndSet() {
        return false;
    }

    @Override
    public void set() {
        //  Do nothing
    }

    @Override
    public void forceSet() {
        //  Do nothing
    }

    @Override
    public void forceCooldown() {
        //  Do nothing
    }
}
