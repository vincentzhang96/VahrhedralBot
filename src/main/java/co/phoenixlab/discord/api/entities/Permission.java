package co.phoenixlab.discord.api.entities;

import java.util.EnumSet;

public enum Permission {
    GEN_CREATE_INSTANT_INVITE(0),
    GEN_KICK_MEMBERS(1),
    GEN_BAN_MEMBERS(2),
    GEN_MANAGE_ROLES(3),
    GEN_MANAGE_PERMISSIONS(4),
    GEN_MANAGE_CHANNELS(5),
    GEN_MANAGE_SERVER(6),
    CHAT_READ_MESSAGES(10),
    CHAT_SEND_MESSAGES(11),
    CHAT_SEND_TTS_MESSAGES(12),
    CHAT_MANAGE_MESSAGES(13),
    CHAT_EMBED_LINKS(14),
    CHAT_ATTACH_FILES(15),
    CHAT_READ_MESSAGE_HISTORY(16),
    CHAT_MENTION_EVERYONE(17),
    VOICE_CONNECT(20),
    VOICE_SPEAK(21),
    VOICE_MUTE_MEMBERS(22),
    VOICE_DEAFEN_MEMBERS(23),
    VOICE_MOVE_MEMBERS(24),
    VOICE_USE_VAD(25);


    private final int bitNum;

    Permission(int bitNum) {
        this.bitNum = bitNum;
    }

    public int getBitNum() {
        return bitNum;
    }

    public long toLong() {
        return 1L << bitNum;
    }

    public boolean test(long l) {
        return (l & toLong()) != 0;
    }

    public static EnumSet<Permission> fromLong(long l) {
        EnumSet<Permission> ret = EnumSet.noneOf(Permission.class);
        for (Permission permission : values()) {
            if (permission.test(l)) {
                ret.add(permission);
            }
        }
        return ret;
    }

    public static long toLong(EnumSet<Permission> set) {
        long accum = 0L;
        for (Permission permission : set) {
            accum |= permission.toLong();
        }
        return accum;
    }
}
