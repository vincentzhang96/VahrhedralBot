package co.phoenixlab.discord.api.entities;

public class Role {

    private int position;
    private long permissions;
    private String name;
    private boolean managed;
    private String id;
    private boolean hoist;
    private int color;

    public Role() {
    }

    public Role(int position, long permissions, String name, boolean managed, String id, boolean hoist, int color) {
        this.position = position;
        this.permissions = permissions;
        this.name = name;
        this.managed = managed;
        this.id = id;
        this.hoist = hoist;
        this.color = color;
    }

    public int getPosition() {
        return position;
    }

    public long getPermissions() {
        return permissions;
    }

    public String getName() {
        return name;
    }

    public boolean isManaged() {
        return managed;
    }

    public String getId() {
        return id;
    }

    public boolean isHoist() {
        return hoist;
    }

    public int getColor() {
        return color;
    }
}