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

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class AdvancementProgressionUpdateEvent
extends Event {
    private final TeamProgression team;
    private final @Range(from=0L, to=0x7FFFFFFFL) int oldProgression;
    private final @Range(from=0L, to=0x7FFFFFFFL) int newProgression;
    private final Advancement advancement;
    private static final HandlerList handlers = new HandlerList();

    public AdvancementProgressionUpdateEvent(@NotNull TeamProgression team, @Range(from=0L, to=0x7FFFFFFFL) int oldProgression, @Range(from=0L, to=0x7FFFFFFFL) int newProgression, @NotNull Advancement advancement) {
        this.team = AdvancementUtils.validateTeamProgression(team);
        this.oldProgression = AdvancementUtils.validateProgressionValue(oldProgression);
        this.newProgression = AdvancementUtils.validateProgressionValue(newProgression);
        this.advancement = Objects.requireNonNull(advancement, "Advancement is null.");
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
        return "AdvancementProgressionUpdateEvent{team=" + String.valueOf(this.team) + ", oldProgression=" + this.oldProgression + ", newProgression=" + this.newProgression + ", advancement=" + String.valueOf(this.advancement) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || ((Object)((Object)this)).getClass() != o.getClass()) {
            return false;
        }
        AdvancementProgressionUpdateEvent that = (AdvancementProgressionUpdateEvent)((Object)o);
        if (this.oldProgression != that.oldProgression) {
            return false;
        }
        if (this.newProgression != that.newProgression) {
            return false;
        }
        if (!this.team.equals(that.team)) {
            return false;
        }
        return this.advancement.equals(that.advancement);
    }

    public int hashCode() {
        int result = this.team.hashCode();
        result = 31 * result + this.oldProgression;
        result = 31 * result + this.newProgression;
        result = 31 * result + this.advancement.hashCode();
        return result;
    }
}

