package co.phoenixlab.discord.commands.tempstorage;

public class TempServerConfig {

    private ServerTimeoutStorage serverTimeouts;

    private String serverId;

    private String voiceLogChannelId;

    private String customWelcomeMessage;

    private String customLeaveMessage;

    private String dnTrackChannel;

    private String customWelcomeChannel;

    private String customLeaveChannel;

    public TempServerConfig() {
    }

    public TempServerConfig(String serverId) {
        this.serverId = serverId;
    }

    public TempServerConfig(String serverId, ServerTimeoutStorage serverTimeouts, String voiceLogChannelId) {
        this.serverId = serverId;
        this.serverTimeouts = serverTimeouts;
        this.voiceLogChannelId = voiceLogChannelId;
    }

    public ServerTimeoutStorage getServerTimeouts() {
        return serverTimeouts;
    }

    public void setServerTimeouts(ServerTimeoutStorage serverTimeouts) {
        this.serverTimeouts = serverTimeouts;
    }

    public String getVoiceLogChannelId() {
        return voiceLogChannelId;
    }

    public void setVoiceLogChannelId(String voiceLogChannelId) {
        this.voiceLogChannelId = voiceLogChannelId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getCustomWelcomeMessage() {
        return customWelcomeMessage;
    }

    public void setCustomWelcomeMessage(String customWelcomeMessage) {
        this.customWelcomeMessage = customWelcomeMessage;
    }

    public String getCustomLeaveMessage() {
        return customLeaveMessage;
    }

    public void setCustomLeaveMessage(String customLeaveMessage) {
        this.customLeaveMessage = customLeaveMessage;
    }

    public String getDnTrackChannel() {
        return dnTrackChannel;
    }

    public void setDnTrackChannel(String dnTrackChannel) {
        this.dnTrackChannel = dnTrackChannel;
    }

    public String getCustomWelcomeChannel() {
        return customWelcomeChannel;
    }

    public void setCustomWelcomeChannel(String customWelcomeChannel) {
        this.customWelcomeChannel = customWelcomeChannel;
    }

    public String getCustomLeaveChannel() {
        return customLeaveChannel;
    }

    public void setCustomLeaveChannel(String customLeaveChannel) {
        this.customLeaveChannel = customLeaveChannel;
    }
}
