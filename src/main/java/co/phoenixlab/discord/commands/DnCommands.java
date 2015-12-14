package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.number.ParseLong;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;

public class DnCommands {

    public static final double DEFENSE_80_SCALAR = 0.0009326D;
    public static final double DEFENSE_80_CONSTANT = 0.0037D;
    private final CommandDispatcher dispatcher;
    private final VahrhedralBot bot;
    private Localizer loc;

    public DnCommands(VahrhedralBot bot) {
        this.bot = bot;
        dispatcher = new CommandDispatcher(bot, "");
        loc = bot.getLocalizer();
    }

    public void registerDnCommands() {
        dispatcher.registerCommand("commands.dn.defense", this::defenseCalculator);
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    private void defenseCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length == 3) {
            try {
                double rawHp = parseStat(split[0]);
                double rawDef = parseStat(split[1]);
                double rawMDef = parseStat(split[2]);

                double def = DEFENSE_80_SCALAR * rawDef + DEFENSE_80_CONSTANT;
                double mDef = DEFENSE_80_SCALAR * rawMDef + DEFENSE_80_CONSTANT;
                //  Prevent div0 by forcing defs to not be 100
                def = Math.min(def, 99D);
                mDef = Math.min(mDef, 99D);

                double eDHp = rawHp / (1D - (def / 100D));
                double eMHp = rawHp / (1D - (mDef / 100D));

                apiClient.sendMessage(loc.localize("commands.dn.defense.response.format",
                        (int) def, (int) mDef, (long) eDHp, (long) eMHp),
                        message.getChannelId());

                return;
            } catch (NumberFormatException ignored) {
            }
        }
        apiClient.sendMessage(loc.localize("commands.dn.defense.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                message.getChannelId());
    }

    private double parseStat(String s) throws NumberFormatException {
        //  Check if it ends in thousand or million
        String thousandSuffix = loc.localize("commands.dn.defense.suffix.thousand");
        int start = s.indexOf(thousandSuffix);
        double working;
        double ret = 0;
        if (start == 0) {
            //  Invalid, cannot be just "k"
            throw new NumberFormatException();
        }
        if (start != -1) {
            String num = s.substring(0, start);
            try {
                working = Double.parseDouble(num);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException();
            }
            ret = working * 1000.0;
        } else {
            String millionSuffix = loc.localize("commands.dn.defense.suffix.million");
            start = s.indexOf(millionSuffix);
            if (start == 0) {
                //  Invalid, cannot be just "m"
                throw new NumberFormatException();
            }
            if (start != -1) {
                String num = s.substring(0, start);
                try {
                    working = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    throw new NumberFormatException();
                }
                ret = working * 1000000.0;
            } else {
                String billionSuffix = loc.localize("commands.dn.defense.suffix.billion");
                start = s.indexOf(billionSuffix);
                if (start == 0) {
                    //  Invalid, cannot be just "b"
                    throw new NumberFormatException();
                }
                if (start != -1) {
                    String num = s.substring(0, start);
                    try {
                        working = Double.parseDouble(num);
                    } catch (NumberFormatException nfe) {
                        throw new NumberFormatException();
                    }
                    ret = working * 1000000000.0;
                } else {
                    ret = ParseLong.parseDec(s);
                }
            }
        }
        return ret;
    }

}
