/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.chat.BaseComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.tasks;

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.tasks.AbstractMultiTasksAdvancement;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.AdvancementProgressionUpdateEvent;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class TaskAdvancement
extends BaseAdvancement {
    public TaskAdvancement(@NotNull String key, @NotNull AbstractMultiTasksAdvancement multitask) {
        this(key, multitask, 1);
    }

    public TaskAdvancement(@NotNull String key, @NotNull AbstractMultiTasksAdvancement multitask, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        this(key, new AdvancementDisplay.Builder(Material.GRASS_BLOCK, Objects.requireNonNull(key, "Key is null.")).build(), multitask, maxProgression);
    }

    public TaskAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull AbstractMultiTasksAdvancement multitask) {
        this(key, display, multitask, 1);
    }

    public TaskAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull AbstractMultiTasksAdvancement multitask, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(key, display, Objects.requireNonNull(multitask, "AbstractMultiTasksAdvancement is null."), maxProgression);
    }

    @Override
    @Nullable
    @Contract(pure=true, value="_ -> null")
    public final BaseComponent[] getAnnounceMessage(@NotNull Player player) {
        return null;
    }

    @Override
    protected void setProgression(@NotNull TeamProgression pro, @Nullable Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveRewards) {
        AdvancementUtils.validateTeamProgression(pro);
        AdvancementUtils.validateProgressionValueStrict(progression, this.maxProgression);
        DatabaseManager ds = this.advancementTab.getDatabaseManager();
        int old = ds.updateProgression(this.key, pro, progression);
        try {
            Bukkit.getPluginManager().callEvent((Event)new AdvancementProgressionUpdateEvent(pro, old, progression, this));
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        this.handlePlayer(pro, player, progression, old, giveRewards, null);
        this.getMultiTasksAdvancement().reloadTasks(pro, player, giveRewards);
    }

    @Override
    @Contract(value="_ -> false")
    public final boolean isVisible(@NotNull Player player) {
        return false;
    }

    @Override
    @Contract(value="_ -> false")
    public final boolean isVisible(@NotNull UUID uuid) {
        return false;
    }

    @Override
    @Contract(value="_ -> false")
    public final boolean isVisible(@NotNull TeamProgression progression) {
        return false;
    }

    @Override
    public void onGrant(@NotNull Player player, boolean giveRewards) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        if (giveRewards) {
            this.giveReward(player);
        }
    }

    @Override
    public boolean isValid() {
        return this.getMultiTasksAdvancement().isValid();
    }

    @NotNull
    public AbstractMultiTasksAdvancement getMultiTasksAdvancement() {
        return (AbstractMultiTasksAdvancement)this.parent;
    }

    @Override
    public void validateRegister() throws InvalidAdvancementException {
        throw new InvalidAdvancementException("TaskAdvancements cannot be registered in any AdvancementTab.");
    }

    @Override
    @NotNull
    public final AdvancementWrapper getNMSWrapper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void displayToastToPlayer(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onUpdate(@NotNull TeamProgression teamProgression, @NotNull Map<AdvancementWrapper, Integer> addedAdvancements) {
        throw new UnsupportedOperationException();
    }
}

