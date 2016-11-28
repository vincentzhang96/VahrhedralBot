package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class EmbedAuthor {

    private String name;
    private String url;
    @SerializedName("icon_url")
    private String iconUrl;
    @SerializedName("proxy_icon_url")
    private String proxyIconUrl;

    public EmbedAuthor() {
    }

    public EmbedAuthor(String name, String url, String iconUrl, String proxyIconUrl) {
        this.name = name;
        this.url = url;
        this.iconUrl = iconUrl;
        this.proxyIconUrl = proxyIconUrl;
    }

    public EmbedAuthor(String name, String url, String iconUrl) {
        this.name = name;
        this.url = url;
        this.iconUrl = iconUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getProxyIconUrl() {
        return proxyIconUrl;
    }

    public void setProxyIconUrl(String proxyIconUrl) {
        this.proxyIconUrl = proxyIconUrl;
    }
}
