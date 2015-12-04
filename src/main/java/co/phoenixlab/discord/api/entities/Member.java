package co.phoenixlab.discord.api.entities;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Member member = (Member) o;
        return Objects.equals(user, member.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }
}
