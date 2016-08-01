package co.phoenixlab.discord;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.*;
import co.phoenixlab.discord.commands.tempstorage.TempServerConfig;
import com.google.common.eventbus.Subscribe;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EventListener {

    private final VahrhedralBot bot;
    public Map<String, Consumer<MemberChangeEvent>> memberChangeEventListener;
    public Map<String, String> joinMessageRedirect;
    public Map<String, String> leaveMessageRedirect;
    public Set<String> ignoredServers;
    public int excessiveMentionThreshold;
    public Map<String, Deque<String>> newest = new HashMap<>();
    private Map<String, Consumer<Message>> messageListeners;
    private Map<String, Long> currentDateTimeLastUse = new HashMap<>();

    public EventListener(VahrhedralBot bot) {
        this.bot = bot;
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
        excessiveMentionThreshold = 5;
        messageListeners.put("mention-autotimeout", this::handleExcessiveMentions);
        messageListeners.put("invite-pm", this::onInviteLinkPrivateMessage);
        messageListeners.put("other-prefixes", this::onOtherTypesCommand);
//        messageListeners.put("date-time", this::currentDateTime);
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
            api.sendMessage("NOFLIPIt's " + ZonedDateTime.now().getYear() + ", `" + message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current date")) {
            if (cd) {
                return;
            }
            api.sendMessage("NOFLIPIt's " + DateTimeFormatter.ofPattern("MMM dd uuuu").format(ZonedDateTime.now()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current day")) {
            if (cd) {
                return;
            }
            api.sendMessage("NOFLIPIt's the " + th(ZonedDateTime.now().getDayOfMonth()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current time")) {
            if (cd) {
                return;
            }
            api.sendMessage("NOFLIPIt's " + DateTimeFormatter.ofPattern("HH:mm:ss z").format(ZonedDateTime.now()) + ", `" +
                    message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
        if (content.contains("current president")) {
            if (cd) {
                return;
            }
            api.sendMessage("NOFLIPIt's Bernie Trump, `" + message.getAuthor().getUsername() + "`.",
                channelId);
            currentDateTimeLastUse.put(channelId, time);
        }
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
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        boolean isCommand = message.getContent().startsWith(bot.getConfig().getCommandPrefix());
        if (isSelfBot() && isCommand) {
            bot.getMainCommandDispatcher().handleCommand(message);
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

    private void handleMention(Message message) {
        if (isSelfBot()) {
            return;
        }
        if (!bot.getMainCommandDispatcher().active().get()) {
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
//        if (Duration.between(ZonedDateTime.
//                from(Commands.DATE_TIME_FORMATTER.parse(member.getJoinedAt())),
//                ZonedDateTime.now()).abs().compareTo(Duration.ofDays(3)) > 0) {
//            return;
//        }
        Set<User> unique = new HashSet<>();
        Collections.addAll(unique, message.getMentions());
        if (unique.size() > excessiveMentionThreshold) {
            bot.getCommands().getModCommands().applyTimeout(bot.getApiClient().getClientUser(), channel,
                server, author, Duration.ofHours(1));
//            bot.getCommands().getModCommands().banImpl(author.getId(), author.getUsername(),
//                    server.getId(), channel.getId());
            bot.getApiClient().sendMessage(String.format("`%s#%s` (%s) has been banned for mention spam",
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
    public void onLogIn(LogInEvent logInEvent) {
        bot.getCommands().onLogIn(logInEvent);
    }

}
