/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.ApiStatus$Obsolete
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 *  org.jetbrains.annotations.UnmodifiableView
 */
package com.fren_gor.ultimateAdvancementAPI;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.IDatabase;
import com.fren_gor.ultimateAdvancementAPI.database.impl.InMemory;
import com.fren_gor.ultimateAdvancementAPI.database.impl.MySQL;
import com.fren_gor.ultimateAdvancementAPI.database.impl.SQLite;
import com.fren_gor.ultimateAdvancementAPI.events.EventManager;
import com.fren_gor.ultimateAdvancementAPI.exceptions.AsyncExecutionException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DuplicatedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidVersionException;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.BukkitLibraryManager;
import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.fren_gor.ultimateAdvancementAPI.util.Versions;
import com.google.common.base.Preconditions;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

public final class AdvancementMain {
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean INVALID_VERSION = new AtomicBoolean(false);
    private final Plugin owningPlugin;
    private EventManager eventManager;
    private DatabaseManager databaseManager;
    private BukkitLibraryManager libbyManager;
    private final String libFolder;
    private final Map<String, AdvancementTab> tabs = new HashMap<String, AdvancementTab>();
    private final Map<Plugin, List<AdvancementTab>> pluginMap = new HashMap<Plugin, List<AdvancementTab>>();

    public AdvancementMain(@NotNull Plugin owningPlugin) {
        Preconditions.checkNotNull((Object)owningPlugin, (Object)"Plugin is null.");
        this.owningPlugin = owningPlugin;
        this.libFolder = ".libs";
    }

    public AdvancementMain(@NotNull Plugin owningPlugin, String libFolder) {
        Preconditions.checkNotNull((Object)owningPlugin, (Object)"Plugin is null.");
        Preconditions.checkNotNull((Object)libFolder, (Object)"Lib folder is null.");
        this.owningPlugin = owningPlugin;
        this.libFolder = libFolder;
    }

    public void load() throws InvalidVersionException {
        AdvancementMain.checkSync();
        if (!LOADED.compareAndSet(false, true)) {
            throw new IllegalStateException("UltimateAdvancementAPI is getting loaded twice.");
        }
        Optional<String> version = Versions.getNMSVersion();
        if (version.isEmpty()) {
            INVALID_VERSION.set(true);
            String fancy = Versions.getSupportedNMSVersions().stream().map(Versions::getNMSVersionsRange).collect(Collectors.joining(", ", "[", "]"));
            throw new InvalidVersionException(fancy, ReflectionUtil.MINECRAFT_VERSION, "Invalid minecraft version, couldn't load UltimateAdvancementAPI. Supported versions are " + fancy + ".");
        }
        this.libbyManager = new BukkitLibraryManager(this.owningPlugin, this.libFolder);
        this.libbyManager.addMavenCentral();
    }

    @ApiStatus.Obsolete(since="2.5.0")
    public void enableSQLite(File SQLiteDatabase) {
        this.enable(() -> new SQLite(this, SQLiteDatabase));
    }

    @ApiStatus.Obsolete(since="2.5.0")
    public void enableMySQL(String username, String password, String databaseName, String host, @Range(from=1L, to=0x7FFFFFFFL) int port, @Range(from=1L, to=0x7FFFFFFFL) int poolSize, @Range(from=250L, to=0x7FFFFFFFFFFFFFFFL) long connectionTimeout) {
        this.enable(() -> new MySQL(this, username, password, databaseName, host, port, poolSize, connectionTimeout));
    }

    @ApiStatus.Obsolete(since="2.5.0")
    public void enableInMemory() {
        this.enable(() -> new InMemory(this));
    }

    public void enable(@NotNull @NotNull Callable<@NotNull IDatabase> databaseImplProvider) {
        Preconditions.checkNotNull(databaseImplProvider, (Object)"Database impl provider is null");
        this.commonEnablePreDatabase();
        try {
            IDatabase dbImpl = Objects.requireNonNull(databaseImplProvider.call(), "Database impl is null");
            this.databaseManager = new DatabaseManager(this, dbImpl);
        }
        catch (Exception e) {
            this.failEnable(e);
        }
        this.commonEnablePostDatabase();
    }

