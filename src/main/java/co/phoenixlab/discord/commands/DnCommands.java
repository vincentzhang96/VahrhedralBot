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
    public static final double DEFENSE_MAX_PERCENT = 85D;
    public static final int CRIT_80_CAP = 127685;
    public static final int CRIT_90_CAP = 235880;
    public static final int CRITDMG_80_CAP = 431970;
    public static final int CRITDMG_90_CAP = 671160;
    public static final double CRITDMG_MAX_PERCENT = 1D;
    public static final double CRIT_MAX_PERCENT = 0.89D;
    public static final int[] fdCaps = {
            300, 420, 600, 850, 1290, 1850, 2974, 4520, 6367, 8622
    };

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
        dispatcher.registerCommand("commands.dn.crit", this::critChanceCalculator);
        dispatcher.registerCommand("commands.dn.critdmg", this::critDamageCalculator);
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    private void critChanceCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length >= 1) {
            int crit = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level < 80 || level > 90) {
                apiClient.sendMessage(loc.localize("commands.dn.crit.response.level_out_of_range",
                        80, 90),
                        context.getChannel());
                return;
            }
            double alpha = (double)(level - 80) / 10D;
            double percent = crit / (lerp(CRIT_80_CAP, CRIT_90_CAP, alpha));
            percent = Math.max(0, Math.min(CRIT_MAX_PERCENT, percent)) * 100D;
            apiClient.sendMessage(loc.localize("commands.dn.crit.response.format",
                    level, percent),
                    context.getChannel());
            return;
        }
        apiClient.sendMessage(loc.localize("commands.dn.crit.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private void critDamageCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length >= 1) {
            int crit = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level < 80 || level > 90) {
                apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.level_out_of_range",
                        80, 90),
                        context.getChannel());
                return;
            }
            double alpha = (double)(level - 80) / 10D;
            double percent = crit / (lerp(CRITDMG_80_CAP, CRITDMG_90_CAP, alpha));
            percent = Math.max(0, Math.min(CRITDMG_MAX_PERCENT, percent)) * 100D + 200D;
            apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.format",
                    level, percent),
                    context.getChannel());
            return;
        }
        apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private void finalDamageCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        if (split.length >= 1) {
            if ("bdo".equalsIgnoreCase(split[0])) {
                apiClient.sendMessage("**Level -1 Final Damage:** `-1.0%` <#158252528885039104>", context.getChannel());
                return;
            }
            int fd = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level < 10 || level > 100) {
                apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.level_out_of_range",
                        10, 100),
                        context.getChannel());
                return;
            }
            double fdPercent;
            int fdCap;
            if (level % 10 == 0) {
                fdCap = fdCaps[level / 10 - 1];
            } else {
                int low = fdCaps[level / 10 - 1];
                int high = fdCaps[level / 10];
                fdCap = (int) lerp(low, high, (level % 10) / 10D);
            }
            double ratio = (double) fd / fdCap;
            if (ratio < 0.417D) {
                fdPercent = (0.35D * fd) / fdCap;
            } else {
                fdPercent = Math.pow((double) fd / fdCap, 2.2D);
            }
            fdPercent = Math.max(0, Math.min(100D, fdPercent * 100));
            apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.format",
                    level, fdPercent, fdCap),
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
                def = Math.min(def, DEFENSE_MAX_PERCENT);
                mDef = Math.min(mDef, DEFENSE_MAX_PERCENT);

                double eDHp = rawHp / (1D - (def / 100D));
                double eMHp = rawHp / (1D - (mDef / 100D));

                apiClient.sendMessage(loc.localize("commands.dn.defense.response.format",
                        def, mDef, (long) eDHp, (long) eMHp, level),
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
