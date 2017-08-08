package com.divinitor.discord.vahrhedralbot;

public interface BotComponent {

    void register(EntryPoint entryPoint) throws Exception;

    void init() throws Exception;

}
