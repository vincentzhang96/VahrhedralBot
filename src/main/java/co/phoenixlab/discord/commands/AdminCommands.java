package co.phoenixlab.discord.commands;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.ApiConst;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.OutboundMessage;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHeaders;

import javax.script.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static co.phoenixlab.discord.api.DiscordApiClient.NO_USER;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;

public class AdminCommands {

    private final CommandDispatcher dispatcher;
    private Localizer loc;
    private final ScriptEngine scriptEngine;
    private final Gson gson;
    public static final String TRIPLE_BACKTICK = "```";

    public AdminCommands(VahrhedralBot bot) {
        dispatcher = new CommandDispatcher(bot, "");
        loc = bot.getLocalizer();
        scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        gson = new GsonBuilder().
                setPrettyPrinting().
                disableInnerClassSerialization().
                create();
    }

    public CommandDispatcher getAdminCommandDispatcher() {
        return dispatcher;
    }

    public void registerAdminCommands() {
        CommandDispatcher d = dispatcher;
        d.registerAlwaysActiveCommand("commands.admin.start", this::adminStart);
        d.registerAlwaysActiveCommand("commands.admin.stop", this::adminStop);
        d.registerAlwaysActiveCommand("commands.admin.status", this::adminStatus);
        d.registerAlwaysActiveCommand("commands.admin.kill", this::adminKill);
        d.registerAlwaysActiveCommand("commands.admin.restart", this::adminRestart);
        d.registerAlwaysActiveCommand("commands.admin.blacklist", this::adminBlacklist);
        d.registerAlwaysActiveCommand("commands.admin.pardon", this::adminPardon);
        d.registerAlwaysActiveCommand("commands.admin.join", this::adminJoin);
        d.registerAlwaysActiveCommand("commands.admin.telegram", this::adminTelegram);
        d.registerAlwaysActiveCommand("commands.admin.raw", this::adminRaw);
        d.registerAlwaysActiveCommand("commands.admin.prefix", this::adminPrefix);
        d.registerAlwaysActiveCommand("commands.admin.sandwich", this::makeSandwich);
        d.registerAlwaysActiveCommand("commands.admin.eval", this::eval);
    }

    private void adminStart(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        if (mainDispatcher.active().compareAndSet(false, true)) {
            apiClient.sendMessage(loc.localize("commands.admin.start.response.ok"),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.admin.start.response.already_started"),
                    context.getMessage().getChannelId());
        }
    }

