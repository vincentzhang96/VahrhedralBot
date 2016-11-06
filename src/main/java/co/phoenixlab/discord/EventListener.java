package co.phoenixlab.discord;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.*;
import co.phoenixlab.discord.cfg.JoinLeaveLimits;
import co.phoenixlab.discord.commands.tempstorage.DnTrackInfo;
import co.phoenixlab.discord.commands.tempstorage.TempServerConfig;
import co.phoenixlab.discord.dntrack.StatusTracker;
import co.phoenixlab.discord.dntrack.VersionTracker;
import co.phoenixlab.discord.dntrack.event.RegionDescriptor;
import co.phoenixlab.discord.dntrack.event.StatusChangeEvent;
import co.phoenixlab.discord.dntrack.event.VersionUpdateEvent;
import co.phoenixlab.discord.util.RateLimiter;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static co.phoenixlab.discord.dntrack.event.StatusChangeEvent.StatusChange.WENT_OFFLINE;

public class EventListener {

    public static final DateTimeFormatter UPDATE_FORMATTER = DateTimeFormatter.ofPattern("M/d HH:mm z");
    private static final Pattern SIGNATURE_MATCHER = Pattern.compile("[0-9a-fA-F]{29}\\b");
    private final VahrhedralBot bot;
    private final ScheduledExecutorService executorService;
    public Map<String, Consumer<MemberChangeEvent>> memberChangeEventListener;
    public Map<String, String> joinMessageRedirect;
    public Map<String, String> leaveMessageRedirect;
    public Set<String> ignoredServers;
    public Map<String, Deque<String>> newest = new HashMap<>();
    private Map<String, VersionTracker> versionTrackers = new HashMap<>();
    private Map<String, StatusTracker> statusTrackers = new HashMap<>();
    private Map<String, Consumer<Message>> messageListeners;
    private Map<String, Long> currentDateTimeLastUse = new HashMap<>();
    private LoadingCache<String, RateLimiter> joinLeaveLimiters;
    private LoadingCache<String, RateLimiter> dnnacdMentionLimiters;


    public EventListener(VahrhedralBot bot) {
        this.bot = bot;
        executorService = Executors.newSingleThreadScheduledExecutor();
        messageListeners = new HashMap<>();
        memberChangeEventListener = new HashMap<>();
        joinMessageRedirect = new HashMap<>();
        leaveMessageRedirect = new HashMap<>();
        ignoredServers = new HashSet<>();
        messageListeners.put("mention-bot", message -> {
            User me = bot.getApiClient().getClientUser();
            for (User user : message.getMentions()) {
                if (me.equals(user)) {
                    handleMention(message);
                    return;
                }
            }
        });
        messageListeners.put("mention-autotimeout", this::handleExcessiveMentions);
        messageListeners.put("invite-pm", this::onInviteLinkPrivateMessage);
        messageListeners.put("other-prefixes", this::onOtherTypesCommand);
//        dnnacdRecentMessages = EvictingQueue.create(10);
        messageListeners.put("date-time", this::currentDateTime);
    }

    public static String createJoinLeaveMessage(User user, Server server, String fmt) {
        return fmt.
            replace("$n", user.getUsername()).
            replace("$d", user.getDiscriminator()).
            replace("$i", user.getId()).
            replace("$s", server.getName()).
            replace("$t", DateTimeFormatter.ofPattern("HH:mm:ss z").format(ZonedDateTime.now())).
            replace("$m", "<@" + user.getId() + ">");
    }

    private RateLimiter buildLimiter(String key) {
        JoinLeaveLimits lim = bot.getConfig().getJlLimit();
        return new RateLimiter("JL-" + key, lim.getRlPeriodMs(), lim.getRmMaxCharges());
    }

    private RateLimiter buildMentionLimiter(String key) {
        Configuration config = bot.getConfig();
        return new RateLimiter("@M-" + key, config.getExMentionPeriodMs(), config.getExMentionTemporalThreshold());
    }

