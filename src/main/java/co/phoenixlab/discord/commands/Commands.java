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
import co.phoenixlab.discord.api.event.LogInEvent;
import co.phoenixlab.discord.commands.tempstorage.Minific;
import co.phoenixlab.discord.commands.tempstorage.MinificStorage;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static co.phoenixlab.discord.api.entities.Permission.CHAT_MANAGE_MESSAGES;
import static co.phoenixlab.discord.api.entities.Permission.GEN_MANAGE_ROLES;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class Commands {

    private final AdminCommands adminCommands;
    private final DnCommands dnCommands;
    private final ModCommands modCommands;
    private final Localizer loc;
    private final Random random;

    //  Temporary until command throttling is implemented
    private Instant lastInsultTime;

    private MinificStorage minificStorage;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd uuuu");
    private static final Path MINIFIC_STORE = Paths.get("config/minific.json");

    public Commands(VahrhedralBot bot) {
        adminCommands = new AdminCommands(bot);
        dnCommands = new DnCommands(bot);
        modCommands = new ModCommands(bot);
        loc = bot.getLocalizer();
        random = new Random();
    }

    public void register(CommandDispatcher d) {
        adminCommands.registerAdminCommands();
        dnCommands.registerDnCommands();
        modCommands.registerModCommands();
        d.registerAlwaysActiveCommand("commands.general.admin", this::admin);
        d.registerAlwaysActiveCommand("commands.general.mod", this::mod);
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
        d.registerCommand("commands.general.minific", this::minific);
    }

    public AdminCommands getAdminCommands() {
        return adminCommands;
    }

    public DnCommands getDnCommands() {
        return dnCommands;
    }

    public ModCommands getModCommands() {
        return modCommands;
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
        if (args.isEmpty()) {
            args = "help";
        }
        adminCommands.getAdminCommandDispatcher().
                handleCommand(new Message(message.getAuthor(), message.getChannelId(),
                        args, message.getId(), message.getMentions(), message.getTimestamp()));
    }

    private void mod(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        User author = context.getAuthor();
        Server server = context.getServer();

        if (!checkPermission(CHAT_MANAGE_MESSAGES, apiClient.getUserMember(author, server), server, apiClient)) {
            apiClient.sendMessage(loc.localize("commands.general.mod.response.reject", author.getUsername()),
                    context.getChannel());
            return;
        }
        if (args.isEmpty()) {
            args = "help";
        }
        modCommands.getModCommandDispatcher().
                handleCommand(new Message(message.getAuthor(), message.getChannelId(),
                        args, message.getId(), message.getMentions(), message.getTimestamp()));
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
        if (!(checkPermission(GEN_MANAGE_ROLES, issuer, server, apiClient) ||
                context.getBot().getConfig().isAdmin(message.getAuthor().getId()))) {
            apiClient.sendMessage(loc.localize("commands.general.rolecolor.response.no_user_perms"),
                    context.getChannel());
            return;
        }
        Member bot = apiClient.getUserMember(apiClient.getClientUser(), server);
        if (!checkPermission(GEN_MANAGE_ROLES, bot, server, apiClient)) {
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
                handleCommand(new Message(message.getAuthor(), message.getChannelId(),
                        args, message.getId(), message.getMentions(), message.getTimestamp()));
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
        if (context.getBot().getConfig().isAdmin(user.getId())) {
            user = context.getAuthor();
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
        if (member.getUser().getId().equals(server.getOwnerId())) {
            return true;
        }
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

    private void minific(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        String authorId = context.getAuthor().getId();
        if (args.startsWith("!#/")) {
            if (!context.getBot().getConfig().isAdmin(authorId)) {
                apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.reject"),
                        context.getChannel());
                return;
            }
            manageMinific(args, apiClient, context.getChannel());
        } else if (args.isEmpty()) {
            Minific fic = getRandomMinific();
            if (fic == null) {
                apiClient.sendMessage(loc.localize("commands.general.minific.response.none"),
                        context.getChannel());
            } else {
                User user = apiClient.getUserById(fic.getAuthorId());
                apiClient.sendMessage(loc.localize("commands.general.minific.response.random",
                        fic.getId(), user.getUsername(), fic.getDate(), fic.getContent()),
                        context.getChannel());
            }
        } else {
            if (minificStorage.getAuthorizedAuthorUids().contains(authorId) ||
                    context.getBot().getConfig().isAdmin(authorId)) {
                Minific fic = addMinific(args, authorId);
                apiClient.sendMessage(loc.localize("commands.general.minific.response.added",
                        fic.getId()),
                        context.getChannel());
            } else {
                apiClient.sendMessage(loc.localize("commands.general.minific.response.reject"),
                        context.getChannel());
            }
        }
    }

    private void manageMinific(String args, DiscordApiClient apiClient, Channel ctxChannel) {
        String[] split = args.split(" ", 2);
        String cmd = split[0].substring(3).toLowerCase();
        switch (cmd) {
            case "delete":
                if (split.length == 2) {
                    deleteMinificCmd(apiClient, ctxChannel, split[1]);
                } else {
                    apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.error"),
                            ctxChannel);
                }
                return;
            case "setauthor":
                if (split.length == 2) {
                    if (setMinificAuthorCmd(apiClient, ctxChannel, split[1])) {
                        return;
                    }
                }
                apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.error"),
                        ctxChannel);
                return;
        }
    }

    private boolean setMinificAuthorCmd(DiscordApiClient apiClient, Channel ctxChannel, String s) {
        String[] ss = s.split(" ", 2);
        if (ss.length == 2) {
            String id = ss[0];
            String authorId = ss[1];
            for (Minific minific : minificStorage.getMinifics()) {
                if (minific.getId().equals(id)) {
                    minific.setAuthorId(authorId);
                    apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.setauthor",
                            id, authorId),
                            ctxChannel);
                    return true;
                }
            }
            apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.not_found",
                    id),
                    ctxChannel);
            return true;
        }
        return false;
    }

    private void deleteMinificCmd(DiscordApiClient apiClient, Channel ctxChannel, String id) {
        if (deleteMinific(id)) {
            apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.delete",
                    id),
                    ctxChannel);
        } else {
            apiClient.sendMessage(loc.localize("commands.general.minific.response.manage.not_found",
                    id),
                    ctxChannel);
        }
    }

    private boolean deleteMinific(String id) {
        boolean deleted = false;
        List<Minific> minifics = minificStorage.getMinifics();
        for (Iterator<Minific> iter = minifics.iterator(); iter.hasNext(); ) {
            Minific minific = iter.next();
            if (minific.getId().equals(id)) {
                iter.remove();
                deleted = true;
                break;
            }
        }
        if (deleted) {
            //  Re-ID fics
            List<Minific> copy = new ArrayList<>(minifics);
            minifics.clear();
            for (int i = 0; i < copy.size(); i++) {
                Minific minific = copy.get(i);
                minific = new Minific(Integer.toString(i), minific.getAuthorId(), minific.getDate(), minific.getContent());
                minifics.add(minific);
            }
        }
        return deleted;
    }

    private Minific getRandomMinific() {
        int size = minificStorage.getMinifics().size();
        if (size == 0) {
            return null;
        }
        return minificStorage.getMinifics().get(random.nextInt(size));
    }

    private Minific addMinific(String content, String authorId) {
        ZonedDateTime now = ZonedDateTime.now();
        Minific minific = new Minific(Integer.toString(minificStorage.getMinifics().size()),
                authorId, DATE_FORMATTER.format(now), content);
        minificStorage.getMinifics().add(minific);
        saveMinificStorage();
        return minific;
    }

    private void saveMinificStorage() {
        Gson gson = new Gson();
        try (BufferedWriter writer = Files.newBufferedWriter(MINIFIC_STORE, UTF_8, CREATE, TRUNCATE_EXISTING)) {
            gson.toJson(minificStorage, writer);
            writer.flush();
            VahrhedralBot.LOGGER.info("Saved minific store");
        } catch (IOException e) {
            VahrhedralBot.LOGGER.warn("Unable to save minific store", e);
        }
    }

    private void loadMinificStorage() {
        Gson gson = new Gson();
        if (!Files.exists(MINIFIC_STORE)) {
            minificStorage = new MinificStorage();
            saveMinificStorage();
        }
        try (Reader reader = Files.newBufferedReader(MINIFIC_STORE, UTF_8)) {
            minificStorage = gson.fromJson(reader, MinificStorage.class);
            VahrhedralBot.LOGGER.info("Loaded minific store");
        } catch (IOException e) {
            VahrhedralBot.LOGGER.warn("Unable to load minific store", e);
        }
    }

    public void onLogIn(LogInEvent logInEvent) {
        loadMinificStorage();

        modCommands.onReady();
    }

}
