package co.phoenixlab.discord.api.entities.voice;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Server;
import com.google.gson.annotations.SerializedName;

public class VoiceServerUpdate {

    private final String endpoint;

    @SerializedName("guild_id")
    private final String serverId;

    private transient Server server = DiscordApiClient.NO_SERVER;

    private final String token;

    public VoiceServerUpdate(String endpoint, String serverId, String token) {
        this.endpoint = endpoint;
        this.serverId = serverId;
        this.token = token;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getServerId() {
        return serverId;
    }

    public Server getServer() {
        return server;
    }

    public String getToken() {
        return token;
    }

    public void fix(DiscordApiClient apiClient) {
        server = apiClient.getServerByID(serverId);
    }

    @Override
    public String toString() {
        return "VoiceServerUpdate{" +
                "endpoint='" + endpoint + '\'' +
                ", serverId='" + serverId + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
