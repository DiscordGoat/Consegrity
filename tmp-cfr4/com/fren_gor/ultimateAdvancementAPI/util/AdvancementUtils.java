/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ComponentBuilder
 *  net.md_5.bungee.api.chat.ComponentBuilder$FormatRetention
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.util;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.AsyncExecutionException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotLoadedException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.VanillaAdvancementDisablerWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.PacketPlayOutAdvancementsWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancementUtils {
    public static final MinecraftKeyWrapper ROOT_KEY;
    public static final MinecraftKeyWrapper NOTIFICATION_KEY;
    private static final String ADV_DESCRIPTION = "\n\u00a77A notification.";
    private static final AdvancementWrapper ROOT;

    public static void displayToast(@NotNull Player player, @NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Preconditions.checkNotNull((Object)icon, (Object)"Icon is null.");
        Preconditions.checkNotNull((Object)title, (Object)"Title is null.");
        Preconditions.checkNotNull((Object)((Object)frame), (Object)"AdvancementFrameType is null.");
        Preconditions.checkArgument((icon.getType() != Material.AIR ? 1 : 0) != 0, (Object)"ItemStack is air.");
        try {
            AdvancementDisplayWrapper display = AdvancementDisplayWrapper.craft(icon, title, ADV_DESCRIPTION, frame.getNMSWrapper(), 1.0f, 0.0f, true, false, false);
            AdvancementWrapper notification = AdvancementWrapper.craftBaseAdvancement(NOTIFICATION_KEY, ROOT, display, 1);
            PacketPlayOutAdvancementsWrapper.craftSendPacket(Map.of(ROOT, 1, notification, 1)).sendTo(player);
            PacketPlayOutAdvancementsWrapper.craftRemovePacket(Set.of(ROOT_KEY, NOTIFICATION_KEY)).sendTo(player);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static void displayToastDuringUpdate(@NotNull Player player, @NotNull Advancement advancement) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkArgument((boolean)advancement.isValid(), (Object)"Advancement isn't valid.");
        AdvancementDisplay display = advancement.getDisplay();
        MinecraftKeyWrapper keyWrapper = AdvancementUtils.getUniqueKey(advancement.getAdvancementTab()).getNMSWrapper();
        try {
            AdvancementDisplayWrapper displayWrapper = AdvancementDisplayWrapper.craft(display.getIcon(), display.getTitle(), ADV_DESCRIPTION, display.getFrame().getNMSWrapper(), 0.0f, 0.0f, true, false, false);
            AdvancementWrapper advWrapper = AdvancementWrapper.craftBaseAdvancement(keyWrapper, advancement.getNMSWrapper(), displayWrapper, 1);
            PacketPlayOutAdvancementsWrapper.craftSendPacket(Map.of(advWrapper, 1)).sendTo(player);
            PacketPlayOutAdvancementsWrapper.craftRemovePacket(Set.of(keyWrapper)).sendTo(player);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private static AdvancementKey getUniqueKey(@NotNull AdvancementTab tab) {
        AdvancementKey key;
        String namespace = tab.getNamespace();
        StringBuilder builder = new StringBuilder("i");
        while (tab.getAdvancement(key = new AdvancementKey(namespace, builder.toString())) != null) {
            builder.append('i');
        }
        return key;
    }

    public static void disableVanillaAdvancements() throws Exception {
        VanillaAdvancementDisablerWrapper.disableVanillaAdvancements(true, false);
    }

    public static void disableVanillaRecipeAdvancements() throws Exception {
        VanillaAdvancementDisablerWrapper.disableVanillaAdvancements(false, true);
    }

    @NotNull
    public static BaseComponent[] fromStringList(@NotNull List<String> list) {
        return AdvancementUtils.fromStringList(null, list);
    }

    @NotNull
    public static BaseComponent[] fromStringList(@Nullable String title, @NotNull List<String> list) {
        Preconditions.checkNotNull(list);
        ComponentBuilder builder = new ComponentBuilder();
        if (title != null) {
            builder.append(TextComponent.fromLegacyText((String)title), ComponentBuilder.FormatRetention.NONE);
            if (list.isEmpty()) {
                return builder.create();
            }
            builder.append("\n", ComponentBuilder.FormatRetention.NONE);
        } else if (list.isEmpty()) {
            return builder.create();
        }
        int i = 0;
        for (String s : list) {
            builder.append(TextComponent.fromLegacyText((String)s), ComponentBuilder.FormatRetention.NONE);
            if (++i >= list.size()) continue;
            builder.append("\n", ComponentBuilder.FormatRetention.NONE);
        }
        return builder.create();
    }

    public static boolean startsWithEmptyLine(@NotNull String text) {
        Preconditions.checkNotNull((Object)text);
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\u00a7') {
                ++i;
                continue;
            }
            return c == '\n';
        }
        return false;
    }

    @Contract(value="_ -> param1")
    public static int validateProgressionValue(int progression) {
        if (progression < 0) {
            throw new IllegalArgumentException("Progression value cannot be < 0");
        }
        return progression;
    }

    public static void validateProgressionValueStrict(int progression, int maxProgression) {
        AdvancementUtils.validateProgressionValue(progression);
        if (progression > maxProgression) {
            throw new IllegalArgumentException("Progression value cannot be greater than the maximum progression (" + maxProgression + ")");
        }
    }

    public static void validateIncrement(int increment) {
        if (increment <= 0) {
            throw new IllegalArgumentException("Increment cannot be zero or less.");
        }
    }

    @Contract(value="null -> fail; !null -> param1")
    public static TeamProgression validateTeamProgression(TeamProgression pro) {
        Preconditions.checkNotNull((Object)pro, (Object)"TeamProgression is null.");
        Preconditions.checkArgument((boolean)pro.isValid(), (Object)"Invalid TeamProgression.");
        return pro;
    }

    public static void checkTeamProgressionNotNull(TeamProgression progression) {
        if (progression == null) {
            throw new UserNotLoadedException();
        }
    }

    public static void checkTeamProgressionNotNull(TeamProgression progression, UUID uuid) {
        if (progression == null) {
            throw new UserNotLoadedException(uuid);
        }
    }

    public static void checkSync() {
        if (!Bukkit.isPrimaryThread()) {
            throw new AsyncExecutionException("Illegal async method call. This method can be called only from the main thread.");
        }
    }

    public static void runSync(@NotNull AdvancementMain main, @NotNull Runnable runnable) {
        AdvancementUtils.runSync(main.getOwningPlugin(), runnable);
    }

    public static void runSync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        AdvancementUtils.runSync(plugin, 1L, runnable);
    }

    public static void runSync(@NotNull AdvancementMain main, long delay, @NotNull Runnable runnable) {
        AdvancementUtils.runSync(main.getOwningPlugin(), delay, runnable);
    }

    public static void runSync(@NotNull Plugin plugin, long delay, @NotNull Runnable runnable) {
        Preconditions.checkNotNull((Object)plugin, (Object)"Plugin is null.");
        Preconditions.checkNotNull((Object)runnable, (Object)"Runnable is null.");
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
    }

    @NotNull
    public static UUID uuidFromPlayer(@NotNull Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        return player.getUniqueId();
    }

    @NotNull
    public static UUID uuidFromPlayer(@NotNull OfflinePlayer player) {
        Preconditions.checkNotNull((Object)player, (Object)"OfflinePlayer is null.");
        return player.getUniqueId();
    }

    @NotNull
    public static TeamProgression progressionFromPlayer(@NotNull Player player, @NotNull Advancement advancement) {
        return AdvancementUtils.progressionFromPlayer(player, advancement.getAdvancementTab());
    }

    @NotNull
    public static TeamProgression progressionFromUUID(@NotNull UUID uuid, @NotNull Advancement advancement) {
        return AdvancementUtils.progressionFromUUID(uuid, advancement.getAdvancementTab());
    }

    @NotNull
    public static TeamProgression progressionFromPlayer(@NotNull Player player, @NotNull AdvancementTab tab) {
        return AdvancementUtils.progressionFromUUID(AdvancementUtils.uuidFromPlayer(player), tab);
    }

    @NotNull
    public static TeamProgression progressionFromUUID(@NotNull UUID uuid, @NotNull AdvancementTab tab) {
        Preconditions.checkNotNull((Object)uuid, (Object)"UUID is null.");
        return tab.getDatabaseManager().getTeamProgression(uuid);
    }

    private AdvancementUtils() {
        throw new UnsupportedOperationException("Utility class.");
    }

    static {
        try {
            ROOT_KEY = MinecraftKeyWrapper.craft("com.fren_gor", "root");
            NOTIFICATION_KEY = MinecraftKeyWrapper.craft("com.fren_gor", "notification");
            AdvancementDisplayWrapper display = AdvancementDisplayWrapper.craft(new ItemStack(Material.GRASS_BLOCK), "\u00a7f\u00a7lNotifications\u00a71\u00a72\u00a73\u00a74\u00a75\u00a76\u00a77\u00a78\u00a79\u00a70", "\u00a77Notification page.\n\u00a77Close and reopen advancements to hide.", AdvancementFrameTypeWrapper.TASK, 0.0f, 0.0f, "textures/block/stone.png");
            ROOT = AdvancementWrapper.craftRootAdvancement(ROOT_KEY, display, 1);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

