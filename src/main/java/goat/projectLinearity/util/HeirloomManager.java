package goat.projectLinearity.util;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages gild values for special heirloom items.
 */
public class HeirloomManager {
    private static HeirloomManager instance;
    private final NamespacedKey gildKey;
    private final NamespacedKey maxGildKey;

    private HeirloomManager(JavaPlugin plugin) {
        this.gildKey = new NamespacedKey(plugin, "heirloom_gild");
        this.maxGildKey = new NamespacedKey(plugin, "heirloom_gild_max");
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new HeirloomManager(plugin);
        }
    }

    public static HeirloomManager getInstance() {
        return instance;
    }

    public boolean isHeirloom(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(maxGildKey, PersistentDataType.INTEGER);
    }

    public int getGild(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer val = meta.getPersistentDataContainer().get(gildKey, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public int getMaxGild(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer val = meta.getPersistentDataContainer().get(maxGildKey, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public void setGild(ItemStack item, int amount) {
        int max = getMaxGild(item);
        if (max <= 0) {
            max = Math.max(amount, 0);
        }
        setGild(item, amount, max);
    }

    public void setGild(ItemStack item, int amount, int max) {
        if (item == null) return;
        if (amount < 0) amount = 0;
        if (amount > max) amount = max;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(gildKey, PersistentDataType.INTEGER, amount);
        data.set(maxGildKey, PersistentDataType.INTEGER, max);
        item.setItemMeta(meta);
        updateLore(item, amount, max);
        updateDurabilityBar(item, amount, max);
    }

    public void addGild(ItemStack item, int amount) {
        int current = getGild(item);
        int max = getMaxGild(item);
        setGild(item, current + amount, max);
    }

    private void updateLore(ItemStack item, int amount, int max) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String line = ChatColor.GOLD + "Gild: " + amount + "/" + max;
        int index = -1;
        for (int i = 0; i < lore.size(); i++) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped.startsWith("Gild:")) {
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

    private void updateDurabilityBar(ItemStack item, int amount, int max) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int vanillaMax = item.getType().getMaxDurability();
            if (vanillaMax > 0 && max > 0) {
                int newDamage = vanillaMax - (int)Math.round(((double) amount / max) * vanillaMax);
                if (newDamage < 0) newDamage = 0;
                if (newDamage > vanillaMax) newDamage = vanillaMax;
                damageable.setDamage(newDamage);
                item.setItemMeta(meta);
            }
        }
    }
}

