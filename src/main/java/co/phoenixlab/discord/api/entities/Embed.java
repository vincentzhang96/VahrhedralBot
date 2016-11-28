package co.phoenixlab.discord.api.entities;

public class Embed {

    public static String TYPE_RICH = "rich";

    private String title;
    private String type;
    private String description;
    private String url;
    private String timestamp;
    private int color;
    private EmbedFooter footer;
    private EmbedImage image;
    private EmbedThumbnail thumbnail;
    private EmbedVideo video;
    private EmbedProvider provider;
    private EmbedAuthor author;
    private EmbedField[] fields;

    public Embed() {
    }

    public Embed(String title, String type, String description, String url, String timestamp, int color,
                 EmbedFooter footer, EmbedImage image, EmbedThumbnail thumbnail, EmbedVideo video,
                 EmbedProvider provider, EmbedAuthor author, EmbedField[] fields) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.url = url;
        this.timestamp = timestamp;
        this.color = color;
        this.footer = footer;
        this.image = image;
        this.thumbnail = thumbnail;
        this.video = video;
        this.provider = provider;
        this.author = author;
        this.fields = fields;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public EmbedFooter getFooter() {
        return footer;
    }

    public void setFooter(EmbedFooter footer) {
        this.footer = footer;
    }

    public EmbedImage getImage() {
        return image;
    }

    public void setImage(EmbedImage image) {
        this.image = image;
    }

    public EmbedThumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(EmbedThumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public EmbedVideo getVideo() {
        return video;
    }

    public void setVideo(EmbedVideo video) {
        this.video = video;
    }

    public EmbedProvider getProvider() {
        return provider;
    }

    public void setProvider(EmbedProvider provider) {
        this.provider = provider;
    }

    public EmbedAuthor getAuthor() {
        return author;
    }

    public void setAuthor(EmbedAuthor author) {
        this.author = author;
    }

    public EmbedField[] getFields() {
        return fields;
    }

    public void setFields(EmbedField[] fields) {
        this.fields = fields;
    }
}
