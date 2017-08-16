package co.phoenixlab.discord.commands.admin;

import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;

public class DmCommand implements Command {

    private final VahrhedralBot bot;

    public DmCommand(VahrhedralBot bot) {
        this.bot = bot;
    }

    @Override
    public void handleCommand(MessageContext context, String args) {
        String[] split = args.split(" ", 2);
        String userId = split[0];
        String content = split[1];

        DiscordApiClient api = context.getApiClient();
        Channel ch = api.createDM(userId);
        api.sendMessage(content, ch);
    }
}
