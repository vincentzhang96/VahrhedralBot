package com.divinitor.discord.vahrhedralbot.secrets;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public interface SecretsStore {

    void putSecret(String key, String value);

    String getSecret(String key) throws NoSuchElementException;

    String registerChangeListener(String key, Consumer<String> listener);

    void unregisterChangeListener(String listenerId);

    SecretHandle getSecretHandler(String key);

}
