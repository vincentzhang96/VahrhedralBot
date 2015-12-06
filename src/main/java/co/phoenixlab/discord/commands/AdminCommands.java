package co.phoenixlab.discord.commands;

import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.DiscordWebSocketClient;
import co.phoenixlab.discord.api.entities.OutboundMessage;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static co.phoenixlab.discord.api.DiscordApiClient.NO_USER;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;

public class AdminCommands {

    private final CommandDispatcher adminCommandDispatcher;

    public AdminCommands(VahrhedralBot bot) {
        adminCommandDispatcher = new CommandDispatcher(bot, "");
    }

    public CommandDispatcher getAdminCommandDispatcher() {
        return adminCommandDispatcher;
    }

    public void registerAdminCommands() {
        adminCommandDispatcher.registerAlwaysActiveCommand("start", this::adminStart, "Start bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("stop", this::adminStop, "Stop bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("status", this::adminStatus, "Bot status");
        adminCommandDispatcher.registerAlwaysActiveCommand("kill", this::adminKill, "Kill the bot (terminate app)");
        adminCommandDispatcher.registerAlwaysActiveCommand("restart", this::adminRestart, "Restart the bot");
        adminCommandDispatcher.registerAlwaysActiveCommand("blacklist", this::adminBlacklist,
                "Prints the blacklist, or blacklists the given user. Supports @mention and partial front matching");
        adminCommandDispatcher.registerAlwaysActiveCommand("pardon", this::adminPardon,
                "Pardons the given user. Supports @mention and partial front matching");
        adminCommandDispatcher.registerAlwaysActiveCommand("join", this::adminJoin,
                "Joins using the provided Discord.gg invite link");
        adminCommandDispatcher.registerAlwaysActiveCommand("telegram", this::adminTelegram,
                "Send a message to another channel");
        adminCommandDispatcher.registerAlwaysActiveCommand("prefix", this::adminPrefix,
                "Get or set the main command prefix");
        adminCommandDispatcher.registerAlwaysActiveCommand("raw", this::adminRaw,
                "Send a message using raw JSON");
        adminCommandDispatcher.registerAlwaysActiveCommand("stats", this::adminStats,
                "Display session statistics");
    }

    private void adminStats(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        CommandDispatcher.Statistics mdStats = mainDispatcher.getStatistics();
        DiscordWebSocketClient.Statistics wsStats = apiClient.getWebSocketClient().getStatistics();
        apiClient.sendMessage(String.format("__**Bot Statistics**__\n" +
                        "**MainCommandDispatcher**\n" +
                        "CmdHandleTime: %s ms\n" +
                        "CmdOKHandleTime: %s ms\n" +
                        "ReceivedCommands: %,d T/%,d OK/%,d KO\n" +

                        "**WebSocketClient**\n" +
                        "MsgHandleTime: %s ms\n" +
                        "MsgCount: %,d\n" +
                        "KeepAliveCount: %,d\n" +
                        "ErrorCount: %,d\n",
                mdStats.commandHandleTime.summary(),
                mdStats.acceptedCommandHandleTime.summary(),
                mdStats.commandsReceived.sum(),
                mdStats.commandsHandledSuccessfully.sum() + 1,  //  +1 since this executed OK but hasnt counted yet
                mdStats.commandsRejected.sum(),
                wsStats.avgMessageHandleTime.summary(),
                wsStats.messageReceiveCount.sum(),
                wsStats.keepAliveCount.sum(),
                wsStats.errorCount.sum()),
                context.getMessage().getChannelId());
    }


    private void adminKill(MessageContext context, String args) {
        context.getApiClient().sendMessage("Sudoku time, bye", context.getMessage().getChannelId());
        context.getBot().shutdown();
    }

    private void adminRestart(MessageContext context, String args) {
        context.getApiClient().sendMessage("brb quick sudoku game", context.getMessage().getChannelId());
        context.getBot().shutdown(20);
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
        long s = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        String uptime = String.format("%d:%02d:%02d:%02d", s / 86400, (s / 3600) % 24, (s % 3600) / 60, (s % 60));
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        String memory = getMemoryInfo(heapMemoryUsage);
        String serverDetail = Integer.toString(apiClient.getServers().size());
        if (args.contains("servers")) {
            serverDetail = listServers(apiClient);
        }
        String response = String.format("**Status:** %s\n**Servers:** %s\n**Uptime:** %s\n**Heap:** `%s`\n" +
                        "**Load:** %.4f\n**Hamster Wheels in use:** %s",
                mainDispatcher.active().get() ? "Running" : "Stopped",
                serverDetail,
                uptime,
                memory,
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        apiClient.sendMessage(response, context.getMessage().getChannelId());
    }

    private String listServers(DiscordApiClient apiClient) {
        String serverDetail;StringJoiner serverJoiner = new StringJoiner(",\n");
        for (Server server : apiClient.getServers()) {
            serverJoiner.add(String.format("`%s`:%s", server.getName(), server.getId()));
        }
        serverDetail = String.format("%d servers: \n%s", apiClient.getServers().size(), serverJoiner.toString());
        return serverDetail;
    }

    private String getMemoryInfo(MemoryUsage heapMemoryUsage) {
        return String.format("%,dMB used %,dMB committed %,dMB max",
                heapMemoryUsage.getUsed() / 1048576L,
                heapMemoryUsage.getCommitted() / 1048576L,
                heapMemoryUsage.getMax() / 1048576L);
    }

    private void adminBlacklist(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        if (args.isEmpty()) {
            listBlacklistedUsers(context, apiClient, bot);
            return;
        }
        User user = findUser(context, args);
        if (user == NO_USER) {
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

    private void listBlacklistedUsers(MessageContext context, DiscordApiClient apiClient, VahrhedralBot bot) {
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
    }

    private void adminPardon(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (args.isEmpty()) {
            apiClient.sendMessage("Please specify a user", context.getMessage().getChannelId());
            return;
        }
        User user = findUser(context, args);
        if (user == NO_USER) {
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
        makeJoinPOSTRequest(context, apiClient, path);
    }

    private void makeJoinPOSTRequest(MessageContext context, DiscordApiClient apiClient, String path) {
        try {
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

    private void adminPrefix(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        if (s.isEmpty()) {
            apiClient.sendMessage(String.format("Command prefix: `%s`", bot.getConfig().getCommandPrefix()),
                    context.getMessage().getChannelId());
        } else {
            context.getBot().getMainCommandDispatcher().setCommandPrefix(s);
            bot.getConfig().setCommandPrefix(s);
            bot.saveConfig();
            apiClient.sendMessage(String.format("Command prefix set to `%s`", bot.getConfig().getCommandPrefix()),
                    context.getMessage().getChannelId());
        }
    }

    public void adminRaw(MessageContext context, String args) {
        String channel = null;
        String raw = null;
        if (args.startsWith("cid=")) {
            String[] split = args.split(" ", 2);
            if (split.length == 2) {
                channel = split[0].substring("cid=".length());
                raw = split[1];
            }
        }
        if (channel == null || raw == null) {
            channel = context.getMessage().getChannelId();
            raw = args;
        }
        OutboundMessage outboundMessage = new Gson().fromJson(raw, OutboundMessage.class);
        context.getApiClient().sendMessage(outboundMessage.getContent(),
                channel,
                outboundMessage.getMentions());
    }
}
