package co.phoenixlab.discord.api.entities;

public class Reaction {

    private int count;
    private boolean me;
    private ReactionEmoji emoji;


    public Reaction() {
    }

    public Reaction(int count, boolean me, ReactionEmoji emoji) {
        this.count = count;
        this.me = me;
        this.emoji = emoji;
    }

    public int getCount() {
        return count;
    }

    public boolean isMe() {
        return me;
    }

    public ReactionEmoji getEmoji() {
        return emoji;
    }
}