    private void commonEnablePreDatabase() {
        AdvancementMain.checkSync();
        if (INVALID_VERSION.get()) {
            throw new InvalidVersionException("Incorrect minecraft version. Couldn't enable UltimateAdvancementAPI.");
        }
        if (!LOADED.get()) {
            throw new IllegalStateException("UltimateAdvancementAPI is not loaded.");
        }
        if (!this.owningPlugin.isEnabled()) {
            throw new IllegalStateException(this.owningPlugin.getName() + " is not enabled, cannot enable UltimateAdvancementAPI.");
        }
        if (!ENABLED.compareAndSet(false, true)) {
            throw new IllegalStateException("UltimateAdvancementAPI is getting enabled twice.");
        }
        this.eventManager = new EventManager(this.owningPlugin);
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
    }

    private void commonEnablePostDatabase() {
        this.eventManager.register(this, PluginDisableEvent.class, EventPriority.HIGHEST, e -> this.unregisterAdvancementTabs(e.getPlugin()));
        this.eventManager.register(this, ServerCommandEvent.class, e -> {
            if (AdvancementMain.isMcReload(e.getCommand())) {
                AdvancementUtils.runSync(this, 20L, () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayer));
            }
        });
        this.eventManager.register(this, PlayerCommandPreprocessEvent.class, e -> {
            if (AdvancementMain.isMcReload(e.getMessage())) {
                AdvancementUtils.runSync(this, 20L, () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayer));
            }
        });
        UltimateAdvancementAPI.main = this;
    }

    @Contract(value="_ -> fail")
    private void failEnable(Exception e) {
        this.disable();
        throw new RuntimeException("Exception setting up database.", e);
    }

    public void disable() {
        AdvancementMain.checkSync();
        if (INVALID_VERSION.get()) {
            throw new InvalidVersionException("Incorrect minecraft version. Couldn't disable UltimateAdvancementAPI.");
        }
        if (!LOADED.compareAndSet(true, false)) {
            return;
        }
        UltimateAdvancementAPI.main = null;
        if (ENABLED.getAndSet(false)) {
            if (this.eventManager != null) {
                this.eventManager.disable();
            }
            this.pluginMap.clear();
            Iterator<AdvancementTab> it = this.tabs.values().iterator();
            while (it.hasNext()) {
                try {
                    AdvancementTab tab = it.next();
                    if (tab.isActive()) {
                        tab.dispose();
                    }
                    it.remove();
                }
                catch (Exception t) {
                    t.printStackTrace();
                }
            }
            if (this.databaseManager != null) {
                this.databaseManager.unregister();
            }
        }
    }

    @NotNull
    @Contract(value="_, _ -> new")
    public AdvancementTab createAdvancementTab(@NotNull Plugin plugin, @NotNull String namespace) throws DuplicatedException {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        if (this.tabs.containsKey(namespace)) {
            throw new DuplicatedException("An AdvancementTab with '" + namespace + "' namespace already exists.");
        }
        AdvancementTab tab = new AdvancementTab(plugin, this.databaseManager, namespace);
        this.tabs.put(namespace, tab);
        this.pluginMap.computeIfAbsent(plugin, p -> new LinkedList()).add(tab);
        return tab;
    }

    @Nullable
    public AdvancementTab getAdvancementTab(@NotNull String namespace) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        return this.tabs.get(namespace);
    }

    public boolean isAdvancementTabRegistered(@NotNull String namespace) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        return this.tabs.containsKey(namespace);
    }

    @NotNull
    public @UnmodifiableView @NotNull Collection<@NotNull AdvancementTab> getPluginAdvancementTabs(@NotNull Plugin plugin) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        return Collections.unmodifiableCollection(this.pluginMap.getOrDefault(plugin, Collections.emptyList()));
    }

    public void unregisterAdvancementTab(@NotNull String namespace) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        AdvancementTab tab = this.tabs.remove(namespace);
        if (tab != null) {
            tab.dispose();
        }
    }

    public void unregisterAdvancementTabs(@NotNull Plugin plugin) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        List<AdvancementTab> tabs = this.pluginMap.remove(plugin);
        if (tabs != null) {
            for (AdvancementTab t : tabs) {
                this.unregisterAdvancementTab(t.getNamespace());
            }
        }
    }

    @Nullable
    public Advancement getAdvancement(@NotNull String namespacedKey) {
        AdvancementMain.checkInitialisation();
        int colon = namespacedKey.indexOf(58);
        if (colon <= 0 || colon == namespacedKey.length() - 1) {
            throw new IllegalArgumentException("Malformed namespaced key '" + namespacedKey + "'");
        }
        return this.getAdvancement(namespacedKey.substring(0, colon), namespacedKey.substring(colon + 1));
    }

    @Nullable
    public Advancement getAdvancement(@NotNull String namespace, @NotNull String key) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        Preconditions.checkNotNull((Object)key, (Object)"Key is null.");
        return this.getAdvancement(new AdvancementKey(namespace, key));
    }

    @Nullable
    public Advancement getAdvancement(@NotNull AdvancementKey namespacedKey) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)namespacedKey, (Object)"AdvancementKey is null.");
        AdvancementTab tab = this.tabs.get(namespacedKey.getNamespace());
        if (tab == null || !tab.isActive()) {
            return null;
        }
        return tab.getAdvancement(namespacedKey);
    }

    @NotNull
    @Contract(pure=true)
    public @NotNull @UnmodifiableView Set<@NotNull String> getAdvancementTabNamespaces() {
        AdvancementMain.checkInitialisation();
        return Collections.unmodifiableSet(this.tabs.keySet());
    }

    @NotNull
    @Contract(pure=true, value="_ -> new")
    public @NotNull List<@NotNull String> filterNamespaces(@Nullable String input) {
        AdvancementMain.checkInitialisation();
        LinkedList<String> l = new LinkedList<String>();
        if (input == null || input.isEmpty()) {
            for (Map.Entry<String, AdvancementTab> e : this.tabs.entrySet()) {
                if (!e.getValue().isActive()) continue;
                l.addAll(e.getValue().getAdvancementsAsStrings());
            }
        } else {
            int index = input.indexOf(58);
            if (index != -1) {
                String sub = input.substring(0, index);
                for (Map.Entry<String, AdvancementTab> e : this.tabs.entrySet()) {
                    if (!e.getValue().isActive() || !e.getKey().equals(sub)) continue;
                    for (String s : e.getValue().getAdvancementsAsStrings()) {
                        if (!s.startsWith(input)) continue;
                        l.add(s);
                    }
                }
            } else {
                for (Map.Entry<String, AdvancementTab> e : this.tabs.entrySet()) {
                    if (!e.getValue().isActive() || !e.getKey().startsWith(input)) continue;
                    l.addAll(e.getValue().getAdvancementsAsStrings());
                }
            }
        }
        return l;
    }

    @NotNull
    public @UnmodifiableView @NotNull Collection<@NotNull AdvancementTab> getTabs() {
        AdvancementMain.checkInitialisation();
        return Collections.unmodifiableCollection(this.tabs.values());
    }

    public void updatePlayer(@NotNull Player player) {
        AdvancementMain.checkInitialisation();
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        for (AdvancementTab tab : this.tabs.values()) {
            if (!tab.isActive() || !tab.isShownTo(player)) continue;
            tab.updateAdvancementsToTeam(player);
        }
    }

    private static void checkInitialisation() {
        if (!AdvancementMain.isLoaded() || !AdvancementMain.isEnabled()) {
            throw new IllegalStateException("UltimateAdvancementAPI is not enabled.");
        }
    }

    private static boolean isMcReload(@NotNull String command) {
        return command.startsWith("/minecraft:reload") || command.startsWith("minecraft:reload");
    }

    @NotNull
    public Plugin getOwningPlugin() {
        return this.owningPlugin;
    }

    @NotNull
    public EventManager getEventManager() {
        return this.eventManager;
    }

    @NotNull
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    @NotNull
    public BukkitLibraryManager getLibbyManager() {
        return this.libbyManager;
    }

    public static boolean isLoaded() {
        return LOADED.get() && !INVALID_VERSION.get();
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    @NotNull
    public Logger getLogger() {
        return this.owningPlugin.getLogger();
    }

    public File getDataFolder() {
        return this.owningPlugin.getDataFolder();
    }

    private static void checkSync() {
        if (!Bukkit.isPrimaryThread()) {
            throw new AsyncExecutionException("Illegal async method call. This method can be called only from the main thread.");
        }
    }
}

