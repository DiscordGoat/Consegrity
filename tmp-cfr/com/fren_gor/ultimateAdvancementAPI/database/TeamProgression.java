/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Sets
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.IDatabase;
import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalOperationException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public final class TeamProgression {
    final AtomicBoolean inCache = new AtomicBoolean(false);
    private final int teamId;
    private final Set<UUID> players;
    private final Map<AdvancementKey, Integer> advancements;

    public TeamProgression(int teamId, @NotNull UUID member) {
        this.validateCaller(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        Preconditions.checkNotNull((Object)member, (Object)"Member is null.");
        this.advancements = new ConcurrentHashMap<AdvancementKey, Integer>();
        this.teamId = teamId;
        this.players = new HashSet<UUID>();
        this.players.add(member);
    }

    public TeamProgression(@NotNull Map<AdvancementKey, Integer> advancements, int teamId, @NotNull Collection<UUID> members) {
        this.validateCaller(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
        Preconditions.checkNotNull(advancements, (Object)"Advancements is null.");
        Preconditions.checkNotNull(members, (Object)"Members is null.");
        this.advancements = new ConcurrentHashMap<AdvancementKey, Integer>(advancements);
        this.teamId = teamId;
        this.players = Sets.newHashSetWithExpectedSize((int)(members.size() + 4));
        this.players.addAll(members);
    }

    private void validateCaller(@NotNull Class<?> caller) throws IllegalOperationException {
        if (!IDatabase.class.isAssignableFrom(caller)) {
            throw new IllegalOperationException("TeamProgression can be instantiated only by database implementations (IDatabase).");
        }
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getProgression(@NotNull Advancement advancement) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Integer progression = this.advancements.get(advancement.getKey());
        if (progression != null) {
            if (progression <= advancement.getMaxProgression()) {
                return progression;
            }
            return advancement.getMaxProgression();
        }
        return 0;
    }

    @Contract(pure=true)
    public boolean contains(@NotNull Player player) {
        return this.contains(AdvancementUtils.uuidFromPlayer(player));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Contract(pure=true, value="null -> false")
    public boolean contains(UUID uuid) {
        Set<UUID> set = this.players;
        synchronized (set) {
            return this.players.contains(uuid);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Contract(pure=true, value="-> new")
    public Set<@NotNull UUID> getMembersCopy() {
        Set<UUID> set = this.players;
        synchronized (set) {
            return new HashSet<UUID>(this.players);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Contract(pure=true)
    public @Range(from=0L, to=0x7FFFFFFFL) int getSize() {
        Set<UUID> set = this.players;
        synchronized (set) {
            return this.players.size();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void forEachMember(@NotNull Consumer<UUID> action) {
        Preconditions.checkNotNull(action, (Object)"Consumer is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            for (UUID u : this.players) {
                action.accept(u);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean everyMemberMatch(@NotNull Predicate<UUID> action) {
        Preconditions.checkNotNull(action, (Object)"Predicate is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            for (UUID u : this.players) {
                if (action.test(u)) continue;
                return false;
            }
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean anyMemberMatch(@NotNull Predicate<UUID> action) {
        Preconditions.checkNotNull(action, (Object)"Predicate is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            for (UUID u : this.players) {
                if (!action.test(u)) continue;
                return true;
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean noMemberMatch(@NotNull Predicate<UUID> action) {
        Preconditions.checkNotNull(action, (Object)"Predicate is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            for (UUID u : this.players) {
                if (!action.test(u)) continue;
                return false;
            }
            return true;
        }
    }

    public boolean isValid() {
        return this.inCache.get();
    }

    int updateProgression(@NotNull AdvancementKey key, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        AdvancementUtils.validateProgressionValue(progression);
        Integer i = this.advancements.put(key, progression);
        return i == null ? 0 : i;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void removeMember(UUID uuid) {
        Set<UUID> set = this.players;
        synchronized (set) {
            this.players.remove(uuid);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void addMember(@NotNull UUID uuid) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            this.players.add(uuid);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public UUID getAMember() {
        Set<UUID> set = this.players;
        synchronized (set) {
            return (UUID)Iterables.getFirst(this.players, null);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public Player getAnOnlineMember(@NotNull DatabaseManager manager) {
        Preconditions.checkNotNull((Object)manager, (Object)"DatabaseManager is null.");
        Set<UUID> set = this.players;
        synchronized (set) {
            for (UUID u : this.players) {
                if (!manager.isLoadedAndOnline(u)) continue;
                return Bukkit.getPlayer((UUID)u);
            }
        }
        return null;
    }

    public String toString() {
        return "TeamProgression{teamId=" + this.teamId + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        TeamProgression that = (TeamProgression)o;
        return this.teamId == that.teamId;
    }

    public int hashCode() {
        return this.teamId;
    }

    public int getTeamId() {
        return this.teamId;
    }
}

