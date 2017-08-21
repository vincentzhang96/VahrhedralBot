package com.divinitor.discord.vahrhedralbot.component.modcmd;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Server;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorage;

public class DmWelcomeCommand implements Command {

    public static final String DMWELCOME_MESSAGE_STORAGE_KEY = "dmwelcome.message";
    private final VahrhedralBot bot;
    private final Localizer loc;

    public DmWelcomeCommand(VahrhedralBot bot) {
        this.bot = bot;
        this.loc = bot.getLocalizer();
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

        ServerStorage storage = bot.getEntryPoint().getServerStorage().getOrInit(server.getId());

        if (args.equalsIgnoreCase("none")) {
            storage.delete(DMWELCOME_MESSAGE_STORAGE_KEY);
            apiClient.sendMessage(loc.localize("commands.mod.joindm.response.none"), channel);
            return;
        }

        storage.put(DMWELCOME_MESSAGE_STORAGE_KEY, args);

        DmWelcomeCommandListener listener = bot.getEntryPoint().getComponent(DmWelcomeCommandListener.class);
        listener.send(server, context.getAuthor());

        apiClient.sendMessage(loc.localize("commands.mod.joindm.response.set"), channel);
    }
}
