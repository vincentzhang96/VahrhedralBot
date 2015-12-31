package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.lang.number.ParseLong;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Role;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.api.event.MemberChangeEvent;
import co.phoenixlab.discord.api.event.MemberChangeEvent.MemberChange;
import co.phoenixlab.discord.commands.tempstorage.ServerTimeout;
import co.phoenixlab.discord.commands.tempstorage.ServerTimeoutStorage;
import co.phoenixlab.discord.util.WeakEventSubscriber;
import co.phoenixlab.discord.util.adapters.DurationGsonTypeAdapter;
import co.phoenixlab.discord.util.adapters.InstantGsonTypeAdapter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.phoenixlab.discord.VahrhedralBot.LOGGER;
import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class ModCommands {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("L dd HH:mm:ss z");
    private final CommandDispatcher dispatcher;
    private Localizer loc;
    private final VahrhedralBot bot;
    private final DiscordApiClient apiClient;

    private final Consumer<MemberChangeEvent> memberJoinListener;

    private final Map<String, ServerTimeoutStorage> timeoutStorage;

    private static final Path serverStorageDir = Paths.get("config/tempServerStorage/");

    private final Gson gson;

    private final ScheduledExecutorService timeoutService;

    public ModCommands(VahrhedralBot bot) {
        this.bot = bot;
        dispatcher = new CommandDispatcher(bot, "");
        loc = bot.getLocalizer();
        apiClient = bot.getApiClient();
        memberJoinListener = this::onMemberJoinedServer;
        timeoutStorage = new HashMap<>();
        gson = new GsonBuilder().
                registerTypeAdapter(Instant.class, new InstantGsonTypeAdapter()).
                registerTypeAdapter(Duration.class, new DurationGsonTypeAdapter()).
                create();
        timeoutService = Executors.newScheduledThreadPool(10);
    }

    public CommandDispatcher getModCommandDispatcher() {
        return dispatcher;
    }

    public void registerModCommands() {
        CommandDispatcher d = dispatcher;
        d.registerAlwaysActiveCommand("commands.mod.timeout", this::timeout);
        d.registerAlwaysActiveCommand("commands.mod.stoptimeout", this::stopTimeout);
        d.registerAlwaysActiveCommand("commands.mod.settimeoutrole", this::setTimeoutRole);

        EventBus eventBus = bot.getApiClient().getEventBus();
        eventBus.register(new WeakEventSubscriber<>(memberJoinListener, eventBus, MemberChangeEvent.class));
    }

    private void timeout(MessageContext context, String args) {
        Channel channel = context.getChannel();
        if (!args.isEmpty()) {
            String[] split = args.split(" ", 2);
            String uid = split[0];
            if (uid.length() > 4) {
                if (uid.startsWith("<@")) {
                    uid = uid.substring(2, uid.length() - 1);
                }
                Server server = context.getServer();
                String serverId = server.getId();
                User user = apiClient.getUserById(uid, server);
                if (user == NO_USER) {
                    user = new User("UNKNOWN", uid, "", null);
                }
                final User theUser = user;
                if (split.length == 2) {
                    Duration duration = parseDuration(split[1]);
                    if (duration != null) {
                        ServerTimeout timeout = new ServerTimeout(duration,
                                Instant.now(), user.getId(), serverId,
                                user.getUsername(), context.getAuthor().getId());
                        ServerTimeoutStorage storage = timeoutStorage.get(serverId);
                        if (storage == null) {
                            storage = new ServerTimeoutStorage(serverId);
                            timeoutStorage.put(serverId, storage);
                        }
                        storage.getTimeouts().put(user.getId(), timeout);
                        ScheduledFuture future = timeoutService.schedule(() ->
                                onTimeoutExpire(theUser, server), duration.getSeconds(), TimeUnit.SECONDS);
                        timeout.setTimerFuture(future);
                        saveServerTimeoutStorage(storage);
                        if (applyTimeoutRole(user, server, channel)) {
                            apiClient.sendMessage(loc.localize("commands.mod.timeout.response",
                                    user.getUsername(), user.getId(),
                                    formatDuration(duration),
                                    formatInstant(timeout.getEndTime())),
                                    channel);
                        }
                        //  No else with error - applyTimeoutRole does that for us
                        return;
                    } else {
                        LOGGER.warn("Invalid duration format");
                    }
                } else if (split.length == 1) {
                    if (isUserTimedOut(user, server)) {
                        ServerTimeout timeout = SafeNav.of(timeoutStorage.get(serverId)).
                                next(ServerTimeoutStorage::getTimeouts).
                                next(m -> m.get(theUser.getId())).get();
                        //  Timeout cannot be null since we just checked
                        User timeoutIssuer = apiClient.getUserById(timeout.getIssuedByUserId(), server);
                        apiClient.sendMessage(loc.localize("commands.mod.timeout.response.check",
                                user.getUsername(), user.getId(),
                                formatDuration(Duration.between(Instant.now(), timeout.getEndTime())),
                                formatInstant(timeout.getEndTime()),
                                timeoutIssuer.getUsername(), timeout.getIssuedByUserId()),
                                channel);
                    } else {
                        apiClient.sendMessage(loc.localize("commands.mod.timeout.response.check.not_found",
                                user.getUsername(), user.getId()),
                                channel);
                    }
                    return;
                } else {
                    LOGGER.warn("Split length not 1 or 2, was {}", split.length);
                }
            } else {
                LOGGER.warn("UID/mention not long enough");
            }
        } else {
            LOGGER.warn("Args was empty");
        }
        apiClient.sendMessage(loc.localize("commands.mod.timeout.response.invalid"),
                channel);
    }

    private Duration parseDuration(String s) {
        //  TODO make this work better
        return Duration.ofSeconds(ParseLong.parseOrDefault(s, 0));

        /*
        if (s.isEmpty()) {
            return null;
        }
        long seconds = 0;
        char[] chars = s.toLowerCase().toCharArray();
        int pos = 0;
        StringBuilder builder = new StringBuilder();
        while ((pos = readDurationToken(chars, pos, builder)) < chars.length) {
            String token = builder.toString();
            char last = token.charAt(token.length() - 1);
            String subToken = token.substring(0, token.length() - 1);
            int multiplier = 1;
            switch (last) {
                case 'd':
                    multiplier = 86400;
                    break;
                case 'h':
                    multiplier = 3600;
                    break;
                case 'm':
                    multiplier = 60;
                    break;
                case 's':
                    multiplier = 1;
                    break;
                default:
                    multiplier = 0;
                    break;
            }
            long addTime;
            if (multiplier == 0) {
                addTime = ParseLong.parseOrDefault(token, -1);
                multiplier = 1;
            } else {
                addTime = ParseLong.parseOrDefault(subToken, -1);
            }
            if (addTime == -1) {
                LOGGER.warn("Invalid duration format: Parse failed for t:{} st:{}", token, subToken);
                return null;
            }
            addTime *= multiplier;
            seconds += addTime;
            builder.setLength(0);
        }
        return Duration.ofSeconds(seconds);
        */
    }

    private int readDurationToken(char[] chars, int start, StringBuilder builder) {
        int i;
        for (i = start; i < chars.length; i++) {
            char c = chars[i];
            if (c != ' ') {
                builder.append(c);
                switch (c) {
                    case 's':
                    case 'm':
                    case 'h':
                    case 'd':
                        return i + 1;
                }
            }
        }
        return i;
    }

    private void stopTimeout(MessageContext context, String args) {
        //  TODO
    }

    private void setTimeoutRole(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        if (args.isEmpty()) {
            apiClient.sendMessage(loc.localize("commands.mod.settimeoutrole.response.missing"),
                    context.getChannel());
            return;
        }
        Role role = apiClient.getRole(args, context.getServer());
        if (role == NO_ROLE) {
            apiClient.sendMessage(loc.localize("commands.mod.settimeoutrole.response.not_found",
                    args),
                    context.getChannel());
            return;
        }
        String serverId = context.getServer().getId();
        ServerTimeoutStorage storage = timeoutStorage.get(serverId);
        if (storage == null) {
            storage = new ServerTimeoutStorage(serverId);
            timeoutStorage.put(serverId, storage);
        }
        storage.setTimeoutRoleId(role.getId());
        apiClient.sendMessage(loc.localize("commands.mod.settimeoutrole.response",
                role.getName(), role.getId()),
                context.getChannel());
        saveServerTimeoutStorage(storage);
    }

    @Subscribe
    public void onMemberJoinedServer(MemberChangeEvent memberChangeEvent) {
        if (memberChangeEvent.getMemberChange() == MemberChange.ADDED) {
            User user = memberChangeEvent.getMember().getUser();
            Server server = memberChangeEvent.getServer();
            if (isUserTimedOut(user, server)) {
                refreshTimeoutOnEvade(user, server);
            } else {
                onTimeoutExpire(user, server);
            }
        }
    }

    private void refreshTimeoutOnEvade(User user, Server server) {
        ServerTimeout timeout = SafeNav.of(timeoutStorage.get(server.getId())).
                next(ServerTimeoutStorage::getTimeouts).
                next(timeouts -> timeouts.get(user.getId())).
                get();
        if (timeout == null) {
            LOGGER.warn("Attempted to refresh a timeout on a user who was not timed out!");
            return;
        }
        LOGGER.info("User {} ({}) attempted to evade a timeout on {} ({})!",
                user.getUsername(), user.getId(),
                server.getName(), server.getId());
        Channel channel = apiClient.getChannelById(server.getId(), server);
        apiClient.sendMessage(loc.localize("listener.mod.timeout.on_evasion",
                user.getId(), formatDuration(Duration.between(Instant.now(), timeout.getEndTime())),
                formatInstant(timeout.getEndTime())),
                channel);
        applyTimeoutRole(user, server, channel);

    }

    /**
     * Applies the timeout role to the given user. This does NOT create or manage any storage/persistence, it only
     * sets the user's roles
     *
     * @param user              The user to add to the timeout role
     * @param server            The server on which to add the user to the timeout role
     * @param invocationChannel The channel to send messages on error
     */
    public boolean applyTimeoutRole(User user, Server server, Channel invocationChannel) {
        String serverId = server.getId();
        ServerTimeoutStorage storage = timeoutStorage.get(serverId);
        String serverName = server.getName();
        if (storage != null && storage.getTimeoutRoleId() != null) {
            String timeoutRoleId = storage.getTimeoutRoleId();
            Role timeoutRole = apiClient.getRole(timeoutRoleId, server);
            if (timeoutRole != NO_ROLE) {
                //  Add role to user
                Set<Role> userRoles = apiClient.getMemberRoles(apiClient.getUserMember(user, server), server);
                //  Push the ban role to the front
                LinkedHashSet<String> newRoles = new LinkedHashSet<>(userRoles.size() + 1);
                newRoles.add(timeoutRoleId);
                userRoles.stream().map(Role::getId).
                        forEach(newRoles::add);
                //  Update
                apiClient.updateRoles(user, server, newRoles);
                return userRoles.size() < newRoles.size();
            } else {
                LOGGER.warn("Timeout role ID {} for server {} ({}) does not exist",
                        timeoutRoleId, serverName, serverId);
                apiClient.sendMessage(loc.localize("message.mod.timeout.bad_role", timeoutRoleId),
                        invocationChannel);
            }
        } else {
            storage = new ServerTimeoutStorage(serverId);
            timeoutStorage.put(serverId, storage);
            LOGGER.warn("Timeout role for server {} ({}) is not configured",
                    storage.getTimeoutRoleId(), serverName, serverId);
            apiClient.sendMessage(loc.localize("message.mod.timeout.not_configured"), invocationChannel);
        }
        return false;
    }

    public boolean isUserTimedOut(User user, Server server) {
        return isUserTimedOut(user.getId(), server.getId());
    }

    public boolean isUserTimedOut(String userId, String serverId) {
        ServerTimeoutStorage storage = timeoutStorage.get(serverId);
        if (storage != null) {
            ServerTimeout timeout = storage.getTimeouts().get(userId);
            if (timeout != null) {
                Instant now = Instant.now();
                return timeout.getEndTime().compareTo(now) > 0;
            }
        }
        return false;
    }

    public void cancelTimeout(User user, Server server, Channel invocationChannel) {
        ServerTimeoutStorage storage = timeoutStorage.get(server.getId());
        removeTimeoutRole(user, server, apiClient.getChannelById(server.getId()));
        if (storage != null) {
            ServerTimeout timeout = storage.getTimeouts().remove(user.getId());
            if (timeout != null) {
                SafeNav.of(timeout.getTimerFuture()).ifPresent(f -> f.cancel(true));
                LOGGER.info("Cancelling timeout for {} ({}) in {} ({})",
                        user.getUsername(), user.getId(),
                        server.getName(), server.getId());
                apiClient.sendMessage(loc.localize("commands.mod.stoptimeout.response",
                        user.getUsername(), user.getId()),
                        invocationChannel);
                return;
            }
        }
        LOGGER.warn("Unable to cancel: cannot find server or timeout entry for {} ({}) in {} ({})",
                user.getUsername(), user.getId(),
                server.getName(), server.getId());
        apiClient.sendMessage(loc.localize("commands.mod.stoptimeout.response.not_found",
                user.getUsername(), user.getId()),
                invocationChannel);
    }

    public void onTimeoutExpire(User user, Server server) {
        ServerTimeoutStorage storage = timeoutStorage.get(server.getId());
        if (storage != null) {
            ServerTimeout timeout = storage.getTimeouts().remove(user.getId());
            if (timeout != null) {
                LOGGER.info("Expiring timeout for {} ({}) in {} ({})",
                        user.getUsername(), user.getId(),
                        server.getName(), server.getId());
                apiClient.sendMessage(loc.localize("message.mod.timeout.expire",
                        user.getId()),
                        server.getId());
                removeTimeoutRole(user, server, apiClient.getChannelById(server.getId()));
                return;
            }
        }
        LOGGER.warn("Unable to expire: find server or timeout entry for {} ({}) in {} ({})",
                user.getUsername(), user.getId(),
                server.getName(), server.getId());
    }

    /**
     * Removes the timeout role from the given user. This does NOT create or manage any storage/persistence, it only
     * sets the user's roles
     *
     * @param user              The user to remove the timeout role
     * @param server            The server on which to remove the user from the timeout role
     * @param invocationChannel The channel to send messages on error
     */
    public boolean removeTimeoutRole(User user, Server server, Channel invocationChannel) {
        String serverId = server.getId();
        ServerTimeoutStorage storage = timeoutStorage.get(serverId);
        String serverName = server.getName();
        if (storage != null && storage.getTimeoutRoleId() != null) {
            String timeoutRoleId = storage.getTimeoutRoleId();
            Role timeoutRole = apiClient.getRole(timeoutRoleId, server);
            if (timeoutRole != NO_ROLE) {
                //  Get roles
                Set<Role> userRoles = apiClient.getMemberRoles(apiClient.getUserMember(user, server), server);
                //  Delete the ban role
                LinkedHashSet<String> newRoles = new LinkedHashSet<>(userRoles.size() - 1);
                userRoles.stream().map(Role::getId).
                        filter(s -> !timeoutRoleId.equals(s)).
                        forEach(newRoles::add);
                //  Update
                apiClient.updateRoles(user, server, newRoles);
                return userRoles.size() == newRoles.size();
            } else {
                LOGGER.warn("Timeout role ID {} for server {} ({}) does not exist",
                        timeoutRoleId, serverName, serverId);
                apiClient.sendMessage(loc.localize("message.mod.timeout.bad_role", timeoutRoleId),
                        invocationChannel);
            }
        } else {
            storage = new ServerTimeoutStorage(serverId);
            timeoutStorage.put(serverId, storage);
            LOGGER.warn("Timeout role for server {} ({}) is not configured",
                    storage.getTimeoutRoleId(), serverName, serverId);
            apiClient.sendMessage(loc.localize("message.mod.timeout.not_configured"), invocationChannel);
        }
        return false;
    }


    public void saveServerTimeoutStorage(ServerTimeoutStorage storage) {
        try {
            Files.createDirectories(serverStorageDir);
        } catch (IOException e) {
            LOGGER.warn("Unable to create server storage directory", e);
            return;
        }
        Path serverStorageFile = serverStorageDir.resolve(storage.getServerId() + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(serverStorageFile, UTF_8, CREATE, TRUNCATE_EXISTING)) {
            gson.toJson(storage, writer);
            writer.flush();
        } catch (IOException e) {
            LOGGER.warn("Unable to write server storage file for " + storage.getServerId(), e);
            return;
        }
        LOGGER.info("Saved server {}", storage.getServerId());
    }

    public void loadServerTimeoutStorageFiles() {
        if (!Files.exists(serverStorageDir)) {
            LOGGER.info("Server storage directory doesn't exist, not loading anything");
            return;
        }
        try (Stream<Path> files = Files.list(serverStorageDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).
                    forEach(this::loadServerTimeoutStorage);
        } catch (IOException e) {
            LOGGER.warn("Unable to load server storage files", e);
            return;
        }
    }

    public void loadServerTimeoutStorage(Path path) {
        boolean purge = false;
        ServerTimeoutStorage storage;
        try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
            storage = gson.fromJson(reader, ServerTimeoutStorage.class);
            if (storage != null) {
                Server server = apiClient.getServerByID(storage.getServerId());
                if (server == NO_SERVER) {
                    LOGGER.warn("Rejecting {} server storage file: server not found", storage.getServerId());
                    return;
                }
                timeoutStorage.put(storage.getServerId(), storage);
                LOGGER.info("Loaded {} ({}) server storage file",
                        server.getName(), server.getId(), storage.getTimeoutRoleId());
                //  Prune expired entries
                for (Iterator<Map.Entry<String, ServerTimeout>> iter = storage.getTimeouts().entrySet().iterator();
                     iter.hasNext(); ) {
                    Map.Entry<String, ServerTimeout> e = iter.next();
                    ServerTimeout timeout = e.getValue();
                    String userId = timeout.getUserId();
                    User user = apiClient.getUserById(userId, server);
                    if (!isUserTimedOut(userId, server.getId())) {
                        //  Purge!
                        purge = true;
                        if (user == NO_USER) {
                            LOGGER.info("Ending timeout for departed user {} ({}) in {} ({})",
                                    timeout.getLastUsername(), userId,
                                    server.getName(), server.getId());
                            apiClient.sendMessage(loc.localize("message.mod.timeout.expire.not_found",
                                    user.getId()),
                                    server.getId());
                            //  Don't need to remove the timeout role because leaving does that for us
                        } else {
                            //  Duplicated from onTimeoutExpire except without remove since we're removing in an iter
                            LOGGER.info("Expiring timeout for {} ({}) in {} ({})",
                                    user.getUsername(), user.getId(),
                                    server.getName(), server.getId());
                            //  Only send message if they still have the role
                            if (removeTimeoutRole(user, server, apiClient.getChannelById(server.getId()))) {
                                apiClient.sendMessage(loc.localize("message.mod.timeout.expire",
                                        user.getId()),
                                        server.getId());
                            }
                        }
                        SafeNav.of(timeout.getTimerFuture()).ifPresent(f -> f.cancel(true));
                        iter.remove();
                    } else {
                        //  Start our futures
                        Duration duration = Duration.between(Instant.now(), timeout.getEndTime());
                        ScheduledFuture future = timeoutService.schedule(() ->
                                onTimeoutExpire(user, server), duration.getSeconds(), TimeUnit.SECONDS);
                        timeout.setTimerFuture(future);
                    }
                }
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Unable to load server storage file " + path.toString(), e);
            return;
        }

        if (purge) {
            saveServerTimeoutStorage(storage);
        }
    }

    public void onReady() {
        //  Load configs
        loadServerTimeoutStorageFiles();

        //  Reapply timeouts that may have dropped during downtime
        timeoutStorage.forEach((sid, st) -> {
            st.getTimeouts().forEach((uid, t) -> {
                if(isUserTimedOut(uid, sid)) {
                    //  Check if the user still has timeout role
                    Server server = apiClient.getServerByID(sid);
                    Set<Role> roles = apiClient.getMemberRoles(apiClient.getUserMember(uid, server), server);
                    Set<String> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
                    if (!roleIds.contains(st.getTimeoutRoleId())) {
                        refreshTimeoutOnEvade(apiClient.getUserById(uid, server), server);
                    }
                }
            });
        });
    }

    private String formatInstant(Instant instant) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatDuration(Duration duration) {
        StringJoiner joiner = new StringJoiner(", ");
        long days = duration.toDays();
        if (days > 0) {
            joiner.add(String.format("%,d days", days));
        }
        long hours = duration.toHours() % 24L;
        if (hours > 0) {
            joiner.add(String.format("%,d hours", hours));
        }
        long minutes = duration.toMinutes() % 60L;
        if (minutes > 0) {
            joiner.add(String.format("%,d minutes", minutes));
        }
        long seconds = duration.getSeconds() % 60L;
        if (seconds > 0) {
            joiner.add(String.format("%,d seconds", seconds));
        }
        return joiner.toString();
    }
}
