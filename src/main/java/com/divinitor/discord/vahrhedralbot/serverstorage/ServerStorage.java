package com.divinitor.discord.vahrhedralbot.serverstorage;

import java.util.Map;

public interface ServerStorage {

    void put(String key, Object value);

    void put(String key, String value);

    <T> T get(String key, Class<T> clazz);

    String getString(String key);

    Map<String, String> getAll();

    void delete(String key);
}
