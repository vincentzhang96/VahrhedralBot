package co.phoenixlab.discord;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

    private String email;
    private String password;
    private String commandPrefix;
    private transient int prefixLength;
    private Set<String> blacklist;
    private Set<String> admins;

    public Configuration() {
        email = "";
        password = "";
        commandPrefix = "!";
        prefixLength = 0;
        blacklist = new HashSet<>();
        admins = new HashSet<>();
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

    public int getPrefixLength() {
        //  Lazy load after we load from config file
        if (prefixLength == 0) {
            prefixLength = commandPrefix.length();
        }
        return prefixLength;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
        prefixLength = this.commandPrefix.length();
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public Set<String> getAdmins() {
        return admins;
    }
}
