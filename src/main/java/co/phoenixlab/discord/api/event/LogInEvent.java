package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.ReadyMessage;

public class LogInEvent {

    private final ReadyMessage readyMessage;

    public LogInEvent(ReadyMessage readyMessage) {
        this.readyMessage = readyMessage;
    }

    public ReadyMessage getReadyMessage() {
        return readyMessage;
    }
}
