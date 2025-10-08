package goat.projectLinearity.libs;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replaces vanilla durability with a persistent custom implementation that also supports
 * golden durability supplied by the heirloom system.
 */
public final class CustomDurabilityManager implements Listener {

    private static CustomDurabilityManager instance;

    private final NamespacedKey currentKey;
    private final NamespacedKey maxKey;
    private final NamespacedKey goldenKey;
    private final NamespacedKey goldenMaxKey;

    private CustomDurabilityManager(JavaPlugin plugin) {
        this.currentKey = new NamespacedKey(plugin, "custom_durability");
        this.maxKey = new NamespacedKey(plugin, "custom_max_durability");
        this.goldenKey = new NamespacedKey(plugin, "golden_durability");
        this.goldenMaxKey = new NamespacedKey(plugin, "golden_max_durability");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new CustomDurabilityManager(plugin);
        }
    }

    public static CustomDurabilityManager getInstance() {
        return Objects.requireNonNull(instance, "CustomDurabilityManager not initialised");
    }

    public boolean isTracked(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(currentKey, PersistentDataType.INTEGER) && data.has(maxKey, PersistentDataType.INTEGER);
    }

    public void ensureTracking(ItemStack item) {
        if (item == null) return;
        if (item.getType().getMaxDurability() <= 0) return;
        if (isTracked(item)) {
            // Refresh the visual appearance in case lore/damage was lost (e.g. through reload).
            int current = getCurrentDurability(item);
            int max = getMaxDurability(item);
            updateLore(item, current, max);
            updateDurabilityBar(item, current, max);
            return;
        }

        int max = Math.max(1, item.getType().getMaxDurability());
        int current = max;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            current = Math.max(0, max - damageable.getDamage());
        }
        setCustomDurability(item, current, max);
    }

    public void setCustomDurability(ItemStack item, int current, int max) {
        if (item == null) return;
        if (max <= 0) return;
        if (current < 0) current = 0;
        if (current > max) current = max;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(currentKey, PersistentDataType.INTEGER, current);
        data.set(maxKey, PersistentDataType.INTEGER, max);
        item.setItemMeta(meta);
        updateLore(item, current, max);
        updateDurabilityBar(item, current, max);
    }

    public void setMaxDurability(ItemStack item, int max) {
        if (item == null || max <= 0) return;
        ensureTracking(item);
        int current = Math.min(getCurrentDurability(item), max);
        setCustomDurability(item, current, max);
    }

    public int getCurrentDurability(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer stored = data.get(currentKey, PersistentDataType.INTEGER);
        if (stored != null) {
            return Math.max(0, stored);
        }
        int max = Math.max(1, item.getType().getMaxDurability());
        if (meta instanceof Damageable damageable) {
            return Math.max(0, max - damageable.getDamage());
        }
        return max;
    }

    public int getMaxDurability(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer stored = data.get(maxKey, PersistentDataType.INTEGER);
        if (stored != null && stored > 0) {
            return stored;
        }
        return Math.max(1, item.getType().getMaxDurability());
    }

    public boolean hasGoldenDurability(ItemStack item) {
        return getGoldenDurability(item) > 0;
    }

    public int getGoldenDurability(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer stored = data.get(goldenKey, PersistentDataType.INTEGER);
        return stored != null ? Math.max(0, stored) : 0;
    }

    public int getGoldenMaxDurability(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer stored = data.get(goldenMaxKey, PersistentDataType.INTEGER);
        return stored != null ? Math.max(0, stored) : 0;
    }

    public void setGoldenDurability(ItemStack item, int amount) {
        if (item == null || amount <= 0) {
            removeGoldenDurability(item);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(goldenKey, PersistentDataType.INTEGER, amount);
        data.set(goldenMaxKey, PersistentDataType.INTEGER, amount);
        item.setItemMeta(meta);

        int current = getCurrentDurability(item);
        int max = getMaxDurability(item);
        updateLore(item, current, max);
        updateDurabilityBar(item, current, max);
    }

    private void setGoldenCurrent(ItemStack item, int amount) {
        if (item == null) return;
        if (amount <= 0) {
            removeGoldenDurability(item);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(goldenKey, PersistentDataType.INTEGER, amount);
        item.setItemMeta(meta);

        int current = getCurrentDurability(item);
        int max = getMaxDurability(item);
        updateLore(item, current, max);
        updateDurabilityBar(item, current, max);
    }

    public void removeGoldenDurability(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.remove(goldenKey);
        data.remove(goldenMaxKey);
        item.setItemMeta(meta);

        int current = getCurrentDurability(item);
        int max = getMaxDurability(item);
        updateLore(item, current, max);
        updateDurabilityBar(item, current, max);
    }

    public void repair(ItemStack item, int amount) {
        if (item == null || amount <= 0) return;
        ensureTracking(item);
        int max = getMaxDurability(item);
        int current = Math.min(max, getCurrentDurability(item) + amount);
        setCustomDurability(item, current, max);
    }

    public void repairFully(ItemStack item) {
        if (item == null) return;
        ensureTracking(item);
        int max = getMaxDurability(item);
        setCustomDurability(item, max, max);
    }

    public void applyDamage(Player player, ItemStack item, int amount) {
        if (item == null || amount <= 0) return;
        if (item.getType().getMaxDurability() <= 0) return;
        ensureTracking(item);

        int golden = getGoldenDurability(item);
        if (golden > 0) {
            int remaining = golden - amount;
            if (remaining > 0) {
                setGoldenCurrent(item, remaining);
            } else {
                removeGoldenDurability(item);
                repairFully(item);
            }
            return;
        }

        int max = getMaxDurability(item);
        int current = getCurrentDurability(item);
        int updated = Math.max(0, current - amount);
        setCustomDurability(item, updated, max);

        if (updated <= 0) {
            if (player != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
            item.setAmount(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().getMaxDurability() <= 0) return;
        event.setCancelled(true);
        applyDamage(event.getPlayer(), item, event.getDamage());
    }

    private void updateLore(ItemStack item, int current, int max) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String line;
        if (hasGoldenDurability(item)) {
            int gCur = getGoldenDurability(item);
            int gMax = getGoldenMaxDurability(item);
            line = ChatColor.GOLD + "Golden Durability: " + gCur + "/" + gMax;
        } else {
            line = ChatColor.GRAY + "Durability: " + current + "/" + max;
        }

        int index = -1;
        for (int i = 0; i < lore.size(); i++) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped.startsWith("Durability:") || stripped.startsWith("Golden Durability:")) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            lore.set(index, line);
        } else {
            lore.add(line);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void updateDurabilityBar(ItemStack item, int current, int max) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int vanillaMax = item.getType().getMaxDurability();
        if (vanillaMax <= 0) return;

        if (hasGoldenDurability(item)) {
            damageable.setDamage(0);
        } else {
            double ratio = max <= 0 ? 0.0 : 1.0 - ((double) current / max);
            int newDamage = (int) Math.round(ratio * vanillaMax);
            if (newDamage < 0) newDamage = 0;
            if (newDamage > vanillaMax) newDamage = vanillaMax;
            damageable.setDamage(newDamage);
        }
        item.setItemMeta(meta);
    }
}
