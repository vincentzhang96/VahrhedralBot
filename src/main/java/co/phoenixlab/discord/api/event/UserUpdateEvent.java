package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.UserUpdate;

public class UserUpdateEvent {

    private final UserUpdate update;

    public UserUpdateEvent(UserUpdate update) {
        this.update = update;
    }

    public UserUpdate getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return "UserUpdateEvent:" + update;
    }
}
