package com.divinitor.discord.vahrhedralbot.component.modcmd;

import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.event.MemberChangeEvent;
import com.divinitor.discord.vahrhedralbot.AbstractBotComponent;
import com.divinitor.discord.vahrhedralbot.EntryPoint;
import com.google.common.eventbus.Subscribe;
import redis.clients.jedis.JedisPool;

public class DmWelcomeCommandListener extends AbstractBotComponent {

    private JedisPool jedisPool;

    @Override
    public void register(EntryPoint entryPoint) throws Exception {
        super.register(entryPoint);

        this.jedisPool = entryPoint.getBot().getJedisPool();
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Subscribe
    public void onUserJoin(MemberChangeEvent event) {
        if (event.getMemberChange() != MemberChangeEvent.MemberChange.ADDED) {
            return;
        }

        //  TODO
    }

    public void sendDm(Server server, String userId) {

    }
}
