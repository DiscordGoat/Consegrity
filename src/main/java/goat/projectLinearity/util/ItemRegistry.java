package goat.projectLinearity.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Provides factory methods for all custom items used in the plugin and allows lookup by name.
 */
public final class ItemRegistry {

    private static final Map<String, Method> ITEM_METHODS = new HashMap<>();
    private static final Method SET_GLINT_OVERRIDE_METHOD;
    private static final boolean GLINT_OVERRIDE_PRIMITIVE;

    static {
        cacheItemMethods();
        Method method = null;
        boolean primitive = false;
        try {
            method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
        } catch (NoSuchMethodException ex) {
            try {
                method = ItemMeta.class.getMethod("setEnchantmentGlintOverride", boolean.class);
                primitive = true;
            } catch (NoSuchMethodException ignored) {
                method = null;
            }
        }
        SET_GLINT_OVERRIDE_METHOD = method;
        GLINT_OVERRIDE_PRIMITIVE = primitive;
    }

    private ItemRegistry() {
    }

    private static void cacheItemMethods() {
        for (Method method : ItemRegistry.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (!ItemStack.class.isAssignableFrom(method.getReturnType())) continue;
            if (method.getParameterCount() != 0) continue;
            method.setAccessible(true);
            String key = normalize(method.getName());
            ITEM_METHODS.putIfAbsent(key, method);
        }
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(input);
        return stripped == null ? "" : stripped.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
    }

