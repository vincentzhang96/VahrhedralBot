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

    public static final float CRIT_MAX_PERCENT = 0.89F;
    public static final float[] critCaps = {
            1000.0F, 1160.0F, 1320.0F, 1480.0F, 1640.0F, 1800.0F, 1960.0F, 2120.0F,
            2280.0F, 2440.0F, 2600.0F, 2760.0F, 2920.0F, 3080.0F, 3800.0F, 4000.0F,
            4200.0F, 4400.0F, 4600.0F, 4800.0F, 5000.0F, 5200.0F, 5400.0F, 5600.0F,
            5900.0F, 6200.0F, 6500.0F, 6800.0F, 7100.0F, 7400.0F, 7700.0F, 8000.0F,
            8400.0F, 8800.0F, 9200.0F, 9600.0F, 10000.0F, 10400.0F, 10800.0F, 11200.0F,
            12000.0F, 12800.0F, 13600.0F, 14400.0F, 15300.0F, 16200.0F, 17100.0F,
            18000.0F, 19000.0F, 20000.0F, 21500.0F, 23000.0F, 24600.0F, 26200.0F,
            27900.0F, 29600.0F, 31400.0F, 33200.0F, 35200.0F, 37200.0F, 40200.0F,
            43200.0F, 46200.0F, 49200.0F, 52400.0F, 55600.0F, 58800.0F, 62000.0F,
            65400.0F, 68800.0F, 74745.0F, 80619.0F, 86438.0F, 92373.0F, 98284.0F,
            104245.0F, 110176.0F, 116008.0F, 121899.0F, 127685.0F, 138684.0F, 149565.0F,
            160545.0F, 171433.0F, 182263.0F, 192994.0F, 203931.0F, 214891.0F, 225855.0F,
            236880.0F, 277830.0F, 321300.0F, 367290.0F, 415800.0F, 468877.0F, 524790.0F,
            583537.0F, 645120.0F, 709537.0F, 776790.0F
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
        float critPercent = 0;
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


    public float calculateCritPercent(int crit, int level) {
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
        float critCap = critCaps[levelIndex];
        float critPercent = crit / critCap;
        return Math.max(0, Math.min(CRIT_MAX_PERCENT, critPercent)) * 100F;
    }

    public int calculateCritRequiredForPercent(float percent, int level) {
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
        float critCap = critCaps[levelIndex];
        float crit = critCap * percent;
        return (int) crit;
    }

}
