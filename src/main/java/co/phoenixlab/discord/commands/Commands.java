package co.phoenixlab.discord.commands;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.Configuration;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;

import java.util.StringJoiner;

import static co.phoenixlab.discord.api.DiscordApiClient.NO_SERVER;
import static co.phoenixlab.discord.api.DiscordApiClient.NO_USER;
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

    private void selfCheck(MessageContext context, User user) {
        if (context.getMessage().getAuthor().equals(user)) {
            context.getApiClient().sendMessage(loc.localize("commands.common.self_reference", user.getUsername()),
                    context.getMessage().getChannelId());
        }
    }
}
