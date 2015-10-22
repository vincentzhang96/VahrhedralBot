package co.phoenixlab.discord;

import org.slf4j.Logger;
import sx.blah.discord.DiscordClient;
import sx.blah.discord.handle.obj.Message;
import sx.blah.discord.util.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class CommandDispatcher {

    private static final Logger LOGGER = VahrhedralBot.LOGGER;

    private final VahrhedralBot bot;
    private final DiscordClient discord;

    private final Map<String, CommandWrapper> commands;

    public CommandDispatcher(DiscordClient discord, VahrhedralBot bot) {
        this.discord = discord;
        this.bot = bot;
        commands = new HashMap<>();
        addHelpCommand();
    }

    private void addHelpCommand() {
        Command helpCommand = (context, args) -> {
            String prefix = bot.getConfig().getCommandPrefix();
            StringJoiner joiner = new StringJoiner("\r\n", "Available Commands\r\n", "");
            for (Map.Entry<String, CommandWrapper> entry : commands.entrySet()) {
                joiner.add(String.format("%s%s - %s", prefix, entry.getKey(), entry.getValue().helpDesc));
            }
            final String result = joiner.toString();
            final String channelId = context.getMessage().getChannel().getID();
            bot.getTaskQueue().runOnMain(() -> {
                new MessageBuilder().
                        withChannel(channelId).
                        withContent(result).
                        send();
            });
        };
        registerCommand("help", helpCommand, "Lists available commands");
    }

    public void registerCommand(String commandName, Command command, String desc) {
        commands.put(commandName, new CommandWrapper(command, desc));
        LOGGER.debug("Registered command \"{}\"", commandName);
    }

    public void handleCommand(Message msg) {
        String content = msg.getContent();
        LOGGER.info("Received command {} from {} ({})",
                content, msg.getAuthor().getName(), msg.getAuthor().getID());
        //  Remove prefix
        String noPrefix = content.substring(bot.getConfig().getPrefixLength());
        //  Split
        String[] split = noPrefix.split(" ", 2);
        if (split.length == 0) {
            //  Invalid command
            LOGGER.info("Invalid command ignored: {}", content);
        }
        String cmd = split[0];
        String args = split.length > 1 ? split[1] : "";
        CommandWrapper wrapper = commands.get(cmd);
        if (wrapper != null) {
            LOGGER.info("Dispatching command {}", cmd);
            wrapper.command.handleCommand(new MessageContext(discord, msg, bot), args);
        } else {
            LOGGER.info("Unknown command \"{}\"", cmd);
        }
    }

}

class CommandWrapper {
    final Command command;
    final String helpDesc;

    public CommandWrapper(Command command, String helpDesc) {
        this.command = command;
        this.helpDesc = helpDesc;
    }
}
