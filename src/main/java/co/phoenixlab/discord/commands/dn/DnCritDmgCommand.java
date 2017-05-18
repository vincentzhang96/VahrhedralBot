package co.phoenixlab.discord.commands.dn;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Embed;
import co.phoenixlab.discord.api.entities.EmbedField;
import co.phoenixlab.discord.api.entities.EmbedFooter;

public class DnCritDmgCommand implements Command {

    public static final float CRITDMG_MAX_PERCENT = 1.0F;
    public static final float[] critDmgCaps = {
            2650.0F, 3074.0F, 3498.0F, 3922.0F, 4346.0F, 4770.0F, 5194.0F, 5618.0F,
            6042.0F, 6466.0F, 6890.0F, 7314.0F, 7738.0F, 8162.0F, 10070.0F, 10600.0F,
            11130.0F, 11660.0F, 12190.0F, 12720.0F, 13250.0F, 13780.0F, 14310.0F,
            14840.0F, 15635.0F, 16430.0F, 17225.0F, 18020.0F, 18815.0F, 19610.0F,
            20405.0F, 21200.0F, 22260.0F, 23320.0F, 24380.0F, 25440.0F, 26500.0F,
            27560.0F, 28620.0F, 29680.0F, 31641.0F, 33575.0F, 35510.0F, 37206.0F,
            39326.0F, 41419.0F, 43513.0F, 45553.0F, 47832.0F, 50350.0F, 55650.0F,
            59757.0F, 64580.0F, 69589.0F, 74756.0F, 80109.0F, 86310.0F, 93121.0F,
            99375.0F, 103350.0F, 107987.0F, 113950.0F, 121237.0F, 129850.0F, 139787.0F,
            151050.0F, 163637.0F, 177894.0F, 193794.0F, 211337.0F, 228555.0F, 245520.0F,
            262220.0F, 278587.0F, 296902.0F, 320100.0F, 343263.0F, 374976.0F, 407979.0F,
            431970.0F, 453390.0F, 474810.0F, 496230.0F, 517650.0F, 542640.0F, 567630.0F,
            592620.0F, 617610.0F, 642600.0F, 671160.0F, 769692.0F, 801108.0F, 832524.0F,
            1021020.0F, 1348924.0F, 1486905.0F, 1631311.0F, 1782144.0F, 1986705.0F, 2249814.0F
    };

    private final Localizer loc;
    public static final int[] CRITDMG_DEFAULT_LEVELS;

    static {
        CRITDMG_DEFAULT_LEVELS = new int[]{95, 93, 90, 80};
    }

    public DnCritDmgCommand(Localizer loc) {
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
        int critDmg = -1;
        float critDmgPercent = 0;
        String critDmgAmt = split[0];
        try {
            if (critDmgAmt.endsWith("%")) {
                String noPercent = critDmgAmt.substring(0, critDmgAmt.length() - 1);
                critDmgPercent = ((float) Double.parseDouble(noPercent) - 200F) / 100F;
                critDmgPercent = Math.min(critDmgPercent, CRITDMG_MAX_PERCENT);
            } else {
                critDmg = (int) DnCommandUtils.parseStat(critDmgAmt, loc);
                if (critDmg < 0) {
                    throw new IllegalArgumentException(loc.localize(
                            "commands.dn.critdmg.response.critdmg_out_of_range",
                            0, 200
                    ));
                }
            }
            if (level == -1) {
                String title;
                //  Default level, use embed style for showing lvl 80, 90, and 93 critdmg
                Embed embed = new Embed();
                embed.setType(Embed.TYPE_RICH);
                embed.setColor(5941733);    //  GLAZE Accent 2 (temporary, replace with rolecolor)
                EmbedField[] fields = new EmbedField[CRITDMG_DEFAULT_LEVELS.length];
                if (critDmg == -1) {
                    title = String.format("**__%.1f%% Critical Damage__**", critDmgPercent * 100F + 200F);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", CRITDMG_DEFAULT_LEVELS[i]),
                                String.format("%,d",
                                        calculateCritDmgRequiredForPercent(critDmgPercent, CRITDMG_DEFAULT_LEVELS[i])),
                                true
                        );
                    }
                } else {
                    title = String.format("**__%,d Critical Damage__**", critDmg);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", CRITDMG_DEFAULT_LEVELS[i]),
                                String.format("%.1f%%",
                                        calculateCritDmgPercent(critDmg, CRITDMG_DEFAULT_LEVELS[i]) + 200F),
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
                if (critDmg == -1) {
                    critDmg = calculateCritDmgRequiredForPercent(critDmgPercent, level);
                    String msg = loc.localize(
                            "commands.dn.critdmg.response.format.required",
                            level,
                            critDmg,
                            critDmgPercent * 100D + 200D
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                } else {
                    critDmgPercent = calculateCritDmgPercent(critDmg, level) + 200F;
                    String msg = loc.localize(
                            "commands.dn.critdmg.response.format",
                            level,
                            critDmgPercent,
                            (int) (critDmgCaps[level - 1] * CRITDMG_MAX_PERCENT),
                            (int) (CRITDMG_MAX_PERCENT * 100D + 200F)
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                }
            }
        } catch (NumberFormatException nfe) {
            apiClient.sendMessage(loc.localize("commands.dn.critdmg.response.invalid",
                    context.getBot().getMainCommandDispatcher().getCommandPrefix()),
                    context.getChannel());
        } catch (IllegalArgumentException ile) {
            apiClient.sendMessage(ile.getMessage(), context.getChannel());
        }
    }

    public float calculateCritDmgPercent(int critDmg, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > critDmgCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.critdmg.response.level_out_of_range",
                    1,
                    critDmgCaps.length
            ));
        }
        if (critDmg < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.critdmg.response.critdmg_out_of_range",
                    0, 200
            ));
        }
        float critDmgCap = critDmgCaps[levelIndex];
        float critDmgPercent = critDmg / critDmgCap;
        return Math.max(0, Math.min(CRITDMG_MAX_PERCENT, critDmgPercent)) * 100F;
    }

    public int calculateCritDmgRequiredForPercent(float percent, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > critDmgCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.critdmg.response.level_out_of_range",
                    1,
                    critDmgCaps.length
            ));
        }
        if (percent < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.critdmg.response.critdmg_out_of_range",
                    0, 200
            ));
        }
        percent = Math.min(percent, CRITDMG_MAX_PERCENT);
        float critDmgCap = critDmgCaps[levelIndex];
        float critDmg = critDmgCap * percent;
        return (int) critDmg;
    }
}
