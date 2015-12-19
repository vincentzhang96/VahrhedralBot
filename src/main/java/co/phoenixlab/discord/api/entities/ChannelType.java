package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public enum ChannelType {
    @SerializedName("text")
    TEXT,
    @SerializedName("voice")
    VOICE;

}
