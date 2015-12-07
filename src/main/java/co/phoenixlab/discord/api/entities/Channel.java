package co.phoenixlab.discord.api.entities;

import java.util.Objects;

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
    private String name;

    private String topic;

    private Server parent;

    public Channel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Channel() {
        this(null, null);
    }

    public Server getParent() {
        return parent;
    }

    public void setParent(Server parent) {
        this.parent = parent;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Channel channel = (Channel) o;
        return Objects.equals(id, channel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
