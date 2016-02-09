package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.Set;

public class Server {

    /**
     * Display name of the server
     */
    private String name;

    /**
     * Server identifier
     */
    private final String id;

    /**
     * The text channels that belong to this server
     */
    private Set<Channel> channels;

    /**
     * The members that belong to this server
     */
    private Set<Member> members;

    private Set<Role> roles;

    private String region;

    @SerializedName("owner_id")
    private String ownerId;

    private boolean large;

    private String icon;

    public Server() {
        this(null);
    }

    public Server(String id) {
        this.id = id;
    }

    public Server(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return {@link #name}
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@link #id}
     */
    public String getId() {
        return id;
    }

    /**
     * @return {@link #channels}
     */
    public Set<Channel> getChannels() {
        return channels;
    }

    /**
     * @return {@link #members}
     */
    public Set<Member> getMembers() {
        return members;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getRegion() {
        return region;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isLarge() {
        return large;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Server server = (Server) o;
        return Objects.equals(id, server.id) &&
                Objects.equals(region, server.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, region);
    }
}
