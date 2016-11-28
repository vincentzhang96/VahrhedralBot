package co.phoenixlab.discord.api.entities;

import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.util.ApiUtils;

import java.net.URL;
import java.util.Objects;

public class User {

    /**
     * The User's display name (can change over time)
     */
    private String username;

    /**
     * The User's identifier (does not change, not necessarily unqiue?)
     */
    private final String id;

    /**
     * An identifier used to discriminate between two users with the same name and/or id
     */
    private String discriminator;

    /**
     * The user's avatar ID
     */
    private String avatar;

    /**
     * The URL where the avatar can be downloaded from
     */
    private URL avatarUrl;

    private boolean bot;

    public User(String username, String id, String discriminator, String avatar) {
        this.username = username;
        this.id = id;
        this.discriminator = discriminator;
        this.avatar = avatar;
    }

    public User() {
        this(null, null, null, null);
    }

    /**
     * @return {@link #username}
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return {@link #id}
     */
    public String getId() {
        return id;
    }

    /**
     * @return {@link #discriminator}
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * @return {@link #avatar}
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * @return {@link #avatarUrl}
     */
    public URL getAvatarUrl() {
        if (avatarUrl == null) {
            if (avatar != null) {
                avatarUrl = ApiUtils.url(String.format(ApiConst.AVATAR_URL_PATTERN, id, avatar));
            } else {
                avatarUrl = ApiUtils.url(String.format(ApiConst.AVATAR_URL_PATTERN, id, "NO_AVATAR.JPG"));
            }
        }
        return avatarUrl;
    }

    public String getAvatarUrlStringOrNull() {
        if (avatar == null) {
            return null;
        }
        return getAvatarUrl().toExternalForm();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
        avatarUrl = null;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    public String toString() {
        return String.format("User[username:\"%s\",id:\"%s\",avatar:\"%s\",avatarUrl:\"%s\"]",
                username, id, avatar, getAvatarUrl().toExternalForm());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isBot() {
        return bot;
    }
}
