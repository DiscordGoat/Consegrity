/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.events;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerLoadingFailedEvent
extends Event {
    private final Player player;
    @Nullable
    private final Throwable cause;
    private static final HandlerList handlers = new HandlerList();

    public PlayerLoadingFailedEvent(@NotNull Player player, @Nullable Throwable cause) {
        this.player = Objects.requireNonNull(player, "Player is null.");
        this.cause = cause;
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    @Nullable
    public Throwable getCause() {
        return this.cause;
    }

    public boolean isExceptionOccurred() {
        return this.cause != null;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        PlayerLoadingFailedEvent that = (PlayerLoadingFailedEvent)((Object)o);
        if (!this.player.equals((Object)that.player)) {
            return false;
        }
        return Objects.equals(this.cause, that.cause);
    }

    public int hashCode() {
        int result = this.player.hashCode();
        result = 31 * result + (this.cause != null ? this.cause.hashCode() : 0);
        return result;
    }
}

