/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.scheduler.BukkitTask
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package com.fren_gor.ultimateAdvancementAPI;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.events.EventManager;
import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.AdvancementDisposeEvent;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.AdvancementDisposedEvent;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.AdvancementRegistrationEvent;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DisposedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DuplicatedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotLoadedException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.PacketPlayOutAdvancementsWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.PacketPlayOutSelectAdvancementTabWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.fren_gor.ultimateAdvancementAPI.util.LazyValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public final class AdvancementTab {
    private final Plugin owningPlugin;
    private final EventManager eventManager;
    private final String namespace;
    private final DatabaseManager databaseManager;
    private final Map<AdvancementKey, Advancement> advancements = new HashMap<AdvancementKey, Advancement>();
    private final Map<Player, Set<MinecraftKeyWrapper>> players = new HashMap<Player, Set<MinecraftKeyWrapper>>();
    private final AdvsUpdateRunnable updateManager;
    private RootAdvancement rootAdvancement;
    private boolean initialised = false;
    private boolean disposed = false;
    private boolean automaticallyShown = false;
    private boolean automaticallyGrant = false;
    @LazyValue
    private Collection<String> advNamespacedKeys;
    @LazyValue
    private Collection<BaseAdvancement> advsWithoutRoot;

    AdvancementTab(@NotNull Plugin owningPlugin, @NotNull DatabaseManager databaseManager, @NotNull String namespace) {
        AdvancementKey.checkNamespace(namespace);
        this.namespace = Objects.requireNonNull(namespace);
        this.owningPlugin = Objects.requireNonNull(owningPlugin);
        this.eventManager = new EventManager(owningPlugin);
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.updateManager = new AdvsUpdateRunnable();
        this.eventManager.register(this, PlayerQuitEvent.class, e -> this.players.remove(e.getPlayer()));
    }

    public boolean isActive() {
        return this.initialised && !this.disposed;
    }

    @NotNull
    @Contract(pure=true)
    public RootAdvancement getRootAdvancement() {
        this.checkInitialisation();
        return this.rootAdvancement;
    }

    @NotNull
    @Contract(pure=true)
    public @UnmodifiableView @NotNull Collection<@NotNull Advancement> getAdvancements() {
        this.checkInitialisation();
        return Collections.unmodifiableCollection(this.advancements.values());
    }

    @NotNull
    public @Unmodifiable @NotNull Collection<@NotNull BaseAdvancement> getAdvancementsWithoutRoot() {
        this.checkInitialisation();
        if (this.advsWithoutRoot != null) {
            return this.advsWithoutRoot;
        }
        ArrayList<BaseAdvancement> list = new ArrayList<BaseAdvancement>(this.advancements.size());
        for (Advancement a : this.advancements.values()) {
            if (!(a instanceof BaseAdvancement)) continue;
            BaseAdvancement base = (BaseAdvancement)a;
            list.add(base);
        }
        this.advsWithoutRoot = Collections.unmodifiableList(list);
        return this.advsWithoutRoot;
    }

    @NotNull
    @Contract(pure=true)
    public @Unmodifiable @NotNull Collection<@NotNull Advancement> getAdvancementsByClass(Class<? extends Advancement> filterClass) {
        this.checkInitialisation();
        if (filterClass == null) {
            return Collections.emptyList();
        }
        if (filterClass == Advancement.class) {
            return Collections.unmodifiableCollection(this.advancements.values());
        }
        if (this.rootAdvancement.getClass().isInstance(filterClass)) {
            return Collections.singletonList(this.rootAdvancement);
        }
        if (filterClass.isInstance(BaseAdvancement.class)) {
            ArrayList<Advancement> coll = new ArrayList<Advancement>(this.advancements.size());
            for (Advancement a : this.advancements.values()) {
                if (!a.getClass().isInstance(filterClass)) continue;
                coll.add(a);
            }
            return Collections.unmodifiableCollection(coll);
        }
        return Collections.emptyList();
    }

    @NotNull
    @Contract(pure=true)
    public @UnmodifiableView @NotNull Set<@NotNull AdvancementKey> getAdvancementsNamespacedKeys() {
        this.checkInitialisation();
        return Collections.unmodifiableSet(this.advancements.keySet());
    }

    @Contract(pure=true, value="null -> false")
    public boolean hasAdvancement(Advancement advancement) {
        this.checkInitialisation();
        if (advancement == null) {
            return false;
        }
        return this.advancements.containsKey(advancement.getKey());
    }

    @Contract(pure=true, value="null -> false")
    public boolean hasAdvancement(AdvancementKey namespacedKey) {
        this.checkInitialisation();
        return this.advancements.containsKey(namespacedKey);
    }

    @Nullable
    @Contract(pure=true, value="null -> null")
    public Advancement getAdvancement(AdvancementKey namespacedKey) {
        this.checkInitialisation();
        return this.advancements.get(namespacedKey);
    }

    @NotNull
    @Contract(pure=true)
    public @UnmodifiableView @NotNull Set<@NotNull Player> getPlayers() {
        this.checkInitialisation();
        return Collections.unmodifiableSet(this.players.keySet());
    }

    public void grantRootAdvancement(@NotNull Player player) {
        this.checkInitialisation();
        this.rootAdvancement.setProgression(player, this.rootAdvancement.getMaxProgression());
    }

    public void grantRootAdvancement(@NotNull UUID uuid) {
        this.checkInitialisation();
        this.rootAdvancement.setProgression(uuid, this.rootAdvancement.getMaxProgression());
    }

    public void grantRootAdvancement(@NotNull Player player, boolean giveRewards) {
        this.checkInitialisation();
        this.rootAdvancement.setProgression(player, this.rootAdvancement.getMaxProgression(), giveRewards);
    }

    public void grantRootAdvancement(@NotNull UUID uuid, boolean giveRewards) {
        this.checkInitialisation();
        this.rootAdvancement.setProgression(uuid, this.rootAdvancement.getMaxProgression(), giveRewards);
    }

    public void updateAdvancementsToTeam(@NotNull Player player) throws UserNotLoadedException {
        this.updateAdvancementsToTeam(AdvancementUtils.uuidFromPlayer(player));
    }

    public void updateAdvancementsToTeam(@NotNull UUID uuid) throws UserNotLoadedException {
        this.updateAdvancementsToTeam(this.databaseManager.getTeamProgression(uuid));
    }

    public void updateAdvancementsToTeam(@NotNull TeamProgression pro) {
        this.checkInitialisation();
        AdvancementUtils.validateTeamProgression(pro);
        this.updateManager.schedule(pro);
    }

    @Deprecated(since="2.2.0")
    public void updateEveryAdvancement(@NotNull Player player) throws UserNotLoadedException {
        this.updateAdvancementsToTeam(player);
    }

    public void registerAdvancements(@NotNull RootAdvancement rootAdvancement, BaseAdvancement ... advancements) {
        this.registerAdvancements(rootAdvancement, Sets.newHashSet((Object[])advancements));
    }

    public void registerAdvancements(@NotNull RootAdvancement rootAdvancement, @NotNull Set<BaseAdvancement> advancements) {
        if (this.disposed) {
            throw new DisposedException("AdvancementTab is disposed.");
        }
        if (this.initialised) {
            throw new IllegalStateException("Tab is already initialised.");
        }
        Preconditions.checkNotNull((Object)rootAdvancement, (Object)"RootAdvancement is null.");
        Preconditions.checkArgument((boolean)this.isOwnedByThisTab(rootAdvancement), (Object)("RootAdvancement " + String.valueOf(rootAdvancement) + " is not owned by this tab."));
        for (BaseAdvancement a : advancements) {
            if (a == null) {
                throw new IllegalArgumentException("An advancement is null.");
            }
            if (this.isOwnedByThisTab(a)) continue;
            throw new IllegalArgumentException("Advancement " + a.getKey().toString() + " is not owned by this tab.");
        }
        this.advancements.clear();
        this.rootAdvancement = rootAdvancement;
        this.advancements.put(rootAdvancement.getKey(), rootAdvancement);
        PluginManager pluginManager = Bukkit.getPluginManager();
        this.callOnRegister(rootAdvancement);
        try {
            pluginManager.callEvent((Event)new AdvancementRegistrationEvent(rootAdvancement));
        }
        catch (IllegalStateException e) {
            this.onRegisterFail();
            throw e;
        }
        for (BaseAdvancement baseAdvancement : advancements) {
            if (this.advancements.put(baseAdvancement.getKey(), baseAdvancement) != null) {
                this.onRegisterFail();
                throw new DuplicatedException("Advancement " + String.valueOf(baseAdvancement.getKey()) + " is duplicated.");
            }
            this.callOnRegister(baseAdvancement);
            try {
                pluginManager.callEvent((Event)new AdvancementRegistrationEvent(baseAdvancement));
            }
            catch (IllegalStateException e) {
                this.onRegisterFail();
                throw e;
            }
        }
        this.initialised = true;
        for (Advancement advancement : this.advancements.values()) {
            this.callValidation(advancement);
        }
    }

    private void callOnRegister(Advancement adv) {
        try {
            adv.onRegister();
        }
        catch (Exception e) {
            this.onRegisterFail();
            throw new RuntimeException("Exception occurred while registering advancement " + String.valueOf(adv.getKey()) + ":", e);
        }
    }

    private void callValidation(Advancement adv) {
        try {
            adv.validateRegister();
        }
        catch (InvalidAdvancementException e) {
            this.onRegisterFail();
            throw new RuntimeException("Advancement " + String.valueOf(adv.getKey()) + " is not valid:", e);
        }
        catch (Exception e) {
            this.onRegisterFail();
            throw new RuntimeException("Exception occurred while validating advancement " + String.valueOf(adv.getKey()) + ":", e);
        }
    }

    private void onRegisterFail() {
        this.initialised = false;
        this.advancements.clear();
        this.rootAdvancement = null;
    }

    public void showTab(Player ... players) {
        this.checkInitialisation();
        Preconditions.checkNotNull((Object)players, (Object)"Player[] is null.");
        for (Player p : players) {
            try {
                this.showTab(p);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showTab(@NotNull Player player) {
        this.checkInitialisation();
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        if (!this.players.containsKey(player)) {
            this.players.put(player, Collections.emptySet());
            this.updateAdvancementsToTeam(player);
        }
    }

    public void hideTab(Player ... players) {
        this.checkInitialisation();
        Preconditions.checkNotNull((Object)players, (Object)"Player[] is null.");
        for (Player p : players) {
            try {
                this.hideTab(p);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void hideTab(@NotNull Player player) {
        this.checkInitialisation();
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        if (this.isShownTo(player)) {
            this.removePlayer(player, this.players.remove(player));
        }
    }

    private void removePlayer(@NotNull Player player, Set<MinecraftKeyWrapper> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            PacketPlayOutAdvancementsWrapper.craftRemovePacket(keys).sendTo(player);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    void dispose() {
        this.checkInitialisation();
        this.disposed = true;
        this.eventManager.disable();
        this.updateManager.dispose();
        Iterator<Map.Entry<Player, Set<MinecraftKeyWrapper>>> it = this.players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, Set<MinecraftKeyWrapper>> e = it.next();
            this.removePlayer(e.getKey(), e.getValue());
            it.remove();
        }
        PluginManager pluginManager = Bukkit.getPluginManager();
        for (Advancement a : this.advancements.values()) {
            try {
                try {
                    pluginManager.callEvent((Event)new AdvancementDisposeEvent(a));
                }
                catch (IllegalStateException e) {
                    System.err.println("An exception has occurred while calling AdvancementDisposeEvent for " + String.valueOf(a));
                    e.printStackTrace();
                }
                a.onDispose();
                try {
                    pluginManager.callEvent((Event)new AdvancementDisposedEvent(a.getKey()));
                }
                catch (IllegalStateException e) {
                    System.err.println("An exception has occurred while calling AdvancementDisposedEvent for " + String.valueOf(a));
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.advancements.clear();
        this.rootAdvancement = null;
        this.advNamespacedKeys = null;
        this.advsWithoutRoot = null;
    }

    @Contract(pure=true, value="null -> false")
    public boolean isShownTo(Player player) {
        this.checkInitialisation();
        return this.players.containsKey(player);
    }

    @NotNull
    public @Unmodifiable @NotNull Collection<@NotNull String> getAdvancementsAsStrings() {
        this.checkInitialisation();
        if (this.advNamespacedKeys != null) {
            return this.advNamespacedKeys;
        }
        ArrayList<String> list = new ArrayList<String>(this.advancements.size());
        for (AdvancementKey key : this.advancements.keySet()) {
            list.add(key.toString());
        }
        this.advNamespacedKeys = Collections.unmodifiableCollection(list);
        return this.advNamespacedKeys;
    }

    @Contract(pure=true)
    public boolean isOwnedByThisTab(@NotNull Advancement advancement) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        return advancement.getKey().getNamespace().equals(this.namespace);
    }

    @NotNull
    @Contract(value="-> this")
    public AdvancementTab automaticallyShowToPlayers() {
        this.checkInitialisation();
        if (!this.automaticallyShown) {
            this.automaticallyShown = true;
            this.registerEvent(PlayerLoadingCompletedEvent.class, EventPriority.LOWEST, e -> this.showTab(e.getPlayer()));
        }
        return this;
    }

    @NotNull
    @Contract(value="-> this")
    public AdvancementTab automaticallyGrantRootAdvancement() {
        this.checkInitialisation();
        if (!this.automaticallyGrant) {
            this.automaticallyGrant = true;
            this.registerEvent(PlayerLoadingCompletedEvent.class, EventPriority.LOW, e -> this.grantRootAdvancement(e.getPlayer()));
        }
        return this;
    }

    public <E extends Event> void registerEvent(@NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        try {
            this.eventManager.register(this, eventClass, consumer);
        }
        catch (IllegalStateException e) {
            throw new DisposedException(e);
        }
    }

    public <E extends Event> void registerEvent(@NotNull Class<E> eventClass, @NotNull EventPriority priority, @NotNull Consumer<E> consumer) {
        try {
            this.eventManager.register(this, eventClass, priority, consumer);
        }
        catch (IllegalStateException e) {
            throw new DisposedException(e);
        }
    }

    private void checkInitialisation() {
        if (this.disposed) {
            throw new DisposedException("AdvancementTab is disposed");
        }
        if (!this.initialised) {
            throw new IllegalStateException("AdvancementTab has not been initialised yet.");
        }
    }

    @NotNull
    public String toString() {
        return this.namespace;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AdvancementTab that = (AdvancementTab)o;
        return this.namespace.equals(that.namespace);
    }

    public int hashCode() {
        return this.namespace.hashCode();
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
    public String getNamespace() {
        return this.namespace;
    }

    @NotNull
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public boolean isInitialised() {
        return this.initialised;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    private class AdvsUpdateRunnable
    implements Runnable {
        private final Set<TeamProgression> advsToUpdate = new HashSet<TeamProgression>();
        private boolean scheduled = false;
        private BukkitTask task;

        private AdvsUpdateRunnable() {
        }

        public void schedule(@NotNull TeamProgression progression) {
            if (!this.scheduled) {
                this.scheduled = true;
                this.task = Bukkit.getScheduler().runTaskLater(AdvancementTab.this.owningPlugin, (Runnable)this, 1L);
            }
            this.advsToUpdate.add(progression);
        }

        public void dispose() {
            if (this.task != null) {
                this.task.cancel();
                this.advsToUpdate.clear();
                this.scheduled = false;
            }
        }

        @Override
        public void run() {
            int best = AdvancementTab.this.advancements.size() + 16;
            HashMap advs = Maps.newHashMapWithExpectedSize((int)best);
            for (TeamProgression pro : this.advsToUpdate) {
                PacketPlayOutSelectAdvancementTabWrapper thisTab;
                PacketPlayOutSelectAdvancementTabWrapper noTab;
                PacketPlayOutAdvancementsWrapper sendPacket;
                HashSet keys = Sets.newHashSetWithExpectedSize((int)best);
                for (Advancement advancement : AdvancementTab.this.advancements.values()) {
                    advancement.onUpdate(pro, advs);
                    keys.add(advancement.getKey().getNMSWrapper());
                }
                try {
                    sendPacket = PacketPlayOutAdvancementsWrapper.craftSendPacket(advs);
                    noTab = PacketPlayOutSelectAdvancementTabWrapper.craftSelectNone();
                    thisTab = PacketPlayOutSelectAdvancementTabWrapper.craftSelect(AdvancementTab.this.rootAdvancement.getKey().getNMSWrapper());
                }
                catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                    continue;
                }
                pro.forEachMember(u -> {
                    Player player = Bukkit.getPlayer((UUID)u);
                    if (player != null) {
                        noTab.sendTo(player);
                        @Nullable Set<MinecraftKeyWrapper> set = AdvancementTab.this.players.put(player, keys);
                        if (set != null && !set.isEmpty()) {
                            try {
                                PacketPlayOutAdvancementsWrapper.craftRemovePacket(keys).sendTo(player);
                            }
                            catch (ReflectiveOperationException e) {
                                e.printStackTrace();
                                thisTab.sendTo(player);
                                return;
                            }
                        }
                        sendPacket.sendTo(player);
                        thisTab.sendTo(player);
                    }
                });
                advs.clear();
            }
            this.task = null;
            this.advsToUpdate.clear();
            this.scheduled = false;
        }
    }
}

