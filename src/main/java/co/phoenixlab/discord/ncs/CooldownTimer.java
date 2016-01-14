package co.phoenixlab.discord.ncs;

/**
 *
 */
public interface CooldownTimer {

    /**
     * Gets the length of the cooldown set for this timer, in seconds
     */
    long getCooldown();

    /**
     * Sets the length of the cooldown for this timer, in seconds
     * @param cooldown The cooldown, in seconds, set for this timer
     */
    void setCooldown(long cooldown);

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



}