    private void adminStop(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        CommandDispatcher mainDispatcher = context.getBot().getMainCommandDispatcher();
        if (mainDispatcher.active().compareAndSet(true, false)) {
            apiClient.sendMessage(loc.localize("commands.admin.stop.response.ok"),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.admin.stop.response.already_stopped"),
                    context.getMessage().getChannelId());
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
        if (args.contains(loc.localize("commands.admin.status.subcommand.servers"))) {
            serverDetail = listServers(apiClient);
        }
        String response = loc.localize("commands.admin.status.response.format",
                loc.localize("commands.admin.status.response.state." +
                        (mainDispatcher.active().get() ? "running" : "stopped")),
                serverDetail,
                uptime,
                memory,
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        apiClient.sendMessage(response, context.getMessage().getChannelId());
    }

    private String listServers(DiscordApiClient apiClient) {
        String serverDetail;
        StringJoiner serverJoiner = new StringJoiner(",\n");
        for (Server server : apiClient.getServers()) {
            serverJoiner.add(loc.localize("commands.admin.status.response.servers.entry", server.getName(), server.getId()));
        }
        serverDetail = loc.localize("commands.admin.status.response.servers.format",
                apiClient.getServers().size(), serverJoiner.toString());
        return serverDetail;
    }

    private String getMemoryInfo(MemoryUsage heapMemoryUsage) {
        return loc.localize("commands.admin.status.response.memory.format",
                heapMemoryUsage.getUsed() / 1048576L,
                heapMemoryUsage.getCommitted() / 1048576L,
                heapMemoryUsage.getMax() / 1048576L);
    }

    private void adminKill(MessageContext context, String args) {
        context.getApiClient().sendMessage(loc.localize("commands.admin.kill.response"),
                context.getMessage().getChannelId());
        context.getBot().shutdown();
    }

    private void adminRestart(MessageContext context, String args) {
        context.getApiClient().sendMessage(loc.localize("commands.admin.restart.response"),
                context.getMessage().getChannelId());
        context.getBot().shutdown(20);
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
            apiClient.sendMessage(loc.localize("commands.admin.blacklist.response.not_found"),
                    context.getMessage().getChannelId());
            return;
        }
        if (bot.getConfig().getAdmins().contains(user.getId())) {
            apiClient.sendMessage(loc.localize("commands.admin.blacklist.response.admin"),
                    context.getMessage().getChannelId());
            return;
        }
        bot.getConfig().getBlacklist().add(user.getId());
        bot.saveConfig();
        apiClient.sendMessage(loc.localize("commands.admin.blacklist.response.format",
                user.getUsername()),
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
            res = loc.localize("commands.admin.blacklist.response.list.none");
        }
        apiClient.sendMessage(loc.localize("commands.admin.blacklist.response.list.format", res),
                context.getMessage().getChannelId());
    }

    private void adminPardon(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (args.isEmpty()) {
            apiClient.sendMessage(loc.localize("commands.admin.pardon.response.no_user"),
                    context.getMessage().getChannelId());
            return;
        }
        User user = findUser(context, args);
        if (user == NO_USER) {
            apiClient.sendMessage(loc.localize("commands.admin.pardon.response.not_found"),
                    context.getMessage().getChannelId());
            return;
        }
        boolean removed = context.getBot().getConfig().getBlacklist().remove(user.getId());
        context.getBot().saveConfig();
        if (removed) {
            apiClient.sendMessage(loc.localize("commands.admin.pardon.response.format",
                    user.getUsername()),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.admin.pardon.response.not_blacklisted",
                    user.getUsername()),
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
            apiClient.sendMessage(loc.localize("commands.admin.join.response.invalid"),
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
                apiClient.sendMessage(loc.localize("commands.admin.join.response.http_error", response.getStatus()),
                        context.getMessage().getChannelId());
            }
        } catch (UnirestException e) {
            VahrhedralBot.LOGGER.warn("Unable to join using invite link", e);
            apiClient.sendMessage(loc.localize("commands.admin.join.response.network_error"),
                    context.getMessage().getChannelId());
        }
    }

    private void adminTelegram(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        String[] split = s.split(" ", 2);
        if (split.length != 2) {
            apiClient.sendMessage(loc.localize("commands.admin.telegram.response.invalid"),
                    context.getMessage().getChannelId());
            return;
        }
        User[] mentions = context.getMessage().getMentions();
        apiClient.sendMessage(split[1], split[0], Arrays.stream(mentions).
                map(User::getId).
                collect(Collectors.toList()).toArray(new String[mentions.length]));
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
        try {
            OutboundMessage outboundMessage = new Gson().fromJson(raw, OutboundMessage.class);
            context.getApiClient().sendMessage(outboundMessage.getContent(),
                    channel,
                    outboundMessage.getMentions());
        } catch (JsonParseException e) {
            context.getApiClient().sendMessage(loc.localize("commands.admin.raw.response.invalid"),
                    context.getMessage().getChannelId());
        }
    }

    private void adminPrefix(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        if (s.isEmpty()) {
            apiClient.sendMessage(loc.localize("commands.admin.prefix.response.get",
                    bot.getConfig().getCommandPrefix()),
                    context.getMessage().getChannelId());
        } else {
            context.getBot().getMainCommandDispatcher().setCommandPrefix(s);
            bot.getConfig().setCommandPrefix(s);
            bot.saveConfig();
            apiClient.sendMessage(loc.localize("commands.admin.prefix.response.set",
                    bot.getConfig().getCommandPrefix()),
                    context.getMessage().getChannelId());
        }
    }

    private void makeSandwich(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (loc.localize("commands.admin.sandwich.magic_word").equalsIgnoreCase(args) ||
                new Random().nextBoolean()) {
            apiClient.sendMessage(loc.localize("commands.admin.sandwich.response"),
                    context.getMessage().getChannelId());
        } else {
            apiClient.sendMessage(loc.localize("commands.admin.sandwich.response.magic"),
                    context.getMessage().getChannelId());
        }
    }

    private void eval(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        if (!args.startsWith(TRIPLE_BACKTICK) || !args.endsWith(TRIPLE_BACKTICK)) {
            apiClient.sendMessage("##TEMPORARY_MSG##Use triple backtick code block", message.getChannelId());
            return;
        }
        try {
            ScriptHelper helper = new ScriptHelper(context);
            ScriptContext scriptContext = new SimpleScriptContext();
            Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("_", helper);
            bindings.put("ctx", context);
            bindings.put("msg", context.getMessage());
            bindings.put("cid", context.getMessage().getChannelId());
            bindings.put("author", context.getMessage().getAuthor());
            bindings.put("bot", context.getBot());
            bindings.put("loc", loc);
            bindings.put("api", context.getApiClient());
            String baseScript = args.substring(TRIPLE_BACKTICK.length(),
                    args.length() - TRIPLE_BACKTICK.length());
            Object ret = scriptEngine.eval(baseScript, scriptContext);
            if (!helper.suppressOutput) {
                String retStr = "null";
                if (ret != null) {
                    if (helper.outputAsJson) {
                        retStr = gson.toJson(ret);
                    } else {
                        retStr = ret.toString();
                    }
                }
                apiClient.sendMessage(TRIPLE_BACKTICK + retStr + TRIPLE_BACKTICK,
                        message.getChannelId());
            }
        } catch (Exception | StackOverflowError e) {
            VahrhedralBot.LOGGER.warn("Unable to evaluate script", e);
            String msg = e.getLocalizedMessage();
            if (msg == null) {
                msg = e.getClass().getSimpleName();
            }
            apiClient.sendMessage(TRIPLE_BACKTICK + msg + TRIPLE_BACKTICK, message.getChannelId());
        }
    }

    public class ScriptHelper {

        private final MessageContext context;
        public boolean suppressOutput;
        public boolean outputAsJson;

        public ScriptHelper(MessageContext context) {
            this.context = context;
            suppressOutput = false;
        }

        public void sendMessage(String content) {
            sendMessageCid(content, context.getMessage().getChannelId());
        }

        public void sendMessageCid(String content, String cid) {
            context.getApiClient().sendMessage(content, cid);
        }

        public void suppress() {
            suppressOutput = true;
        }

        public void outputAsJson() {
            outputAsJson = true;
        }

        public Object field(Object object, String fieldName) throws Exception {
            if (fieldName.contains(".")) {
                String[] split = fieldName.split("\\.");
                Object ret = object;
                for (int i = 0; i < split.length; i++) {
                    ret = field(ret, split[i]);
                }
                return ret;
            }
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        }

        public Method method(Object object, String methodName, String[] argSimpleClassNames) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                        method.getParameterCount() == argSimpleClassNames.length) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    for (int i = 0; i < argSimpleClassNames.length; i++) {
                        if (!argSimpleClassNames[i].equals(parameterTypes[i].getSimpleName())) {
                            return null;
                        }
                    }
                    method.setAccessible(true);
                    return method;
                }
            }
            return null;
        }

        public String fields(Object object) {
            Field[] fields = object.getClass().getDeclaredFields();
            StringJoiner j = new StringJoiner("\n");
            for (Field field : fields) {
                j.add(field.getType().getName() + " " + field.getName());
            }
            return j.toString();
        }

        public String methods(Object object) {
            Method[] methods = object.getClass().getDeclaredMethods();
            StringJoiner j = new StringJoiner("\n");
            for (Method method : methods) {
                j.add(method.toString());
            }
            return j.toString();
        }
    }
}
