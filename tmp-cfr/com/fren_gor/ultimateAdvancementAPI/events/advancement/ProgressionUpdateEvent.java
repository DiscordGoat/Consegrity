/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.events.advancement;

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class ProgressionUpdateEvent
extends Event {
    private final TeamProgression team;
    private final @Range(from=0L, to=0x7FFFFFFFL) int oldProgression;
    private final @Range(from=0L, to=0x7FFFFFFFL) int newProgression;
    private final AdvancementKey advancementKey;
    private static final HandlerList handlers = new HandlerList();

    public ProgressionUpdateEvent(@NotNull TeamProgression team, @Range(from=0L, to=0x7FFFFFFFL) int oldProgression, @Range(from=0L, to=0x7FFFFFFFL) int newProgression, @NotNull AdvancementKey advancementKey) {
        this.team = AdvancementUtils.validateTeamProgression(team);
        this.oldProgression = AdvancementUtils.validateProgressionValue(oldProgression);
        this.newProgression = AdvancementUtils.validateProgressionValue(newProgression);
        this.advancementKey = Objects.requireNonNull(advancementKey, "AdvancementKey is null.");
    }

    public TeamProgression getTeamProgression() {
        return this.team;
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getOldProgression() {
        return this.oldProgression;
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getNewProgression() {
        return this.newProgression;
    }

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
        return "ProgressionUpdateEvent{team=" + String.valueOf(this.team) + ", oldProgression=" + this.oldProgression + ", newProgression=" + this.newProgression + ", advancementKey=" + String.valueOf(this.advancementKey) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        ProgressionUpdateEvent that = (ProgressionUpdateEvent)((Object)o);
        if (this.oldProgression != that.oldProgression) {
            return false;
        }
        if (this.newProgression != that.newProgression) {
            return false;
        }
        if (!this.team.equals(that.team)) {
            return false;
        }
        return this.advancementKey.equals(that.advancementKey);
    }

    public int hashCode() {
        int result = this.team.hashCode();
        result = 31 * result + this.oldProgression;
        result = 31 * result + this.newProgression;
        result = 31 * result + this.advancementKey.hashCode();
        return result;
    }
}

