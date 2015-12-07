package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;

public class MemberChangeEvent {

    private final Member member;
    private final Server server;
    private final MemberChange memberChange;

    public MemberChangeEvent(Member member, Server server, MemberChange memberChange) {
        this.member = member;
        this.server = server;
        this.memberChange = memberChange;
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

    public enum MemberChange {
        ADDED,
        DELETED,
        UPDATED
    }

}
