package com.divinitor.discord.vahrhedralbot.serverstorage.impl;

import com.divinitor.discord.vahrhedralbot.serverstorage.ServerStorage;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class RedisServerStorage implements ServerStorage {

    private final RedisServerStorageManager manager;
    private final JedisPool jedisPool;
    private final String serverId;
    private final String redisKey;
    private final Gson gson;

    public RedisServerStorage(RedisServerStorageManager manager, JedisPool jedisPool, String serverId) {
        this.manager = manager;
        this.jedisPool = jedisPool;
        this.serverId = serverId;
        this.redisKey = RedisServerStorageManager.serversKeyBase() + serverId;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    @Override
    public void put(String key, Object value) {
        this.put(key, gson.toJson(value));
    }

    @Override
    public void put(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(redisKey, key, value);
        }
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String val = getString(key);
        if (val == null) {
            return null;
        }

        return gson.fromJson(val, clazz);
    }

    @Override
    public String getString(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(redisKey, key);
        }
    }

    @Override
    public Map<String, String> getAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(redisKey);
        }
    }

    @Override
    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(redisKey, key);
        }
    }

    public void delete() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKey);
        }
    }
}
