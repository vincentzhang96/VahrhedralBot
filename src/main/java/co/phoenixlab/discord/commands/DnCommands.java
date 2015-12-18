package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.lang.number.ParseLong;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;

public class DnCommands {

    public static final double DEFENSE_80_SCALAR = 0.0009326D;
    public static final double DEFENSE_90_SCALAR = 0.0006D;
    public static final double DEFENSE_80_CONSTANT = 0.0037D;
    public static final double DEFENSE_90_CONSTANT = 0D;
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
        dispatcher.registerCommand("commands.dn.finaldamage", this::finalDamageCalculator);
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    private void finalDamageCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length >= 1) {
            int fd = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level != 80) {
                apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.level_out_of_range",
                        80, 80),
                        context.getChannel());
            }
            double a = 0.00774 * fd;
            double b = 0.0000009093 * Math.pow(fd, 2.2);
            double fdPercent = Math.max(a, b);
            apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.format",
                    level, (int) fdPercent),
                    context.getChannel());
            return;
        }
        apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private void defenseCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        Message message = context.getMessage();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length >= 3) {
            try {
                double rawHp = parseStat(split[0]);
                double rawDef = parseStat(split[1]);
                double rawMDef = parseStat(split[2]);
                if (rawHp < 0 || rawDef < 0 || rawMDef < 0) {
                    throw new NumberFormatException();
                }
                int level = 80;
                if (split.length >= 4) {
                    level = ParseInt.parseOrDefault(split[3], level);
                }
                double def = calculateDef(rawDef, level);
                double mDef = calculateDef(rawMDef, level);
                if (def < 0 || mDef < 0) {
                    apiClient.sendMessage(loc.localize("commands.dn.defense.response.level_out_of_range",
                            80, 90),
                            context.getChannel());
                    return;
                }
                //  Cap defense is 85%
                def = Math.min(def, 85D);
                mDef = Math.min(mDef, 85D);

                double eDHp = rawHp / (1D - (def / 100D));
                double eMHp = rawHp / (1D - (mDef / 100D));

                apiClient.sendMessage(loc.localize("commands.dn.defense.response.format",
                        (int) def, (int) mDef, (long) eDHp, (long) eMHp, level),
                        context.getChannel());

                return;
            } catch (NumberFormatException ignored) {
            }
        }
        apiClient.sendMessage(loc.localize("commands.dn.defense.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private double calculateDef(double def, int level) {
        if (level < 80 || level > 90) {
            return -1;
        }
        double alpha = (double)(level - 80) / 10D;
        double scalar = lerp(DEFENSE_80_SCALAR, DEFENSE_90_SCALAR, alpha);
        double constant = lerp(DEFENSE_80_CONSTANT, DEFENSE_90_CONSTANT, alpha);
        return scalar * def + constant;
    }

    private double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
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
