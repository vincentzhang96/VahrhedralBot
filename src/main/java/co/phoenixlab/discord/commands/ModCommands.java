package co.phoenixlab.discord.commands;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.VahrhedralBot;

public class ModCommands {

        private final CommandDispatcher dispatcher;
        private Localizer loc;
        private final VahrhedralBot bot;

        public ModCommands(VahrhedralBot bot) {
            this.bot = bot;
            dispatcher = new CommandDispatcher(bot, "");
            loc = bot.getLocalizer();
        }

        public CommandDispatcher getModCommandDispatcher() {
            return dispatcher;
        }

        public void registerModCommands() {

        }
}
