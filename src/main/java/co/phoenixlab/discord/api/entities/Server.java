package co.phoenixlab.discord.api.entities;

import java.util.HashSet;
import java.util.Set;

public class Server {

    /**
     * Display name of the server
     */
    private final String name;

    /**
     * Server identifier
     */
    private final String id;

    /**
     * The text channels that belong to this server
     */
    private final Set<Channel> channels;

    /**
     * The members that belong to this server
     */
    private final Set<Member> members;

    public Server(String id, String name, Set<Channel> channels, Set<Member> members) {
        this.name = name;
        this.id = id;
        this.channels = channels;
        this.members = members;

    }

    public Server(String id, String name) {
        this(id, name, new HashSet<>(), new HashSet<>());
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
    public Set<Channel> getChannel() {
        return channels;
    }

    /**
     * @return {@link #members}
     */
    public Set<Member> getMembers() {
        return members;
    }
}
