package co.phoenixlab.discord.api.entities;

/**
 * Represents a text channel
 */
public class Channel {

    /**
     * The channel's unique ID (does not change over the lifespan of a channel)
     */
    private final String id;

    /**
     * The channel's human readable name (can change over time)
     */
    private final String name;

    public Channel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Channel() {
        this(null, null);
    }

    /**
     * @return {@link #id}
     */
    public String getId() {
        return id;
    }

    /**
     * @return {@link #name}
     */
    public String getName() {
        return name;
    }
}
