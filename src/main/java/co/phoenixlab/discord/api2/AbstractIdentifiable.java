package co.phoenixlab.discord.api2;

import java.util.Objects;

/**
 * Convenience abstract class implementing {@link Identifiable} that manages and provides access to the ID.
 */
public abstract class AbstractIdentifiable implements Identifiable {

    private static final String UNINITIALIZED_ID = "0";

    protected String id;

    /**
     * Empty constructor only for deserialization initialization. The ID is set to an internal value. The ID must be
     * set to a {@link Identifiable#getId() valid ID} when it the object has been fully initiated.
     */
    protected AbstractIdentifiable() {
        this.id = UNINITIALIZED_ID;
    }

    /**
     * Public constructor that sets this object's ID to the given ID.
     *
     * @param id The object's ID.
     * @throws NullPointerException     If {@code id} is null.
     * @throws IllegalArgumentException If {@code id} is effectively empty.
     */
    public AbstractIdentifiable(String id) throws NullPointerException, IllegalArgumentException {
        this.id = Objects.requireNonNull(id);
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be effectively empty");
        }
    }

    /**
     * Checks the validity of the ID according to the contract in {@link Identifiable#getId()} and
     * {@link #AbstractIdentifiable()}.
     *
     * @return True if the ID is valid according to the contracts, false otherwise.
     */
    @Override
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() && !id.equals(UNINITIALIZED_ID);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Identifiable)) {
            return false;
        }
        Identifiable that = (Identifiable) o;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
