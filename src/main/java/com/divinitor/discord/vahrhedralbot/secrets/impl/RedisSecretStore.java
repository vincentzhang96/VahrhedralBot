package com.divinitor.discord.vahrhedralbot.secrets.impl;

import com.divinitor.discord.vahrhedralbot.secrets.SecretHandle;
import com.divinitor.discord.vahrhedralbot.secrets.SecretsStore;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class RedisSecretStore implements SecretsStore {

    public static final String SECRETS_KEY_BASE = "com.divinitor.discord.vahrhedralbot.secrets.";
    private JedisPool pool;

    private Table<String, String, Consumer<String>> listeners;


    public RedisSecretStore(JedisPool pool) {
        this.pool = pool;
        listeners = HashBasedTable.create();
    }

    @Override
    public void putSecret(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(SECRETS_KEY_BASE + key, value);
            listeners.column(key).values().forEach(c -> c.accept(value));
        }
    }

    @Override
    public String getSecret(String key) throws NoSuchElementException {
        try (Jedis jedis = pool.getResource()) {
            String ret = jedis.get(SECRETS_KEY_BASE + key);
            if (ret == null) {
                throw new NoSuchElementException(key);
            }
            return ret;
        }
    }

    @Override
    public String registerChangeListener(String key, Consumer<String> listener) {
        String listenerId = listener.toString();    //  really lazy here
        String publicListenerId = key + ":" + listenerId;
        listeners.put(key, listenerId, listener);
        return publicListenerId;
    }

    @Override
    public void unregisterChangeListener(String listenerId) {
        String[] split = listenerId.split(":");
        String key = split[0];
        String internalListenerId = split[1];
        listeners.remove(key, internalListenerId);
    }

    @Override
    public SecretHandle getSecretHandler(String key) {
        return () -> getSecret(key);
    }
}
