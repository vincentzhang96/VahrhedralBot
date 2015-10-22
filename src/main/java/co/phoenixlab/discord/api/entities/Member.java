package co.phoenixlab.discord.api.entities;

import java.util.List;

/**
 * Represents a member of a {@link Server}
 */
public class Member {

    /**
     * The {@link User} whose membership this is
     */
    private final User user;

    /**
     * A list of role IDs that the user possesses
     */
    private final List<String> roles;

    public Member(User user, List<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    public Member() {
        this(null, null);
    }

    /**
     * @return {@link #user}
     */
    public User getUser() {
        return user;
    }

    /**
     * @return {@link #roles}
     */
    public List<String> getRoles() {
        return roles;
    }
}
