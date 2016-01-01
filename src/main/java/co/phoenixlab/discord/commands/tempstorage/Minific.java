package co.phoenixlab.discord.commands.tempstorage;

public class Minific {

    private final String id;
    private String authorId;
    private final String date;
    private String content;

    public Minific(String id, String authorId, String date, String content) {
        this.id = id;
        this.authorId = authorId;
        this.date = date;
        this.content = content;
    }

    public Minific() {
        this(null, null, null, null);
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
