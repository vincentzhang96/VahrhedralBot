package co.phoenixlab.discord.api.entities;

/**
 * Represents a text channel
 */
public class Channel {

    /**
     * The channel's unique ID (does not change over the lifespan of a channel)
     */
    private String id;

    /**
     * The channel's human readable name (can change over time)
     */
    private String name;

    public Channel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Channel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
