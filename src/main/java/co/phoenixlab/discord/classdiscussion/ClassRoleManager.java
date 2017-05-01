package co.phoenixlab.discord.classdiscussion;

import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.MessageReactionUpdate;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.api.event.MessageReactionChangeEvent;
import co.phoenixlab.discord.api.event.MessageReactionChangeEvent.ReactionChange;
import co.phoenixlab.discord.api.event.ServerJoinLeaveEvent;
import co.phoenixlab.discord.cfg.ClassDiscussionConfig;
import com.google.common.eventbus.Subscribe;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class ClassRoleManager {

    private ClassDiscussionConfig config;

    private Map<String, EmojiRoleBinding> emojiIdToBindings;

    private BlockingDeque<UpdateInfo> pendingUpdates;
    private Future<Void> taskFuture;
    private AtomicBoolean run;

    public ClassRoleManager(ClassDiscussionConfig config) {
        this.config = config;
        emojiIdToBindings = this.config.getEmojiRoleBindings()
            .stream()
            .collect(Collectors.toMap(EmojiRoleBinding::getEmojiId, identity()));
        pendingUpdates = new LinkedBlockingDeque<>();
        run = new AtomicBoolean();
    }

    @Subscribe
    public void onServerJoinLeave(ServerJoinLeaveEvent event) {
        if (!config.getServerId().equals(event.getServer().getId())) {
            return;
        }
        if (event.isJoin() && taskFuture == null) {
            //  Start workers and listeners
            VahrhedralBot.LOGGER.info("Starting {} server watcher", event.getServer().getName());
            run.set(true);
            taskFuture = event.getApiClient().getExecutorService().submit(this::doUpdateLoop);
        } else {
            VahrhedralBot.LOGGER.info("Stopping {} server watcher", event.getServer().getName());
            //  Poison the queue and wait for the task to finish
            run.set(false);
            try {
                taskFuture.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException tme) {
                VahrhedralBot.LOGGER
                    .info("Class discussion server watcher didn't stop within 2 seconds of poisoning, interrupting...");
                taskFuture.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                VahrhedralBot.LOGGER.warn("Exception while waiting for ClassRoleManager event loop to finish", e);
                taskFuture.cancel(true);
            }
            taskFuture = null;
        }
    }

    @Subscribe
    public void onReactionChange(MessageReactionChangeEvent event) {
        MessageReactionUpdate update = event.getUpdate();
        if (!config.getServerId().equals(event.getServer().getId())) {
            return;
        }
        if (!config.getChannelId().equals(update.getChannelId())) {
            return;
        }
        EmojiRoleBinding binding = emojiIdToBindings.get(update.getEmoji().getId());
        if (binding != null) {
            //  Check on correct message
            if (!binding.getParentMsg().equals(update.getMessageId())) {
                return;
            }
            UpdateInfo info = new UpdateInfo(update.getUserId(),
                binding.getRoleId(),
                event.getType() == ReactionChange.ADDED,
                event.getApiClient());
            pendingUpdates.add(info);
        }
    }

    private void addRole(String userId, String roleId, DiscordApiClient apiClient) {
        Server server = apiClient.getServerByID(config.getServerId());
        if (server == DiscordApiClient.NO_SERVER) {
            VahrhedralBot.LOGGER.warn("Unable to get server {}", config.getServerId());
            return;
        }
        Member member = apiClient.getUserMember(userId, server);
        User user = member.getUser();
        if (member == DiscordApiClient.NO_MEMBER) {
            VahrhedralBot.LOGGER.warn("Unable to find member with ID {} in {} server", userId, server.getName());
        }
        Set<String> roles = new HashSet<>(member.getRoles());
        if (!roles.contains(roleId)) {
            roles.add(roleId);
            apiClient.updateRoles(user, server, roles, true);
        }
    }

    private void deleteRole(String userId, String roleId, DiscordApiClient apiClient) {
        Server server = apiClient.getServerByID(config.getServerId());
        if (server == DiscordApiClient.NO_SERVER) {
            VahrhedralBot.LOGGER.warn("Unable to get server {}", config.getServerId());
            return;
        }
        Member member = apiClient.getUserMember(userId, server);
        User user = member.getUser();
        if (member == DiscordApiClient.NO_MEMBER) {
            VahrhedralBot.LOGGER.warn("Unable to find member with ID {} in {} server", userId, server.getName());
        }
        Set<String> roles = new HashSet<>(member.getRoles());
        if (roles.contains(roleId)) {
            roles.remove(roleId);
            apiClient.updateRoles(user, server, roles, true);
        }
    }

    private Void doUpdateLoop() {
        UpdateInfo info = null;
        VahrhedralBot.LOGGER.info("Started main loop for server watcher");
        while(run.get()) {
            do {
                try {
                    info = pendingUpdates.take();
                } catch (InterruptedException e) {
                    //  Interrupted, check if task die
                    if (!run.get()) {
                        return null;
                    }
                }
            } while(info == null);
            if (!run.get()) {
                return null;
            }
            try {
                if (info.isAdd()) {
                    addRole(info.getUserId(), info.getRoleId(), info.getApiClient());
                } else {
                    deleteRole(info.getUserId(), info.getRoleId(), info.getApiClient());
                }
                //  Sleep for 1/2 second after processing to stay below rate limit
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                //  Interrupted, check if task die
                if (!run.get()) {
                    return null;
                }
            } catch (Exception e) {
                VahrhedralBot.LOGGER.warn("Exception during update loop for server", e);
            }
            info = null;
        }
        return null;
    }

    private static class UpdateInfo {
        private final String userId;
        private final String roleId;
        private final boolean add;
        private final DiscordApiClient apiClient;

        UpdateInfo(String userId, String roleId, boolean add, DiscordApiClient apiClient) {
            this.userId = userId;
            this.roleId = roleId;
            this.add = add;
            this.apiClient = apiClient;
        }

        public String getUserId() {
            return userId;
        }

        public String getRoleId() {
            return roleId;
        }

        public boolean isAdd() {
            return add;
        }

        public DiscordApiClient getApiClient() {
            return apiClient;
        }
    }
}
