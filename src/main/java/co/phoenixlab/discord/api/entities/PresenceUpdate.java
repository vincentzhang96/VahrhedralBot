package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PresenceUpdate {

    private Game game;

    @SerializedName("guild_id")
    private String guildId;

    private List<String> roles;

    private Presence status;

    private User user;

    public PresenceUpdate() {
    }

    public Game getGame() {
        return game;
    }

    public String getServerId() {
        return guildId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Presence getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }
}