    private void currentDateTime(Message message) {
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        String channelId = message.getChannelId();
        boolean cd = false;
        if (currentDateTimeLastUse.containsKey(channelId)) {
            long time = currentDateTimeLastUse.get(channelId);
            if (time >= System.currentTimeMillis()) {
                currentDateTimeLastUse.remove(channelId);
            } else if (System.currentTimeMillis() - time < TimeUnit.SECONDS.toMillis(30)) {
                cd = true;
            }
        }
        long time = System.currentTimeMillis();
        DiscordApiClient api = bot.getApiClient();
        String content = message.getContent().toLowerCase();
        if (content.contains("current year")) {
            if (cd) {
                return;
            }
            api.sendMessage("It's " + ZonedDateTime.now().getYear() + ", `" + message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current date")) {
            if (cd) {
                return;
            }
            api.sendMessage("It's " + DateTimeFormatter.ofPattern("MMM dd uuuu").format(ZonedDateTime.now()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current day")) {
            if (cd) {
                return;
            }
            api.sendMessage("It's the " + th(ZonedDateTime.now().getDayOfMonth()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current time")) {
            if (cd) {
                return;
            }
            api.sendMessage("It's " + DateTimeFormatter.ofPattern("HH:mm:ss z").format(ZonedDateTime.now()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
//        if (content.contains("current president")) {
//            if (cd) {
//                return;
//            }
//            api.sendMessage("It's Bernie Trump, `" + message.getAuthor().getUsername() + "`.",
//                channelId);
//            currentDateTimeLastUse.put(channelId, time);
//        }
    }

    private String th(int i) {
        switch (i) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return Integer.toString(i) + "th";
        }
    }

    @Subscribe
    public void onVersionChange(VersionUpdateEvent event) {
        if (isSelfBot()) {
            return;
        }
        if (!event.isInitial()) {
            DiscordApiClient api = bot.getApiClient();
            for (Server server : api.getServers()) {
                TempServerConfig config = bot.getCommands().getModCommands().getServerStorage().get(server.getId());
                if (config != null) {
                    String chid = config.getDnTrackChannel();
                    if (chid != null) {
                        Localizer loc = bot.getLocalizer();
                        api.sendMessage(loc.localize("commands.dn.track.version.updated",
                            loc.localize(event.getRegion().getRegionNameKey()),
                            event.getOldVersion(),
                            event.getNewVersion(),
                            UPDATE_FORMATTER.format(ZonedDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault()))),
                            chid);
                    }
                }
            }
        }
        Map<String, DnTrackInfo> regions = bot.getDnTrackStorage().getRegions();
        DnTrackInfo info = regions.get(event.getRegion().getRegionCode());
        if (info == null) {
            info = new DnTrackInfo();
            info.setPatchVersion(-1);
            info.setServerStatus(-1);
            regions.put(event.getRegion().getRegionCode(), info);
        }
        info.setLastPatchTime(event.getTimestamp().toEpochMilli());
        info.setPatchVersion(event.getNewVersion());
        bot.saveDnTrackInfo();
    }

    @Subscribe
    public void onStatusChange(StatusChangeEvent event) {
        if (isSelfBot()) {
            return;
        }
        if (!event.isInitial()) {
            DiscordApiClient api = bot.getApiClient();
            for (Server server : api.getServers()) {
                TempServerConfig config = bot.getCommands().getModCommands().getServerStorage().get(server.getId());
                if (config != null) {
                    String chid = config.getDnTrackChannel();
                    if (chid != null) {
                        Localizer loc = bot.getLocalizer();
                        api.sendMessage(loc.localize("commands.dn.track.status.updated",
                            loc.localize(event.getRegion().getRegionNameKey()),
                            loc.localize(event.getChange() == WENT_OFFLINE ?
                                "commands.dn.track.status.down" : "commands.dn.track.status.up"),
                            UPDATE_FORMATTER.format(ZonedDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault()))),
                            chid);
                    }
                }
            }
        }
        Map<String, DnTrackInfo> regions = bot.getDnTrackStorage().getRegions();
        DnTrackInfo info = regions.get(event.getRegion().getRegionCode());
        if (info == null) {
            info = new DnTrackInfo();
            info.setPatchVersion(-1);
            info.setServerStatus(-1);
            regions.put(event.getRegion().getRegionCode(), info);
        }
        info.setLastStatusChangeTime(event.getTimestamp().toEpochMilli());
        info.setServerStatus(event.getChange() == WENT_OFFLINE ? 0 : 1);
        bot.saveDnTrackInfo();
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        boolean isCommand = message.getContent().startsWith(bot.getConfig().getCommandPrefix());
        if (isSelfBot() && isCommand) {
            if (message.getAuthor().equals(bot.getApiClient().getClientUser())) {
                bot.getMainCommandDispatcher().handleCommand(message);
            }
            return;
        }
        if (bot.getConfig().getBlacklist().contains(message.getAuthor().getId())) {
            return;
        }
        Channel channel = bot.getApiClient().getChannelById(message.getChannelId());
        if (channel != DiscordApiClient.NO_CHANNEL && channel.getParent() != null) {
            if (ignoredServers.contains(channel.getParent().getId())) {
                return;
            }
        }
        if (isCommand && !message.getAuthor().isBot()) {
            bot.getMainCommandDispatcher().handleCommand(message);
            return;
        }
        messageListeners.values().forEach(c -> c.accept(message));
    }

    @Subscribe
    public void onMessageDelete(MessageDeleteEvent messageDeleteEvent) {


    }

    private void handleMention(Message message) {
        if (isSelfBot()) {
            return;
        }
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        if (message.getAuthor().isBot()) {
            return;
        }
        String otherId = message.getAuthor().getId();
        bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.mention.response",
            message.getAuthor().getUsername()),
            message.getChannelId(), new String[]{otherId});
    }

    private void handleExcessiveMentions(Message message) {
        if (isSelfBot()) {
            return;
        }
        if (message.getMentions().length == 0) {
            return;
        }
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        User author = message.getAuthor();
        if (bot.getConfig().isAdmin(author.getId())) {
            return;
        }
        Channel channel = bot.getApiClient().getChannelById(message.getChannelId());
        Server server;
        if (channel != DiscordApiClient.NO_CHANNEL && channel.getParent() != null) {
            server = channel.getParent();
        } else {
            return;
        }
        if (!server.getId().equals("106293726271246336")) {
            return;
        }
        Member member = bot.getApiClient().getUserMember(author, server);
        if (member == DiscordApiClient.NO_MEMBER) {
            return;
        }
        Set<User> unique = new HashSet<>();
        Collections.addAll(unique, message.getMentions());

        if (dnnacdMentionLimiters == null) {
            dnnacdMentionLimiters = CacheBuilder.newBuilder()
                .expireAfterAccess(bot.getConfig().getExMentionCacheEvictionTimeMs(),
                    TimeUnit.MILLISECONDS)
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(String key) throws Exception {
                        return buildMentionLimiter(key);
                    }
                });
        }

        Matcher matcher = SIGNATURE_MATCHER.matcher(message.getContent());
        int numHashes = matcher.groupCount();
        int size = unique.size();
        boolean overBanThreshold = size >= bot.getConfig().getExMentionBanThreshold() ||
            (Math.abs(numHashes - size) >= 1 && numHashes >= 4);
        boolean overTimeoutThreshold = size >= bot.getConfig().getExMentionTimeoutThreshold() ||
            (Math.abs(numHashes - size) >= 1 && numHashes >= 2);
        try {
            RateLimiter limiter = dnnacdMentionLimiters.get(author.getId());
            for (int i = 0; i < size; ++i) {
                if (limiter.tryMark() != 0) {
                    overTimeoutThreshold = true;
                    break;
                }
            }
        } catch (ExecutionException e) {
            VahrhedralBot.LOGGER.warn("Failed to load rate limiter for " + author.getId(), e);
        }
        if (overBanThreshold) {
            if (bot.getCommands().getModCommands().banChecked(channel, bot.getApiClient().getClientUser(),
                author, server)) {
                bot.getApiClient().sendMessage(String.format("`%s#%s` (%s) has been banned for mention spam",
                    author.getUsername(), author.getDiscriminator(), author.getId()), channel);
            } else {
                bot.getApiClient().sendMessage(String.format("**[ERROR]** Failed to ban `%s#%s` (%s) for mention spam",
                    author.getUsername(), author.getDiscriminator(), author.getId()), channel);
            }
        } else if (overTimeoutThreshold) {
            bot.getCommands().getModCommands().applyTimeout(bot.getApiClient().getClientUser(), channel,
                server, author, Duration.ofHours(8));
            bot.getApiClient().sendMessage(String.format("`%s#%s` (%s) has been timed out for mention spam. " +
                    "If this is a mistake, please contact a moderator",
                author.getUsername(), author.getDiscriminator(), author.getId()), channel);
        }
    }

    @Subscribe
    public void onServerJoinLeave(ServerJoinLeaveEvent event) {
        if (isSelfBot()) {
            return;
        }
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        if (event.isJoin()) {
            Server server = event.getServer();
            //  Default channel has same ID as server
            Channel channel = bot.getApiClient().getChannelById(server.getId());
            if (channel != DiscordApiClient.NO_CHANNEL) {
//                bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.on_join.response",
//                        bot.getApiClient().getClientUser().getUsername()),
//                        channel.getId());
            }
        }
    }

    @Subscribe
    public void onMemberChangeEvent(MemberChangeEvent event) {
        if (isSelfBot()) {
            return;
        }
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        Server server = event.getServer();
        //  Default channel has same ID as server
        Channel channel = bot.getApiClient().getChannelById(server.getId());
        if (channel == DiscordApiClient.NO_CHANNEL) {
            return;
        }
        String cid = channel.getId();
        String key;
        if (event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED) {
            key = "message.new_member.response";
            cid = SafeNav.of(joinMessageRedirect.get(server.getId())).orElse(cid);
        } else if (event.getMemberChange() == MemberChangeEvent.MemberChange.DELETED) {
            key = "message.member_quit.response";
            cid = SafeNav.of(leaveMessageRedirect.get(server.getId())).orElse(cid);
        } else {
            return;
        }
        User user = event.getMember().getUser();

        //  167264528537485312 dnnacd #activity-log
        //  avoid duplicates
        if (event.getServer().getId().equals("106293726271246336") && !"167264528537485312".equals(cid)) {
            bot.getApiClient().sendMessage(bot.getLocalizer().localize(key,
                user.getUsername(),
                user.getId(),
                user.getDiscriminator(),
                DateTimeFormatter.ofPattern("HH:mm:ss z").format(ZonedDateTime.now())),
                "167264528537485312");
        }

        //  Join-leave spam prevention
        if (event.getServer().getId().equals("106293726271246336")) {
            if (joinLeaveLimiters == null) {
                joinLeaveLimiters = CacheBuilder.newBuilder()
                    .expireAfterAccess(bot.getConfig().getJlLimit().getCacheEvictionTimeMs(),
                        TimeUnit.MILLISECONDS)
                    .build(new CacheLoader<String, RateLimiter>() {
                        @Override
                        public RateLimiter load(String key) throws Exception {
                            return buildLimiter(key);
                        }
                    });
            }
            try {
                RateLimiter limiter = joinLeaveLimiters.get(user.getId());
                if (limiter.tryMark() != 0) {
                    //  Join-leave spam detected
                    bot.getCommands().getModCommands().banChecked(channel,
                        bot.getApiClient().getClientUser(), user, server);
                    bot.getApiClient().sendMessage(String.format("`%s#%s` (%s) has been banned for join-leave spam",
                        user.getUsername(), user.getDiscriminator(), user.getId()), channel);
                    return;
                }
            } catch (ExecutionException e) {
                VahrhedralBot.LOGGER.warn("Failed to load rate limiter for " + user.getId(), e);
            }
        }

        TempServerConfig config = bot.getCommands().getModCommands().getServerStorage().get(server.getId());
        String customWelcomeMessage = SafeNav.of(config).get(TempServerConfig::getCustomWelcomeMessage);
        String customLeaveMessage = SafeNav.of(config).get(TempServerConfig::getCustomLeaveMessage);
        if (event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED && customWelcomeMessage != null) {
            if (!customWelcomeMessage.isEmpty()) {
                bot.getApiClient().sendMessage(createJoinLeaveMessage(user, server, customWelcomeMessage),
                    cid);
            }
        } else if (event.getMemberChange() == MemberChangeEvent.MemberChange.DELETED && customLeaveMessage != null) {
            if (!customLeaveMessage.isEmpty()) {
                bot.getApiClient().sendMessage(createJoinLeaveMessage(user, server, customLeaveMessage),
                    cid);
            }
        } else {
            bot.getApiClient().sendMessage(bot.getLocalizer().localize(key,
                user.getUsername(),
                user.getId(),
                user.getDiscriminator(),
                DateTimeFormatter.ofPattern("HH:mm:ss z").format(ZonedDateTime.now())),
                cid);
        }
        memberChangeEventListener.values().forEach(c -> c.accept(event));
    }

    public boolean isJoin(MemberChangeEvent event) {
        return event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED;
    }

    @Subscribe
    public void onMemberPresenceUpdate(PresenceUpdateEvent event) {
        if (isSelfBot()) {
            return;
        }
        //  not dnnacd
        if (!event.getServer().getId().equals("106293726271246336")) {
            return;
        }
        User user = event.getPresenceUpdate().getUser();
        if (user.getUsername() != null && !event.getOldUsername().equals(user.getUsername())) {
            //  167264528537485312 dnnacd #activity-log
            bot.getApiClient().sendMessage(String.format("`%s` changed name to `%s` (%s)",
                event.getOldUsername(), user.getUsername(),
                user.getId()), "167264528537485312");
        }
    }

    private void onInviteLinkPrivateMessage(Message message) {
        if (isSelfBot()) {
            return;
        }
        if (!message.isPrivateMessage()) {
            return;
        }
        if (message.getContent().startsWith("https://discord.gg/")) {
            bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.private.invite"), message.getChannelId());
            User author = message.getAuthor();
            VahrhedralBot.LOGGER.info("Received invite link from {}#{} ({}), rejecting: {}",
                author.getUsername(), author.getDiscriminator(), author.getId(), message.getContent());
        }
    }

    private void onOtherTypesCommand(Message message) {
        if (isSelfBot()) {
            return;
        }
        if (!message.isPrivateMessage()) {
            return;
        }
        String content = message.getContent().toLowerCase();
        if (content.startsWith("~") || content.startsWith("!") || content.startsWith("-")) {
            bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.private.misc",
                bot.getConfig().getCommandPrefix()), message.getChannelId());
        }
    }

    private boolean isSelfBot() {
        return bot.getConfig().isSelfBot();
    }

    public void registerMessageListner(String name, Consumer<Message> listener) {
        messageListeners.put(Objects.requireNonNull(name), Objects.requireNonNull(listener));
    }

    public void deleteMessageListener(String name) {
        messageListeners.remove(Objects.requireNonNull(name));
    }

    @Subscribe
    public void onBanChange(ServerBanChangeEvent event) {
        if (isSelfBot()) {
            return;
        }
        Server server = event.getServer();
        User user = event.getUser();
        if (server == null || user == null) {
            return;
        }
        if (server.getId().equals("106293726271246336")) {
            //  167264528537485312 dnnacd #activity-log
            if (event.getChange() == ServerBanChangeEvent.BanChange.ADDED) {
                bot.getApiClient().sendMessage(String.format("`%s` (%s) was banned",
                    user.getUsername(),
                    user.getId()), "167264528537485312");
            } else if (event.getChange() == ServerBanChangeEvent.BanChange.DELETED) {
                bot.getApiClient().sendMessage(String.format("`%s` (%s) was unbanned",
                    user.getUsername(),
                    user.getId()), "167264528537485312");
            }
        }
    }

    @Subscribe
    public void onLogIn(LogInEvent logInEvent) {
        if (!bot.getConfig().isSelfBot() && versionTrackers.isEmpty()) {
            for (RegionDescriptor regionDescriptor : bot.getConfig().getDnRegions()) {

                if (!Strings.isNullOrEmpty(regionDescriptor.getVersionCheckUrl())) {
                    VersionTracker versionTracker = new VersionTracker(regionDescriptor,
                        bot.getApiClient().getEventBus());
                    Map<String, DnTrackInfo> regions = bot.getDnTrackStorage().getRegions();
                    DnTrackInfo info = regions.get(regionDescriptor.getRegionCode());
                    if (info == null) {
                        info = new DnTrackInfo();
                        info.setPatchVersion(-1);
                        info.setServerStatus(-1);
                        regions.put(regionDescriptor.getRegionCode(), info);
                    }
                    if (info.getLastPatchTime() != 0) {
                        versionTracker.lastChangeEvent().set(new VersionUpdateEvent(false, regionDescriptor,
                            -1,
                            info.getPatchVersion(),
                            Instant.ofEpochMilli(info.getLastPatchTime())));
                    }
                    versionTracker.currentVersion().set(info.getPatchVersion());
                    versionTrackers.put(regionDescriptor.getRegionCode(),
                        versionTracker);
                    VahrhedralBot.LOGGER.info("Registered version tracker for " + regionDescriptor.getRegionCode());
                    executorService.scheduleAtFixedRate(versionTracker, 0, 1, TimeUnit.MINUTES);
                }
                if (!Strings.isNullOrEmpty(regionDescriptor.getStatusCheckUrl())) {
                    StatusTracker statusTracker = new StatusTracker(regionDescriptor,
                        bot.getApiClient().getEventBus());
                    Map<String, DnTrackInfo> regions = bot.getDnTrackStorage().getRegions();
                    DnTrackInfo info = regions.get(regionDescriptor.getRegionCode());
                    if (info == null) {
                        info = new DnTrackInfo();
                        info.setPatchVersion(-1);
                        info.setServerStatus(-1);
                        regions.put(regionDescriptor.getRegionCode(), info);
                    }
                    if (info.getLastStatusChangeTime() != 0) {
                        statusTracker.lastChangeEvent().set(new StatusChangeEvent(false, regionDescriptor,
                            null,
                            Instant.ofEpochMilli(info.getLastStatusChangeTime())));
                    }
                    statusTracker.currentStatus().set(info.getServerStatus());
                    statusTrackers.put(regionDescriptor.getRegionCode(), statusTracker);
                    VahrhedralBot.LOGGER.info("Registered status tracker for " + regionDescriptor.getRegionCode());
                    executorService.scheduleAtFixedRate(statusTracker, 0, 1, TimeUnit.MINUTES);
                }
            }
        }
        bot.getCommands().onLogIn(logInEvent);
    }

    public Map<String, VersionTracker> getVersionTrackers() {
        return versionTrackers;
    }
}
