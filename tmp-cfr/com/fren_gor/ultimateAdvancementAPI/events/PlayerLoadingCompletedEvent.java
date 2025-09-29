/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events;

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerLoadingCompletedEvent
extends Event {
    private final Player player;
    private final TeamProgression progression;
    private static final HandlerList handlers = new HandlerList();

    public PlayerLoadingCompletedEvent(@NotNull Player player, @NotNull TeamProgression progression) {
        this.player = Objects.requireNonNull(player, "Player is null.");
        this.progression = AdvancementUtils.validateTeamProgression(progression);
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    @NotNull
    public TeamProgression getTeamProgression() {
        return this.progression;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "PlayerLoadingCompletedEvent{player=" + String.valueOf(this.player) + ", progression=" + String.valueOf(this.progression) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        PlayerLoadingCompletedEvent that = (PlayerLoadingCompletedEvent)((Object)o);
        if (!this.player.equals((Object)that.player)) {
            return false;
        }
        return this.progression.equals(that.progression);
    }

    public int hashCode() {
        int result = this.player.hashCode();
        result = 31 * result + this.progression.hashCode();
        return result;
    }
}

