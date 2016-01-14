package co.phoenixlab.discord.ncs.cdtimer;

/**
 *
 */
public interface CooldownTimer {

    /**
     * Gets the length of the cooldown set for this timer, in seconds
     */
    long getCooldown();

    /**
     * Gets the currently available number of charges
     */
    int getCharges();

    /**
     * Gets the maximum number of charges that can be accumulated
     */
    int getMaxCharges();

    /**
     * Checks if this timer is on cooldown. More specifically, if the number of charges available is zero then this
     * method returns true, false otherwise
     */
    default boolean isOnCooldown() {
        return getCharges() == 0;
    }

    /**
     * Same as {@link CooldownTimer#isOnCooldown()}
     */
    default boolean check() {
        return isOnCooldown();
    }

    /**
     * Resets this cooldown timer, granting all charges
     */
    void reset();

    /**
     * Checks if the timer is <b>NOT</b> on cooldown, and consumes a charge if it isn't
     * @return The cooldown state of the timer before this call
     * @see CooldownTimer#set()
     */
    default boolean checkAndSet() {
        boolean prev = isOnCooldown();
        if (!prev) {
            set();
        }
        return prev;
    }

    /**
     * Consumes a charge, if available
     */
    void set();

    /**
     * If there are available charges, works the same as {@link CooldownTimer#set()}.
     * If there are no available charges, Forcibly re-consumes the youngest charge
     */
    void forceSet();

    /**
     * Forcibly puts the cooldown timer on cooldown, consuming all charges
     */
    void forceCooldown();

}
