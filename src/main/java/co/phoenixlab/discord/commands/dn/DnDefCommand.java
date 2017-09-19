package co.phoenixlab.discord.commands.dn;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Embed;
import co.phoenixlab.discord.api.entities.EmbedField;
import co.phoenixlab.discord.api.entities.EmbedFooter;

public class DnDefCommand implements Command {
    
    public static final float DEF_MAX_PERCENT = 0.85F;
    public static final float[] defenseCaps = {
            750.0F, 870.0F, 990.0F, 1110.0F, 1230.0F, 1350.0F, 1470.0F, 1590.0F,
            1710.0F, 1830.0F, 1950.0F, 2070.0F, 2190.0F, 2310.0F, 2850.0F, 3000.0F,
            3150.0F, 3300.0F, 3450.0F, 3600.0F, 3750.0F, 3900.0F, 4050.0F, 4200.0F,
            4425.0F, 4650.0F, 4875.0F, 5100.0F, 5325.0F, 5550.0F, 5775.0F, 6000.0F,
            6300.0F, 6600.0F, 6900.0F, 7200.0F, 7500.0F, 7800.0F, 8100.0F, 8400.0F,
            9000.0F, 9600.0F, 10200.0F, 10800.0F, 11475.0F, 12150.0F, 12825.0F,
            13500.0F, 14250.0F, 15000.0F, 15750.0F, 16912.0F, 18277.0F, 19695.0F,
            21157.0F, 22672.0F, 24427.0F, 26355.0F, 28125.0F, 29250.0F, 30868.0F,
            33056.0F, 35685.0F, 38771.0F, 42331.0F, 46383.0F, 50943.0F, 56137.0F,
            61977.0F, 68186.0F, 72523.0F, 76637.0F, 80534.0F, 84201.0F, 88328.0F,
            92188.0F, 95006.0F, 99837.0F, 104590.0F, 106722.0F, 112014.0F, 117306.0F,
            122598.0F, 127890.0F, 134064.0F, 140238.0F, 146412.0F, 152586.0F,
            158760.0F, 165816.0F, 187278.0F, 209916.0F, 233730.0F, 258720.0F,
            286135.0F, 314874.0F, 344935.0F, 376320.0F, 409027.0F, 443058.0F
    };

    private final Localizer loc;
    public static final int[] DEF_DEFAULT_LEVELS = DnCommandUtils.getDefaultLevels();

    public DnDefCommand(Localizer loc) {
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
        int def = -1;
        float defPercent = 0;
        String defAmt = split[0];
        try {
            if (defAmt.equalsIgnoreCase("cap")) {
                defPercent = DEF_MAX_PERCENT;
            } else if (defAmt.endsWith("%")) {
                defPercent = (float) Double.parseDouble(defAmt.substring(0, defAmt.length() - 1)) / 100F;
                defPercent = Math.min(defPercent, DEF_MAX_PERCENT);
            } else {
                def = (int) DnCommandUtils.parseStat(defAmt, loc);
                if (def < 0) {
                    throw new IllegalArgumentException(loc.localize(
                            "commands.dn.def.response.def_out_of_range",
                            0
                    ));
                }
            }
            if (level == -1) {
                String title;
                //  Default level, use embed style for showing lvl 80, 90, and 93 def
                Embed embed = new Embed();
                embed.setType(Embed.TYPE_RICH);
                embed.setColor(5941733);    //  GLAZE Accent 2 (temporary, replace with rolecolor)
                EmbedField[] fields = new EmbedField[DEF_DEFAULT_LEVELS.length];
                if (def == -1) {
                    title = String.format("**__%.1f%% Defense__**", defPercent * 100F);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", DEF_DEFAULT_LEVELS[i]),
                                String.format("%,d", calculateDefRequiredForPercent(defPercent, DEF_DEFAULT_LEVELS[i])),
                                true
                        );
                    }
                } else {
                    title = String.format("**__%,d Defense__**", def);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", DEF_DEFAULT_LEVELS[i]),
                                String.format("%.1f%%", calculateDefPercent(def, DEF_DEFAULT_LEVELS[i])),
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
                if (def == -1) {
                    def = calculateDefRequiredForPercent(defPercent, level);
                    String msg = loc.localize(
                            "commands.dn.def.response.format.required",
                            level,
                            def,
                            defPercent * 100F,
                            1F / (1F - defPercent)
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                } else {
                    defPercent = calculateDefPercent(def, level);
                    String msg = loc.localize(
                            "commands.dn.def.response.format",
                            level,
                            defPercent,
                            (int) (defenseCaps[level - 1] * DEF_MAX_PERCENT),
                            (int) (DEF_MAX_PERCENT * 100F),
                            1F / (1F - (defPercent / 100F))
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                }
            }
        } catch (NumberFormatException nfe) {
            apiClient.sendMessage(loc.localize("commands.dn.def.response.invalid",
                    context.getBot().getMainCommandDispatcher().getCommandPrefix()),
                    context.getChannel());
        } catch (IllegalArgumentException ile) {
            apiClient.sendMessage(ile.getMessage(), context.getChannel());
        }
    }


    public float calculateDefPercent(int def, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > defenseCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.def.response.level_out_of_range",
                    1,
                    defenseCaps.length
            ));
        }
        if (def < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.def.response.def_out_of_range",
                    0
            ));
        }
        float defCap = defenseCaps[levelIndex];
        float defPercent = def / defCap;
        return Math.max(0, Math.min(DEF_MAX_PERCENT, defPercent)) * 100F;
    }

    public int calculateDefRequiredForPercent(float percent, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > defenseCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.def.response.level_out_of_range",
                    1,
                    defenseCaps.length
            ));
        }
        if (percent < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.def.response.def_out_of_range",
                    0
            ));
        }
        percent = Math.min(percent, DEF_MAX_PERCENT);
        float derCap = defenseCaps[levelIndex];
        float def = derCap * percent;
        return (int) def;
    }
}
