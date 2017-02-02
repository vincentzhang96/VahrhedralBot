package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

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

    @SerializedName("joined_at")
    private String joinedAt;

    private String nick;

    public Member(User user, List<String> roles, String joinedAt, String nick) {
        this.user = user;
        this.roles = roles;
        this.joinedAt = joinedAt;
        this.nick = nick;
    }

    public Member() {
        this(null, null, null, null);
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

    public String getJoinedAt() {
        return joinedAt;
    }

    public String getNick() {
        return nick;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getNickOrUsername() {
        if (nick == null || nick.isEmpty()) {
            return getUser().getUsername();
        }
        return nick;
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
