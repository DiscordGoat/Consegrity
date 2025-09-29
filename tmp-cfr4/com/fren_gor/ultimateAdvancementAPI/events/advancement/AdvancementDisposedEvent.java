/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events.advancement;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AdvancementDisposedEvent
extends Event {
    private final AdvancementKey advancementKey;
    private static final HandlerList handlers = new HandlerList();

    public AdvancementDisposedEvent(@NotNull AdvancementKey advancementKey) {
        this.advancementKey = Objects.requireNonNull(advancementKey, "AdvancementKey is null.");
    }

    @NotNull
    public AdvancementKey getAdvancementKey() {
        return this.advancementKey;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "AdvancementDisposedEvent{advancementKey=" + String.valueOf(this.advancementKey) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AdvancementDisposedEvent that = (AdvancementDisposedEvent)((Object)o);
        return this.advancementKey.equals(that.advancementKey);
    }

    public int hashCode() {
        return this.advancementKey.hashCode();
    }
}

