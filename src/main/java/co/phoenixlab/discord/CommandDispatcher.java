package co.phoenixlab.discord;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.cfg.FeatureToggle;
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
    private final Statistics statistics;
    private final AtomicBoolean active;
    private String commandPrefix;
    private BiPredicate<CommandWrapper, MessageContext> customCommandDispatchChecker;

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

    public void setCustomCommandDispatchChecker(BiPredicate<CommandWrapper, MessageContext> customCommandDispatchChecker) {
        this.customCommandDispatchChecker = customCommandDispatchChecker;
    }

    private void addHelpCommand() {
        registerCommand("commands.help", this::help);
        registerCommand("commands.commands", this::help);
    }

    private void help(MessageContext context, String args) {
        Localizer l = context.getBot().getLocalizer();
        if (!args.isEmpty()) {
            showDetailedCommandHelp(context, args, l);
            return;
        }
        LongAdder adder = new LongAdder();
        StringJoiner joiner = new StringJoiner("\n", "", "");
        commands.entrySet().stream().
            filter(entry -> !entry.getValue().hidden).
            forEach(entry -> {
                joiner.add(l.localize("commands.help.response.entry",
                    commandPrefix, entry.getKey().toLowerCase(), entry.getValue().helpDesc));
                adder.increment();
            });

        String header = l.localize("commands.help.response.head", adder.intValue());
        final String result = header + joiner.toString();
        context.getApiClient().sendMessage(result, context.getChannel());
    }

    private void showDetailedCommandHelp(MessageContext context, String args, Localizer l) {
        CommandWrapper wrapper = commands.get(args.toUpperCase());
        if (wrapper != null) {
            if (wrapper.examples != null) {
                context.getApiClient().sendMessage(l.localize("commands.help.response.detailed.examples",
                    args, wrapper.detailedHelp, wrapper.argumentsHelp, wrapper.examples),
                    context.getChannel());
            } else {
                context.getApiClient().sendMessage(l.localize("commands.help.response.detailed",
                    args, wrapper.detailedHelp, wrapper.argumentsHelp),
                    context.getChannel());
            }
        } else {
            context.getApiClient().sendMessage(l.localize("commands.help.response.not_found",
                args),
                context.getChannel());
        }
    }


    public void registerCommand(String commandNameBaseKey, Command command) {
        registerCommand(commandNameBaseKey, command, false);
    }

    public void registerCommand(String commandNameBaseKey, Command command, boolean hidden) {
        Localizer localizer = bot.getLocalizer();
        String commandStr = localizer.localize(commandNameBaseKey + ".command").toUpperCase();
        String helpStr = localizer.localize(commandNameBaseKey + ".help");
        String detailedHelpStr = localizer.localize(commandNameBaseKey + ".detailed_help");
        String argumentsStr = localizer.localize(commandNameBaseKey + ".arguments");
        String examplesStr = null;
        if (localizer.containsKey(commandNameBaseKey + ".examples")) {
            examplesStr = localizer.localize(commandNameBaseKey + ".examples");
        }
        commands.put(commandStr, new CommandWrapper(command,
            commandNameBaseKey, helpStr, detailedHelpStr, argumentsStr, examplesStr, false, hidden));
        LOGGER.debug("Registered command \"{}\"", commandNameBaseKey);
    }

    public void registerAlwaysActiveCommand(String commandNameBaseKey, Command command) {
        registerAlwaysActiveCommand(commandNameBaseKey, command, false);
    }

    public void registerAlwaysActiveCommand(String commandNameBaseKey, Command command, boolean hidden) {
        Localizer localizer = bot.getLocalizer();
        String commandStr = localizer.localize(commandNameBaseKey + ".command").toUpperCase();
        String helpStr = localizer.localize(commandNameBaseKey + ".help");
        String detailedHelpStr = localizer.localize(commandNameBaseKey + ".detailed_help");
        String argumentsStr = localizer.localize(commandNameBaseKey + ".arguments");
        String examplesStr = null;
        if (localizer.containsKey(commandNameBaseKey + ".examples")) {
            examplesStr = localizer.localize(commandNameBaseKey + ".examples");
        }
        commands.put(commandStr, new CommandWrapper(command,
            commandNameBaseKey, helpStr, detailedHelpStr, argumentsStr, examplesStr, true, hidden));
        LOGGER.debug("Registered command \"{}\"", commandNameBaseKey);
    }

    public boolean deleteCommandByName(String commandName) {
        if (commandName != null) {
            CommandWrapper wrapper = commands.remove(commandName.toUpperCase());
            if (wrapper != null) {
                LOGGER.debug("Deleted command \"{}\"", wrapper.command);
                return true;
            }
        }
        LOGGER.debug("Unable to delete command \"{}\"", commandName);
        return false;
    }

    public boolean deleteCommandByBaseKey(String baseKey) {
        String name = bot.getLocalizer().localize(baseKey + ".command");
        if (name != null) {
            CommandWrapper wrapper = commands.remove(name.toUpperCase());
            if (wrapper != null) {
                LOGGER.debug("Deleted command \"{}\"", wrapper.command);
                return true;
            }
        }
        LOGGER.debug("Unable to delete command key \"{}\"", baseKey);
        return false;
    }

    public void handleCommand(Message msg) {
        long cmdStartTime = System.nanoTime();
        try {
            statistics.commandsReceived.increment();
            String content = msg.getContent();
            MessageContext messageContext = new MessageContext(msg, bot, this);
            String serverName = SafeNav.of(messageContext.getServer())
                .next(Server::getName)
                .orElse("a private message");
            String serverId = SafeNav.of(messageContext.getServer())
                .next(Server::getId)
                .orElse("N/A");
            LOGGER.info("Received command {} from {} ({}) in {} ({})",
                content, msg.getAuthor().getUsername(), msg.getAuthor().getId(),
                serverName, serverId);
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
                if (shouldCommandBeDispatched(wrapper, messageContext)) {
                    LOGGER.debug("Dispatching command {}", cmd);
                    long handleStartTime = System.nanoTime();
                    wrapper.command.handleCommand(messageContext, args);
                    statistics.acceptedCommandHandleTime.
                        add(MILLISECONDS.convert(System.nanoTime() - handleStartTime, NANOSECONDS));
                    if (bot.getConfig().isSelfBot()) {
                        bot.getApiClient().deleteMessage(msg.getChannelId(), msg.getId());
                    }
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

    public boolean shouldCommandBeDispatched(CommandWrapper command, MessageContext context) {
        if (command.alwaysActive) {
            return true;
        }
        boolean generalAllow = active().get() &&
            customCommandDispatchChecker.test(command, context);
        VahrhedralBot bot = context.getBot();
        FeatureToggle toggle = bot.getToggleConfig().getToggle(command.commandKey);
        return toggle.use(context.getServer().getId(), context.getChannel().getId()) && generalAllow;
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
        final String commandKey;
        final String helpDesc;
        final String detailedHelp;
        final String argumentsHelp;
        final String examples;
        final boolean alwaysActive;
        final boolean hidden;

        public CommandWrapper(Command command, String helpDesc, String detailedHelp, String argumentsHelp,
                              String examples, String commandKey) {
            this(command, commandKey, helpDesc, detailedHelp, argumentsHelp, examples, false, false);
        }

        public CommandWrapper(Command command, String commandKey, String helpDesc, String detailedHelp,
                              String argumentsHelp,
                              String examples,
                              boolean alwaysActive, boolean hidden) {
            this.command = command;
            this.commandKey = commandKey;
            this.helpDesc = helpDesc;
            this.detailedHelp = detailedHelp;
            this.argumentsHelp = argumentsHelp;
            this.examples = examples;
            this.alwaysActive = alwaysActive;
            this.hidden = hidden;
        }

    }
}
