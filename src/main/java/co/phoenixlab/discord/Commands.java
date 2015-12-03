package co.phoenixlab.discord;

import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.User;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;

import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Commands {

    private Instant registerTime;

    private final CommandDispatcher adminCommandDispatcher;

    public Commands(VahrhedralBot bot) {
        adminCommandDispatcher = new CommandDispatcher(bot, "");
    }

    public void register(CommandDispatcher dispatcher) {
        registerAdminCommands();

        dispatcher.registerAlwaysActiveCommand("admin", this::admin,
                "Administrative commands");
        dispatcher.registerCommand("admins", this::listAdmins,
                "List admins");
        dispatcher.registerCommand("info", this::info,
                "Display information about the caller or the provided name, if present. @Mentions and partial front " +
                        "matches are supported");
        dispatcher.registerCommand("avatar", this::avatar,
                "Display the avatar of the caller or the provided name, if present. @Mentions and partial front " +
                        "matches are supported");

        registerTime = Instant.now();
    }

    private void registerAdminCommands() {
        adminCommandDispatcher.registerAlwaysActiveCommand("start", this::adminStart, "Start bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("stop", this::adminStop, "Stop bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("status", this::adminStatus, "Bot status");
        adminCommandDispatcher.registerAlwaysActiveCommand("kill", this::adminKill, "Kill the bot (terminate app)");
        adminCommandDispatcher.registerAlwaysActiveCommand("blacklist", this::adminBlacklist,
                "Prints the blacklist, or blacklists the given user. Supports @mention and partial front matching");
        adminCommandDispatcher.registerAlwaysActiveCommand("pardon", this::adminPardon,
                "Pardons the given user. Supports @mention and partial front matching");
        adminCommandDispatcher.registerAlwaysActiveCommand("join", this::adminJoin,
                "Joins using the provided Discord.gg invite link");
        adminCommandDispatcher.registerAlwaysActiveCommand("telegram", this::adminTelegram,
                "Send a message to another channel");
    }


    private void adminKill(MessageContext context, String args) {
        context.getApiClient().sendMessage("Sudoku time, bye", context.getMessage().getChannelId());
        System.exit(0);
    }

    private void adminStart(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        if (mainDispatcher.active().compareAndSet(false, true)) {
            apiClient.sendMessage("Bot started", context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage("Bot was already started", context.getMessage().getChannelId());
        }
    }

    private void adminStop(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        if (mainDispatcher.active().compareAndSet(true, false)) {
            apiClient.sendMessage("Bot stopped", context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage("Bot was already stopped", context.getMessage().getChannelId());
        }
    }

    private void adminStatus(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        Instant now = Instant.now();
        Duration duration = Duration.between(registerTime, now);
        long s = duration.getSeconds();
        String uptime = String.format("%d:%02d:%02d:%02d", s / 86400, (s / 3600) % 24, (s % 3600) / 60, (s % 60));
        Runtime r = Runtime.getRuntime();
        String memory = String.format("%,dMB Used %,dMB Free %,dMB Max",
                (r.maxMemory() - r.freeMemory()) / 1048576,
                r.freeMemory() / 1048576,
                r.maxMemory() / 1048576);
        String response = String.format("**Status:** %s\n**Servers:** %d\n**Uptime:** %s\n**Memory:** `%s`\n" +
                "**Load:** %.4f\n**TCID:** %s\n**TSID:** %s",
                mainDispatcher.active().get() ? "Running" : "Stopped",
                apiClient.getServers().size(),
                uptime,
                memory,
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                context.getMessage().getChannelId(),
                apiClient.getChannelById(context.getMessage().getChannelId()).getParent().getId());
        apiClient.sendMessage(response, context.getMessage().getChannelId());
    }

    private void adminBlacklist(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        if (args.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            bot.getConfig().getBlacklist().stream().
                    map(apiClient::getUserById).
                    filter(user -> user != null).
                    map(User::getUsername).
                    forEach(joiner::add);
            String res = joiner.toString();
            if (res.isEmpty()) {
                res = "None";
            }
            apiClient.sendMessage("Blacklisted users: " + res, context.getMessage().getChannelId());
            return;
        }
        User user = findUser(context, args);
        if (user == null) {
            apiClient.sendMessage("Unable to find user", context.getMessage().getChannelId());
            return;
        }
        if (bot.getConfig().getAdmins().contains(user.getId())) {
            apiClient.sendMessage("Cannot blacklist an admin", context.getMessage().getChannelId());
            return;
        }
        bot.getConfig().getBlacklist().add(user.getId());
        bot.saveConfig();
        apiClient.sendMessage(String.format("`%s` has been blacklisted", user.getUsername()),
                context.getMessage().getChannelId());
    }

    private void adminPardon(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (args.isEmpty()) {
            apiClient.sendMessage("Please specify a user", context.getMessage().getChannelId());
            return;
        }
        User user = findUser(context, args);
        if (user == null) {
            apiClient.sendMessage("Unable to find user", context.getMessage().getChannelId());
            return;
        }
        boolean removed = context.getBot().getConfig().getBlacklist().remove(user.getId());
        context.getBot().saveConfig();
        if (removed) {
            apiClient.sendMessage(String.format("`%s` has been pardoned", user.getUsername()),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(String.format("`%s` was not blacklisted", user.getUsername()),
                    context.getMessage().getChannelId());
        }
    }

    private void adminJoin(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        URL inviteUrl;
        try {
            inviteUrl = new URL(args);
        } catch (MalformedURLException e) {
            VahrhedralBot.LOGGER.warn("Invalid invite link received", e);
            apiClient.sendMessage("Invalid link",
                    context.getMessage().getChannelId());
            return;
        }
        String path = inviteUrl.getPath();
        try {
            System.out.println(ApiConst.INVITE_ENDPOINT + path);
            HttpResponse<String> response = Unirest.post(ApiConst.INVITE_ENDPOINT + path).
                    header(HttpHeaders.AUTHORIZATION, apiClient.getToken()).
                    asString();
            if (response.getStatus() != 200) {
                VahrhedralBot.LOGGER.warn("Unable to join using invite link: HTTP {}: {}: {}",
                        response.getStatus(), response.getStatusText(), response.getBody());
                apiClient.sendMessage("Unable to join server: HTTP error",
                        context.getMessage().getChannelId());
            }
        } catch (UnirestException e) {
            VahrhedralBot.LOGGER.warn("Unable to join using invite link", e);
            apiClient.sendMessage("Unable to join server: Network error",
                    context.getMessage().getChannelId());
        }
    }

    private void adminTelegram(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        String[] split = s.split(" ", 2);
        if (split.length != 2) {
            apiClient.sendMessage("Format: channelId Message",
                    context.getMessage().getChannelId());
            return;
        }
        User[] mentions = context.getMessage().getMentions();
        apiClient.sendMessage(split[1], split[0], Arrays.stream(mentions).
                map(User::getId).
                collect(Collectors.toList()).toArray(new String[mentions.length]));
    }

    private User findUser(MessageContext context, String username) {
        Message message = context.getMessage();
        User user = null;
        Channel channel = context.getApiClient().getChannelById(message.getChannelId());
        //  Attempt to find the given user
        //  If the user is @mentioned, try that first
        if (message.getMentions() != null && message.getMentions().length > 0) {
            user = message.getMentions()[0];
        } else {
            user = context.getApiClient().findUser(username, channel.getParent());
        }
        return user;
    }

    private void admin(MessageContext context, String args) {
        //  Permission check
        if (!context.getBot().getConfig().getAdmins().contains(context.getMessage().getAuthor().getId())) {
            return;
        }
        Message original = context.getMessage();
        adminCommandDispatcher.handleCommand(new Message(original.getAuthor(), original.getChannelId(), args,
                original.getChannelId(), original.getMentions(), original.getTime()));
    }

    private void listAdmins(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        StringJoiner joiner = new StringJoiner(", ");
        bot.getConfig().getAdmins().stream().
                map(apiClient::getUserById).
                filter(user -> user != null).
                map(User::getUsername).
                forEach(joiner::add);
        String res = joiner.toString();
        if (res.isEmpty()) {
            res = "None";
        }
        apiClient.sendMessage("Admins: " + res, context.getMessage().getChannelId());
    }

    private void info(MessageContext context, String args) {
        Message message = context.getMessage();
        Configuration config = context.getBot().getConfig();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
        } else {
            user = message.getAuthor();
        }
        if (user == null) {
            context.getApiClient().sendMessage("Unable to find user. Try typing their name EXACTLY or" +
                    " @mention them instead", message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ? "N/A" : user.getAvatarUrl().toExternalForm());
            String response = String.format("**Username:** %s\n**ID:** %s:%s\n%s%s**Avatar:** %s",
                    user.getUsername(), user.getId(), user.getDiscriminator(),
                    config.getBlacklist().contains(user.getId()) ? "**Blacklisted**\n" : "",
                    config.getAdmins().contains(user.getId()) ? "**Bot Administrator**\n" : "",
                    avatar);
            context.getApiClient().sendMessage(response, message.getChannelId());
        }
    }

    private void avatar(MessageContext context, String args) {
        Message message = context.getMessage();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
        } else {
            user = message.getAuthor();
        }
        if (user == null) {
            context.getApiClient().sendMessage("Unable to find user. Try typing their name EXACTLY or" +
                    " @mention them instead", message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ? "No avatar" : user.getAvatarUrl().toExternalForm());
            context.getApiClient().sendMessage(String.format("%s's avatar: %s", user.getUsername(), avatar),
                    message.getChannelId());
        }
    }

}
