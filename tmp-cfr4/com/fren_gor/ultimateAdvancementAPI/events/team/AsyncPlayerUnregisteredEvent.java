/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events.team;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncPlayerUnregisteredEvent
extends Event {
    private final UUID uuid;
    private static final HandlerList handlers = new HandlerList();

    public AsyncPlayerUnregisteredEvent(@NotNull UUID uuid) {
        super(!Bukkit.isPrimaryThread());
        this.uuid = Objects.requireNonNull(uuid, "UUID is null.");
    }

    @NotNull
    public UUID getPlayerUUID() {
        return this.uuid;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "AsyncPlayerUnregisteredEvent{uuid=" + String.valueOf(this.uuid) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AsyncPlayerUnregisteredEvent that = (AsyncPlayerUnregisteredEvent)((Object)o);
        return this.uuid.equals(that.uuid);
    }

    public int hashCode() {
        return this.uuid.hashCode();
    }
}

