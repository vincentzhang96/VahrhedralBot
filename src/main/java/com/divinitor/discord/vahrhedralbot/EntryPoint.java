package com.divinitor.discord.vahrhedralbot;

import co.phoenixlab.discord.VahrhedralBot;
import com.divinitor.discord.vahrhedralbot.secrets.SecretsStore;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class EntryPoint {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot");

    private final VahrhedralBot bot;

    private final List<BotComponent> components;
    private final Map<Class<? extends BotComponent>, BotComponent> componentsAsMap;

    private SecretsStore secretsStore;

    public EntryPoint(VahrhedralBot bot) {
        this.bot = bot;
        this.components = new ArrayList<>();
        this.componentsAsMap = new HashMap<>();
    }

    public void init() {
        //  Load components
        Reflections reflections = new Reflections("com.divinitor.discord.vahrhedralbot.component");
        Set<Class<? extends BotComponent>> componentClasses = reflections.getSubTypesOf(BotComponent.class);
        componentClasses.stream()
            .filter(c -> !(c.isInterface() || Modifier.isAbstract(c.getModifiers())))
            .forEach(this::createComponent);

        components.forEach(this::initComponent);

        LOGGER.info("Loaded {} components", components.size());
    }

    private void createComponent(Class<? extends BotComponent> clazz) {
        BotComponent component = loadComponent(clazz);
        try {
            component.register(this);
        } catch (Exception e) {
            LOGGER.warn("Unable to initialize component " + clazz.getName(), e);
            return;
        }

        components.add(component);
        componentsAsMap.put(clazz, component);
        LOGGER.info("Registered component {}", clazz.getName());
    }

    @SuppressWarnings("unchecked")
    private BotComponent loadComponent(Class<? extends BotComponent> clazz) {
        try {
            Constructor<? extends BotComponent> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("BotComponent class " + clazz.getSimpleName() +
                " has no valid constructor matching ctor(EntryPoint)");
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Unable to init " + clazz.getSimpleName(), e);
        }
    }

    private void initComponent(BotComponent component) {
        try {
            component.init();
            bot.getApiClient().getEventBus().register(component);
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize component " + component.getClass().getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BotComponent> T get(Class<? extends BotComponent> clazz) {
        return (T) componentsAsMap.get(clazz);
    }

    public VahrhedralBot getBot() {
        return bot;
    }

    public SecretsStore getSecretsStore() {
        return secretsStore;
    }
}
