package co.phoenixlab.discord.commands;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.lang.number.ParseLong;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.EventListener;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.commands.dn.CalculateCriticalDamage;
import co.phoenixlab.discord.dntrack.VersionTracker;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.StringJoiner;

public class DnCommands {

    public static final double DEFENSE_MAX_PERCENT = 0.85D;
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
            750.0, 870.0, 990.0, 1110.0, 1230.0, 1350.0, 1470.0, 1590.0,
            1710.0, 1830.0, 1950.0, 2070.0, 2190.0, 2310.0, 2850.0, 3000.0,
            3150.0, 3300.0, 3450.0, 3600.0, 3750.0, 3900.0, 4050.0, 4200.0,
            4425.0, 4650.0, 4875.0, 5100.0, 5325.0, 5550.0, 5775.0, 6000.0,
            6300.0, 6600.0, 6900.0, 7200.0, 7500.0, 7800.0, 8100.0, 8400.0,
            9000.0, 9600.0, 10200.0, 10800.0, 11475.0, 12150.0, 12825.0,
            13500.0, 14250.0, 15000.0, 15750.0, 16912.0, 18277.0, 19695.0,
            21157.0, 22672.0, 24427.0, 26355.0, 28125.0, 29250.0, 30868.0,
            33056.0, 35685.0, 38771.0, 42331.0, 46383.0, 50943.0, 56137.0,
            61977.0, 68186.0, 72523.0, 76637.0, 80534.0, 84201.0, 88328.0,
            92188.0, 95006.0, 99837.0, 104590.0, 106722.0, 112014.0, 117306.0,
            122598.0, 127890.0, 134064.0, 140238.0, 146412.0, 152586.0,
            158760.0, 165816.0, 187278.0, 209916.0, 233730.0, 258720.0,
            286135.0, 314874.0, 344935.0, 376320.0, 409027.0, 443058.0
    };
    public static final double[] critCaps = {
            1000.0, 1160.0, 1320.0, 1480.0, 1640.0, 1800.0, 1960.0, 2120.0,
            2280.0, 2440.0, 2600.0, 2760.0, 2920.0, 3080.0, 3800.0, 4000.0,
            4200.0, 4400.0, 4600.0, 4800.0, 5000.0, 5200.0, 5400.0, 5600.0,
            5900.0, 6200.0, 6500.0, 6800.0, 7100.0, 7400.0, 7700.0, 8000.0,
            8400.0, 8800.0, 9200.0, 9600.0, 10000.0, 10400.0, 10800.0, 11200.0,
            12000.0, 12800.0, 13600.0, 14400.0, 15300.0, 16200.0, 17100.0,
            18000.0, 19000.0, 20000.0, 21500.0, 23000.0, 24600.0, 26200.0,
            27900.0, 29600.0, 31400.0, 33200.0, 35200.0, 37200.0, 40200.0,
            43200.0, 46200.0, 49200.0, 52400.0, 55600.0, 58800.0, 62000.0,
            65400.0, 68800.0, 74745.0, 80619.0, 86438.0, 92373.0, 98284.0,
            104245.0, 110176.0, 116008.0, 121899.0, 127685.0, 138684.0, 149565.0,
            160545.0, 171433.0, 182263.0, 192994.0, 203931.0, 214891.0, 225855.0,
            236880.0, 277830.0, 321300.0, 367290.0, 415800.0, 468877.0, 524790.0,
            583537.0, 645120.0, 709537.0, 776790.0
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
    	Command command = null;
        dispatcher.registerCommand("commands.dn.defense", this::defenseCalculator);
        dispatcher.registerCommand("commands.dn.finaldamage", this::finalDamageCalculator);
        dispatcher.registerCommand("commands.dn.crit", this::critChanceCalculator);
        command = new CalculateCriticalDamage();
        dispatcher.registerCommand("commands.dn.critdmg", command);
        dispatcher.registerCommand("commands.dn.track.version", this::getVersion);
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    private void getVersion(MessageContext context, String args) {
        DiscordApiClient api = context.getApiClient();
        Map<String, VersionTracker> trackers = bot.getEventListener().getVersionTrackers();
        if (args.isEmpty()) {
            StringJoiner joiner = new StringJoiner("\n");
            for (VersionTracker tracker : trackers.values()) {
                String lastVersionChangeTimeStr = SafeNav.of(tracker.getLastVersionChangeTime())
                    .next(ZonedDateTime::from)
                    .next(EventListener.UPDATE_FORMATTER::format)
                    .orElse("unknown");
                joiner.add(loc.localize("commands.dn.track.version.all.entry",
                    loc.localize(tracker.getRegion().getRegionNameKey()),
                    tracker.getCurrentVersion(),
                    lastVersionChangeTimeStr,
                    tracker.getRegion().getRegionCode()));
            }
            api.sendMessage(loc.localize("commands.dn.track.version.all.format", joiner.toString()),
                context.getChannel());
        } else {
            String regionCode = args.toLowerCase();
            VersionTracker tracker = trackers.get(regionCode);
            if (tracker != null) {
                String lastVersionChangeTimeStr = SafeNav.of(tracker.getLastVersionChangeTime())
                    .next(EventListener.UPDATE_FORMATTER::format)
                    .orElse("unknown");
                api.sendMessage(loc.localize("commands.dn.track.version.current",
                    loc.localize(tracker.getRegion().getRegionNameKey()),
                    tracker.getCurrentVersion(),
                    lastVersionChangeTimeStr,
                    tracker.getRegion().getRegionCode()),
                    context.getChannel());
            } else {
                StringJoiner joiner = new StringJoiner(", ");
                trackers.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(k -> joiner.add("`" + k + "`"));
                api.sendMessage(loc.localize("commands.dn.track.version.not_found", joiner.toString()),
                    context.getChannel());
            }
        }
    }

    private void critChanceCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        try {
            //  Strip commas
            args = args.replace(",", "");
            String[] split = args.split(" ");
            if (split.length >= 1) {
                String value = split[0];
                if (value.endsWith("%")) {
                    double critPercent = Math.min(Double.parseDouble(value.substring(0, value.length() - 1)) / 100D,
                        CRIT_MAX_PERCENT);
                    if (critPercent < 0) {
                        throw new IllegalArgumentException("must be at least 0%");
                    }
                    int level = 80;
                    if (split.length >= 2) {
                        level = ParseInt.parseOrDefault(split[1], level);
                    }
                    if (level < 1 || level > 100) {
                        apiClient.sendMessage(loc.localize("commands.dn.crit.response.level_out_of_range",
                            1, 100),
                            context.getChannel());
                        return;
                    }
                    double crit = critCaps[level - 1] * critPercent;
                    apiClient.sendMessage(loc.localize("commands.dn.crit.response.format.required",
                        level, (int) crit, critPercent * 100D),
                        context.getChannel());
                } else {
                    int crit = (int) parseStat(split[0]);
                    int level = 80;
                    if (split.length >= 2) {
                        level = ParseInt.parseOrDefault(split[1], level);
                    }
                    if (level < 1 || level > 100) {
                        apiClient.sendMessage(loc.localize("commands.dn.crit.response.level_out_of_range",
                            1, 100),
                            context.getChannel());
                        return;
                    }
                    double critPercent;
                    double critCap = critCaps[level - 1];
                    critPercent = crit / critCap;
                    critPercent = Math.max(0, Math.min(CRIT_MAX_PERCENT, critPercent)) * 100D;
                    apiClient.sendMessage(loc.localize("commands.dn.crit.response.format",
                        level, critPercent, (int) (critCap * CRIT_MAX_PERCENT), (int) (CRIT_MAX_PERCENT * 100D)),
                        context.getChannel());
                }
                return;
            }
        } catch (Exception ignored) {
        }
        apiClient.sendMessage(loc.localize("commands.dn.crit.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private void finalDamageCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ");
        try {
            if (split.length >= 1) {
                String quantity = split[0];
                if (quantity.endsWith("%")) {
                    double fdPercent = Math.min(Double.parseDouble(quantity.substring(0, quantity.length() - 1)) / 100D,
                            1D);
                    if (fdPercent < 0) {
                        throw new IllegalArgumentException("must be at least 0%");
                    }
                    int level = 80;
                    if (split.length >= 2) {
                        level = ParseInt.parseOrDefault(split[1], level);
                    }
                    if (level < 1 || level > 100) {
                        apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.level_out_of_range",
                                1, 100),
                                context.getChannel());
                        return;
                    }
                    double fd;
                    if (fdPercent < 0.146D) {
                        fd = 2.857142857D * fdPercent * fdCaps[level - 1];
                    } else {
                        //  Since we know fdPercent must be between 0 and 100, we can't overflow
                        //noinspection NumericOverflow
                        fd = fdCaps[level - 1] * Math.pow(fdPercent, 1D / 2.2D);
                    }
                    apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.format.required",
                            level, (int) fd, fdPercent * 100D),
                            context.getChannel());
                } else {
                    int fd = (int) parseStat(quantity);
                    int level = 80;
                    if (split.length >= 2) {
                        level = ParseInt.parseOrDefault(split[1], level);
                    }
                    if (level < 1 || level > 100) {
                        apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.level_out_of_range",
                                1, 100),
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
                            level, fdPercent, (int) (fdCap * FD_MAX_PERCENT), (int) (FD_MAX_PERCENT * 100D)),
                            context.getChannel());
                }
            }
            return;
        } catch (Exception ignored) {
        }
        apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private void defenseCalculator(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
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
                if (level < 1 || level > 100) {
                    apiClient.sendMessage(loc.localize("commands.dn.defense.response.level_out_of_range",
                            1, 100),
                            context.getChannel());
                    return;
                }
                double defCap = defenseCaps[level - 1];
                double def = calculateDef(rawDef, defCap);
                double mDef = calculateDef(rawMDef, defCap);

                double eDHp = rawHp / (1D - (def / 100D));
                double eMHp = rawHp / (1D - (mDef / 100D));

                apiClient.sendMessage(loc.localize("commands.dn.defense.response.format",
                        def, mDef, (long) eDHp, (long) eMHp, level, (int) defCap,
                        (int) (DEFENSE_MAX_PERCENT * 100D)),
                        context.getChannel());

                return;
            } catch (NumberFormatException ignored) {
            }
        }
        apiClient.sendMessage(loc.localize("commands.dn.defense.response.invalid",
                bot.getMainCommandDispatcher().getCommandPrefix()),
                context.getChannel());
    }

    private double calculateDef(double def, double defCap) {
        double defPercent;
        defPercent = def / defCap;
        defPercent = Math.max(0, Math.min(DEFENSE_MAX_PERCENT, defPercent)) * 100D;
        return defPercent;
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
