package co.phoenixlab.discord;

import co.phoenixlab.discord.cfg.JoinLeaveLimits;
import co.phoenixlab.discord.dntrack.event.RegionDescriptor;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

    private String email;
    private String password;
    private String token;
    private String commandPrefix;
    private transient int prefixLength;
    private Set<String> blacklist;
    private Set<String> admins;
    private RegionDescriptor[] dnRegions;
    private boolean selfBot;
    private int exMentionTimeoutThreshold;
    private int exMentionBanThreshold;
    private JoinLeaveLimits jlLimit;

    public Configuration() {
        email = "";
        password = "";
        token = "";
        commandPrefix = "!";
        prefixLength = 0;
        blacklist = new HashSet<>();
        admins = new HashSet<>();
        dnRegions = new RegionDescriptor[0];
        selfBot = false;
        exMentionTimeoutThreshold = Integer.MAX_VALUE;
        exMentionBanThreshold = Integer.MAX_VALUE;
        jlLimit = new JoinLeaveLimits();
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

    public boolean isAdmin(String userId) {
        return admins.contains(userId);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSelfBot() {
        return selfBot;
    }

    public void setSelfBot(boolean selfBot) {
        this.selfBot = selfBot;
    }

    public RegionDescriptor[] getDnRegions() {
        return dnRegions;
    }

    public void setDnRegions(RegionDescriptor[] dnRegions) {
        this.dnRegions = dnRegions;
    }


    public int getExMentionTimeoutThreshold() {
        return exMentionTimeoutThreshold;
    }

    public void setExMentionTimeoutThreshold(int exMentionTimeoutThreshold) {
        this.exMentionTimeoutThreshold = exMentionTimeoutThreshold;
    }

    public int getExMentionBanThreshold() {
        return exMentionBanThreshold;
    }

    public void setExMentionBanThreshold(int exMentionBanThreshold) {
        this.exMentionBanThreshold = exMentionBanThreshold;
    }

    public JoinLeaveLimits getJlLimit() {
        return jlLimit;
    }
}
