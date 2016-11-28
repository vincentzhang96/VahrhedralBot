package co.phoenixlab.discord.api.entities;

import com.google.gson.annotations.SerializedName;

public class EmbedFooter {

    private String text;
    @SerializedName("icon_url")
    private String iconUrl;
    @SerializedName("proxy_icon_url")
    private String proxyIconUrl;

    public EmbedFooter() {
    }

    public EmbedFooter(String text, String iconUrl) {
        this.text = text;
        this.iconUrl = iconUrl;
    }

    public EmbedFooter(String text, String iconUrl, String proxyIconUrl) {
        this.text = text;
        this.iconUrl = iconUrl;
        this.proxyIconUrl = proxyIconUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
