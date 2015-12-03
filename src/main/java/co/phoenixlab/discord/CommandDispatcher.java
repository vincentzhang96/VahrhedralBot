package co.phoenixlab.discord;

import co.phoenixlab.discord.api.entities.Message;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandDispatcher {

    private static final Logger LOGGER = VahrhedralBot.LOGGER;

    private final VahrhedralBot bot;

    private final Map<String, CommandWrapper> commands;

    private AtomicBoolean active;

    private final String commandPrefix;

    public CommandDispatcher(VahrhedralBot bot, String commandPrefix) {
        this.bot = bot;
        this.commandPrefix = commandPrefix;
        active = new AtomicBoolean(true);
        commands = new HashMap<>();
        addHelpCommand();
    }

    private void addHelpCommand() {
        Command helpCommand = (context, args) -> {
            StringJoiner joiner = new StringJoiner("\n", "__**Available Commands**__\n", "");
            for (Map.Entry<String, CommandWrapper> entry : commands.entrySet()) {
                joiner.add(String.format("**%s%s** - %s", commandPrefix, entry.getKey(), entry.getValue().helpDesc));
            }
            final String result = joiner.toString();
            context.getBot().getApiClient().sendMessage(result, context.getMessage().getChannelId());
        };
        registerCommand("help", helpCommand, "Lists available commands");
    }

    public void registerCommand(String commandName, Command command, String desc) {
        commands.put(commandName, new CommandWrapper(command, desc));
        LOGGER.debug("Registered command \"{}\"", commandName);
    }

    public void registerAlwaysActiveCommand(String commandName, Command command, String desc) {
        commands.put(commandName, new CommandWrapper(command, desc, true));
        LOGGER.debug("Registered command \"{}\"", commandName);
    }

    public void handleCommand(Message msg) {
        String content = msg.getContent();
        LOGGER.info("Received command {} from {} ({})",
                content, msg.getAuthor().getUsername(), msg.getAuthor().getId());
        //  Remove prefix
        String noPrefix = content.substring(commandPrefix.length());
        //  Split
        String[] split = noPrefix.split(" ", 2);
        if (split.length == 0) {
            //  Invalid command
            LOGGER.info("Invalid command ignored: {}", content);
        }
        String cmd = split[0];
        String args = (split.length > 1 ? split[1] : "").trim();
        CommandWrapper wrapper = commands.get(cmd);
        if (wrapper != null) {
            if (active.get() || wrapper.alwaysActive) {
                //  Blacklist check
                if (!bot.getConfig().getBlacklist().contains(msg.getAuthor().getId())) {
                    LOGGER.debug("Dispatching command {}", cmd);
                    wrapper.command.handleCommand(new MessageContext(msg, bot, this), args);
                }
            }
        } else {
            LOGGER.info("Unknown command \"{}\"", cmd);
        }
    }


    public AtomicBoolean active() {
        return active;
    }
}

class CommandWrapper {
    final Command command;
    final String helpDesc;
    final boolean alwaysActive;

    public CommandWrapper(Command command, String helpDesc) {
        this(command, helpDesc, false);
    }

    public CommandWrapper(Command command, String helpDesc, boolean alwaysActive) {
        this.command = command;
        this.helpDesc = helpDesc;
        this.alwaysActive = alwaysActive;
    }

}
