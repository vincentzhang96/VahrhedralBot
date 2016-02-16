package co.phoenixlab.discord.api2;

/**
 * Represents only an ID, for use when dealing with raw IDs and passing to APIs that operate
 * off of IDs. General usage is discouraged, please use more suitable implementations
 */
public class Identity extends AbstractIdentifiable {

    public Identity(String id)
            throws NullPointerException, IllegalArgumentException {
        super(id);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
