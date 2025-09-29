/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.CacheFreeingOption;
import com.fren_gor.ultimateAdvancementAPI.database.IDatabase;
import com.fren_gor.ultimateAdvancementAPI.database.ObjectResult;
import com.fren_gor.ultimateAdvancementAPI.database.Result;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.database.impl.InMemory;
import com.fren_gor.ultimateAdvancementAPI.database.impl.MySQL;
import com.fren_gor.ultimateAdvancementAPI.database.impl.SQLite;
import com.fren_gor.ultimateAdvancementAPI.events.EventManager;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingFailedEvent;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.ProgressionUpdateEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.AsyncPlayerUnregisteredEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.AsyncTeamLoadEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.AsyncTeamUnloadEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.AsyncTeamUpdateEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.TeamLoadEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.TeamUnloadEvent;
import com.fren_gor.ultimateAdvancementAPI.events.team.TeamUpdateEvent;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotLoadedException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import java.io.File;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public final class DatabaseManager {
    public static final int MAX_SIMULTANEOUS_LOADING_REQUESTS = 65535;
    private static final int LOAD_EVENTS_DELAY = 3;
    private final AdvancementMain main;
    private final Map<UUID, TeamProgression> progressionCache = new HashMap<UUID, TeamProgression>();
    private final Map<UUID, TempUserMetadata> tempLoaded = new HashMap<UUID, TempUserMetadata>();
    private final EventManager eventManager;
    private final IDatabase database;
    private final Map<UUID, Runnable> waitingForJoinEvent = Collections.synchronizedMap(new HashMap());
    private static final Runnable LOGIN_SENTINEL = () -> {};
    private static final Runnable JOIN_SENTINEL = () -> {};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void registerForJoinEvent(Player player, Runnable runnable) {
        UUID uuid = player.getUniqueId();
        Map<UUID, Runnable> map = this.waitingForJoinEvent;
        synchronized (map) {
            Runnable run = this.waitingForJoinEvent.remove(uuid);
            if (run == null) {
                return;
            }
            if (run == LOGIN_SENTINEL) {
                this.waitingForJoinEvent.put(uuid, runnable);
                return;
            }
        }
        AdvancementUtils.runSync(this.main, 3L, runnable);
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public DatabaseManager(@NotNull AdvancementMain main) throws Exception {
        this(main, new InMemory(main));
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public DatabaseManager(@NotNull AdvancementMain main, @NotNull File dbFile) throws Exception {
        this(main, new SQLite(main, dbFile));
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public DatabaseManager(@NotNull AdvancementMain main, @NotNull String username, @NotNull String password, @NotNull String databaseName, @NotNull String host, @Range(from=1L, to=0x7FFFFFFFL) int port, @Range(from=1L, to=0x7FFFFFFFL) int poolSize, @Range(from=250L, to=0x7FFFFFFFFFFFFFFFL) long connectionTimeout) throws Exception {
        this(main, new MySQL(main, username, password, databaseName, host, port, poolSize, connectionTimeout));
    }

    @ApiStatus.Internal
    public DatabaseManager(@NotNull AdvancementMain main, @NotNull IDatabase database) throws Exception {
        Preconditions.checkNotNull((Object)main, (Object)"AdvancementMain is null.");
        Preconditions.checkNotNull((Object)database, (Object)"Database is null.");
        this.main = main;
        this.eventManager = main.getEventManager();
        this.database = database;
        this.commonSetUp();
    }

    private void commonSetUp() throws SQLException {
        this.database.setUp();
        this.eventManager.register(this, PlayerLoginEvent.class, EventPriority.LOWEST, e -> CompletableFuture.runAsync(() -> {
            this.waitingForJoinEvent.put(e.getPlayer().getUniqueId(), LOGIN_SENTINEL);
            try {
                this.loadPlayerMainFunction(e.getPlayer());
            }
            catch (Exception ex) {
                System.err.println("Cannot load player " + e.getPlayer().getName() + ":");
                ex.printStackTrace();
                this.registerForJoinEvent(e.getPlayer(), () -> Bukkit.getPluginManager().callEvent((Event)new PlayerLoadingFailedEvent(e.getPlayer(), ex)));
            }
        }));
        this.eventManager.register(this, PlayerJoinEvent.class, EventPriority.MONITOR, e -> {
            Runnable runnable;
            Map<UUID, Runnable> map = this.waitingForJoinEvent;
            synchronized (map) {
                runnable = this.waitingForJoinEvent.remove(e.getPlayer().getUniqueId());
                if (runnable == null) {
                    return;
                }
                if (runnable == LOGIN_SENTINEL || runnable == JOIN_SENTINEL) {
                    this.waitingForJoinEvent.put(e.getPlayer().getUniqueId(), JOIN_SENTINEL);
                    return;
                }
            }
            AdvancementUtils.runSync(this.main, 3L, runnable);
        });
        this.eventManager.register(this, PlayerQuitEvent.class, EventPriority.MONITOR, e -> {
            this.waitingForJoinEvent.remove(e.getPlayer().getUniqueId());
            DatabaseManager databaseManager = this;
            synchronized (databaseManager) {
                TempUserMetadata meta = this.tempLoaded.get(e.getPlayer().getUniqueId());
                if (meta != null) {
                    meta.isOnline = false;
                } else {
                    TeamProgression t = this.progressionCache.remove(e.getPlayer().getUniqueId());
                    if (t != null) {
                        if (t.noMemberMatch(this.progressionCache::containsKey)) {
                            t.inCache.set(false);
                            DatabaseManager.callEventCatchingExceptions(new AsyncTeamUnloadEvent(t));
                            Bukkit.getPluginManager().callEvent((Event)new TeamUnloadEvent(t));
                        }
                    }
                }
            }
        });
        this.eventManager.register(this, PluginDisableEvent.class, EventPriority.HIGHEST, e -> {
            DatabaseManager databaseManager = this;
            synchronized (databaseManager) {
                LinkedList<UUID> list = new LinkedList<UUID>();
                for (Map.Entry<UUID, TempUserMetadata> en : this.tempLoaded.entrySet()) {
                    if (en.getValue().pluginRequests.remove(e.getPlugin()) == null) continue;
                    list.add(en.getKey());
                }
                for (UUID u : list) {
                    this.unloadOfflinePlayer(u, e.getPlugin());
                }
            }
        });
        CompletableFuture.runAsync(() -> {
            try {
                this.database.clearUpTeams();
            }
            catch (SQLException e) {
                System.err.println("Cannot clear up unused team ids:");
                e.printStackTrace();
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void unregister() {
        if (this.eventManager.isEnabled()) {
            this.eventManager.unregister(this);
        }
        try {
            this.database.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        DatabaseManager databaseManager = this;
        synchronized (databaseManager) {
            this.tempLoaded.clear();
            this.progressionCache.forEach((u, t) -> t.inCache.set(false));
            this.progressionCache.clear();
        }
    }

    private void loadPlayerMainFunction(@NotNull Player player) throws SQLException {
        Map.Entry<TeamProgression, Boolean> entry = this.loadOrRegisterPlayer(player);
        TeamProgression pro = entry.getKey();
        this.registerForJoinEvent(player, () -> {
            Bukkit.getPluginManager().callEvent((Event)new PlayerLoadingCompletedEvent(player, pro));
            if (((Boolean)entry.getValue()).booleanValue()) {
                DatabaseManager.callEventCatchingExceptions(new AsyncTeamUpdateEvent(pro, player.getUniqueId(), AsyncTeamUpdateEvent.Action.JOIN));
                DatabaseManager.callEventCatchingExceptions(new TeamUpdateEvent(pro, player.getUniqueId(), TeamUpdateEvent.Action.JOIN));
            }
            this.main.updatePlayer(player);
            CompletableFuture.runAsync(() -> this.processUnredeemed(player, pro));
        });
    }

    @NotNull
    private synchronized Map.Entry<TeamProgression, Boolean> loadOrRegisterPlayer(@NotNull Player player) throws SQLException {
        UUID uuid = player.getUniqueId();
        TeamProgression pro = this.progressionCache.get(uuid);
        if (pro != null) {
            TempUserMetadata meta = this.tempLoaded.get(uuid);
            if (meta != null) {
                meta.isOnline = true;
            }
            return new AbstractMap.SimpleEntry<TeamProgression, Boolean>(pro, false);
        }
        pro = this.searchTeamProgressionDeeply(uuid);
        if (pro != null) {
            this.progressionCache.put(uuid, pro);
            this.updatePlayerName(player);
            return new AbstractMap.SimpleEntry<TeamProgression, Boolean>(pro, false);
        }
        Map.Entry<TeamProgression, Boolean> e = this.database.loadOrRegisterPlayer(uuid, player.getName());
        this.updatePlayerName(player);
        e.getKey().inCache.set(true);
        this.progressionCache.put(uuid, e.getKey());
        DatabaseManager.callEventCatchingExceptions(new AsyncTeamLoadEvent(e.getKey()));
        AdvancementUtils.runSync(this.main, () -> Bukkit.getPluginManager().callEvent((Event)new TeamLoadEvent((TeamProgression)e.getKey())));
        return e;
    }

    @Nullable
    private synchronized TeamProgression searchTeamProgressionDeeply(@NotNull UUID uuid) {
        for (TeamProgression progression : this.progressionCache.values()) {
            if (!progression.contains(uuid)) continue;
            return progression;
        }
        return null;
    }

    private void processUnredeemed(@NotNull Player player, @NotNull TeamProgression pro) {
        List<Map.Entry<AdvancementKey, Boolean>> list;
        try {
            list = this.database.getUnredeemed(pro.getTeamId());
        }
        catch (SQLException e) {
            System.err.println("Cannot fetch unredeemed advancements:");
            e.printStackTrace();
            return;
        }
        if (list.size() != 0) {
            AdvancementUtils.runSync(this.main, () -> {
                Iterator it = list.iterator();
                LinkedList<AbstractMap.SimpleEntry<Advancement, Boolean>> advs = new LinkedList<AbstractMap.SimpleEntry<Advancement, Boolean>>();
                while (it.hasNext()) {
                    Map.Entry k = (Map.Entry)it.next();
                    Advancement a = this.main.getAdvancement((AdvancementKey)k.getKey());
                    if (a == null || !a.getAdvancementTab().isShownTo(player)) {
                        it.remove();
                        continue;
                    }
                    advs.add(new AbstractMap.SimpleEntry<Advancement, Boolean>(a, (Boolean)k.getValue()));
                }
                if (advs.size() != 0) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            this.database.unsetUnredeemed(list, pro.getTeamId());
                        }
                        catch (SQLException e) {
                            System.err.println("Cannot unset unredeemed advancements:");
                            e.printStackTrace();
                            return;
                        }
                        AdvancementUtils.runSync(this.main, () -> {
                            for (Map.Entry e : advs) {
                                ((Advancement)e.getKey()).onGrant(player, (Boolean)e.getValue());
                            }
                        });
                    });
                }
            });
        }
    }

    @NotNull
    public CompletableFuture<Result> updatePlayerName(@NotNull Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"Player cannot be null.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.database.updatePlayerName(player.getUniqueId(), player.getName());
            }
            catch (SQLException e) {
                System.err.println("Cannot update player " + player.getName() + " name:");
                e.printStackTrace();
                return new Result(e);
            }
            catch (Exception e) {
                return new Result(e);
            }
            return Result.SUCCESSFUL;
        });
    }

    @NotNull
    public CompletableFuture<Result> updatePlayerTeam(@NotNull Player playerToMove, @NotNull Player otherTeamMember) throws UserNotLoadedException {
        return this.updatePlayerTeam(playerToMove, this.getTeamProgression(otherTeamMember));
    }

    @NotNull
    public CompletableFuture<Result> updatePlayerTeam(@NotNull UUID playerToMove, @NotNull UUID otherTeamMember) throws UserNotLoadedException {
        return this.updatePlayerTeam(playerToMove, Bukkit.getPlayer((UUID)playerToMove), this.getTeamProgression(otherTeamMember));
    }

    @NotNull
    public CompletableFuture<Result> updatePlayerTeam(@NotNull Player playerToMove, @NotNull TeamProgression otherTeamProgression) throws UserNotLoadedException {
        return this.updatePlayerTeam(AdvancementUtils.uuidFromPlayer(playerToMove), playerToMove, otherTeamProgression);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @NotNull
    private CompletableFuture<Result> updatePlayerTeam(@NotNull UUID playerToMove, @Nullable Player ptm, @NotNull TeamProgression otherTeamProgression) throws UserNotLoadedException {
        Preconditions.checkNotNull((Object)playerToMove, (Object)"Player to move is null.");
        AdvancementUtils.validateTeamProgression(otherTeamProgression);
        DatabaseManager databaseManager = this;
        synchronized (databaseManager) {
            if (!this.progressionCache.containsKey(playerToMove)) {
                throw new UserNotLoadedException(playerToMove);
            }
        }
        if (otherTeamProgression.contains(playerToMove)) {
            return CompletableFuture.completedFuture(Result.SUCCESSFUL);
        }
        return CompletableFuture.supplyAsync(() -> {
            boolean teamUnloaded;
            TeamProgression pro;
            try {
                this.database.movePlayer(playerToMove, otherTeamProgression.getTeamId());
            }
            catch (SQLException e) {
                System.err.println("Cannot move player " + String.valueOf(ptm == null ? playerToMove : ptm) + " into team " + otherTeamProgression.getTeamId());
                e.printStackTrace();
                return new Result(e);
            }
            catch (Exception e) {
                return new Result(e);
            }
            DatabaseManager databaseManager = this;
            synchronized (databaseManager) {
                pro = this.progressionCache.get(playerToMove);
                if (pro != null) {
                    DatabaseManager.callEventCatchingExceptions(new AsyncTeamUpdateEvent(pro, playerToMove, AsyncTeamUpdateEvent.Action.LEAVE));
                }
                otherTeamProgression.addMember(playerToMove);
                this.progressionCache.put(playerToMove, otherTeamProgression);
                if (pro != null) {
                    pro.removeMember(playerToMove);
                    teamUnloaded = pro.noMemberMatch(this.progressionCache::containsKey);
                    if (teamUnloaded) {
                        pro.inCache.set(false);
                        DatabaseManager.callEventCatchingExceptions(new AsyncTeamUnloadEvent(pro));
                    }
                } else {
                    teamUnloaded = false;
                }
                DatabaseManager.callEventCatchingExceptions(new AsyncTeamUpdateEvent(otherTeamProgression, playerToMove, AsyncTeamUpdateEvent.Action.JOIN));
            }
            AdvancementUtils.runSync(this.main, () -> {
                if (pro != null) {
                    Bukkit.getPluginManager().callEvent((Event)new TeamUpdateEvent(pro, playerToMove, TeamUpdateEvent.Action.LEAVE));
                }
                if (teamUnloaded) {
                    Bukkit.getPluginManager().callEvent((Event)new TeamUnloadEvent(pro));
                }
                Bukkit.getPluginManager().callEvent((Event)new TeamUpdateEvent(otherTeamProgression, playerToMove, TeamUpdateEvent.Action.JOIN));
                if (ptm != null) {
                    this.main.updatePlayer(ptm);
                }
            });
            if (ptm != null) {
                this.processUnredeemed(ptm, otherTeamProgression);
            }
            return Result.SUCCESSFUL;
        });
    }

    public CompletableFuture<ObjectResult<@NotNull TeamProgression>> movePlayerInNewTeam(@NotNull Player player) throws UserNotLoadedException {
        return this.movePlayerInNewTeam(AdvancementUtils.uuidFromPlayer(player), player);
    }

    public CompletableFuture<ObjectResult<@NotNull TeamProgression>> movePlayerInNewTeam(@NotNull UUID uuid) throws UserNotLoadedException {
        return this.movePlayerInNewTeam(uuid, Bukkit.getPlayer((UUID)uuid));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private CompletableFuture<ObjectResult<@NotNull TeamProgression>> movePlayerInNewTeam(@NotNull UUID uuid, @Nullable Player ptr) throws UserNotLoadedException {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        DatabaseManager databaseManager = this;
        synchronized (databaseManager) {
            if (!this.progressionCache.containsKey(uuid)) {
                throw new UserNotLoadedException(uuid);
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            boolean teamUnloaded;
            TeamProgression pro;
            TeamProgression newPro;
            try {
                newPro = this.database.movePlayerInNewTeam(uuid);
            }
            catch (SQLException e) {
                System.err.println("Cannot remove player " + String.valueOf(ptr == null ? uuid : ptr.getName()) + " from their team:");
                e.printStackTrace();
                return new ObjectResult(e);
            }
            catch (Exception e) {
                return new ObjectResult(e);
            }
            DatabaseManager databaseManager = this;
            synchronized (databaseManager) {
                pro = this.progressionCache.get(uuid);
                if (pro != null) {
                    DatabaseManager.callEventCatchingExceptions(new AsyncTeamUpdateEvent(pro, uuid, AsyncTeamUpdateEvent.Action.LEAVE));
                }
                newPro.inCache.set(true);
                this.progressionCache.put(uuid, newPro);
                if (pro != null) {
                    pro.removeMember(uuid);
                    teamUnloaded = pro.noMemberMatch(this.progressionCache::containsKey);
                    if (teamUnloaded) {
                        pro.inCache.set(false);
                        DatabaseManager.callEventCatchingExceptions(new AsyncTeamUnloadEvent(pro));
                    }
                } else {
                    teamUnloaded = false;
                }
                DatabaseManager.callEventCatchingExceptions(new AsyncTeamLoadEvent(newPro));
                DatabaseManager.callEventCatchingExceptions(new AsyncTeamUpdateEvent(newPro, uuid, AsyncTeamUpdateEvent.Action.JOIN));
            }
            AdvancementUtils.runSync(this.main, () -> {
                if (pro != null) {
                    Bukkit.getPluginManager().callEvent((Event)new TeamUpdateEvent(pro, uuid, TeamUpdateEvent.Action.LEAVE));
                }
                if (teamUnloaded) {
                    Bukkit.getPluginManager().callEvent((Event)new TeamUnloadEvent(pro));
                }
                Bukkit.getPluginManager().callEvent((Event)new TeamLoadEvent(newPro));
                Bukkit.getPluginManager().callEvent((Event)new TeamUpdateEvent(newPro, uuid, TeamUpdateEvent.Action.JOIN));
                if (ptr != null) {
                    this.main.updatePlayer(ptr);
                }
            });
            return new ObjectResult<TeamProgression>(newPro);
        });
    }

    public CompletableFuture<Result> unregisterOfflinePlayer(@NotNull OfflinePlayer player) throws IllegalStateException {
        return this.unregisterOfflinePlayer(AdvancementUtils.uuidFromPlayer(player));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public CompletableFuture<Result> unregisterOfflinePlayer(@NotNull UUID uuid) throws IllegalStateException {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        AdvancementUtils.checkSync();
        if (Bukkit.getPlayer((UUID)uuid) != null) {
            throw new IllegalStateException("Player is online.");
        }
        DatabaseManager databaseManager = this;
        synchronized (databaseManager) {
            if (this.tempLoaded.containsKey(uuid)) {
                throw new IllegalStateException("Player is temporary loaded.");
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.database.unregisterPlayer(uuid);
            }
            catch (SQLException e) {
                System.err.println("Cannot unregister player " + String.valueOf(uuid) + ":");
                e.printStackTrace();
                return new Result(e);
            }
            catch (Exception e) {
                return new Result(e);
            }
            DatabaseManager.callEventCatchingExceptions(new AsyncPlayerUnregisteredEvent(uuid));
            return Result.SUCCESSFUL;
        });
    }

    public int updateProgression(@NotNull AdvancementKey key, @NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) throws UserNotLoadedException {
        return this.updateProgression(key, AdvancementUtils.uuidFromPlayer(player), newProgression);
    }

    public int updateProgression(@NotNull AdvancementKey key, @NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) throws UserNotLoadedException {
        return this.updateProgression(key, this.getTeamProgression(uuid), newProgression);
    }

    public int updateProgression(@NotNull AdvancementKey key, @NotNull TeamProgression progression, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) {
        return this.updateProgressionWithCompletable(key, progression, newProgression).getKey();
    }

    @NotNull
    public Map.Entry<Integer, CompletableFuture<Result>> updateProgressionWithCompletable(@NotNull AdvancementKey key, @NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) throws UserNotLoadedException {
        return this.updateProgressionWithCompletable(key, AdvancementUtils.uuidFromPlayer(player), newProgression);
    }

    @NotNull
    public Map.Entry<Integer, CompletableFuture<Result>> updateProgressionWithCompletable(@NotNull AdvancementKey key, @NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) throws UserNotLoadedException {
        return this.updateProgressionWithCompletable(key, this.getTeamProgression(uuid), newProgression);
    }

    @NotNull
    public Map.Entry<Integer, CompletableFuture<Result>> updateProgressionWithCompletable(@NotNull AdvancementKey key, @NotNull TeamProgression progression, @Range(from=0L, to=0x7FFFFFFFL) int newProgression) {
        Preconditions.checkNotNull((Object)key, (Object)"Key is null.");
        AdvancementUtils.validateTeamProgression(progression);
        Preconditions.checkArgument((progression.getSize() > 0 ? 1 : 0) != 0, (Object)"TeamProgression doesn't contain any player.");
        AdvancementUtils.checkSync();
        int old = progression.updateProgression(key, newProgression);
        if (old != newProgression) {
            DatabaseManager.callEventCatchingExceptions(new ProgressionUpdateEvent(progression, old, newProgression, key));
            return new AbstractMap.SimpleEntry<Integer, CompletableFuture<Result>>(old, CompletableFuture.supplyAsync(() -> {
                try {
                    this.database.updateAdvancement(key, progression.getTeamId(), newProgression);
                }
                catch (SQLException e) {
                    System.err.println("Cannot update advancement " + String.valueOf(key) + " to team " + progression.getTeamId() + ":");
                    e.printStackTrace();
                    return new Result(e);
                }
                catch (Exception e) {
                    return new Result(e);
                }
                return Result.SUCCESSFUL;
            }));
        }
        return new AbstractMap.SimpleEntry<Integer, CompletableFuture<Result>>(old, CompletableFuture.completedFuture(Result.SUCCESSFUL));
    }

    @NotNull
    public TeamProgression getTeamProgression(@NotNull Player player) throws UserNotLoadedException {
        return this.getTeamProgression(AdvancementUtils.uuidFromPlayer(player));
    }

    @NotNull
    public synchronized TeamProgression getTeamProgression(@NotNull UUID uuid) throws UserNotLoadedException {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        TeamProgression pro = this.progressionCache.get(uuid);
        AdvancementUtils.checkTeamProgressionNotNull(pro, uuid);
        return pro;
    }

    @Contract(pure=true)
    public boolean isLoaded(@NotNull Player player) {
        return this.isLoaded(AdvancementUtils.uuidFromPlayer(player));
    }

    @Contract(pure=true)
    public boolean isLoaded(@NotNull OfflinePlayer player) {
        return this.isLoaded(AdvancementUtils.uuidFromPlayer(player));
    }

    @Contract(pure=true, value="null -> false")
    public synchronized boolean isLoaded(UUID uuid) {
        return this.progressionCache.containsKey(uuid);
    }

    @Contract(pure=true)
    public boolean isLoadedAndOnline(@NotNull Player player) {
        return this.isLoadedAndOnline(AdvancementUtils.uuidFromPlayer(player));
    }

    @Contract(pure=true, value="null -> false")
    public synchronized boolean isLoadedAndOnline(UUID uuid) {
        if (this.isLoaded(uuid)) {
            TempUserMetadata t = this.tempLoaded.get(uuid);
            return t == null || t.isOnline;
        }
        return false;
    }

    @Contract(pure=true)
    public synchronized @Range(from=0L, to=65535L) int getLoadingRequestsAmount(@NotNull Plugin plugin, @NotNull UUID uuid, @NotNull CacheFreeingOption.Option type) {
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        Preconditions.checkNotNull((Object)((Object)type), (Object)"CacheFreeingOption.Option is null.");
        TempUserMetadata t = this.tempLoaded.get(uuid);
        if (t == null) {
            return 0;
        }
        return switch (type) {
            case CacheFreeingOption.Option.AUTOMATIC -> t.getAuto(plugin);
            case CacheFreeingOption.Option.MANUAL -> t.getManual(plugin);
            default -> 0;
        };
    }

    @NotNull
    public @NotNull CompletableFuture<ObjectResult<@NotNull Boolean>> isUnredeemed(@NotNull AdvancementKey key, @NotNull UUID uuid) throws UserNotLoadedException {
        return this.isUnredeemed(key, this.getTeamProgression(uuid));
    }

    @NotNull
    public @NotNull CompletableFuture<ObjectResult<@NotNull Boolean>> isUnredeemed(@NotNull AdvancementKey key, @NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)key, (Object)"AdvancementKey is null.");
        AdvancementUtils.validateTeamProgression(pro);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new ObjectResult<Boolean>(this.database.isUnredeemed(key, pro.getTeamId()));
            }
            catch (SQLException e) {
                System.err.println("Cannot fetch unredeemed advancements of team " + pro.getTeamId() + ":");
                e.printStackTrace();
                return new ObjectResult(e);
            }
            catch (Exception e) {
                e.printStackTrace();
                return new ObjectResult(e);
            }
        });
    }

    @NotNull
    public CompletableFuture<Result> setUnredeemed(@NotNull AdvancementKey key, boolean giveRewards, @NotNull UUID uuid) throws UserNotLoadedException {
        return this.setUnredeemed(key, giveRewards, this.getTeamProgression(uuid));
    }

    @NotNull
    public CompletableFuture<Result> setUnredeemed(@NotNull AdvancementKey key, boolean giveRewards, @NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)key, (Object)"AdvancementKey is null.");
        AdvancementUtils.validateTeamProgression(pro);
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.database.setUnredeemed(key, giveRewards, pro.getTeamId());
            }
            catch (SQLException e) {
                System.err.println("Cannot set unredeemed advancement " + String.valueOf(key) + " to team " + pro.getTeamId() + ":");
                e.printStackTrace();
                return new Result(e);
            }
            catch (Exception e) {
                return new Result(e);
            }
            return Result.SUCCESSFUL;
        });
    }

    @NotNull
    public CompletableFuture<Result> unsetUnredeemed(@NotNull AdvancementKey key, @NotNull UUID uuid) throws UserNotLoadedException {
        return this.unsetUnredeemed(key, this.getTeamProgression(uuid));
    }

    @NotNull
    public CompletableFuture<Result> unsetUnredeemed(@NotNull AdvancementKey key, @NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)key, (Object)"AdvancementKey is null.");
        AdvancementUtils.validateTeamProgression(pro);
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.database.unsetUnredeemed(key, pro.getTeamId());
            }
            catch (SQLException e) {
                System.err.println("Cannot set unredeemed advancement " + String.valueOf(key) + " to team " + pro.getTeamId() + ":");
                e.printStackTrace();
                return new Result(e);
            }
            catch (Exception e) {
                return new Result(e);
            }
            return Result.SUCCESSFUL;
        });
    }

    @NotNull
    public @NotNull CompletableFuture<ObjectResult<@NotNull String>> getStoredPlayerName(@NotNull UUID uuid) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new ObjectResult<String>(this.database.getPlayerName(uuid));
            }
            catch (SQLException e) {
                System.err.println("Cannot fetch player name of " + String.valueOf(uuid) + ":");
                e.printStackTrace();
                return new ObjectResult(e);
            }
            catch (Exception e) {
                return new ObjectResult(e);
            }
        });
    }

    @NotNull
    public synchronized @NotNull CompletableFuture<ObjectResult<@NotNull TeamProgression>> loadOfflinePlayer(@NotNull UUID uuid, @NotNull CacheFreeingOption option) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        Preconditions.checkNotNull((Object)option, (Object)"CacheFreeingOption is null.");
        TeamProgression pro = this.progressionCache.get(uuid);
        if (pro != null) {
            this.handleCacheFreeingOption(uuid, null, option);
            return CompletableFuture.completedFuture(new ObjectResult<TeamProgression>(pro));
        }
        pro = this.searchTeamProgressionDeeply(uuid);
        if (pro != null) {
            this.handleCacheFreeingOption(uuid, pro, option);
            return CompletableFuture.completedFuture(new ObjectResult<TeamProgression>(pro));
        }
        return CompletableFuture.supplyAsync(() -> {
            TeamProgression t;
            try {
                t = this.database.loadUUID(uuid);
            }
            catch (SQLException e) {
                System.err.println("Cannot load offline player " + String.valueOf(uuid) + ":");
                e.printStackTrace();
                return new ObjectResult(e);
            }
            catch (Exception e) {
                return new ObjectResult(e);
            }
            this.handleCacheFreeingOption(uuid, t, option);
            if (option.option != CacheFreeingOption.Option.DONT_CACHE) {
                t.inCache.set(true);
                DatabaseManager.callEventCatchingExceptions(new AsyncTeamLoadEvent(t));
                AdvancementUtils.runSync(this.main, () -> Bukkit.getPluginManager().callEvent((Event)new TeamLoadEvent(t)));
            }
            return new ObjectResult<TeamProgression>(t);
        });
    }

    private void handleCacheFreeingOption(@NotNull UUID uuid, @Nullable TeamProgression pro, @NotNull CacheFreeingOption option) {
        switch (option.option) {
            case AUTOMATIC: {
                AdvancementUtils.runSync(this.main, option.ticks, () -> this.internalUnloadOfflinePlayer(uuid, option.requester, true));
                this.addCachingRequest(uuid, pro, option, true);
                break;
            }
            case MANUAL: {
                this.addCachingRequest(uuid, pro, option, false);
            }
        }
    }

    private synchronized void addCachingRequest(@NotNull UUID uuid, @Nullable TeamProgression pro, @NotNull CacheFreeingOption option, boolean auto) {
        TempUserMetadata meta = this.tempLoaded.computeIfAbsent(uuid, TempUserMetadata::new);
        meta.addRequest(option.requester, auto);
        if (pro != null) {
            this.progressionCache.put(uuid, pro);
        }
    }

    @Contract(pure=true, value="null -> false")
    public synchronized boolean isOfflinePlayerLoaded(UUID uuid) {
        return this.tempLoaded.containsKey(uuid);
    }

    @Contract(pure=true, value="null, null -> false; null, !null -> false; !null, null -> false")
    public synchronized boolean isOfflinePlayerLoaded(UUID uuid, Plugin requester) {
        TempUserMetadata t = this.tempLoaded.get(uuid);
        return t != null && Integer.compareUnsigned(t.getRequests(requester), 0) > 0;
    }

    public void unloadOfflinePlayer(@NotNull UUID uuid, @NotNull Plugin requester) {
        this.internalUnloadOfflinePlayer(uuid, requester, false);
    }

    private synchronized void internalUnloadOfflinePlayer(@NotNull UUID uuid, @NotNull Plugin requester, boolean auto) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        Preconditions.checkNotNull((Object)requester, (Object)"Plugin is null.");
        TempUserMetadata meta = this.tempLoaded.get(uuid);
        if (meta != null) {
            meta.removeRequest(requester, auto);
            if (meta.canBeRemoved()) {
                TeamProgression t;
                this.tempLoaded.remove(uuid);
                if (!meta.isOnline && (t = this.progressionCache.remove(uuid)) != null) {
                    if (t.noMemberMatch(this.progressionCache::containsKey)) {
                        t.inCache.set(false);
                        DatabaseManager.callEventCatchingExceptions(new AsyncTeamUnloadEvent(t));
                        if (Bukkit.isPrimaryThread()) {
                            DatabaseManager.callEventCatchingExceptions(new TeamUnloadEvent(t));
                        } else {
                            AdvancementUtils.runSync(this.main, () -> DatabaseManager.callEventCatchingExceptions(new TeamUnloadEvent(t)));
                        }
                    }
                }
            }
        }
    }

    private static <E extends Event> void callEventCatchingExceptions(E event) {
        try {
            Bukkit.getPluginManager().callEvent(event);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static final class TempUserMetadata {
        final Map<Plugin, Integer> pluginRequests = new HashMap<Plugin, Integer>();
        boolean isOnline;

        public TempUserMetadata(UUID uuid) {
            this.isOnline = Bukkit.getPlayer((UUID)uuid) != null;
        }

        public void addRequest(@NotNull Plugin plugin, boolean auto) {
            this.pluginRequests.compute(plugin, (p, i) -> {
                if (i == null) {
                    i = 0;
                }
                return auto ? this.addAuto((int)i) : this.addManual((int)i);
            });
        }

        public void removeRequest(@NotNull Plugin plugin, boolean auto) {
            Integer i = this.pluginRequests.get(plugin);
            if (i != null) {
                i = auto ? this.removeAuto(i) : this.removeManual(i);
                if (Integer.compareUnsigned(i, 0) <= 0) {
                    this.pluginRequests.remove(plugin);
                } else {
                    this.pluginRequests.put(plugin, i);
                }
            }
        }

        public int getRequests(@NotNull Plugin plugin) {
            return this.pluginRequests.getOrDefault(plugin, 0);
        }

        public int getAuto(@NotNull Plugin plugin) {
            return this.getRequests(plugin) >>> 16;
        }

        public int getManual(@NotNull Plugin plugin) {
            return this.getRequests(plugin) & 0xFFFF;
        }

        public boolean canBeRemoved() {
            return this.pluginRequests.isEmpty();
        }

        private int addAuto(int i) {
            char tmp = (char)(i >>> 16);
            if (tmp == '\uffff') {
                throw new RuntimeException("Max per-plugin automatic simultaneous requests amount exceeded.");
            }
            return tmp + '\u0001' << 16 | i & 0xFFFF;
        }

        private int addManual(int i) {
            char tmp = (char)(i & 0xFFFF);
            if (tmp == '\uffff') {
                throw new RuntimeException("Max per-plugin manual simultaneous requests amount exceeded.");
            }
            return tmp + '\u0001' | i & 0xFFFF0000;
        }

        private int removeAuto(int i) {
            char tmp = (char)(i >>> 16);
            return tmp == '\u0000' ? i & 0xFFFF : tmp - '\u0001' << 16 | i & 0xFFFF;
        }

        private int removeManual(int i) {
            char tmp = (char)(i & 0xFFFF);
            return tmp == '\u0000' ? i & 0xFFFF0000 : tmp - '\u0001' | i & 0xFFFF0000;
        }

        public String toString() {
            return "TempUserMetadata{pluginRequests=" + String.valueOf(this.pluginRequests) + ", isOnline=" + this.isOnline + "}";
        }
    }
}

