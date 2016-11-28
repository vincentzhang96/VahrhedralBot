package co.phoenixlab.discord.api.entities;

public class EmbedVideo {

    private String url;
    private int height;
    private int width;

    public EmbedVideo() {
    }

    public EmbedVideo(String url, int height, int width) {
        this.url = url;
        this.height = height;
        this.width = width;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
