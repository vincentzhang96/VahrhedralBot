package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class EmbedThumbnail {

    private String url;
    @SerializedName("proxy_url")
    private String proxyUrl;
    private int height;
    private int width;

    public EmbedThumbnail(String url, String proxyUrl, int height, int width) {
        this.url = url;
        this.proxyUrl = proxyUrl;
        this.height = height;
        this.width = width;
    }

    public EmbedThumbnail(String url, int height, int width) {
        this.url = url;
        this.height = height;
        this.width = width;
    }

    public EmbedThumbnail(String url) {
        this.url = url;
    }

    public EmbedThumbnail() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
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
