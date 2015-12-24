package co.phoenixlab.discord.api.event.voice;

import co.phoenixlab.discord.api.entities.voice.VoiceStateUpdate;

public class VoiceStateUpdateEvent {

    private final VoiceStateUpdate stateUpdate;

    public VoiceStateUpdateEvent(VoiceStateUpdate stateUpdate) {
        this.stateUpdate = stateUpdate;
    }

    public VoiceStateUpdate getStateUpdate() {
        return stateUpdate;
    }

    @Override
    public String toString() {
        return "VoiceStateUpdateEvent: " + stateUpdate.toString();
    }
}
