package co.phoenixlab.discord;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.User;

import java.time.Duration;
import java.time.Instant;

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
        dispatcher.registerCommand("info", this::info,
                "Display information about the caller or the provided name, if present. @Mentions and partial front " +
                        "matches are supported");

        registerTime = Instant.now();
    }

    private void registerAdminCommands() {
        adminCommandDispatcher.registerAlwaysActiveCommand("start", this::adminStart, "Start bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("stop", this::adminStop, "Stop bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("status", this::adminStatus, "Bot status");
        adminCommandDispatcher.registerAlwaysActiveCommand("kill", this::adminKill, "Kill the bot (terminate app)");
        adminCommandDispatcher.registerAlwaysActiveCommand("blacklist", this::adminBlacklist,
                "Blacklists the given user. Supports @mention and partial front matching");
        adminCommandDispatcher.registerAlwaysActiveCommand("pardon", this::adminPardon,
                "Pardons the given user. Supports @mention and partial front matching");
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
                (r.maxMemory() - r.freeMemory()) / 1024 / 1024,
                r.freeMemory() / 1024 / 1024,
                r.maxMemory() / 1024 / 1024);
        String response = String.format("**Status:** %s\n**Servers:** %d\n**Uptime:** %s\n**Memory:** `%s`",
                mainDispatcher.active().get() ? "Running" : "Stopped",
                apiClient.getServers().size(),
                uptime,
                memory);
        apiClient.sendMessage(response, context.getMessage().getChannelId());
    }

    private void adminBlacklist(MessageContext context, String args) {
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
        context.getBot().getConfig().getBlacklist().add(user.getId());
        context.getBot().saveConfig();
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
        context.getBot().getConfig().getBlacklist().remove(user.getId());
        context.getBot().saveConfig();
        apiClient.sendMessage(String.format("`%s` has been pardoned", user.getUsername()),
                context.getMessage().getChannelId());
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
            User temp = context.getApiClient().findUser(username, channel.getParent());
            if (temp != null) {
                user = temp;
            }
        }
        return user;
    }

    private void admin(MessageContext context, String args) {
        //  Permission check
        //  TODO Implement a more comprehensive permission system
        //  For now check if its me
        if (!context.getMessage().getAuthor().getId().equals("90844514855424000")) {
            return;
        }
        Message original = context.getMessage();
        adminCommandDispatcher.handleCommand(new Message(original.getAuthor(), original.getChannelId(), args,
                original.getChannelId(), original.getMentions(), original.getTime()));
    }

    private void info(MessageContext context, String args) {
        Message message = context.getMessage();
        User user = null;
        if (!args.isEmpty()) {
            user = findUser(context, args);
        } else {
            user = message.getAuthor();
        }
        if (user == null) {
            context.getApiClient().sendMessage("Unable to find user. Try typing their name EXACTLY or" +
                    " @mention them instead", message.getChannelId());
        } else {
            String response;
            if (user.getAvatar() == null) {
                response = String.format("**Username:** %s\n**ID:** %s:%s\n**Avatar:** N/A",
                        user.getUsername(), user.getId(), user.getDiscriminator());
            } else {
                response = String.format("**Username:** %s\n**ID:** %s:%s\n**Avatar:** %s",
                        user.getUsername(), user.getId(), user.getDiscriminator(), user.getAvatarUrl());
            }
            context.getApiClient().sendMessage(response, message.getChannelId());
        }
    }

}
