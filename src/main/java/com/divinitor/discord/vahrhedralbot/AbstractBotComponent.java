package com.divinitor.discord.vahrhedralbot;

import co.phoenixlab.discord.VahrhedralBot;

public abstract class AbstractBotComponent implements BotComponent {

    private VahrhedralBot bot;
    private EntryPoint entryPoint;

    protected final VahrhedralBot getBot() {
        return bot;
    }

    protected final EntryPoint getEntryPoint() {
        return entryPoint;
    }

    @Override
    public void register(EntryPoint entryPoint) throws Exception {
        this.entryPoint = entryPoint;
        this.bot = entryPoint.getBot();
    }

    @Override
    public void init() throws Exception {
    }
}
