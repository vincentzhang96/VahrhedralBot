package co.phoenixlab.discord.util;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class WeakEventSubscriber<T> {

    private final WeakReference<Consumer<T>> reference;
    private final EventBus eventBus;
    private final Class<T> typeOfT;

    public WeakEventSubscriber(Consumer<T> subscriber, EventBus eventBus, Class<T> typeOfT) {
        this.eventBus = eventBus;
        this.typeOfT = typeOfT;
        this.reference = new WeakReference<>(subscriber);
        this.eventBus.register(this);
    }

    @SuppressWarnings("unchecked")
    @Subscribe
    public void handle(Object eventObj) {
        if (typeOfT.isInstance(eventObj)) {
            Consumer<T> handler = reference.get();
            if (handler != null) {
                handler.accept((T) eventObj);
            } else {
                eventBus.unregister(this);
            }
        }
    }



}
