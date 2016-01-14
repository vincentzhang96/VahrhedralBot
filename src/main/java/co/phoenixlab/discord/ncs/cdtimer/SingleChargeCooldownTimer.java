package co.phoenixlab.discord.ncs.cdtimer;

/**
 * A CooldownTimer optimized for having only a single charge
 */
public class SingleChargeCooldownTimer implements CooldownTimer {

    /**
     * How long the cooldown is, in milliseconds
     */
    private long cooldownMillis;

    /**
     * The last time this timer was charged
     */
    private long lastUseTimeMillis;

    /**
     * Constructs a SingleChargeCooldownTimer with a CD of 0s
     */
    public SingleChargeCooldownTimer() {
        this(0);
    }

    /**
     * Constructs a timer with the given CD. CD must be at least 0.
     * @param cooldown The cooldown timer length, in seconds
     * @throws IllegalArgumentException If the cooldown timer length is less than 0
     */
    public SingleChargeCooldownTimer(long cooldown) throws IllegalArgumentException {
        if (cooldown < 0) {
            throw new IllegalArgumentException("Cooldown must be at least zero");
        }
        cooldownMillis = cooldown * 1000L;
        lastUseTimeMillis = 0;
    }

    @Override
    public long getCooldown() {
        return cooldownMillis / 1000L;
    }

    @Override
    public int getCharges() {
        return isOnCooldown() ? 0 : 1;
    }

    @Override
    public int getMaxCharges() {
        return 1;
    }

    @Override
    public boolean isOnCooldown() {
        return (System.currentTimeMillis() - lastUseTimeMillis) < cooldownMillis;
    }

    @Override
    public void reset() {
        lastUseTimeMillis = 0;
    }

    @Override
    public void set() {
        if (!isOnCooldown()) {
             fireCooldown();
        }
    }

    @Override
    public void forceSet() {
        fireCooldown();
    }

    @Override
    public void forceCooldown() {
        fireCooldown();
    }

    private void fireCooldown() {
        lastUseTimeMillis = System.currentTimeMillis();
    }
}
