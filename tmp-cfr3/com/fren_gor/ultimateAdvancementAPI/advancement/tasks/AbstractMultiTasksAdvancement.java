/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.tasks;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class AbstractMultiTasksAdvancement
extends BaseAdvancement {
    public AbstractMultiTasksAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Advancement parent) {
        super(key, display, parent);
    }

    public AbstractMultiTasksAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Advancement parent, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(key, display, parent, maxProgression);
    }

    public void reloadTasks(@NotNull Player player) {
        this.reloadTasks(player, true);
    }

    public void reloadTasks(@NotNull UUID uuid) {
        this.reloadTasks(uuid, true);
    }

    public void reloadTasks(@NotNull Player player, boolean giveRewards) {
        this.reloadTasks(AdvancementUtils.progressionFromPlayer(player, this), player, giveRewards);
    }

    public void reloadTasks(@NotNull UUID uuid, boolean giveRewards) {
        this.reloadTasks(AdvancementUtils.progressionFromUUID(uuid, this), Bukkit.getPlayer((UUID)uuid), giveRewards);
    }

    protected abstract void reloadTasks(@NotNull TeamProgression var1, @Nullable Player var2, boolean var3);
}

