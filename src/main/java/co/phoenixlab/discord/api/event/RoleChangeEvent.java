package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Role;
import co.phoenixlab.discord.api.entities.Server;

public class RoleChangeEvent {

    private final Role role;
    private final Server server;
    private final RoleChange roleChange;

    public RoleChangeEvent(Role role, Server server, RoleChange roleChange) {
        this.role = role;
        this.server = server;
        this.roleChange = roleChange;
    }

    public Role getRole() {
        return role;
    }

    public Server getServer() {
        return server;
    }

    public RoleChange getRoleChange() {
        return roleChange;
    }

    public enum RoleChange {
        CREATED,
        DELETED,
        UPDATED
    }

}
