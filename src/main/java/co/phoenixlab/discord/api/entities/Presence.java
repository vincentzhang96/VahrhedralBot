package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public enum Presence {
    /** User is online and present */
    @SerializedName("online")
    ONLINE("status.online"),
    /** User is online but away */
    @SerializedName("idle")
    AWAY("status.away"),
    /** User is do not disturb */
    @SerializedName("dnd")
    DO_NOT_DISTURB("status.dnd"),
    /** User is offline */
    @SerializedName("offline")
    OFFLINE("status.offline");

    private final String displayKey;

    Presence(String displayKey) {
        this.displayKey = displayKey;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    @Override
    public String toString() {
        return displayKey;
    }
}
