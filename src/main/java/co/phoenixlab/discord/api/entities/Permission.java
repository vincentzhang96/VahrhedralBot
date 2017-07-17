package co.phoenixlab.discord.api.entities;

import java.util.EnumSet;

public enum Permission {
    GEN_CREATE_INSTANT_INVITE(0x00000001),
    GEN_KICK_MEMBERS(0x00000002),
    GEN_BAN_MEMBERS(0x00000004),
    ADMINISTRATOR(0x00000008),
    GEN_MANAGE_CHANNELS(0x00000010),
    GEN_MANAGE_SERVER(0x00000020),
    CHAT_READ_MESSAGES(0x00000400),
    CHAT_SEND_MESSAGES(0x00000800),
    CHAT_SEND_TTS_MESSAGES(0x00001000),
    CHAT_MANAGE_MESSAGES(0x00002000),
    CHAT_EMBED_LINKS(0x00004000),
    CHAT_ATTACH_FILES(0x00008000),
    CHAT_READ_MESSAGE_HISTORY(0x00010000),
    CHAT_MENTION_EVERYONE(0x00020000),
    VOICE_CONNECT(0x00100000),
    VOICE_SPEAK(0x00200000),
    VOICE_MUTE_MEMBERS(0x00400000),
    VOICE_DEAFEN_MEMBERS(0x00800000),
    VOICE_MOVE_MEMBERS(0x01000000),
    VOICE_USE_VAD(0x02000000),
    NICK_CHANGE_NICKNAME(0x04000000),
    NICK_MANAGE_NICKNAMES(0x08000000),
    GEN_MANAGE_ROLES(0x10000000);


    private final int mask;

    Permission(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }

    public long toLong() {
        return (long) mask;
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
