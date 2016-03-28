package co.phoenixlab.discord;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.LogInEvent;
import co.phoenixlab.discord.api.event.MemberChangeEvent;
import co.phoenixlab.discord.api.event.MessageReceivedEvent;
import co.phoenixlab.discord.api.event.ServerJoinLeaveEvent;
import co.phoenixlab.discord.commands.Commands;
import com.google.common.eventbus.Subscribe;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

public class EventListener {

    private final VahrhedralBot bot;

    private Map<String, Consumer<Message>> messageListeners;

    public Map<String, Consumer<MemberChangeEvent>> memberChangeEventListener;

    public Map<String, String> joinMessageRedirect;

    public Set<String> ignoredServers;

    public int excessiveMentionThreshold;

    public EventListener(VahrhedralBot bot) {
        this.bot = bot;
        messageListeners = new HashMap<>();
        memberChangeEventListener = new HashMap<>();
        joinMessageRedirect = new HashMap<>();
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
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        if (bot.getConfig().getBlacklist().contains(message.getAuthor().getId())) {
            return;
        }

        Channel channel = bot.getApiClient().getChannelById(message.getChannelId());
        if (channel != DiscordApiClient.NO_CHANNEL && channel.getParent() != null) {
            if (ignoredServers.contains(channel.getParent().getId())) {
                return;
            }
        }
        if (message.getContent().startsWith(bot.getConfig().getCommandPrefix())) {
            bot.getMainCommandDispatcher().handleCommand(message);
            return;
        }
        messageListeners.values().forEach(c -> c.accept(message));
    }

    private void handleMention(Message message) {
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        String otherId = message.getAuthor().getId();
        bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.mention.response",
                message.getAuthor().getUsername()),
                message.getChannelId(), new String[]{otherId});
    }

    private void handleExcessiveMentions(Message message) {
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
        if (Duration.between(ZonedDateTime.
                from(Commands.DATE_TIME_FORMATTER.parse(member.getJoinedAt())),
                ZonedDateTime.now()).abs().compareTo(Duration.ofDays(3)) > 0) {
            return;
        }
        Set<User> unique = new HashSet<>();
        Collections.addAll(unique, message.getMentions());
        if (unique.size() > excessiveMentionThreshold) {
            bot.getCommands().getModCommands().applyTimeout(bot.getApiClient().getClientUser(), channel,
                    server, author, Duration.ofHours(1));
            bot.getApiClient().sendMessage("You have been timed out for excessive mentions. " +
                    "Please contact a moderator if this is in error and you are in fact not a spambot", channel);
        }
    }

    @Subscribe
    public void onServerJoinLeave(ServerJoinLeaveEvent event) {
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        if (event.isJoin()) {
            Server server = event.getServer();
            //  Default channel has same ID as server
            Channel channel = bot.getApiClient().getChannelById(server.getId());
            if (channel != DiscordApiClient.NO_CHANNEL) {
                bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.on_join.response",
                        bot.getApiClient().getClientUser().getUsername()),
                        channel.getId());
            }
        }
    }

    @Subscribe
    public void onMemberChangeEvent(MemberChangeEvent event) {
        if (!bot.getMainCommandDispatcher().active().get()) {
            return;
        }
        Server server = event.getServer();
        //  Default channel has same ID as server
        Channel channel = bot.getApiClient().getChannelById(server.getId());
        if (channel == DiscordApiClient.NO_CHANNEL) {
            return;
        }
        String key;
        if (event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED) {
            key = "message.new_member.response";
        } else if (event.getMemberChange() == MemberChangeEvent.MemberChange.DELETED) {
            key = "message.member_quit.response";
        } else {
            return;
        }
        String cid = channel.getId();
        if (joinMessageRedirect.containsKey(server.getId())) {
            cid = joinMessageRedirect.get(server.getId());
        }
        User user = event.getMember().getUser();
        bot.getApiClient().sendMessage(bot.getLocalizer().localize(key,
                user.getUsername(),
                user.getId(),
                user.getDiscriminator()),
                cid);
        memberChangeEventListener.values().forEach(c -> c.accept(event));
    }

    public boolean isJoin(MemberChangeEvent event) {
        return event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED;
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
