package co.phoenixlab.discord;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.stats.RunningAverage;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiPredicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class CommandDispatcher {

    private static final Logger LOGGER = VahrhedralBot.LOGGER;

    private final VahrhedralBot bot;

    private final Map<String, CommandWrapper> commands;

    private AtomicBoolean active;

    private String commandPrefix;
    private BiPredicate<CommandWrapper, Message> customCommandDispatchChecker;
    private final Statistics statistics;

    public CommandDispatcher(VahrhedralBot bot, String commandPrefix) {
        this.bot = bot;
        this.commandPrefix = commandPrefix;
        active = new AtomicBoolean(true);
        commands = new HashMap<>();
        statistics = new Statistics();
        customCommandDispatchChecker = (commandWrapper, message) -> true;
        addHelpCommand();
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public void setCustomCommandDispatchChecker(BiPredicate<CommandWrapper, Message> customCommandDispatchChecker) {
        this.customCommandDispatchChecker = customCommandDispatchChecker;
    }

    private void addHelpCommand() {
        Command helpCommand = (context, args) -> {
            Localizer l = context.getBot().getLocalizer();
            if (!args.isEmpty()) {
                CommandWrapper wrapper = commands.get(args.toUpperCase());
                if (wrapper != null) {
                    context.getBot().getApiClient().sendMessage(l.localize("commands.help.response.detailed",
                            args, wrapper.detailedHelp, wrapper.argumentsHelp),
                            context.getMessage().getChannelId());
                } else {
                    context.getBot().getApiClient().sendMessage(l.localize("commands.help.response.not_found",
                            args),
                            context.getMessage().getChannelId());
                }
                return;
            }
            String header = l.localize("commands.help.response.head", commands.size());

            StringJoiner joiner = new StringJoiner("\n", header, "");
            for (Map.Entry<String, CommandWrapper> entry : commands.entrySet()) {
                joiner.add(l.localize("commands.help.response.entry",
                        commandPrefix, entry.getKey().toLowerCase(), entry.getValue().helpDesc));
            }
            final String result = joiner.toString();
            context.getBot().getApiClient().sendMessage(result, context.getMessage().getChannelId());
        };
        registerCommand("commands.help", helpCommand);
    }

    public void registerCommand(String commandNameBaseKey, Command command) {
        Localizer localizer = bot.getLocalizer();
        String commandStr = localizer.localize(commandNameBaseKey + ".command").toUpperCase();
        String helpStr = localizer.localize(commandNameBaseKey + ".help");
        String detailedHelpStr = localizer.localize(commandNameBaseKey + ".detailed_help");
        String argumentsStr = localizer.localize(commandNameBaseKey + ".arguments");
        commands.put(commandStr, new CommandWrapper(command,
                helpStr, detailedHelpStr, argumentsStr));
        LOGGER.debug("Registered command \"{}\"", commandNameBaseKey);
    }

    public void registerAlwaysActiveCommand(String commandNameBaseKey, Command command) {
        Localizer localizer = bot.getLocalizer();
        String commandStr = localizer.localize(commandNameBaseKey + ".command").toUpperCase();
        String helpStr = localizer.localize(commandNameBaseKey + ".help");
        String detailedHelpStr = localizer.localize(commandNameBaseKey + ".detailed_help");
        String argumentsStr = localizer.localize(commandNameBaseKey + ".arguments");
        commands.put(commandStr, new CommandWrapper(command,
                helpStr, detailedHelpStr, argumentsStr, true));
        LOGGER.debug("Registered command \"{}\"", commandNameBaseKey);
    }

    public void handleCommand(Message msg) {
        long cmdStartTime = System.nanoTime();
        try {
            statistics.commandsReceived.increment();
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
                statistics.commandsRejected.increment();
                return;
            }
            String cmd = split[0].toUpperCase();
            String args = (split.length > 1 ? split[1] : "").trim();
            CommandWrapper wrapper = commands.get(cmd);
            if (wrapper != null) {
                if (shouldCommandBeDispatched(wrapper, msg)) {
                    LOGGER.debug("Dispatching command {}", cmd);
                    long handleStartTime = System.nanoTime();
                    wrapper.command.handleCommand(new MessageContext(msg, bot, this), args);
                    statistics.acceptedCommandHandleTime.
                            add(MILLISECONDS.convert(System.nanoTime() - handleStartTime, NANOSECONDS));
                    statistics.commandsHandledSuccessfully.increment();
                    return;
                }
            } else {
                LOGGER.info("Unknown command \"{}\"", cmd);
            }
            statistics.commandsRejected.increment();
        } finally {
            statistics.commandHandleTime.add(MILLISECONDS.convert(System.nanoTime() - cmdStartTime, NANOSECONDS));
        }
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public AtomicBoolean active() {
        return active;
    }

    public boolean shouldCommandBeDispatched(CommandWrapper command, Message msg) {
        return (command.alwaysActive || active().get()) &&
                !bot.getConfig().getBlacklist().contains(msg.getAuthor().getId()) &&
                customCommandDispatchChecker.test(command, msg);
    }

    public static class Statistics {
        public final RunningAverage acceptedCommandHandleTime;
        public final RunningAverage commandHandleTime;
        public final LongAdder commandsReceived;
        public final LongAdder commandsHandledSuccessfully;
        public final LongAdder commandsRejected;

        Statistics() {
            acceptedCommandHandleTime = new RunningAverage();
            commandHandleTime = new RunningAverage();
            commandsReceived = new LongAdder();
            commandsHandledSuccessfully = new LongAdder();
            commandsRejected = new LongAdder();
        }
    }

    public static class CommandWrapper {
        final Command command;
        final String helpDesc;
        final String detailedHelp;
        final String argumentsHelp;
        final boolean alwaysActive;

        public CommandWrapper(Command command, String helpDesc, String detailedHelp, String argumentsHelp) {
            this(command, helpDesc, detailedHelp, argumentsHelp, false);
        }

        public CommandWrapper(Command command, String helpDesc, String detailedHelp, String argumentsHelp, boolean alwaysActive) {
            this.command = command;
            this.helpDesc = helpDesc;
            this.detailedHelp = detailedHelp;
            this.argumentsHelp = argumentsHelp;
            this.alwaysActive = alwaysActive;
        }

    }
}
