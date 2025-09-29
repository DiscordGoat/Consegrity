/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.util;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AfterHandle {
    public static final AfterHandle UPDATE_ADVANCEMENTS_TO_TEAM = (progression, player, adv) -> adv.getAdvancementTab().updateAdvancementsToTeam(progression);

    public void apply(@NotNull TeamProgression var1, @Nullable Player var2, @NotNull Advancement var3);
}

