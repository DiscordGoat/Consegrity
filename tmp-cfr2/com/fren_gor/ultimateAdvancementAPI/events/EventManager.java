/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class EventManager {
    private static final int priorities;
    @NotNull
    private final Plugin plugin;
    private final Object INTERNAL_LISTENER = new Object();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final Map<Class<? extends Event>, EventGroup<? extends Event>> events = new HashMap<Class<? extends Event>, EventGroup<? extends Event>>();

    public EventManager(@NotNull Plugin plugin) throws IllegalArgumentException {
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin cannot be null.");
        Preconditions.checkArgument((boolean)plugin.isEnabled(), (Object)"Plugin isn't enabled.");
        this.plugin = plugin;
        this.registerPluginDisableEvent();
    }

    public <E extends Event> void register(@NotNull Object listener, @NotNull Class<E> event, @NotNull Consumer<E> consumer) throws IllegalStateException, IllegalArgumentException {
        this.register(listener, event, EventPriority.NORMAL, consumer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public <E extends Event> void register(@NotNull Object listener, @NotNull Class<E> event, @NotNull EventPriority priority, @NotNull Consumer<E> consumer) throws IllegalStateException, IllegalArgumentException {
        EventGroup el;
        if (!this.enabled.get()) {
            throw new IllegalStateException("EventManager is disabled. Cannot register any event.");
        }
        if (!this.plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin is disabled. Cannot register any event.");
        }
        Preconditions.checkNotNull((Object)listener, (Object)"Listener cannot be null.");
        Preconditions.checkNotNull(event, (Object)"Event class cannot be null.");
        Preconditions.checkNotNull((Object)priority, (Object)"EventPriority cannot be null.");
        Preconditions.checkNotNull(consumer, (Object)"Consumer cannot be null.");
        Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
        synchronized (map) {
            el = this.events.computeIfAbsent(event, c -> new EventGroup());
        }
        el.getListener(priority, event).register(listener, consumer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void unregister(@NotNull Object listener) throws IllegalStateException, IllegalArgumentException {
        this.checkInitialisation();
        Preconditions.checkNotNull((Object)listener, (Object)"Listener cannot be null.");
        Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
        synchronized (map) {
            for (EventGroup<? extends Event> el : this.events.values()) {
                el.unregisterListener(listener);
            }
            if (listener == this.INTERNAL_LISTENER) {
                this.registerPluginDisableEvent();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clearEventListener(@NotNull Class<? extends Event> event) throws IllegalStateException, IllegalArgumentException {
        this.checkInitialisation();
        Preconditions.checkNotNull(event, (Object)"Event class cannot be null.");
        Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
        synchronized (map) {
            EventGroup<? extends Event> el = this.events.get(event);
            if (el != null) {
                el.clearListeners();
                if (event == PluginDisableEvent.class) {
                    this.registerPluginDisableEvent();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void unregisterEvent(@NotNull Class<? extends Event> event) throws IllegalStateException, IllegalArgumentException {
        this.checkInitialisation();
        Preconditions.checkNotNull(event, (Object)"Event class cannot be null.");
        Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
        synchronized (map) {
            EventGroup<? extends Event> el = this.events.remove(event);
            if (el != null) {
                el.unregisterBukkitListener();
                if (event == PluginDisableEvent.class) {
                    this.registerPluginDisableEvent();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void disable() {
        if (!this.enabled.compareAndSet(true, false)) {
            return;
        }
        Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
        synchronized (map) {
            for (EventGroup<? extends Event> el : this.events.values()) {
                el.unregisterBukkitListener();
            }
            this.events.clear();
        }
    }

    @NotNull
    public Plugin getPlugin() {
        return this.plugin;
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    private void registerPluginDisableEvent() {
        this.register(this.INTERNAL_LISTENER, PluginDisableEvent.class, EventPriority.MONITOR, e -> {
            if (e.getPlugin() == this.plugin) {
                this.enabled.set(false);
                Map<Class<? extends Event>, EventGroup<? extends Event>> map = this.events;
                synchronized (map) {
                    this.events.clear();
                }
            }
        });
    }

    private void checkInitialisation() throws IllegalStateException {
        if (!this.enabled.get()) {
            throw new IllegalStateException("EventManager is disabled. Cannot perform any action.");
        }
    }

    static /* synthetic */ int access$000() {
        return priorities;
    }

    static {
        int max = 0;
        for (EventPriority p : EventPriority.values()) {
            if (max >= p.getSlot()) continue;
            max = p.getSlot();
        }
        priorities = max + 1;
    }

    private final class EventGroup<E extends Event> {
        private final EventListener<E>[] eventListeners = new EventListener[EventManager.access$000()];

        private EventGroup() {
        }

        @NotNull
        public synchronized EventListener<E> getListener(@NotNull EventPriority priority, @NotNull Class<E> event) {
            EventListener<E> l = this.eventListeners[priority.getSlot()];
            if (l == null) {
                EventListener<E> eventListener = new EventListener<E>(event, priority);
                this.eventListeners[priority.getSlot()] = eventListener;
                return eventListener;
            }
            return l;
        }

        public synchronized void unregisterListener(@NotNull Object listener) {
            for (int i = 0; i < this.eventListeners.length; ++i) {
                EventListener<E> l = this.eventListeners[i];
                if (l == null) continue;
                l.unregisterListener(listener);
            }
        }

        public synchronized void clearListeners() {
            for (int i = 0; i < this.eventListeners.length; ++i) {
                EventListener<E> l = this.eventListeners[i];
                if (l == null) continue;
                l.clearListeners();
            }
        }

        public synchronized void unregisterBukkitListener() {
            for (int i = 0; i < this.eventListeners.length; ++i) {
                EventListener<E> l = this.eventListeners[i];
                if (l == null) continue;
                l.unregisterBukkitListener();
                this.eventListeners[i] = null;
            }
        }
    }

    private final class EventListener<E extends Event>
    implements Listener {
        private final Map<Object, List<Consumer<E>>> map = new HashMap<Object, List<Consumer<E>>>();
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Class<E> clazz;

        public EventListener(@NotNull Class<E> clazz, EventPriority priority) {
            this.clazz = Objects.requireNonNull(clazz, "Event class is null.");
            Bukkit.getPluginManager().registerEvent(clazz, (Listener)this, priority, (listener, event) -> this.call(event), EventManager.this.plugin);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void register(@NotNull Object listener, @NotNull Consumer<E> consumer) {
            Preconditions.checkNotNull((Object)listener, (Object)"Listener is null.");
            Preconditions.checkNotNull(consumer, (Object)"Consumer is null.");
            this.readWriteLock.writeLock().lock();
            try {
                List l = this.map.computeIfAbsent(listener, k -> new LinkedList());
                l.add(consumer);
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public void unregisterListener(@NotNull Object listener) {
            this.readWriteLock.writeLock().lock();
            try {
                this.map.remove(listener);
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public void clearListeners() {
            this.readWriteLock.writeLock().lock();
            try {
                this.map.clear();
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public void unregisterBukkitListener() {
            this.readWriteLock.writeLock().lock();
            try {
                HandlerList.unregisterAll((Listener)this);
                this.map.clear();
            }
            finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void call(@NotNull Event e) {
            Preconditions.checkNotNull((Object)e, (Object)"Event cannot be null.");
            if (e.getClass() != this.clazz) {
                return;
            }
            Event ev = e;
            this.readWriteLock.readLock().lock();
            try {
                for (Map.Entry<Object, List<Consumer<E>>> l : this.map.entrySet()) {
                    Object instance = l.getKey();
                    for (Consumer<E> m : l.getValue()) {
                        try {
                            m.accept(ev);
                        }
                        catch (Throwable t) {
                            System.err.println("Event " + this.clazz.getSimpleName() + " in " + instance.getClass().getSimpleName() + " has thrown an error:");
                            t.printStackTrace();
                        }
                    }
                }
            }
            finally {
                this.readWriteLock.readLock().unlock();
            }
        }
    }
}

