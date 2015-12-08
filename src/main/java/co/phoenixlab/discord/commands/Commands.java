package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.Configuration;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.DiscordWebSocketClient;
import co.phoenixlab.discord.api.entities.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

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
        d.registerAlwaysActiveCommand("commands.general.admin", this::admin);
        d.registerCommand("commands.general.admins", this::listAdmins);
        d.registerCommand("commands.general.info", this::info);
        d.registerCommand("commands.general.avatar", this::avatar);
        d.registerCommand("commands.general.version", this::version);
        d.registerCommand("commands.general.stats", this::stats);
        d.registerCommand("commands.general.roles", this::roles);
        d.registerCommand("commands.general.rolecolor", this::roleColor);
        d.registerCommand("commands.general.sandwich", this::makeSandwich);
    }

    private void admin(MessageContext context, String args) {
        //  Permission check
        Message message = context.getMessage();
        if (!context.getBot().getConfig().isAdmin(message.getAuthor().getId())) {
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
            if (server == NO_SERVER) {
                apiClient.sendMessage(loc.localize("commands.general.roles.response.private"), message.getChannelId());
                return;
            }
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
                map(r -> loc.localize("commands.general.roles.response.role.format",
                        r.getName(), r.getId(), r.getColor())).
                forEach(joiner::add);
        return joiner.toString();
    }

    private void roleColor(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        //  Check permissions first
        Channel channel = apiClient.getChannelById(context.getMessage().getChannelId());
        Server server = channel.getParent();
        if (server == NO_SERVER) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.private"),
                    message.getChannelId());
            return;
        }
        Member issuer = apiClient.getUserMember(message.getAuthor(), server);
        if (!(checkPermission(Permission.GEN_MANAGE_ROLES, issuer, server, apiClient) ||
                context.getBot().getConfig().isAdmin(message.getAuthor().getId()))) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.no_user_perms"),
                    message.getChannelId());
            return;
        }
        Member bot = apiClient.getUserMember(apiClient.getClientUser(), server);
        if (!checkPermission(Permission.GEN_MANAGE_ROLES, bot, server, apiClient)) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.no_bot_perms"),
                    message.getChannelId());
            return;
        }
        String[] split = args.split(" ");
        if (split.length != 2) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.help_format"),
                    message.getChannelId());
            return;
        }
        String colorStr = split[1];
        OptionalInt colorOpt = ParseInt.parseOptional(colorStr);
        if (!colorOpt.isPresent()) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.help_format"),
                    message.getChannelId());
            return;
        }
        int color = colorOpt.getAsInt();
        String roleId = split[0];
        Role role = apiClient.getRole(roleId, server);
        if (role == NO_ROLE) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.role_not_found"),
                    message.getChannelId());
            return;
        }

        patchRole(apiClient, message, server, color, role);
    }

    private void patchRole(DiscordApiClient apiClient, Message message, Server server, int color, Role role) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            headers.put(HttpHeaders.AUTHORIZATION, apiClient.getToken());
            JSONObject requestBody = new JSONObject();
            requestBody.put("color", color);
            requestBody.put("hoist", role.isHoist());
            requestBody.put("name", role.getName());
            requestBody.put("permissions", role.getPermissions());
            HttpResponse<JsonNode> response = Unirest.
                    patch(ApiConst.SERVERS_ENDPOINT + server.getId() + "/roles/" + role.getId()).
                    headers(headers).
                    body(new JsonNode(requestBody.toString())).
                    asJson();
            if (response.getStatus() != 200) {
                VahrhedralBot.LOGGER.warn("Unable to PATCH role: HTTP {}: {}",
                        response.getStatus(), response.getStatusText());
                apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.general_error"),
                        message.getChannelId());
                return;
            }
            JsonNode body = response.getBody();
            JSONObject obj = body.getObject();
            if (obj.getInt("color") != color) {
                VahrhedralBot.LOGGER.warn("Unable to PATCH role: Returned color does not match",
                        response.getStatus(), response.getStatusText());
                apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.general_error"),
                        message.getChannelId());
                return;
            }
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response",
                    role.getName(), role.getId(), color),
                    message.getChannelId());
        } catch (UnirestException | JSONException e) {
            VahrhedralBot.LOGGER.warn("Unable to PATCH role", e);
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.general_error"),
                    message.getChannelId());
        }
    }

    private void makeSandwich(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (loc.localize("commands.general.sandwich.magic_word").equalsIgnoreCase(args) ||
                new Random().nextBoolean()) {
            apiClient.sendMessage(loc.localize("commands.general.sandwich.response.deny"),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.general.sandwich.response.magic"),
                    context.getMessage().getChannelId());
        }
    }

    private boolean checkPermission(Permission permission, Member member, Server server, DiscordApiClient apiClient) {
        for (String roleId : member.getRoles()) {
            Role role = apiClient.getRole(roleId, server);
            if (permission.test(role.getPermissions())) {
                return true;
            }
        }
        return false;
    }

    private void selfCheck(MessageContext context, User user) {
        if (context.getMessage().getAuthor().equals(user)) {
            context.getApiClient().sendMessage(loc.localize("commands.common.self_reference", user.getUsername()),
                    context.getMessage().getChannelId());
        }
    }
}
