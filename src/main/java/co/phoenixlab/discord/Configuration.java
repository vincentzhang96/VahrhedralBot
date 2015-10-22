package co.phoenixlab.discord;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

    private String email;
    private String password;
    private String commandPrefix;
    private Set<String> blacklist;

    public Configuration() {
        email = "";
        password = "";
        commandPrefix = "!";
        blacklist = new HashSet<>();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }
}
