package co.phoenixlab.discord;

import co.phoenixlab.discord.cfg.DiscordApiClientConfig;
import co.phoenixlab.discord.cfg.JoinLeaveLimits;
import co.phoenixlab.discord.cfg.RedisConfig;
import co.phoenixlab.discord.dntrack.event.RegionDescriptor;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

    private String email;
    private String password;
    private String token;
    private String commandPrefix;
    private transient int prefixLength;
    private final Set<String> blacklist;
    private final Set<String> admins;
    private RegionDescriptor[] dnRegions;
    private boolean selfBot;
    private int exMentionTimeoutThreshold;
    private int exMentionBanThreshold;
    private int exMentionTemporalThreshold;
    private long exMentionPeriodMs;
    private long exMentionCacheEvictionTimeMs;
    private final JoinLeaveLimits jlLimit;
    private DiscordApiClientConfig apiClientConfig;
    private RedisConfig redis;

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
        apiClientConfig = new DiscordApiClientConfig();
        redis = new RedisConfig();
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

    public int getExMentionTemporalThreshold() {
        return exMentionTemporalThreshold;
    }

    public void setExMentionTemporalThreshold(int exMentionTemporalThreshold) {
        this.exMentionTemporalThreshold = exMentionTemporalThreshold;
    }

    public long getExMentionPeriodMs() {
        return exMentionPeriodMs;
    }

    public void setExMentionPeriodMs(long exMentionPeriodMs) {
        this.exMentionPeriodMs = exMentionPeriodMs;
    }

    public long getExMentionCacheEvictionTimeMs() {
        return exMentionCacheEvictionTimeMs;
    }

    public void setExMentionCacheEvictionTimeMs(long exMentionCacheEvictionTimeMs) {
        this.exMentionCacheEvictionTimeMs = exMentionCacheEvictionTimeMs;
    }

    public DiscordApiClientConfig getApiClientConfig() {
        return apiClientConfig;
    }

    public void setApiClientConfig(DiscordApiClientConfig apiClientConfig) {
        this.apiClientConfig = apiClientConfig;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }
}
