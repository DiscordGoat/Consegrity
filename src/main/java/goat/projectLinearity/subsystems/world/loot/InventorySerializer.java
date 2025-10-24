package goat.projectLinearity.subsystems.world.loot;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serialises and deserialises chest inventories to a compact YAML structure.
 * Format example:
 * size: 27
 * items:
 *   - slot: 0
 *     item: <ConfigurationSerializable ItemStack>
 */
final public class InventorySerializer {

    private InventorySerializer() {
    }

    public static void save(ItemStack[] contents, File file) throws IOException {
        if (contents == null) throw new IllegalArgumentException("contents");
        if (file == null) throw new IllegalArgumentException("file");

        YamlConfiguration config = new YamlConfiguration();
        config.set("size", contents.length);

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("slot", i);
            entry.put("item", item.clone());
            items.add(entry);
        }
        config.set("items", items);
        config.save(file);
    }

    static ItemStack[] load(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int size = Math.max(1, config.getInt("size", 27));
        ItemStack[] contents = new ItemStack[size];

        List<?> rawItems = config.getList("items");
        if (rawItems != null) {
            for (Object entryObj : rawItems) {
                if (!(entryObj instanceof Map<?, ?> rawMap)) continue;
                Object slotObj = rawMap.get("slot");
                if (!(slotObj instanceof Number number)) continue;
                int slot = number.intValue();
                if (slot < 0 || slot >= contents.length) continue;

                Object itemObj = rawMap.get("item");
                ItemStack item = null;
                if (itemObj instanceof ItemStack stack) {
                    item = stack.clone();
                } else if (itemObj instanceof Map<?, ?> serialized) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = convertToStringMap(serialized);
                    ItemStack deserialized = ItemStack.deserialize(cast);
                    if (deserialized != null) {
                        item = deserialized.clone();
                    }
                }
                contents[slot] = item;
            }
        }
        return contents;
    }

    private static Map<String, Object> convertToStringMap(Map<?, ?> map) {
        Map<String, Object> converted = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

    public static String sanitizeFileName(String candidate) {
        if (candidate == null) return null;
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) return null;
        String normalized = trimmed.replaceAll("[^A-Za-z0-9_-]", "");
        if (normalized.isEmpty()) return null;
        return normalized.toLowerCase(Locale.ROOT);
    }
}
