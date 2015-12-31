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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static co.phoenixlab.discord.VahrhedralBot.LOGGER;
import static co.phoenixlab.discord.api.DiscordApiClient.NO_ROLE;

public class ModCommands {

    private final CommandDispatcher dispatcher;
    private Localizer loc;
    private final VahrhedralBot bot;
    private final DiscordApiClient apiClient;

    private final Consumer<MemberChangeEvent> memberJoinListener;

    private final Map<String, ServerTimeoutStorage> timeoutStorage;

    public ModCommands(VahrhedralBot bot) {
        this.bot = bot;
        dispatcher = new CommandDispatcher(bot, "");
        loc = bot.getLocalizer();
        apiClient = bot.getApiClient();
        memberJoinListener = this::onMemberJoinedServer;
        timeoutStorage = new HashMap<>();
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

    }

    @Subscribe
    public void onMemberJoinedServer(MemberChangeEvent memberChangeEvent) {
        if (memberChangeEvent.getMemberChange() == MemberChange.ADDED) {
            User user = memberChangeEvent.getMember().getUser();
            Server server = memberChangeEvent.getServer();
            if (isUserTimedOut(user, server)) {
                refreshTimeoutOnEvade(user, server);
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
     * @param user The user to add to the timeout role
     * @param server The server on which to add the user to the timeout role
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
        ServerTimeoutStorage storage = timeoutStorage.get(server.getId());
        if (storage != null) {
            ServerTimeout timeout = storage.getTimeouts().get(user.getId());
            if (timeout != null) {
                Instant now = Instant.now();
                if (timeout.getEndTime().compareTo(now) > 0) {
                    return true;
                } else {
                    removeTimeout(user, server, storage, timeout);
                }
            }
        }
        return false;
    }

    public void removeTimeout(User user, Server server, ServerTimeoutStorage storage, ServerTimeout timeout) {
        //  TODO
    }

    /**
     * Removes the timeout role from the given user. This does NOT create or manage any storage/persistence, it only
     * sets the user's roles
     * @param user The user to remove the timeout role
     * @param server The server on which to remove the user from the timeout role
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
}
