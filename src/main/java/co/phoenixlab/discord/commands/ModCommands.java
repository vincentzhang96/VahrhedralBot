package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.SafeNav;
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static co.phoenixlab.discord.VahrhedralBot.LOGGER;
import static co.phoenixlab.discord.api.DiscordApiClient.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class ModCommands {

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
        //  TODO
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
    public void applyTimeoutRole(User user, Server server, Channel invocationChannel) {
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
        removeTimeoutRole(user, server, apiClient.getChannelById(server.getId()));
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
    public void removeTimeoutRole(User user, Server server, Channel invocationChannel) {
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
        try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
            ServerTimeoutStorage storage = gson.fromJson(reader, ServerTimeoutStorage.class);
            if (storage != null) {
                Server server = apiClient.getServerByID(storage.getServerId());
                if (server == NO_SERVER) {
                    LOGGER.warn("Rejecting {} server storage file: server not found", storage.getServerId());
                    return;
                }
                timeoutStorage.put(storage.getServerId(), storage);
                LOGGER.info("Loaded {} ({}) server storage file", server.getName(), server.getId());
                //  Prune expired entries
                for (Iterator<Map.Entry<String, ServerTimeout>> iter = storage.getTimeouts().entrySet().iterator();
                     iter.hasNext(); ) {
                    Map.Entry<String, ServerTimeout> e = iter.next();
                    ServerTimeout timeout = e.getValue();
                    String userId = timeout.getUserId();
                    User user = apiClient.getUserById(userId, server);
                    if (!isUserTimedOut(userId, server.getId())) {
                        //  Purge!
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
                            apiClient.sendMessage(loc.localize("message.mod.timeout.expire",
                                    user.getId()),
                                    server.getId());
                            removeTimeoutRole(user, server, apiClient.getChannelById(server.getId()));
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
            LOGGER.warn("Unable to load server storage file " + path.getFileName(), e);
            return;
        }
    }
}
