/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package com.fren_gor.ultimateAdvancementAPI;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.database.CacheFreeingOption;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.ObjectResult;
import com.fren_gor.ultimateAdvancementAPI.database.Result;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.APINotInstantiatedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DuplicatedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.NotGrantedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotLoadedException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public final class UltimateAdvancementAPI {
    static AdvancementMain main;
    private final Plugin plugin;

    private static AdvancementMain getMain() throws IllegalStateException {
        if (main == null) {
            throw new IllegalStateException("UltimateAdvancementAPI is not enabled.");
        }
        return main;
    }

    @NotNull
    public static UltimateAdvancementAPI getInstance(@NotNull Plugin plugin) throws APINotInstantiatedException {
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        Preconditions.checkArgument((boolean)plugin.isEnabled(), (Object)"Plugin is not enabled.");
        if (main == null) {
            throw new APINotInstantiatedException();
        }
        return new UltimateAdvancementAPI(plugin);
    }

    private UltimateAdvancementAPI(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Contract(value="_ -> new")
    public AdvancementTab createAdvancementTab(@NotNull String namespace) throws DuplicatedException {
        return UltimateAdvancementAPI.getMain().createAdvancementTab(this.plugin, namespace);
    }

    @Nullable
    public AdvancementTab getAdvancementTab(@NotNull String namespace) {
        return UltimateAdvancementAPI.getMain().getAdvancementTab(namespace);
    }

    public boolean isAdvancementTabRegistered(@NotNull String namespace) {
        return UltimateAdvancementAPI.getMain().isAdvancementTabRegistered(namespace);
    }

    @NotNull
    public @UnmodifiableView @NotNull Collection<@NotNull AdvancementTab> getPluginAdvancementTabs() {
        return UltimateAdvancementAPI.getMain().getPluginAdvancementTabs(this.plugin);
    }

    public void unregisterAdvancementTab(@NotNull String namespace) {
        UltimateAdvancementAPI.getMain().unregisterAdvancementTab(namespace);
    }

    public void unregisterPluginAdvancementTabs() {
        UltimateAdvancementAPI.getMain().unregisterAdvancementTabs(this.plugin);
    }

    @NotNull
    @Contract(pure=true)
    public @NotNull @UnmodifiableView Set<@NotNull String> getAdvancementTabNamespaces() {
        return UltimateAdvancementAPI.getMain().getAdvancementTabNamespaces();
    }

    @NotNull
    @Contract(pure=true, value="_ -> new")
    public @NotNull List<@NotNull String> filterNamespaces(@Nullable String input) {
        return UltimateAdvancementAPI.getMain().filterNamespaces(input);
    }

    @NotNull
    public @NotNull @UnmodifiableView Collection<@NotNull AdvancementTab> getTabs() {
        return UltimateAdvancementAPI.getMain().getTabs();
    }

    public void updatePlayer(@NotNull Player player) {
        UltimateAdvancementAPI.getMain().updatePlayer(player);
    }

    @Nullable
    public Advancement getAdvancement(@NotNull String namespacedKey) {
        return UltimateAdvancementAPI.getMain().getAdvancement(namespacedKey);
    }

    @Nullable
    public Advancement getAdvancement(@NotNull String namespace, @NotNull String key) {
        return UltimateAdvancementAPI.getMain().getAdvancement(namespace, key);
    }

    @Nullable
    public Advancement getAdvancement(@NotNull AdvancementKey namespacedKey) {
        return UltimateAdvancementAPI.getMain().getAdvancement(namespacedKey);
    }

    public void displayCustomToast(@NotNull Player player, @NotNull AdvancementDisplay display) {
        this.displayCustomToast(player, display.getIcon(), display.getTitle(), display.getFrame());
    }

    public void displayCustomToast(@NotNull Player player, @NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame) {
        AdvancementUtils.displayToast(player, icon, title, frame);
    }

    public void disableVanillaAdvancements() throws RuntimeException {
        try {
            AdvancementUtils.disableVanillaAdvancements();
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't disable minecraft advancements.", e);
        }
    }

    public void disableVanillaRecipeAdvancements() throws RuntimeException {
        try {
            AdvancementUtils.disableVanillaRecipeAdvancements();
        }
        catch (Exception e) {
            throw new RuntimeException("Couldn't disable minecraft recipe advancements.", e);
        }
    }

    @NotNull
    public TeamProgression getTeamProgression(@NotNull Player player) throws UserNotLoadedException {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().getTeamProgression(player);
    }

    @NotNull
    public TeamProgression getTeamProgression(@NotNull UUID uuid) throws UserNotLoadedException {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().getTeamProgression(uuid);
    }

    public void updatePlayerTeam(@NotNull Player playerToMove, @NotNull Player aDestTeamPlayer) {
        this.updatePlayerTeam(playerToMove, aDestTeamPlayer, null);
    }

    public void updatePlayerTeam(@NotNull Player playerToMove, @NotNull Player aDestTeamPlayer, @Nullable Consumer<Result> action) {
        Preconditions.checkNotNull((Object)playerToMove, (Object)"Player to move is null.");
        Preconditions.checkNotNull((Object)aDestTeamPlayer, (Object)"Destination player (representing destination team) is null.");
        this.callAfterLoad(playerToMove, aDestTeamPlayer, (DatabaseManager ds) -> ds.updatePlayerTeam(playerToMove, aDestTeamPlayer), action);
    }

    public void updatePlayerTeam(@NotNull UUID playerToMove, @NotNull UUID aDestTeamPlayer) {
        this.updatePlayerTeam(playerToMove, aDestTeamPlayer, null);
    }

    public void updatePlayerTeam(@NotNull UUID playerToMove, @NotNull UUID aDestTeamPlayer, @Nullable Consumer<Result> action) {
        Preconditions.checkNotNull((Object)playerToMove, (Object)"Player to move is null.");
        Preconditions.checkNotNull((Object)aDestTeamPlayer, (Object)"Destination player (representing destination team) is null.");
        this.callAfterLoad(playerToMove, aDestTeamPlayer, (DatabaseManager ds) -> ds.updatePlayerTeam(playerToMove, aDestTeamPlayer), action);
    }

    public void movePlayerInNewTeam(@NotNull Player playerToMove) {
        this.movePlayerInNewTeam(playerToMove, null);
    }

    public void movePlayerInNewTeam(@NotNull Player playerToMove, @Nullable Consumer<ObjectResult<@NotNull TeamProgression>> action) {
        this.callAfterLoad(playerToMove, (DatabaseManager ds) -> ds.movePlayerInNewTeam(playerToMove), action);
    }

    public void movePlayerInNewTeam(@NotNull UUID playerToMove) {
        this.movePlayerInNewTeam(playerToMove, null);
    }

    public void movePlayerInNewTeam(@NotNull UUID playerToMove, @Nullable Consumer<ObjectResult<@NotNull TeamProgression>> action) {
        this.callAfterLoad(playerToMove, (DatabaseManager ds) -> ds.movePlayerInNewTeam(playerToMove), action);
    }

    public void unregisterOfflinePlayer(@NotNull OfflinePlayer player) throws IllegalStateException {
        this.unregisterOfflinePlayer(player, null);
    }

    public void unregisterOfflinePlayer(@NotNull OfflinePlayer player, @Nullable Consumer<Result> action) throws IllegalStateException {
        this.callSyncIfNotNull(UltimateAdvancementAPI.getMain().getDatabaseManager().unregisterOfflinePlayer(player), action);
    }

    public void unregisterOfflinePlayer(@NotNull UUID uuid) throws IllegalStateException {
        this.unregisterOfflinePlayer(uuid, null);
    }

    public void unregisterOfflinePlayer(@NotNull UUID uuid, @Nullable Consumer<Result> action) throws IllegalStateException {
        this.callSyncIfNotNull(UltimateAdvancementAPI.getMain().getDatabaseManager().unregisterOfflinePlayer(uuid), action);
    }

    public void updatePlayerName(@NotNull Player player) {
        this.updatePlayerName(player, null);
    }

    public void updatePlayerName(@NotNull Player player, @Nullable Consumer<Result> action) {
        this.callSyncIfNotNull(UltimateAdvancementAPI.getMain().getDatabaseManager().updatePlayerName(player), action);
    }

    public void isUnredeemed(@NotNull Advancement advancement, @NotNull Player player, @NotNull @NotNull Consumer<ObjectResult<@NotNull Boolean>> action) {
        this.isUnredeemed(advancement, AdvancementUtils.uuidFromPlayer(player), action);
    }

    public void isUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid, @NotNull @NotNull Consumer<ObjectResult<@NotNull Boolean>> action) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        Preconditions.checkNotNull(action, (Object)"Consumer is null.");
        if (!advancement.isGranted(uuid)) {
            action.accept(new ObjectResult<Boolean>(false));
            return;
        }
        this.callAfterLoad(uuid, (DatabaseManager ds) -> ds.isUnredeemed(advancement.getKey(), uuid), action);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull Player player) throws NotGrantedException {
        this.setUnredeemed(advancement, player, true);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull Player player, @Nullable Consumer<Result> action) throws NotGrantedException {
        this.setUnredeemed(advancement, player, true, action);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull Player player, boolean giveRewards) throws NotGrantedException {
        this.setUnredeemed(advancement, AdvancementUtils.uuidFromPlayer(player), giveRewards);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull Player player, boolean giveRewards, @Nullable Consumer<Result> action) throws NotGrantedException {
        this.setUnredeemed(advancement, AdvancementUtils.uuidFromPlayer(player), giveRewards, action);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid) throws NotGrantedException {
        this.setUnredeemed(advancement, uuid, true);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid, @Nullable Consumer<Result> action) throws NotGrantedException {
        this.setUnredeemed(advancement, uuid, true, action);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid, boolean giveRewards) throws NotGrantedException {
        this.setUnredeemed(advancement, uuid, giveRewards, null);
    }

    public void setUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid, boolean giveRewards, @Nullable Consumer<Result> action) throws NotGrantedException {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        if (!advancement.isGranted(uuid)) {
            throw new NotGrantedException();
        }
        this.callAfterLoad(uuid, (DatabaseManager ds) -> ds.setUnredeemed(advancement.getKey(), giveRewards, uuid), action);
    }

    public void unsetUnredeemed(@NotNull Advancement advancement, @NotNull Player player) {
        this.unsetUnredeemed(advancement, player, null);
    }

    public void unsetUnredeemed(@NotNull Advancement advancement, @NotNull Player player, @Nullable Consumer<Result> action) {
        this.unsetUnredeemed(advancement, AdvancementUtils.uuidFromPlayer(player), action);
    }

    public void unsetUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid) {
        this.unsetUnredeemed(advancement, uuid, null);
    }

    public void unsetUnredeemed(@NotNull Advancement advancement, @NotNull UUID uuid, @Nullable Consumer<Result> action) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        this.callAfterLoad(uuid, (DatabaseManager ds) -> ds.unsetUnredeemed(advancement.getKey(), uuid), action);
    }

    public boolean isLoaded(@NotNull Player player) {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().isLoaded(player);
    }

    public boolean isLoaded(@NotNull OfflinePlayer player) {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().isLoaded(player);
    }

    public boolean isLoaded(@NotNull UUID uuid) {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().isLoaded(uuid);
    }

    public void loadOfflinePlayer(@NotNull OfflinePlayer player) {
        this.loadOfflinePlayer(AdvancementUtils.uuidFromPlayer(player));
    }

    public void loadOfflinePlayer(@NotNull UUID uuid) {
        this.loadOfflinePlayer(uuid, CacheFreeingOption.MANUAL(this.plugin), null);
    }

    public void loadOfflinePlayer(@NotNull OfflinePlayer player, @Nullable @Nullable Consumer<ObjectResult<@Nullable TeamProgression>> action) {
        this.loadOfflinePlayer(AdvancementUtils.uuidFromPlayer(player), action);
    }

    public void loadOfflinePlayer(@NotNull UUID uuid, @Nullable @Nullable Consumer<ObjectResult<@Nullable TeamProgression>> action) {
        this.loadOfflinePlayer(uuid, CacheFreeingOption.DONT_CACHE(), action);
    }

    public void loadOfflinePlayer(@NotNull OfflinePlayer player, @NotNull CacheFreeingOption option, @Nullable @Nullable Consumer<ObjectResult<@Nullable TeamProgression>> action) {
        this.loadOfflinePlayer(AdvancementUtils.uuidFromPlayer(player), option, action);
    }

    public void loadOfflinePlayer(@NotNull UUID uuid, @NotNull CacheFreeingOption option, @Nullable @Nullable Consumer<ObjectResult<@Nullable TeamProgression>> action) {
        this.callSyncIfNotNull(UltimateAdvancementAPI.getMain().getDatabaseManager().loadOfflinePlayer(uuid, option), action);
    }

    public boolean isOfflinePlayerLoaded(@NotNull OfflinePlayer player) {
        return this.isOfflinePlayerLoaded(AdvancementUtils.uuidFromPlayer(player));
    }

    @Contract(pure=true, value="null -> false")
    public boolean isOfflinePlayerLoaded(UUID uuid) {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().isOfflinePlayerLoaded(uuid, this.plugin);
    }

    public int getLoadingRequestsAmount(@NotNull Player player, @NotNull CacheFreeingOption.Option type) {
        return this.getLoadingRequestsAmount(AdvancementUtils.uuidFromPlayer(player), type);
    }

    public int getLoadingRequestsAmount(@NotNull OfflinePlayer offlinePlayer, @NotNull CacheFreeingOption.Option type) {
        return this.getLoadingRequestsAmount(AdvancementUtils.uuidFromPlayer(offlinePlayer), type);
    }

    public int getLoadingRequestsAmount(@NotNull UUID uuid, @NotNull CacheFreeingOption.Option type) {
        return UltimateAdvancementAPI.getMain().getDatabaseManager().getLoadingRequestsAmount(this.plugin, uuid, type);
    }

    public void unloadOfflinePlayer(@NotNull OfflinePlayer player) {
        this.unloadOfflinePlayer(AdvancementUtils.uuidFromPlayer(player));
    }

    public void unloadOfflinePlayer(@NotNull UUID uuid) {
        UltimateAdvancementAPI.getMain().getDatabaseManager().unloadOfflinePlayer(uuid, this.plugin);
    }

    public void getStoredPlayerName(@NotNull OfflinePlayer player, @NotNull Consumer<ObjectResult<@Nullable String>> action) {
        this.getStoredPlayerName(AdvancementUtils.uuidFromPlayer(player), action);
    }

    public void getStoredPlayerName(@NotNull UUID uuid, @NotNull Consumer<ObjectResult<@Nullable String>> action) {
        Preconditions.checkNotNull(action, (Object)"Consumer is null.");
        UltimateAdvancementAPI.getMain().getDatabaseManager().getStoredPlayerName(uuid).thenAccept(s -> AdvancementUtils.runSync(this.plugin, () -> action.accept((ObjectResult<String>)s)));
    }

    private <T extends Result> void callAfterLoad(@NotNull Player player, @NotNull Function<DatabaseManager, CompletableFuture<T>> internalAction, @Nullable Consumer<T> action) {
        this.callAfterLoad(AdvancementUtils.uuidFromPlayer(player), internalAction, action);
    }

    private <T extends Result> void callAfterLoad(@NotNull UUID uuid, @NotNull Function<DatabaseManager, CompletableFuture<T>> internalAction, @Nullable Consumer<T> action) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        DatabaseManager ds = UltimateAdvancementAPI.getMain().getDatabaseManager();
        ds.loadOfflinePlayer(uuid, CacheFreeingOption.MANUAL(this.plugin)).thenAccept(t1 -> {
            CompletableFuture c;
            if (t1.isExceptionOccurred()) {
                new RuntimeException("An exception occurred while loading user " + String.valueOf(uuid) + ":", t1.getOccurredException()).printStackTrace();
                return;
            }
            try {
                c = (CompletableFuture)internalAction.apply(ds);
            }
            catch (Exception t) {
                new RuntimeException("An exception occurred while calling API method:", t).printStackTrace();
                ds.unloadOfflinePlayer(uuid, this.plugin);
                return;
            }
            c.thenAccept(b -> {
                if (action != null) {
                    AdvancementUtils.runSync(this.plugin, () -> {
                        try {
                            if (this.plugin.isEnabled()) {
                                action.accept(b);
                            }
                        }
                        catch (Exception t) {
                            new RuntimeException("An exception occurred while calling " + this.plugin.getName() + "'s Consumer:", t).printStackTrace();
                        }
                        finally {
                            ds.unloadOfflinePlayer(uuid, this.plugin);
                        }
                    });
                } else {
                    ds.unloadOfflinePlayer(uuid, this.plugin);
                }
            });
        });
    }

    private <T extends Result> void callAfterLoad(@NotNull Player player1, @NotNull Player player2, @NotNull Function<DatabaseManager, CompletableFuture<T>> internalAction, @Nullable Consumer<T> action) {
        this.callAfterLoad(Objects.requireNonNull(player1, "Player1 is null.").getUniqueId(), Objects.requireNonNull(player2, "Player2 is null.").getUniqueId(), internalAction, action);
    }

    private <T extends Result> void callAfterLoad(@NotNull UUID uuid1, @NotNull UUID uuid2, @NotNull Function<DatabaseManager, CompletableFuture<T>> internalAction, @Nullable Consumer<T> action) {
        DatabaseManager ds = UltimateAdvancementAPI.getMain().getDatabaseManager();
        CacheFreeingOption cacheFreeingOption = CacheFreeingOption.MANUAL(this.plugin);
        ds.loadOfflinePlayer(uuid1, cacheFreeingOption).thenAccept(t1 -> {
            if (t1.isExceptionOccurred()) {
                new RuntimeException("An exception occurred while loading 1st user " + String.valueOf(uuid1) + ":", t1.getOccurredException()).printStackTrace();
            } else {
                ds.loadOfflinePlayer(uuid2, cacheFreeingOption).thenAccept(t2 -> {
                    CompletableFuture c;
                    if (t2.isExceptionOccurred()) {
                        new RuntimeException("An exception occurred while loading 2nd user " + String.valueOf(uuid2) + ":", t2.getOccurredException()).printStackTrace();
                        ds.unloadOfflinePlayer(uuid1, this.plugin);
                        return;
                    }
                    try {
                        c = (CompletableFuture)internalAction.apply(ds);
                    }
                    catch (Exception t) {
                        new RuntimeException("An exception occurred while calling API method:", t).printStackTrace();
                        ds.unloadOfflinePlayer(uuid1, this.plugin);
                        ds.unloadOfflinePlayer(uuid2, this.plugin);
                        return;
                    }
                    c.thenAccept(b -> {
                        if (action != null) {
                            AdvancementUtils.runSync(this.plugin, () -> {
                                try {
                                    if (this.plugin.isEnabled()) {
                                        action.accept(b);
                                    }
                                }
                                catch (Exception t) {
                                    new RuntimeException("An exception occurred while calling " + this.plugin.getName() + "'s Consumer:", t).printStackTrace();
                                }
                                finally {
                                    ds.unloadOfflinePlayer(uuid1, this.plugin);
                                    ds.unloadOfflinePlayer(uuid2, this.plugin);
                                }
                            });
                        } else {
                            ds.unloadOfflinePlayer(uuid1, this.plugin);
                            ds.unloadOfflinePlayer(uuid2, this.plugin);
                        }
                    });
                });
            }
        });
    }

    private <T extends Result> void callSyncIfNotNull(@NotNull CompletableFuture<T> completableFuture, @Nullable Consumer<T> action) {
        if (action != null) {
            completableFuture.thenAccept(t -> AdvancementUtils.runSync(this.plugin, () -> action.accept(t)));
        }
    }

    private UltimateAdvancementAPI() {
        throw new UnsupportedOperationException();
    }
}

