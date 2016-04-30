package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;

public class MemberChangeEvent {

    private final Member member;
    private final Server server;
    private final MemberChange memberChange;
    private final String oldNickname;

    public MemberChangeEvent(Member member, Server server, MemberChange memberChange) {
        this(member,  server,  memberChange,  null);
    }

    public MemberChangeEvent(Member member, Server server, MemberChange memberChange, String oldNickname) {
        this.member = member;
        this.server = server;
        this.memberChange = memberChange;
        this.oldNickname = oldNickname;
    }

    public Member getMember() {
        return member;
    }

    public Server getServer() {
        return server;
    }

    public MemberChange getMemberChange() {
        return memberChange;
    }

    public String getOldNickname() {
        return oldNickname;
    }

    public enum MemberChange {
        ADDED,
        DELETED,
        UPDATED
    }

}
