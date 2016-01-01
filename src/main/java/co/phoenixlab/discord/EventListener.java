package co.phoenixlab.discord;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.LogInEvent;
import co.phoenixlab.discord.api.event.MemberChangeEvent;
import co.phoenixlab.discord.api.event.MessageReceivedEvent;
import co.phoenixlab.discord.api.event.ServerJoinLeaveEvent;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class EventListener {

    private final VahrhedralBot bot;

    private Map<String, Consumer<Message>> messageListeners;

    public EventListener(VahrhedralBot bot) {
        this.bot = bot;
        messageListeners = new HashMap<>();
        messageListeners.put("mention-bot", message -> {
            User me = bot.getApiClient().getClientUser();
            for (User user : message.getMentions()) {
                if (me.equals(user)) {
                    handleMention(message);
                    return;
                }
            }
        });
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        if (bot.getConfig().getBlacklist().contains(message.getAuthor().getId())) {
            return;
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
                message.getChannelId(), new String[] {otherId});
    }

    @Subscribe
    public void onServerJoinLeave(ServerJoinLeaveEvent event) {
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
        if (event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED) {
            Server server = event.getServer();
            //  TODO TEMPORARY Disable for servers with more than 100 people
            //  until per-server feature controls are implemented
            if (server.getMembers().size() > 100) {
                return;
            }
            //  Default channel has same ID as server
            Channel channel = bot.getApiClient().getChannelById(server.getId());
            if (channel != DiscordApiClient.NO_CHANNEL) {
                bot.getApiClient().sendMessage(bot.getLocalizer().localize("message.new_member.response",
                        event.getMember().getUser().getUsername()),
                        channel.getId());
            }
        }
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
