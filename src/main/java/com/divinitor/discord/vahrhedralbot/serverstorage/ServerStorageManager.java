package com.divinitor.discord.vahrhedralbot.serverstorage;

public interface ServerStorageManager {

    ServerStorage getOrInit(String serverId);

    void delete(String serverId);
}
