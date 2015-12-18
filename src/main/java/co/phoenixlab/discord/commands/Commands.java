package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.SafeNav;
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

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;

public class Commands {

    private final AdminCommands adminCommands;
    private final DnCommands dnCommands;
    private final Localizer loc;
    private final Random random;

    //  Temporary until command throttling is implemented
    private Instant lastInsultTime;

    public Commands(VahrhedralBot bot) {
        adminCommands = new AdminCommands(bot);
        dnCommands = new DnCommands(bot);
        loc = bot.getLocalizer();
        random = new Random();
    }

    public void register(CommandDispatcher d) {
        adminCommands.registerAdminCommands();
        dnCommands.registerDnCommands();
        d.registerAlwaysActiveCommand("commands.general.admin", this::admin);
        d.registerCommand("commands.general.admins", this::listAdmins);
        d.registerCommand("commands.general.info", this::info);
        d.registerCommand("commands.general.avatar", this::avatar);
        d.registerCommand("commands.general.version", this::version);
        d.registerCommand("commands.general.stats", this::stats);
        d.registerCommand("commands.general.roles", this::roles);
        d.registerCommand("commands.general.rolecolor", this::roleColor);
        d.registerCommand("commands.general.sandwich", this::makeSandwich);
        d.registerCommand("commands.general.dn", this::dnCommands);
        d.registerCommand("commands.general.insult", this::insult);
    }

