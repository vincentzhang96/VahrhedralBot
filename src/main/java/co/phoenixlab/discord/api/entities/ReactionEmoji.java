package co.phoenixlab.discord.api.entities;

public class ReactionEmoji {

    private String id;
    private String name;

    public ReactionEmoji() {
    }

    public ReactionEmoji(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isCustom() {
        return !"0".equals(id);
    }

    public String getEmojiCode() {
        return getName() + ":" + getId();
    }
}
