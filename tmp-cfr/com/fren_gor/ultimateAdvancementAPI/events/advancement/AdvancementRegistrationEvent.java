/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events.advancement;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AdvancementRegistrationEvent
extends Event {
    private final Advancement advancement;
    private static final HandlerList handlers = new HandlerList();

    public AdvancementRegistrationEvent(@NotNull Advancement advancement) {
        this.advancement = Objects.requireNonNull(advancement, "Advancement is null.");
    }

    @NotNull
    public Advancement getAdvancement() {
        return this.advancement;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "AdvancementRegistrationEvent{advancement=" + String.valueOf(this.advancement) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AdvancementRegistrationEvent that = (AdvancementRegistrationEvent)((Object)o);
        return this.advancement.equals(that.advancement);
    }

    public int hashCode() {
        return this.advancement.hashCode();
    }
}

