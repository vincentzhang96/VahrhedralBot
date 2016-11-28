package co.phoenixlab.discord.util;

import co.phoenixlab.common.lang.number.ParseLong;

public class SnowflakeUtils {

    public static final String CODEX = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int BASE = CODEX.length();

    private SnowflakeUtils() {}

    public static long decodeSnowflake(String encoded) {
        if (!encoded.startsWith("$")) {
            throw new IllegalArgumentException("Not a valid encoded snowflake: " + encoded);
        }
        encoded = encoded.substring(1);
        return parse(encoded);
    }


    public static String encodeSnowflake(String snowflake) {
        return encodeSnowflake(ParseLong.parseOrDefault(snowflake, 0));
    }

    public static String encodeSnowflake(long snowflake) {
        return "$" + encode(snowflake);
    }

    private static long parse(String r62) {
        char[] chars = r62.toCharArray();
        int length = chars.length;
        long ret = 0;
        //  Read the number from right to left (smallest place to largest)
        //  WARNING: NO UNDER/OVERFLOW CHECKING IS DONE
        int lenLessOne = length - 1;
        for (int i = lenLessOne; i >= 0; i--) {
            long digit = charToDigit(chars[i]);
            int placeValue = lenLessOne - i;
            long addnum = digit * (long) Math.pow(BASE, placeValue);
            ret += addnum;
        }
        return ret;
    }

    private static String encode(long l) {
        long accum = l;
        StringBuilder builder = new StringBuilder();
        long remainder;
        while (Long.compareUnsigned(accum, 0) > 0) {
            long last = accum;
            accum = accum / BASE;
            remainder = last - (accum * BASE);
            builder.append(digitToChar(Math.abs((int) remainder)));
        }
        String ret = builder.reverse().toString();
        //  Strip leading zeros
        for (int i = 0; i < ret.length(); i++) {
            if (ret.charAt(i) != '0') {
                ret = ret.substring(i);
                break;
            }
        }
        return ret;
    }

    private static char digitToChar(int i) {
        return CODEX.charAt(i);
    }

    private static int charToDigit(char c) {
        return CODEX.indexOf(c);
    }

}
