package co.phoenixlab.discord.commands.dn;

import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.common.lang.number.ParseLong;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Embed;
import co.phoenixlab.discord.api.entities.EmbedAuthor;
import co.phoenixlab.discord.api.entities.EmbedField;
import co.phoenixlab.discord.api.entities.EmbedFooter;

import java.util.List;

public class DnFdCommand implements Command {

    public static final float FD_MAX_PERCENT = 1.0F;
    public static final float[] fdCaps = {
            75.0F, 87.0F, 99.0F, 111.0F, 123.0F, 135.0F, 147.0F, 159.0F, 171.0F, 183.0F,
            195.0F, 207.0F, 219.0F, 231.0F, 285.0F, 300.0F, 315.0F, 330.0F, 345.0F,
            360.0F, 375.0F, 390.0F, 405.0F, 420.0F, 442.0F, 465.0F, 487.0F, 510.0F,
            532.0F, 555.0F, 577.0F, 600.0F, 630.0F, 660.0F, 690.0F, 720.0F, 750.0F,
            780.0F, 810.0F, 850.0F, 894.0F, 938.0F, 982.0F, 1026.0F, 1070.0F, 1114.0F,
            1158.0F, 1202.0F, 1246.0F, 1290.0F, 1346.0F, 1402.0F, 1458.0F, 1514.0F,
            1570.0F, 1626.0F, 1682.0F, 1738.0F, 1794.0F, 1850.0F, 1962.0F, 2074.0F,
            2187.0F, 2299.0F, 2412.0F, 2524.0F, 2636.0F, 2749.0F, 2861.0F, 2974.0F,
            3128.0F, 3283.0F, 3437.0F, 3592.0F, 3747.0F, 3901.0F, 4056.0F, 4210.0F,
            4365.0F, 4520.0F, 4704.0F, 4889.0F, 5074.0F, 5258.0F, 5443.0F, 5628.0F,
            5812.0F, 5997.0F, 6182.0F, 6367.0F, 7022.0F, 7678.0F, 8333.0F, 8989.0F,
            9644.0F, 10300.0F, 10955.0F, 11611.0F, 12266.0F, 12922.0F
    };
    public static final float FD_POW = 2.2F;
    public static final float FD_COEFF = 0.35F;
    public static final float FD_BREAKING_POINT = 0.417F;
    public static final float FD_INVERSE_POWER = 0.45454545454545454545F;
    public static final float FD_INVERSE_COEFF = 2.857142857F;
    public static final float FD_INVERSE_BREAKING_POINT = 0.146F;
    public static final int[] FD_DEFAULT_LEVELS;

    static {
        FD_DEFAULT_LEVELS = new int[]{93, 90, 80};
    }

    private final Localizer loc;

    public DnFdCommand(Localizer loc) {
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
        int fd = -1;
        float fdPercent = 0;
        String fdAmt = split[0];
        try {
            if (fdAmt.endsWith("%")) {
                fdPercent = (float) Double.parseDouble(fdAmt.substring(0, fdAmt.length() - 1)) / 100F;
                fdPercent = Math.min(fdPercent, FD_MAX_PERCENT);
            } else {
                fd = (int) DnCommandUtils.parseStat(fdAmt, loc);
                if (fd < 0) {
                    throw new IllegalArgumentException(loc.localize(
                            "commands.dn.finaldamage.response.fd_out_of_range",
                            0
                    ));
                }
            }
            if (level == -1) {
                String title;
                //  Default level, use embed style for showing lvl 80, 90, and 93 FD
                Embed embed = new Embed();
                embed.setType(Embed.TYPE_RICH);
                embed.setColor(5941733);    //  GLAZE Accent 2 (temporary, replace with rolecolor)
                EmbedField[] fields = new EmbedField[FD_DEFAULT_LEVELS.length];
                if (fd == -1) {
                    title = String.format("**__%.1f%% FD__**", fdPercent * 100F);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", FD_DEFAULT_LEVELS[i]),
                                String.format("%,d", calculateFdRequiredForPercent(fdPercent, FD_DEFAULT_LEVELS[i])),
                                true
                        );
                    }
                } else {
                    title = String.format("**__%,d FD__**", fd);
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = new EmbedField(
                                String.format("Level %d", FD_DEFAULT_LEVELS[i]),
                                String.format("%.1f%%", calculateFdPercent(fd, FD_DEFAULT_LEVELS[i])),
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
                if (fd == -1) {
                    fd = calculateFdRequiredForPercent(fdPercent, level);
                    String msg = loc.localize(
                            "commands.dn.finaldamage.response.format.required",
                            level,
                            fd,
                            fdPercent * 100D
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                } else {
                    fdPercent = calculateFdPercent(fd, level);
                    String msg = loc.localize(
                            "commands.dn.finaldamage.response.format",
                            level,
                            fdPercent,
                            (int) (fdCaps[level - 1] * FD_MAX_PERCENT),
                            (int) (FD_MAX_PERCENT * 100D)
                    );
                    apiClient.sendMessage(msg, context.getChannel());
                }
            }
            return;
        } catch (NumberFormatException nfe) {
            apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.invalid",
                    context.getBot().getMainCommandDispatcher().getCommandPrefix()),
                    context.getChannel());
            return;
        } catch (IllegalArgumentException ile) {
            apiClient.sendMessage(ile.getMessage(), context.getChannel());
            return;
        }
    }

    public float calculateFdPercent(int fd, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > fdCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.finaldamage.response.level_out_of_range",
                    1,
                    fdCaps.length
            ));
        }
        if (fd < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.finaldamage.response.fd_out_of_range",
                    0
            ));
        }
        float fdPercent;
        float fdCap = fdCaps[levelIndex];
        float ratio = fd / fdCap;
        if (ratio < FD_BREAKING_POINT) {
            fdPercent = (FD_COEFF * fd) / fdCap;
        } else {
            fdPercent = (float) Math.pow(fd / fdCap, FD_POW);
        }
        return Math.max(0, Math.min(FD_MAX_PERCENT, fdPercent)) * 100F;
    }

    public int calculateFdRequiredForPercent(float percent, int level) {
        int levelIndex = level - 1;
        if (levelIndex < 0 || levelIndex > fdCaps.length) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.finaldamage.response.level_out_of_range",
                    1,
                    fdCaps.length
            ));
        }
        if (percent < 0) {
            throw new IllegalArgumentException(loc.localize(
                    "commands.dn.finaldamage.response.fd_out_of_range",
                    0
            ));
        }
        percent = Math.min(percent, FD_MAX_PERCENT);
        int fd;
        if (percent < FD_INVERSE_BREAKING_POINT) {
            fd = (int) (FD_INVERSE_COEFF * percent * fdCaps[levelIndex]);
        } else {
            //  Since we know fdPercent must be between 0 and 100%, we can't overflow
            //noinspection NumericOverflow
            fd = (int) (fdCaps[levelIndex] * Math.pow(percent, FD_INVERSE_POWER));
        }
        return fd;
    }

}
