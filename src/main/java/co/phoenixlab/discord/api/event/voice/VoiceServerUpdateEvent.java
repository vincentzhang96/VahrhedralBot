package co.phoenixlab.discord.api.event.voice;

import co.phoenixlab.discord.api.entities.voice.VoiceServerUpdate;

public class VoiceServerUpdateEvent {

    private final VoiceServerUpdate serverUpdate;

    public VoiceServerUpdateEvent(VoiceServerUpdate serverUpdate) {
        this.serverUpdate = serverUpdate;
    }

    public VoiceServerUpdate getServerUpdate() {
        return serverUpdate;
    }

    @Override
    public String toString() {
        return "VoiceServerUpdateEvent: " + serverUpdate.toString();
    }
}
