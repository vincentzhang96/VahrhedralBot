package co.phoenixlab.discord.cfg;

import metrics_influxdb.HttpInfluxdbProtocol;

import java.net.MalformedURLException;
import java.net.URL;

public class InfluxDbConfig {

    private String server;
    private transient URL serverUrl;
    private String username;
    private String password;
    private String database;

    public InfluxDbConfig(String server, String username, String password, String database) {
        this.server = server;
        try {
            this.serverUrl = new URL(server);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Bad config", e);
        }
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public HttpInfluxdbProtocol toInfluxDbProtocolConfig() {
        if (serverUrl == null) {
            try {
                this.serverUrl = new URL(server);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad config", e);
            }
        }
        return new HttpInfluxdbProtocol(
            getProtocol(),
            getServer(),
            getPort(),
            getUsername(),
            getPassword(),
            getDatabase()
        );
    }

    public InfluxDbConfig() {
        this("http://127.0.0.1", null, null, "metrics");
    }

    public String getHost() {
        return serverUrl.getHost();
    }

    public int getPort() {
        int port = serverUrl.getPort();
        if (port == -1) {
            String proto = getProtocol().toLowerCase();
            if ("http".equals(proto)) {
                port = 80;
            } else if ("https".equals(proto)) {
                port = 443;
            } else {
                throw new RuntimeException("Unknown port for protocol " + proto);
            }
        }
        return port;
    }

    public String getProtocol() {
        return serverUrl.getProtocol();
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public URL getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(URL serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
