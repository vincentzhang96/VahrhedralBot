package co.phoenixlab.discord.api.entities;

public class OutboundMessage {

    private final String content;
    private final boolean tts;
    private final String[] mentions;

    public OutboundMessage(String content, boolean tts, String[] mentions) {
        this.content = content;
        this.tts = tts;
        this.mentions = mentions;
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
}
