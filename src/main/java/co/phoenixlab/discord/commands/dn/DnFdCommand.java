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
            } else {
                fd = (int) parseStat(fdAmt);
            }
        } catch (NumberFormatException nfe) {
            apiClient.sendMessage(loc.localize("commands.dn.finaldamage.response.invalid",
                    context.getBot().getMainCommandDispatcher().getCommandPrefix()),
                    context.getChannel());
            return;
        }

        if (level == -1) {
            //  Default level, use embed style for showing lvl 80, 90, and 93 FD
            Embed embed = new Embed();
            embed.setType(Embed.TYPE_RICH);
            embed.setColor(5941733);    //  GLAZE Accent 2 (temporary, replace with rolecolor)
            EmbedField[] fields = new EmbedField[3];
            EmbedField lvl93 = new EmbedField("Level 93", "", true);
            fields[0] = lvl93;
            EmbedField lvl90 = new EmbedField("Level 90", "", true);
            fields[1] = lvl90;
            EmbedField lvl80 = new EmbedField("Level 80", "", true);
            fields[2] = lvl80;
            String title;
            if (fd == -1) {
                title = String.format("**__%.1f%% FD__**", fdPercent * 100F);
                lvl93.setValue(String.format("%,d", calculateFdRequiredForPercent(fdPercent, 93)));
                lvl90.setValue(String.format("%,d", calculateFdRequiredForPercent(fdPercent, 90)));
                lvl80.setValue(String.format("%,d", calculateFdRequiredForPercent(fdPercent, 80)));
            } else {
                title = String.format("**__%,d FD__**", fd);
                lvl93.setValue(String.format("%.1f%%", calculateFdPercent(fd, 93)));
                lvl90.setValue(String.format("%.1f%%", calculateFdPercent(fd, 90)));
                lvl80.setValue(String.format("%.1f%%", calculateFdPercent(fd, 80)));
            }
            embed.setFields(fields);
            EmbedFooter footer = new EmbedFooter();
            footer.setText("Divinitor PALADINS");
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
        percent = Math.min(percent, 1.0F);
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
