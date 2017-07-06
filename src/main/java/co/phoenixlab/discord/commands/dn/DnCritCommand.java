package co.phoenixlab.discord.commands.dn;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Embed;
import co.phoenixlab.discord.api.entities.EmbedField;
import co.phoenixlab.discord.api.entities.EmbedFooter;

public class DnCritCommand implements Command {

    public static final double CRIT_MAX_PERCENT = 0.89;
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
            236880.0, 277830.0, 321300.0, 367290.0, 794640.0, 937755.0, 1599360.0,
            1867320.0, 2150400.0, 2448600.0, 2704380.0
    };

    private final Localizer loc;
    public static final int[] CRIT_DEFAULT_LEVELS;

    static {
        CRIT_DEFAULT_LEVELS = new int[]{95, 93, 90, 80};
    }

    public DnCritCommand(Localizer loc) {
        this.loc = loc;
    }

    @Override
    public void handleCommand(MessageContext context, String args) {
        DiscordApiClient apiClient = context.getApiClient();
        //  Strip commas
        args = args.replace(",", "");
        String[] split = args.split(" ", 2);
        int level = -1;
        if (split.length == 2) {
            level = ParseInt.parseOrDefault(split[1], -1);
        }
        int crit = -1;
        double critPercent = 0;
        String critAmt = split[0];
        try {
            if (critAmt.endsWith("%")) {
                critPercent = (float) Double.parseDouble(critAmt.substring(0, critAmt.length() - 1)) / 100F;
                critPercent = Math.min(critPercent, CRIT_MAX_PERCENT);
            } else {
                crit = (int) DnCommandUtils.parseStat(critAmt, loc);
                if (crit < 0) {
                    throw new IllegalArgumentException(loc.localize(
                            "commands.dn.crit.response.crit_out_of_range",
                            0
                    ));
                }
            }
            if (level == -1) {
                String title;
                //  Default level, use embed style for showing lvl 80, 90, and 93 crit
                Embed embed = new Embed();
                embed.setType(Embed.TYPE_RICH);
                embed.setColor(5941733);    //  GLAZE Accent 2 (temporary, replace with rolecolor)
                EmbedField[] fields = new EmbedField[CRIT_DEFAULT_LEVELS.length];
                if (crit == -1) {
                    title = String.format("**__%.1f%% Critical Chance__**", critPercent * 100F);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", CRIT_DEFAULT_LEVELS[i]),
                                String.format("%,d", calculateCritRequiredForPercent(critPercent, CRIT_DEFAULT_LEVELS[i])),
                                true
                        );
                    }
                } else {
                    title = String.format("**__%,d Critical__**", crit);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", CRIT_DEFAULT_LEVELS[i]),
                                String.format("%.1f%%", calculateCritPercent(crit, CRIT_DEFAULT_LEVELS[i])),
                                true
                        );
                    }
                }
                embed.setFields(fields);
                EmbedFooter footer = new EmbedFooter();
                footer.setText(DnCommandUtils.DIVINITOR_FOOTER_TEXT);
                footer.setIconUrl(DnCommandUtils.DIVINITOR_FOOTER_ICON_URL);
                embed.setFooter(footer);
                apiClient.sendMessage(title, context.getChannel(), embed);
            } else {
                if (crit == -1) {
                    crit = calculateCritRequiredForPercent(critPercent, level);
                    String msg = loc.localize(
                            "commands.dn.crit.response.format.required",
                            level,
                            crit,
                            critPercent * 100D
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                } else {
                    critPercent = calculateCritPercent(crit, level);
                    String msg = loc.localize(
                            "commands.dn.crit.response.format",
                            level,
                            critPercent,
                            (int) (critCaps[level - 1] * CRIT_MAX_PERCENT),
                            (int) (CRIT_MAX_PERCENT * 100D)
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                }
            }
        } catch (NumberFormatException nfe) {
            apiClient.sendMessage(loc.localize("commands.dn.crit.response.invalid",
                    context.getBot().getMainCommandDispatcher().getCommandPrefix()),
                    context.getChannel());
        } catch (IllegalArgumentException ile) {
            apiClient.sendMessage(ile.getMessage(), context.getChannel());
        }
    }


    public double calculateCritPercent(int crit, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > critCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.crit.response.level_out_of_range",
                    1,
                    critCaps.length
            ));
        }
        if (crit < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.crit.response.crit_out_of_range",
                    0
            ));
        }
        double critCap = critCaps[levelIndex];
        double critPercent = crit / critCap;
        return Math.max(0, Math.min(CRIT_MAX_PERCENT, critPercent)) * 100D;
    }

    public int calculateCritRequiredForPercent(double percent, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > critCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.crit.response.level_out_of_range",
                    1,
                    critCaps.length
            ));
        }
        if (percent < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.crit.response.crit_out_of_range",
                    0
            ));
        }
        percent = Math.min(percent, CRIT_MAX_PERCENT);
        double critCap = critCaps[levelIndex];
        double crit = critCap * percent;
        return (int) Math.round(crit);
    }

}
