package co.phoenixlab.discord.commands;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.Configuration;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.DiscordWebSocketClient;
import co.phoenixlab.discord.api.entities.*;

import java.util.StringJoiner;

import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;

public class Commands {

    private final AdminCommands adminCommands;
    private final Localizer loc;

    public Commands(VahrhedralBot bot) {
        adminCommands = new AdminCommands(bot);
        loc = bot.getLocalizer();
    }

    public void register(CommandDispatcher d) {
        adminCommands.registerAdminCommands();
        d.registerAlwaysActiveCommand("commands.general.admin.command", this::admin, "commands.general.admin.help");
        d.registerCommand("commands.general.admins.command", this::listAdmins, "commands.general.admins.help");
        d.registerCommand("commands.general.info.command", this::info, "commands.general.info.help");
        d.registerCommand("commands.general.avatar.command", this::avatar, "commands.general.avatar.help");
        d.registerCommand("commands.general.version.command", this::version, "commands.general.version.help");
        d.registerCommand("commands.general.stats.command", this::stats, "commands.general.stats.help");
        d.registerCommand("commands.general.roles.command", this::roles, "commands.general.roles.help");
    }

    private void admin(MessageContext context, String args) {
        //  Permission check
        Message message = context.getMessage();
        if (!context.getBot().getConfig().getAdmins().contains(message.getAuthor().getId())) {
            context.getApiClient().sendMessage(
                    loc.localize("commands.general.admin.response.reject", message.getAuthor().getUsername()),
                    message.getChannelId());
            return;
        }
        adminCommands.getAdminCommandDispatcher().
                handleCommand(new Message(message.getAuthor(), message.getChannelId(), args,
                        message.getChannelId(), message.getMentions(), message.getTime()));
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
            res = loc.localize("commands.general.admins.response.none");
        }
        apiClient.sendMessage(loc.localize("commands.general.admins.response.format", res),
                context.getMessage().getChannelId());
    }

    private void info(MessageContext context, String args) {
        Message message = context.getMessage();
        Configuration config = context.getBot().getConfig();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
            selfCheck(context, user);
        } else {
            user = message.getAuthor();
        }
        if (user == NO_USER) {
            context.getApiClient().sendMessage(loc.localize("commands.general.info.response.not_found"),
                    message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ? loc.localize("commands.general.info.response.no_avatar") :
                    user.getAvatarUrl().toExternalForm());
            String response = loc.localize("commands.general.info.response.format",
                    user.getUsername(), user.getId(),
                    config.getBlacklist().contains(user.getId()) ?
                            loc.localize("commands.general.info.response.blacklisted") : "",
                    config.getAdmins().contains(user.getId()) ?
                            loc.localize("commands.general.info.response.admin") : "",
                    avatar);
            context.getApiClient().sendMessage(response, message.getChannelId());
        }
    }


    private void avatar(MessageContext context, String args) {
        Message message = context.getMessage();
        if (args.startsWith(loc.localize("commands.general.avatar.subcommand.server"))) {
            handleServerAvatar(context, message, args);
            return;
        }
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
            selfCheck(context, user);
        } else {
            user = message.getAuthor();
        }
        if (user == NO_USER) {
            context.getApiClient().sendMessage(loc.localize("commands.general.avatar.response.not_found"),
                    message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ?
                    loc.localize("commands.general.avatar.response.no_avatar") : user.getAvatarUrl().toExternalForm());
            context.getApiClient().sendMessage(loc.localize("commands.general.avatar.response.format",
                    user.getUsername(), avatar),
                    message.getChannelId());
        }
    }

    private void handleServerAvatar(MessageContext context, Message message, String args) {
        Server server;
        DiscordApiClient apiClient = context.getApiClient();
        Channel c = apiClient.getChannelById(message.getChannelId());
        if (args.contains(" ")) {
            args = args.split(" ", 2)[1];
            server = apiClient.getServerByID(args);
            if (server == NO_SERVER) {
                apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.not_member"),
                        message.getChannelId());
            }
        } else if (c == null || c.getParent() == NO_SERVER) {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.private"),
                    message.getChannelId());
            return;
        } else {
            server = c.getParent();
        }
        String icon = server.getIcon();
        if (icon == null) {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.format.none",
                    server.getName()),
                    message.getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.format",
                    server.getName(), String.format(ApiConst.ICON_URL_PATTERN, server.getId(), icon)),
                    message.getChannelId());
        }
    }

    private void version(MessageContext context, String args) {
        context.getApiClient().sendMessage(context.getBot().getVersionInfo(), context.getMessage().getChannelId());
    }

    private void stats(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        CommandDispatcher.Statistics mdStats = mainDispatcher.getStatistics();
        DiscordWebSocketClient.Statistics wsStats = apiClient.getWebSocketClient().getStatistics();
        apiClient.sendMessage(loc.localize("commands.general.stats.response.format",
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

    private void roles(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
            selfCheck(context, user);
        } else {
            user = message.getAuthor();
        }
        if (user == NO_USER) {
            context.getApiClient().sendMessage(loc.localize("commands.general.roles.response.not_found"),
                    message.getChannelId());
        } else {
            Channel channel = apiClient.getChannelById(context.getMessage().getChannelId());
            Server server = channel.getParent();
            Member member = apiClient.getUserMember(user, server);
            if (member != NO_MEMBER) {
                context.getApiClient().sendMessage(loc.localize("commands.general.roles.response.format",
                        user.getUsername(), listRoles(member, server, apiClient)),
                        message.getChannelId());
            } else {
                context.getApiClient().sendMessage(loc.localize("commands.general.roles.response.member_not_found"),
                        message.getChannelId());
            }
        }
    }

    private String listRoles(Member member, Server server, DiscordApiClient client) {
        if (member.getRoles().isEmpty()) {
            return loc.localize("commands.general.roles.response.no_roles");
        }
        StringJoiner joiner = new StringJoiner(", ");
        member.getRoles().stream().
                map(s -> client.getRole(s, server)).
                filter(r -> r != NO_ROLE).
                map(r -> loc.localize("commands.general.roles.response.role.format", r.getName(), r.getId())).
                forEach(joiner::add);
        return joiner.toString();
    }

    private void selfCheck(MessageContext context, User user) {
        if (context.getMessage().getAuthor().equals(user)) {
            context.getApiClient().sendMessage(loc.localize("commands.common.self_reference", user.getUsername()),
                    context.getMessage().getChannelId());
        }
    }
}
