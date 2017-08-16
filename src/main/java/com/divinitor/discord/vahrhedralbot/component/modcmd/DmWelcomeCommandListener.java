package com.divinitor.discord.vahrhedralbot.component.modcmd;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.api.event.MemberChangeEvent;
import com.divinitor.discord.vahrhedralbot.AbstractBotComponent;
import com.divinitor.discord.vahrhedralbot.EntryPoint;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorage;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorageManager;
import com.google.common.eventbus.Subscribe;
import redis.clients.jedis.JedisPool;

public class DmWelcomeCommandListener extends AbstractBotComponent {

    private JedisPool jedisPool;
    private ServerStorageManager storageManager;

    @Override
    public void register(EntryPoint entryPoint) throws Exception {
        super.register(entryPoint);

        this.jedisPool = entryPoint.getBot().getJedisPool();
        this.storageManager = entryPoint.getServerStorage();
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Subscribe
    public void onUserJoin(MemberChangeEvent event) {
        if (event.getMemberChange() != MemberChangeEvent.MemberChange.ADDED) {
            return;
        }

        send(event.getServer(), getBot().getApiClient().getUserById(event.getMember().getUser().getId()));
    }

    public void send(Server server, User user) {
        ServerStorage storage = storageManager.getOrInit(server.getId());
        String template = storage.getString("dmwelcome.message");
        if (template == null) {
            return;
        }

        String msgContent = format(server, user, template);

        DiscordApiClient apiClient = getBot().getApiClient();
        Channel dmChannel = apiClient.createDM(user);

        apiClient.sendMessage(msgContent, dmChannel);
    }

    private String format(Server server, User user, String template) {
        return template
            .replace("$n", user.getUsername())
            .replace("$s", server.getName());
    }
}
