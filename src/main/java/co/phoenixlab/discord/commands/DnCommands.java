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

    public static final double DEFENSE_MAX_PERCENT = 85D;
    public static final double CRITDMG_MAX_PERCENT = 1D;
    public static final double CRIT_MAX_PERCENT = 0.89D;
    public static final double FD_MAX_PERCENT = 1D;
    public static final double[] fdCaps = {
            75.0, 87.0, 99.0, 111.0, 123.0, 135.0, 147.0, 159.0, 171.0, 183.0,
            195.0, 207.0, 219.0, 231.0, 285.0, 300.0, 315.0, 330.0, 345.0,
            360.0, 375.0, 390.0, 405.0, 420.0, 442.0, 465.0, 487.0, 510.0,
            532.0, 555.0, 577.0, 600.0, 630.0, 660.0, 690.0, 720.0, 750.0,
            780.0, 810.0, 850.0, 894.0, 938.0, 982.0, 1026.0, 1070.0, 1114.0,
            1158.0, 1202.0, 1246.0, 1290.0, 1346.0, 1402.0, 1458.0, 1514.0,
            1570.0, 1626.0, 1682.0, 1738.0, 1794.0, 1850.0, 1962.0, 2074.0,
            2187.0, 2299.0, 2412.0, 2524.0, 2636.0, 2749.0, 2861.0, 2974.0,
            3128.0, 3283.0, 3437.0, 3592.0, 3747.0, 3901.0, 4056.0, 4210.0,
            4365.0, 4520.0, 4704.0, 4889.0, 5074.0, 5258.0, 5443.0, 5628.0,
            5812.0, 5997.0, 6182.0, 6367.0, 7022.0, 7678.0, 8333.0, 8989.0,
            9644.0, 10300.0, 10955.0, 11611.0, 12266.0, 12922.0
    };
    public static final double[] defenseCaps = {
            3000, 4200, 6000, 8400, 15000, 29250, 68186, 106722, 165816, 233730, 443058
    };
    public static final double[] critCaps = {
            4000, 5600, 8000, 11200, 20000, 37200, 68800, 127685, 236880, 367290, 776790
    };
    public static final double[] critDmgCaps = {
            10600, 14840, 21200, 29680, 50350, 103350, 211337, 431970, 671160, 832524, 1075998
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
            if ("bdo".equalsIgnoreCase(split[0])) {
                apiClient.sendMessage("**Level -1 Final Damage:** `-1.0%` <#158252528885039104>", context.getChannel());
                return;
            }
            int crit = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level < 10 || level > 100) {
                apiClient.sendMessage(loc.localize("commands.dn.crit.response.level_out_of_range",
                        10, 100),
                        context.getChannel());
                return;
            }
            double critPercent;
            double critCap;
            if (level % 10 == 0) {
                critCap = critCaps[level / 10 - 1];
            } else {
                double low = critCaps[level / 10 - 1];
                double high = critCaps[level / 10];
                critCap = lerp(low, high, (level % 10) / 10D);
            }
            critPercent = crit / critCap;
            critPercent = Math.max(0, Math.min(CRIT_MAX_PERCENT, critPercent)) * 100D;
            apiClient.sendMessage(loc.localize("commands.dn.crit.response.format",
                    level, critPercent, (int) critCap),
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
            if ("bdo".equalsIgnoreCase(split[0])) {
                apiClient.sendMessage("**Level -1 Final Damage:** `-1.0%` <#158252528885039104>", context.getChannel());
                return;
            }
            int critdmg = (int) parseStat(split[0]);
            int level = 80;
            if (split.length >= 2) {
                level = ParseInt.parseOrDefault(split[1], level);
            }
            if (level < 10 || level > 100) {
                apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.level_out_of_range",
                        10, 100),
                        context.getChannel());
                return;
            }
            double critDmgPercent;
            double critDmgCap;
            if (level % 10 == 0) {
                critDmgCap = critDmgCaps[level / 10 - 1];
            } else {
                double low = critDmgCaps[level / 10 - 1];
                double high = critDmgCaps[level / 10];
                critDmgCap = lerp(low, high, (level % 10) / 10D);
            }
            critDmgPercent = critdmg / critDmgCap;
            critDmgPercent = Math.max(0, Math.min(CRITDMG_MAX_PERCENT, critDmgPercent)) * 100D + 200D;
            apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.format",
                    level, critDmgPercent, (int) critDmgCap),
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
            double fdCap = fdCaps[level - 1];
            double ratio = fd / fdCap;
            if (ratio < 0.417D) {
                fdPercent = (0.35D * fd) / fdCap;
            } else {
                fdPercent = Math.pow(fd / fdCap, 2.2D);
            }
            fdPercent = Math.max(0, Math.min(FD_MAX_PERCENT, fdPercent)) * 100D;
            apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.format",
                    level, fdPercent, (int) fdCap),
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
                if (level < 10 || level > 100) {
                    apiClient.sendMessage(loc.localize("commands.dn.defense.response.level_out_of_range",
                            10, 100),
                            context.getChannel());
                    return;
                }
                double def = calculateDef(rawDef, level);
                double mDef = calculateDef(rawMDef, level);

                double eDHp = rawHp / (1D - (def / 100D));
                double eMHp = rawHp / (1D - (mDef / 100D));

                apiClient.sendMessage(loc.localize("commands.dn.defense.response.format",
                        def, mDef, (long) eDHp, (long) eMHp, level, (int) getDefCap(level)),
                        context.getChannel());

                return;
            } catch (NumberFormatException ignored) {
            }
        }
        apiClient.sendMessage(loc.localize("commands.dn.defense.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private double getDefCap(int level) {
        double defCap;
        if (level % 10 == 0) {
            defCap = defenseCaps[level / 10 - 1];
        } else {
            double low = defenseCaps[level / 10 - 1];
            double high = defenseCaps[level / 10];
            defCap = lerp(low, high, (level % 10) / 10D);
        }
        return defCap;
    }

    private double calculateDef(double def, int level) {
        double defPercent;
        defPercent = def / getDefCap(level);
        defPercent = Math.max(0, Math.min(DEFENSE_MAX_PERCENT, defPercent)) * 100D;
        return defPercent;
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
