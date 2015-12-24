package co.phoenixlab.discord.api.entities;

public class UserUpdate {

    private boolean verified;

    private String id;

    private String avatar;

    private String email;

    private String username;

    private String discriminator;

    public UserUpdate() {
    }

    public boolean isVerified() {
        return verified;
    }

    public String getId() {
        return id;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    @Override
    public String toString() {
        return "UserUpdate{" +
                "verified=" + verified +
                ", id='" + id + '\'' +
                ", avatar='" + avatar + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", discriminator='" + discriminator + '\'' +
                '}';
    }
}
