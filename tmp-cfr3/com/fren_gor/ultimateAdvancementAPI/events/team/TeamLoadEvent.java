/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.events.team;

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Deprecated(since="2.2.0", forRemoval=true)
public class TeamLoadEvent
extends Event {
    private final TeamProgression team;
    private static final HandlerList handlers = new HandlerList();

    public TeamLoadEvent(@NotNull TeamProgression team) {
        this.team = AdvancementUtils.validateTeamProgression(team);
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
        return "TeamLoadEvent{team=" + String.valueOf(this.team) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        TeamLoadEvent that = (TeamLoadEvent)((Object)o);
        return this.team.equals(that.team);
    }

    public int hashCode() {
        return this.team.hashCode();
    }
}

