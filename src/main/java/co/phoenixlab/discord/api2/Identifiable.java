package co.phoenixlab.discord.api2;

/**
 * An object implementing {@linkplain Identifiable} represents a resource from Discord that has a unique
 * ID.
 */
public interface Identifiable {

    /**
     * Gets this object's unique ID, which is not null and not effectively empty.
     *
     * @return A String containing the object's unique ID, as assigned by Discord.
     * This value will never be null or empty in a valid {@code Identifiable}
     * @see #isValid()
     */
    String getId();


    /**
     * Checks the validity of the ID according to the contract in {@link Identifiable#getId()}.
     *
     * @return True if the ID is valid according to the contract, false otherwise.
     */
    default boolean isValid() {
        String id = getId();
        return id != null && !id.trim().isEmpty();
    }

}
