package com.divinitor.discord.vahrhedralbot.serverstorage.impl;

import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorage;
import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorageManager;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

public class RedisServerStorageManager implements ServerStorageManager {

    public static final String SERVERS_KEY_BASE = "com.divinitor.discord.vahrhedralbot.servers.";

    public static String serversKeyBase() {
        return SERVERS_KEY_BASE;
    }

    private final JedisPool pool;

    private final Map<String, RedisServerStorage> storages;

    public RedisServerStorageManager(JedisPool pool) {
        this.pool = pool;
        storages = new HashMap<>();
    }

    @Override
    public RedisServerStorage getOrInit(String serverId) {
        return storages.computeIfAbsent(serverId, s -> new RedisServerStorage(this, pool, serverId));
    }

    @Override
    public void delete(String serverId) {
        getOrInit(serverId).delete();
    }
}
