package co.phoenixlab.discord.api.entities;

public class OutboundMessage {

    private final String content;
    private final boolean tts;
    private final String[] mentions;
    private final Embed embed;

    public OutboundMessage(String content, boolean tts, String[] mentions, Embed embed) {
        this.content = content;
        this.tts = tts;
        this.mentions = mentions;
        this.embed = embed;
    }

    public String getContent() {
        return content;
    }

    public boolean isTts() {
        return tts;
    }

    public String[] getMentions() {
        return mentions;
    }

    public Embed getEmbed() {
        return embed;
    }
}
