/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events.team;

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.google.common.base.Preconditions;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncTeamUnloadEvent
extends Event {
    private final TeamProgression team;
    private static final HandlerList handlers = new HandlerList();

    public AsyncTeamUnloadEvent(@NotNull TeamProgression team) {
        super(!Bukkit.isPrimaryThread());
        Preconditions.checkArgument((!Objects.requireNonNull(team, "TeamProgression is null.").isValid() ? 1 : 0) != 0, (Object)"TeamProgression is valid.");
        this.team = team;
    }

    @NotNull
    public TeamProgression getTeamProgression() {
        return this.team;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "AsyncTeamUnloadEvent{team=" + String.valueOf(this.team) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AsyncTeamUnloadEvent that = (AsyncTeamUnloadEvent)((Object)o);
        return this.team.equals(that.team);
    }

    public int hashCode() {
        return this.team.hashCode();
    }
}