    private void admin(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        //  Easter egg
        if ("ku".equalsIgnoreCase(args)) {
            int number = random.nextInt(9) + 1;
            int spotY = random.nextInt(2);
            String spotYKey;
            String spotXKey;
            if (spotY == 0) {
                spotYKey = "commands.general.admin.response.easter_egg.center";
            } else if (spotY == 1) {
                spotYKey = "commands.general.admin.response.easter_egg.top";
            } else {
                spotYKey = "commands.general.admin.response.easter_egg.bottom";
            }
            int spotX = random.nextInt(2);
            if (spotX == 0) {
                spotXKey = "commands.general.admin.response.easter_egg.center";
            } else if (spotX == 1) {
                spotXKey = "commands.general.admin.response.easter_egg.right";
            } else {
                spotXKey = "commands.general.admin.response.easter_egg.left";
            }
            String pos;
            if (spotX == 0 && spotY == 0) {
                pos = loc.localize("commands.general.admin.response.easter_egg.center");
            } else {
                pos = loc.localize("commands.general.admin.response.easter_egg.tuple",
                        loc.localize(spotYKey), loc.localize(spotXKey));
            }
            apiClient.sendMessage(
                    loc.localize("commands.general.admin.response.easter_egg.format", number, pos),
                    context.getChannel());
            return;
        }
        //  Permission check
        if (!context.getBot().getConfig().isAdmin(message.getAuthor().getId())) {
            if (context.getDispatcher().active().get()) {
                apiClient.sendMessage(
                        loc.localize("commands.general.admin.response.reject", message.getAuthor().getUsername()),
                        context.getChannel());
            }
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
                context.getChannel());
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
            context.getApiClient().sendMessage(response, context.getChannel());
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
                    context.getChannel());
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
                        context.getChannel());
            }
        } else if (c == null || c.getParent() == NO_SERVER) {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.private"),
                    context.getChannel());
            return;
        } else {
            server = c.getParent();
        }
        String icon = server.getIcon();
        if (icon == null) {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.format.none",
                    server.getName()),
                    context.getChannel());
        } else {
            apiClient.sendMessage(loc.localize("commands.general.avatar.response.server.format",
                    server.getName(), String.format(ApiConst.ICON_URL_PATTERN, server.getId(), icon)),
                    context.getChannel());
        }
    }

    private void version(MessageContext context, String args) {
        context.getApiClient().sendMessage(context.getBot().getVersionInfo(), context.getChannel());
    }

    private void stats(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        CommandDispatcher.Statistics mdStats = mainDispatcher.getStatistics();
        DiscordWebSocketClient.Statistics wsStats = apiClient.getWebSocketClient().getStatistics();
        DiscordApiClient.Statistics apiStats = apiClient.getStatistics();
        apiClient.sendMessage(loc.localize("commands.general.stats.response.format",
                mdStats.commandHandleTime.summary(),
                mdStats.acceptedCommandHandleTime.summary(),
                mdStats.commandsReceived.sum(),
                mdStats.commandsHandledSuccessfully.sum() + 1,  //  +1 since this executed OK but hasnt counted yet
                mdStats.commandsRejected.sum(),
                wsStats.avgMessageHandleTime.summary(),
                wsStats.messageReceiveCount.sum(),
                wsStats.keepAliveCount.sum(),
                wsStats.errorCount.sum(),
                apiStats.connectAttemptCount.sum(),
                apiStats.eventCount.sum(),
                apiStats.eventDispatchErrorCount.sum(),
                apiStats.restErrorCount.sum()),
                context.getChannel());
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
            Server server = context.getServer();
            if (server == NO_SERVER) {
                apiClient.sendMessage(loc.localize("commands.general.roles.response.private"), context.getChannel());
                return;
            }
            Member member = apiClient.getUserMember(user, server);
            if (member != NO_MEMBER) {
                context.getApiClient().sendMessage(loc.localize("commands.general.roles.response.format",
                        user.getUsername(), listRoles(member, server, apiClient)),
                        context.getChannel());
            } else {
                context.getApiClient().sendMessage(loc.localize("commands.general.roles.response.member_not_found"),
                        context.getChannel());
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
        Server server = context.getServer();
        if (server == NO_SERVER) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.private"),
                    context.getChannel());
            return;
        }
        Member issuer = apiClient.getUserMember(message.getAuthor(), server);
        if (!(checkPermission(Permission.GEN_MANAGE_ROLES, issuer, server, apiClient) ||
                context.getBot().getConfig().isAdmin(message.getAuthor().getId()))) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.no_user_perms"),
                    context.getChannel());
            return;
        }
        Member bot = apiClient.getUserMember(apiClient.getClientUser(), server);
        if (!checkPermission(Permission.GEN_MANAGE_ROLES, bot, server, apiClient)) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.no_bot_perms"),
                    context.getChannel());
            return;
        }
        String[] split = args.split(" ");
        if (split.length != 2) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.help_format"),
                    context.getChannel());
            return;
        }
        String colorStr = split[1];
        OptionalInt colorOpt = ParseInt.parseOptional(colorStr);
        if (!colorOpt.isPresent()) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.help_format"),
                    context.getChannel());
            return;
        }
        int color = colorOpt.getAsInt();
        String roleId = split[0];
        Role role = apiClient.getRole(roleId, server);
        if (role == NO_ROLE) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.role_not_found"),
                    context.getChannel());
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
                    context.getChannel());
        } else {
            apiClient.sendMessage(loc.localize("commands.general.sandwich.response.magic"),
                    context.getChannel());
        }
    }

    private void dnCommands(MessageContext context, String args) {
        Message message = context.getMessage();
        if (args.isEmpty()) {
            args = "help";
        }
        dnCommands.getDispatcher().
                handleCommand(new Message(message.getAuthor(), message.getChannelId(), args,
                        message.getChannelId(), message.getMentions(), message.getTime()));
    }

    private void insult(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        boolean isNotAdmin = !context.getBot().getConfig().isAdmin(message.getAuthor().getId());
        if (isNotAdmin) {
            if (lastInsultTime != null) {
                Instant now = Instant.now();
                if (now.toEpochMilli() - lastInsultTime.toEpochMilli() < TimeUnit.MINUTES.toMillis(1)) {
                    apiClient.sendMessage(loc.localize("commands.general.insult.response.timeout"),
                            message.getChannelId());
                    return;
                }
            }
        }
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
        } else {
            apiClient.sendMessage(loc.localize("commands.general.insult.response.missing"),
                    context.getChannel());
            return;
        }
        if (user == NO_USER) {
            apiClient.sendMessage(loc.localize("commands.general.insult.response.not_found"),
                    context.getChannel());
            return;
        }
        String insult = getInsult();
        if (insult == null) {
            apiClient.sendMessage(loc.localize("commands.general.insult.response.error"),
                    context.getChannel());
        } else {
            apiClient.sendMessage(loc.localize("commands.general.insult.response.format",
                    user.getUsername(), insult),
                    context.getChannel(), new String[]{user.getId()});
            if (isNotAdmin) {
                lastInsultTime = Instant.now();
            }
        }

    }

    private String getInsult() {
        try {
            HttpResponse<JsonNode> response = Unirest.get("http://quandyfactory.com/insult/json").
                    asJson();
            if (response.getStatus() != 200) {
                VahrhedralBot.LOGGER.warn("Unable to load insult, HTTP {}: {}",
                        response.getStatus(), response.getStatusText());
                return null;
            }
            JsonNode node = response.getBody();
            return SafeNav.of(node).
                    next(JsonNode::getObject).
                    next(o -> o.getString("insult")).
                    get();
        } catch (UnirestException e) {
            VahrhedralBot.LOGGER.warn("Unable to load insult", e);
            return null;
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
                    context.getChannel());
        }
    }
}
