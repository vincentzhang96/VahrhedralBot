package co.phoenixlab.discord.ncs;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;

import java.util.concurrent.Future;

public class CommandApiDelegate {

    private final DiscordApiClient apiClient;
    private final CommandContext context;

    public CommandApiDelegate(DiscordApiClient apiClient, CommandContext context) {
        this.apiClient = apiClient;
        this.context = context;
    }

    public void fireMessage(String message) {

    }

    public Future<Message>

}
