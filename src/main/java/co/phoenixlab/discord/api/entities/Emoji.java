package co.phoenixlab.discord.api.entities;

public class Emoji extends ReactionEmoji {

    private long[] roles;
    private boolean requireColons;
    private boolean managed;

    public Emoji(String id, String name, long[] roles, boolean requireColons, boolean managed) {
        super(id, name);
        this.roles = roles;
        this.requireColons = requireColons;
        this.managed = managed;
    }

    public Emoji() {
    }

    public long[] getRoles() {
        return roles;
    }

    public boolean isRequireColons() {
        return requireColons;
    }

    public boolean isManaged() {
        return managed;
    }

}
