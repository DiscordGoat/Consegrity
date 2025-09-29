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

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncTeamUpdateEvent
extends Event {
    private final TeamProgression team;
    private final UUID playerUUID;
    private final Action action;
    private static final HandlerList handlers = new HandlerList();

    public AsyncTeamUpdateEvent(@NotNull TeamProgression team, @NotNull UUID playerUUID, @NotNull Action action) {
        super(!Bukkit.isPrimaryThread());
        this.team = AdvancementUtils.validateTeamProgression(team);
        this.playerUUID = Objects.requireNonNull(playerUUID, "UUID is null.");
        this.action = Objects.requireNonNull(action, "Action is null.");
    }

    @NotNull
    public TeamProgression getTeamProgression() {
        return this.team;
    }

    @NotNull
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    @NotNull
    public Action getAction() {
        return this.action;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public String toString() {
        return "AsyncTeamUpdateEvent{team=" + String.valueOf(this.team) + ", playerUUID=" + String.valueOf(this.playerUUID) + ", action=" + String.valueOf((Object)this.action) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AsyncTeamUpdateEvent that = (AsyncTeamUpdateEvent)((Object)o);
        if (!this.team.equals(that.team)) {
            return false;
        }
        if (!this.playerUUID.equals(that.playerUUID)) {
            return false;
        }
        return this.action == that.action;
    }

    public int hashCode() {
        int result = this.team.hashCode();
        result = 31 * result + this.playerUUID.hashCode();
        result = 31 * result + this.action.hashCode();
        return result;
    }

    public static enum Action {
        JOIN,
        LEAVE;

    }
}