    private static ItemStack invokeSupplier(Method method) {
        try {
            return (ItemStack) method.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    public static ItemStack getItemByName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return null;
        String normalized = normalize(itemName);
        if (normalized.isEmpty()) return null;

        Method direct = ITEM_METHODS.get(normalized);
        if (direct != null) {
            ItemStack item = invokeSupplier(direct);
            if (matches(item, normalized, direct.getName())) {
                return item;
            }
        }

        for (Method method : ITEM_METHODS.values()) {
            ItemStack item = invokeSupplier(method);
            if (matches(item, normalized, method.getName())) {
                return item;
            }
        }

        return null;
    }

    public static List<String> getItemNameSuggestions() {
        Set<String> suggestions = new LinkedHashSet<>();
        for (Method method : ITEM_METHODS.values()) {
            suggestions.add(methodToSuggestion(method.getName()));
            ItemStack item = invokeSupplier(method);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String display = ChatColor.stripColor(meta.getDisplayName());
                    if (display != null && !display.isEmpty()) {
                        suggestions.add(display.toLowerCase(Locale.ENGLISH).replace(' ', '_'));
                    }
                }
            }
        }
        return new ArrayList<>(suggestions);
    }

    private static boolean matches(ItemStack item, String normalized, String methodName) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String display = normalize(meta.getDisplayName());
            if (!display.isEmpty() && display.equals(normalized)) {
                return true;
            }
        }
        String reconstructed = normalize(methodName.startsWith("get") ? methodName.substring(3) : methodName);
        return !reconstructed.isEmpty() && reconstructed.equals(normalized);
    }

    private static String methodToSuggestion(String methodName) {
        String base = methodName.startsWith("get") ? methodName.substring(3) : methodName;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    public static ItemStack createCustomItem(
            Material material,
            String name,
            List<String> lore,
            int amount,
            boolean unbreakable,
            boolean addEnchantmentShimmer
    ) {
        ItemStack item = new ItemStack(material != null ? material : Material.STONE, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name);
            }
            meta.setLore(lore != null && !lore.isEmpty() ? new ArrayList<>(lore) : null);
            meta.setUnbreakable(unbreakable);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            if (addEnchantmentShimmer) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== Rosegold Materials =====

    public static ItemStack getRosegoldChunk() {
        return createCustomItem(
                Material.RAW_GOLD,
                ChatColor.LIGHT_PURPLE + "Rosegold Chunk",
                List.of(ChatColor.DARK_PURPLE + "Smithing Material"),
                1,
                false,
                true
        );
    }

    public static ItemStack getRosegoldIngot() {
        return createCustomItem(
                Material.GOLD_INGOT,
                ChatColor.LIGHT_PURPLE + "Rosegold Ingot",
                List.of(ChatColor.DARK_PURPLE + "Smithing Material"),
                1,
                false,
                true
        );
    }

    private static ItemStack createRosegoldItem(Material material, String displayName, Material referenceMaterial) {
        ItemStack item = createCustomItem(
                material,
                ChatColor.LIGHT_PURPLE + displayName,
                null,
                1,
                false,
                false
        );
        applyCustomDurability(item, referenceMaterial != null ? referenceMaterial.getMaxDurability() : 0);
        if (material == Material.GOLDEN_SWORD) {
            applyRosegoldSwordBonus(item);
        }
        return item;
    }

    public static void applyRosegoldSwordBonus(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_SWORD) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int currentLevel = meta.getEnchantLevel(Enchantment.SHARPNESS);
        if (currentLevel < 2) {
            meta.addEnchant(Enchantment.SHARPNESS, 2, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        setGlintOverride(meta, false);
        item.setItemMeta(meta);
    }

    public static boolean isRosegoldSword(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_SWORD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String stripped = ChatColor.stripColor(meta.getDisplayName());
        return stripped != null && stripped.equalsIgnoreCase("Rosegold Sword");
    }

    private static void setGlintOverride(ItemMeta meta, Boolean override) {
        if (meta == null) {
            return;
        }
        if (SET_GLINT_OVERRIDE_METHOD == null) {
            return;
        }
        try {
            if (GLINT_OVERRIDE_PRIMITIVE && override == null) {
                SET_GLINT_OVERRIDE_METHOD.invoke(meta, false);
            } else {
                SET_GLINT_OVERRIDE_METHOD.invoke(meta, override);
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private static void applyCustomDurability(ItemStack item, int maxDurability) {
        if (item == null || maxDurability <= 0) {
            return;
        }
        CustomDurabilityManager durabilityManager;
        try {
            durabilityManager = CustomDurabilityManager.getInstance();
        } catch (RuntimeException ex) {
            return;
        }
        durabilityManager.ensureTracking(item);
        durabilityManager.setMaxDurability(item, maxDurability);
        durabilityManager.setCustomDurability(item, maxDurability, maxDurability);
    }

    public static ItemStack getEnchantedBook() {
        return createCustomItem(
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Enchanted Book",
                Arrays.asList(ChatColor.DARK_PURPLE + "Smithing Item"),
                1,
                false,
                false
        );
    }

    // ===== Rosegold Gear =====

    public static ItemStack getRosegoldSword() {
        return createRosegoldItem(Material.GOLDEN_SWORD, "Rosegold Sword", Material.IRON_SWORD);
    }

    public static ItemStack getRosegoldPickaxe() {
        return createRosegoldItem(Material.GOLDEN_PICKAXE, "Rosegold Pickaxe", Material.IRON_PICKAXE);
    }

    public static ItemStack getRosegoldAxe() {
        return createRosegoldItem(Material.GOLDEN_AXE, "Rosegold Axe", Material.IRON_AXE);
    }

    public static ItemStack getRosegoldShovel() {
        return createRosegoldItem(Material.GOLDEN_SHOVEL, "Rosegold Shovel", Material.IRON_SHOVEL);
    }

    public static ItemStack getRosegoldHoe() {
        return createRosegoldItem(Material.GOLDEN_HOE, "Rosegold Hoe", Material.IRON_HOE);
    }

    public static ItemStack getRosegoldHelmet() {
        return createRosegoldItem(Material.GOLDEN_HELMET, "Rosegold Helmet", Material.IRON_HELMET);
    }

    public static ItemStack getRosegoldChestplate() {
        return createRosegoldItem(Material.GOLDEN_CHESTPLATE, "Rosegold Chestplate", Material.IRON_CHESTPLATE);
    }

    public static ItemStack getRosegoldLeggings() {
        return createRosegoldItem(Material.GOLDEN_LEGGINGS, "Rosegold Leggings", Material.IRON_LEGGINGS);
    }

    public static ItemStack getRosegoldBoots() {
        return createRosegoldItem(Material.GOLDEN_BOOTS, "Rosegold Boots", Material.IRON_BOOTS);
    }

    // ===== Heirlooms =====

    public static ItemStack getGoldenRing() {
        ItemStack item = createCustomItem(
                Material.LEATHER_CHESTPLATE,
                ChatColor.GOLD + "Golden Ring",
                Arrays.asList(ChatColor.DARK_PURPLE + "Smithing Item"),
                1,
                false,
                true
        );
        HeirloomManager mgr = HeirloomManager.getInstance();
        if (mgr != null) {
            mgr.setGild(item, 0, 100);
        }
        return item;
    }

    public static ItemStack getGoldenChalice() {
        ItemStack item = createCustomItem(
                Material.LEATHER_CHESTPLATE,
                ChatColor.GOLD + "Golden Chalice",
                Arrays.asList(ChatColor.DARK_PURPLE + "Smithing Item"),
                1,
                false,
                true
        );
        HeirloomManager mgr = HeirloomManager.getInstance();
        if (mgr != null) {
            mgr.setGild(item, 0, 250);
        }
        return item;
    }

    public static ItemStack getGoldenCrown() {
        ItemStack item = createCustomItem(
                Material.LEATHER_CHESTPLATE,
                ChatColor.GOLD + "Golden Crown",
                Arrays.asList(ChatColor.DARK_PURPLE + "Smithing Item"),
                1,
                false,
                true
        );
        HeirloomManager mgr = HeirloomManager.getInstance();
        if (mgr != null) {
            mgr.setGild(item, 0, 500);
        }
        return item;
    }
}
