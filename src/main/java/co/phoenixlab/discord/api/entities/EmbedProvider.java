package co.phoenixlab.discord.api.entities;

public class EmbedProvider {

    private String name;
    private String url;

    public EmbedProvider(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public EmbedProvider() {
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
}
