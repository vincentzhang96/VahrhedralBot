package co.phoenixlab.discord.ncs;

public interface CommandHandler {

    void handleCommand(CommandContext context, CommandConfig config, String args);
}
