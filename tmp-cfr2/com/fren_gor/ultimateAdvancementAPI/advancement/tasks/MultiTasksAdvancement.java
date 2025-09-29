/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Sets
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 *  org.jetbrains.annotations.UnmodifiableView
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.tasks;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.tasks.AbstractMultiTasksAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.tasks.TaskAdvancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.events.team.TeamUnloadEvent;
import com.fren_gor.ultimateAdvancementAPI.exceptions.ArbitraryMultiTaskProgressionUpdateException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.fren_gor.ultimateAdvancementAPI.util.AfterHandle;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

public class MultiTasksAdvancement
extends AbstractMultiTasksAdvancement {
    protected boolean ENABLE_ARBITRARY_SET_PROGRESSION = false;
    protected boolean DISABLE_EXCEPTION_ON_ARBITRARY_SET_PROGRESSION = false;
    protected final Set<TaskAdvancement> tasks = new HashSet<TaskAdvancement>();
    protected final Map<Integer, Integer> progressionsCache = new HashMap<Integer, Integer>();
    private boolean initialised = false;
    private boolean doReloads = true;

    public MultiTasksAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Advancement parent, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(key, display, parent, maxProgression);
    }

    public void registerTasks(TaskAdvancement ... tasks) {
        this.registerTasks(Sets.newHashSet((Object[])Objects.requireNonNull(tasks)));
    }

    public void registerTasks(@NotNull Set<TaskAdvancement> tasks) {
        if (this.initialised) {
            throw new IllegalStateException("MultiTaskAdvancement is already initialised.");
        }
        Preconditions.checkNotNull(tasks, (Object)"Set<TaskAdvancement> is null.");
        int progression = 0;
        for (TaskAdvancement t : tasks) {
            if (t == null) {
                throw new NullPointerException("A TaskAdvancement is null.");
            }
            if (t.getMultiTasksAdvancement() != this) {
                throw new IllegalArgumentException("TaskAdvancement's AbstractMultiTasksAdvancement (" + String.valueOf(t.getMultiTasksAdvancement().getKey()) + ") doesn't match with this MultiTasksAdvancement (" + String.valueOf(this.key) + ").");
            }
            progression += t.getMaxProgression();
        }
        if (progression != this.maxProgression) {
            throw new IllegalArgumentException("Expected max progression (" + this.maxProgression + ") doesn't match the tasks' total one (" + progression + ").");
        }
        this.tasks.addAll(tasks);
        this.registerEvent(TeamUnloadEvent.class, e -> this.progressionsCache.remove(e.getTeamProgression().getTeamId()));
        this.initialised = true;
    }

    @NotNull
    public @NotNull @UnmodifiableView Set<@NotNull TaskAdvancement> getTasks() {
        this.checkInitialisation();
        return Collections.unmodifiableSet(this.tasks);
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int getProgression(@NotNull TeamProgression progression) {
        this.checkInitialisation();
        AdvancementUtils.validateTeamProgression(progression);
        Integer progr = this.progressionsCache.get(progression.getTeamId());
        if (progr == null) {
            int c = 0;
            for (TaskAdvancement t : this.tasks) {
                c += progression.getProgression(t);
            }
            this.progressionsCache.put(progression.getTeamId(), c);
            return c;
        }
        return progr;
    }

    @Override
    public boolean isGranted(@NotNull TeamProgression pro) {
        this.checkInitialisation();
        AdvancementUtils.validateTeamProgression(pro);
        return this.getProgression(pro) >= this.maxProgression;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void setProgression(@NotNull TeamProgression progression, @Nullable Player player, @Range(from=0L, to=0x7FFFFFFFL) int newProgression, boolean giveRewards) throws ArbitraryMultiTaskProgressionUpdateException {
        int current;
        block16: {
            this.checkInitialisation();
            AdvancementUtils.validateTeamProgression(progression);
            AdvancementUtils.validateProgressionValueStrict(newProgression, this.maxProgression);
            current = this.getProgression(progression);
            if (current == newProgression) {
                return;
            }
            this.doReloads = false;
            try {
                if (newProgression == this.maxProgression) {
                    for (TaskAdvancement t : this.tasks) {
                        t.setProgression(progression, player, t.getMaxProgression(), giveRewards);
                    }
                    break block16;
                }
                if (newProgression == 0) {
                    for (TaskAdvancement t : this.tasks) {
                        t.setProgression(progression, player, 0, giveRewards);
                    }
                    break block16;
                }
                if (this.ENABLE_ARBITRARY_SET_PROGRESSION) {
                    if (newProgression < current) {
                        for (TaskAdvancement t : this.tasks) {
                            int tc = t.getProgression(progression);
                            if (current - tc > newProgression) {
                                t.setProgression(progression, player, 0, false);
                                continue;
                            }
                            if (current - tc > newProgression) continue;
                            t.setProgression(progression, player, tc + newProgression - current, false);
                            break block16;
                        }
                        break block16;
                    }
                    for (TaskAdvancement t : this.tasks) {
                        int ta = t.getProgression(progression);
                        int tc = t.getMaxProgression() - ta;
                        if (current + tc < newProgression) {
                            t.setProgression(progression, player, t.getMaxProgression(), giveRewards);
                            continue;
                        }
                        if (current + tc < newProgression) continue;
                        t.setProgression(progression, player, ta + newProgression - current, giveRewards);
                        break block16;
                    }
                    break block16;
                }
                if (this.DISABLE_EXCEPTION_ON_ARBITRARY_SET_PROGRESSION) {
                    return;
                }
                throw new ArbitraryMultiTaskProgressionUpdateException();
            }
            finally {
                this.doReloads = true;
            }
        }
        this.updateProgressionCache(progression, newProgression);
        this.handlePlayer(progression, player, newProgression, current, giveRewards, AfterHandle.UPDATE_ADVANCEMENTS_TO_TEAM);
    }

    @Override
    protected void reloadTasks(@NotNull TeamProgression progression, @Nullable Player player, boolean giveRewards) {
        this.checkInitialisation();
        if (this.doReloads) {
            AdvancementUtils.validateTeamProgression(progression);
            int current = this.getProgression(progression);
            this.resetProgressionCache(progression);
            this.handlePlayer(progression, player, this.getProgression(progression), current, giveRewards, AfterHandle.UPDATE_ADVANCEMENTS_TO_TEAM);
        }
    }

    public void resetProgressionCache() {
        this.progressionsCache.clear();
    }

    public void resetProgressionCache(@NotNull TeamProgression pro) {
        AdvancementUtils.validateTeamProgression(pro);
        this.progressionsCache.remove(pro.getTeamId());
    }

    protected void updateProgressionCache(@NotNull TeamProgression pro, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        AdvancementUtils.validateTeamProgression(pro);
        AdvancementUtils.validateProgressionValueStrict(progression, this.maxProgression);
        this.progressionsCache.put(pro.getTeamId(), progression);
    }

    private void checkInitialisation() {
        if (!this.initialised) {
            throw new IllegalStateException("MultiTasksAdvancement hasn't been initialised yet.");
        }
    }

    @Override
    public void validateRegister() throws InvalidAdvancementException {
        if (!this.initialised) {
            throw new InvalidAdvancementException("MultiTasksAdvancement hasn't been initialised yet.");
        }
    }

    @Override
    public void onDispose() {
        this.checkInitialisation();
        for (TaskAdvancement t : this.tasks) {
            t.onDispose();
        }
        super.onDispose();
    }

    public boolean isInitialised() {
        return this.initialised;
    }
}

