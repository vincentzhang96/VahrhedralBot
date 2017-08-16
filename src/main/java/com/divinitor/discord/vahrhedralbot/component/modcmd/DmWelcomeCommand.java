package com.divinitor.discord.vahrhedralbot.component.modcmd;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Server;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorage;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorageManager;
import redis.clients.jedis.Jedis;

public class DmWelcomeCommand implements Command {

    private final VahrhedralBot bot;
    private final Localizer loc;
    private ServerStorageManager storageManager;

    public DmWelcomeCommand(VahrhedralBot bot) {
        this.bot = bot;
        this.loc = bot.getLocalizer();
        this.storageManager = bot.getEntryPoint().getServerStorage();
    }

    @Override
    public void handleCommand(MessageContext context, String args) {
        if (args.isEmpty()) {
            //  TODO Display help
            return;
        }

        Server server = context.getServer();
        if (server == null || server == DiscordApiClient.NO_SERVER) {
            return;
        }

        DiscordApiClient apiClient = context.getApiClient();
        Channel channel = context.getChannel();

        ServerStorage storage = storageManager.getOrInit(server.getId());

        if (args.equalsIgnoreCase("none")) {
            storage.delete("dmwelcome.message");
            apiClient.sendMessage(loc.localize("commands.mod.joindm.response.none"), channel);
            return;
        }

        storage.put("dmwelcome.message", args);



    }
}
