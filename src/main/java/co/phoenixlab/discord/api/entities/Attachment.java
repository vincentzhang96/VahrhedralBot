package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class Attachment {

    private final String filename;
    private final int size;
    private final String id;
    @SerializedName("proxy_url")
    private final String proxyUrl;
    private final String url;

    public Attachment(String filename, int size, String id, String proxyUrl, String url) {
        this.filename = filename;
        this.size = size;
        this.id = id;
        this.proxyUrl = proxyUrl;
        this.url = url;
    }

    public Attachment() {
        this(null, 0, null, null, null);
    }

    public String getFilename() {
        return filename;
    }

    public int getSize() {
        return size;
    }

    public String getId() {
        return id;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return String.format("Attachment \"%s\" %s", filename, url);
    }
}
