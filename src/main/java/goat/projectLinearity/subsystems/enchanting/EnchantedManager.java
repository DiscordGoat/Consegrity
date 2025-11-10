package goat.projectLinearity.subsystems.enchanting;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Sound;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.function.Consumer;

/**
 * Applies and manages the custom Enchanted equipment tiers, including hidden enchantments,
 * glint behaviour, and Enchanted Book upgrades.
 */
public final class EnchantedManager implements Listener {
    private static final long GLINT_DURATION_TIER_I_MS = 5_000L;
    private static final long GLINT_DURATION_TIER_II_MS = 10_000L;
    private static final String LORE_PREFIX = ChatColor.LIGHT_PURPLE + "Enchanted ";

    private static final Set<Enchantment> SWORD_ENCHANTS = Set.of(
            Enchantment.SHARPNESS,
            Enchantment.SMITE,
            Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.IMPALING,
            Enchantment.LOOTING,
            Enchantment.UNBREAKING
    );
    private static final Set<Enchantment> TOOL_ENCHANTS = Set.of(
            Enchantment.EFFICIENCY,
            Enchantment.UNBREAKING
    );
    private static final Set<Enchantment> ARMOR_ENCHANTS = Set.of(
            Enchantment.PROTECTION,
            Enchantment.UNBREAKING,
            Enchantment.PROJECTILE_PROTECTION,
            Enchantment.FIRE_PROTECTION,
            Enchantment.BLAST_PROTECTION
    );
    private static final Set<Enchantment> FISHING_ROD_ENCHANTS = Set.of(
            Enchantment.UNBREAKING,
            Enchantment.LURE,
            Enchantment.LUCK_OF_THE_SEA
    );
    private static final Set<Enchantment> DEFAULT_ENCHANTS;
    private static final Method SET_GLINT_OVERRIDE_METHOD;
    private static final boolean GLINT_OVERRIDE_PRIMITIVE;

    static {
        Set<Enchantment> all = new HashSet<>();
        all.addAll(SWORD_ENCHANTS);
        all.addAll(TOOL_ENCHANTS);
        all.addAll(ARMOR_ENCHANTS);
        all.addAll(FISHING_ROD_ENCHANTS);
        DEFAULT_ENCHANTS = Collections.unmodifiableSet(all);
    }

    static {
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

    private final JavaPlugin plugin;
    private final NamespacedKey levelKey;
    private final NamespacedKey glintExpiryKey;
    private final NamespacedKey bookKey;
    private boolean glintOverrideInvokeFailureLogged;

    public EnchantedManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "enchanted_level");
        this.glintExpiryKey = new NamespacedKey(plugin, "enchanted_glint_expire");
        this.bookKey = new NamespacedKey(plugin, "enchanted_book");
    }

    public void start() {
        if (SET_GLINT_OVERRIDE_METHOD == null) {
            plugin.getLogger().warning(
                    "Server does not support ItemMeta#setEnchantmentGlintOverride; Enchanted tiers will always show a glint.");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTask(plugin, this::resetAllPlayerGlints);
    }

    /**
     * Applies the base enchanting tier (usually from an enchanting table interaction).
     */
    public void applyBaseEnchant(ItemStack item, int tier) {
        if (item == null || tier <= 0) return;
        int current = getEnchantedLevel(item);
        int newLevel = Math.max(current, Math.min(3, tier));
        applyTier(item, newLevel);
    }

    /**
     * Upgrades an item by one tier if possible, returning true when successful.
     */
    public boolean upgradeItem(ItemStack item) {
        if (item == null) return false;
        int current = getEnchantedLevel(item);
        if (current <= 0) {
            return false;
        }
        if (current >= 3) {
            return false;
        }
        applyTier(item, current + 1);
        return true;
    }

    /**
     * Sets the enchanted tier directly.
     */
    public void applyTier(ItemStack item, int tier) {
        if (item == null) return;
        tier = Math.max(0, Math.min(3, tier));
        int finalTier = tier;
        modifyMeta(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (finalTier == 0) {
                pdc.remove(levelKey);
                pdc.remove(glintExpiryKey);
                clearLore(meta);
                removeManagedEnchantments(meta, DEFAULT_ENCHANTS);
                setGlintOverride(meta, null);
            } else {
                pdc.set(levelKey, PersistentDataType.INTEGER, finalTier);
                updateLore(meta, finalTier);
                applyHiddenEnchantments(item.getType(), meta, finalTier);
                ensureHiddenFlags(meta);
                if (finalTier >= 3) {
                    setGlintOverride(meta, true);
                    pdc.remove(glintExpiryKey);
                } else {
                    setGlintOverride(meta, false);
                    pdc.remove(glintExpiryKey);
                }
            }
        });
        if (tier == 0 && ItemRegistry.isRosegoldSword(item)) {
            ItemRegistry.applyRosegoldSwordBonus(item);
        }
    }

    public int getEnchantedLevel(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer level = pdc.get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : Math.max(0, Math.min(3, level));
    }

    public boolean isCustomEnchantedBook(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(bookKey, PersistentDataType.BYTE)) {
            return true;
        }

        ItemStack template = ItemRegistry.getEnchantedBook();
        ItemMeta templateMeta = template.getItemMeta();
        if (templateMeta == null) {
            return false;
        }
        if (stack.getType() != template.getType()) {
            return false;
        }
        if (!meta.hasDisplayName() || !templateMeta.hasDisplayName()) {
            return false;
        }

        String display = meta.getDisplayName();
        String templateName = templateMeta.getDisplayName();
        boolean matchesName = display.equals(templateName)
                || ChatColor.stripColor(display).equalsIgnoreCase(ChatColor.stripColor(templateName));
        if (!matchesName) {
            return false;
        }

        tagBook(stack, meta);
        return true;
    }

    public ItemStack convertToCustomBook(ItemStack source) {
        if (source == null || source.getType() != Material.ENCHANTED_BOOK) return source;
        ItemMeta meta = source.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                String stripped = ChatColor.stripColor(line);
                if (stripped != null && stripped.equalsIgnoreCase("Smithing Item")) {
                    return source;
                }
            }
        }
        ItemStack custom = ItemRegistry.getEnchantedBook();
        custom.setAmount(source.getAmount());
        tagBook(custom);
        return custom;
    }

    public boolean isEnchantable(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        Material type = stack.getType();
        return type.getMaxDurability() > 0;
    }

    public boolean isGoldItem(ItemStack stack) {
        if (stack == null) return false;
        String name = stack.getType().name();
        return name.startsWith("GOLDEN_");
    }

    // --- Event handlers ---

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        int tier = getEnchantedLevel(item);
        if (tier <= 0 || tier >= 3) {
            return;
        }
        long duration = tier == 1 ? GLINT_DURATION_TIER_I_MS : GLINT_DURATION_TIER_II_MS;
        long expiry = System.currentTimeMillis() + duration;
        modifyMeta(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(glintExpiryKey, PersistentDataType.LONG, expiry);
            setGlintOverride(meta, true);
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> clearGlintIfExpired(item, expiry), ticksFromMillis(duration));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sanitizePlayerInventory(event.getPlayer());
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack.getType() == Material.ENCHANTED_BOOK) {
            ItemStack converted = convertToCustomBook(stack);
            event.getItem().setItemStack(converted);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() == Material.ENCHANTED_BOOK) {
            event.setCurrentItem(convertToCustomBook(current));
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() == Material.ENCHANTED_BOOK) {
            event.setCursor(convertToCustomBook(cursor));
        }

        cursor = event.getCursor();
        current = event.getCurrentItem();
        if (cursor != null && cursor.equals(ItemRegistry.getEnchantedBook()) && isEnchantable(current)) {
            event.setCancelled(true);
            if (!ensureUpgradeable(player, current)) {
                return;
            }
            upgradeItem(current);
            event.setCurrentItem(current);
            decrementCursor(event, player);
            notifyUpgrade(player, getEnchantedLevel(current));
            return;
        }

        if (current != null && cursor.equals(ItemRegistry.getEnchantedBook()) && isEnchantable(cursor)) {
            event.setCancelled(true);
            if (!ensureUpgradeable(player, cursor)) {
                return;
            }
            upgradeItem(cursor);
            event.setCursor(cursor);
            decrementSlotItem(event, player);
            notifyUpgrade(player, getEnchantedLevel(cursor));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack cursor = event.getOldCursor();
        if (cursor != null && cursor.getType() == Material.ENCHANTED_BOOK) {
            event.setCursor(convertToCustomBook(cursor));
        }
    }

    // --- Helpers ---

    private boolean ensureUpgradeable(Player player, ItemStack target) {
        if (target == null) return false;
        int level = getEnchantedLevel(target);
        if (level <= 0) {
            player.sendMessage(ChatColor.RED + "You must Enchant the item at a table first.");
            return false;
        }
        if (level >= 3) {
            player.sendMessage(ChatColor.RED + "That item is already Enchanted III.");
            return false;
        }
        return true;
    }

    private void decrementCursor(InventoryClickEvent event, Player player) {
        ItemStack cursor = event.getCursor();
        if (cursor == null) return;
        int amount = cursor.getAmount();
        if (amount <= 1) {
            event.setCursor(null);
        } else {
            cursor.setAmount(amount - 1);
            event.setCursor(cursor);
        }
        player.updateInventory();
    }

    private void decrementSlotItem(InventoryClickEvent event, Player player) {
        ItemStack current = event.getCurrentItem();
        if (current == null) return;
        int amount = current.getAmount();
        if (amount <= 1) {
            event.setCurrentItem(null);
        } else {
            current.setAmount(amount - 1);
            event.setCurrentItem(current);
        }
        player.updateInventory();
    }

    private void notifyUpgrade(Player player, int newLevel) {
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Enchanted level increased to " + roman(newLevel) + ".");
    }

    private void clearGlintIfExpired(ItemStack item, long expectedExpiry) {
        if (item == null) return;
        modifyMeta(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Long stored = pdc.get(glintExpiryKey, PersistentDataType.LONG);
            if (stored == null || stored > System.currentTimeMillis() || !stored.equals(expectedExpiry)) {
                return;
            }
            int tier = getEnchantedLevel(item);
            if (tier >= 3) {
                setGlintOverride(meta, true);
                return;
            }
            setGlintOverride(meta, false);
            pdc.remove(glintExpiryKey);
        });
    }

    private void updateLore(ItemMeta meta, int tier) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> {
            String stripped = ChatColor.stripColor(line);
            return stripped != null && stripped.toLowerCase(Locale.ENGLISH).startsWith("enchanted");
        });
        lore.add(LORE_PREFIX + roman(tier));
        meta.setLore(lore);
    }

    private void clearLore(ItemMeta meta) {
        if (!meta.hasLore()) return;
        List<String> lore = new ArrayList<>(meta.getLore());
        boolean removed = lore.removeIf(line -> {
            String stripped = ChatColor.stripColor(line);
            return stripped != null && stripped.toLowerCase(Locale.ENGLISH).startsWith("enchanted");
        });
        if (removed) {
            meta.setLore(lore.isEmpty() ? null : lore);
        }
    }

    private void ensureHiddenFlags(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        if (meta instanceof Damageable damageable && !damageable.isUnbreakable()) {
            // No-op, just ensuring damageable metadata is retained
        }
    }

    private void applyHiddenEnchantments(Material type, ItemMeta meta, int tier) {
        Set<Enchantment> toApply = enchantmentsFor(type);
        removeManagedEnchantments(meta, DEFAULT_ENCHANTS);
        boolean rosegoldSword = type == Material.GOLDEN_SWORD && isRosegoldSwordMeta(meta);
        for (Enchantment enchantment : toApply) {
            int level = tier;
            if (rosegoldSword && enchantment == Enchantment.SHARPNESS) {
                level = Math.min(5, tier + 2);
            }
            meta.addEnchant(enchantment, level, true);
        }
    }

    private void removeManagedEnchantments(ItemMeta meta, Set<Enchantment> enchants) {
        for (Enchantment enchantment : enchants) {
            if (meta.hasEnchant(enchantment)) {
                meta.removeEnchant(enchantment);
            }
        }
    }

    private Set<Enchantment> enchantmentsFor(Material type) {
        String name = type.name();
        if (name.endsWith("_SWORD")) {
            return SWORD_ENCHANTS;
        }
        if (name.endsWith("_PICKAXE")) {
            return TOOL_ENCHANTS;
        }
        if (name.endsWith("_AXE")) {
            return TOOL_ENCHANTS;
        }
        if (name.endsWith("_SHOVEL")) {
            return TOOL_ENCHANTS;
        }
        if (name.equals("FISHING_ROD")) {
            return FISHING_ROD_ENCHANTS;
        }
        if (isArmorMaterial(name)) {
            return ARMOR_ENCHANTS;
        }
        return DEFAULT_ENCHANTS;
    }

    private boolean isArmorMaterial(String name) {
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("ELYTRA")
                || name.equals("TURTLE_HELMET");
    }

    private void modifyMeta(ItemStack item, Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        consumer.accept(meta);
        item.setItemMeta(meta);
    }

    private void tagBook(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        tagBook(stack, meta);
    }

    private void tagBook(ItemStack stack, ItemMeta meta) {
        meta.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
    }

    private long ticksFromMillis(long millis) {
        return Math.max(1L, millis / 50L);
    }

    private void setGlintOverride(ItemMeta meta, Boolean override) {
        if (SET_GLINT_OVERRIDE_METHOD == null) {
            return;
        }
        try {
            if (GLINT_OVERRIDE_PRIMITIVE && override == null) {
                SET_GLINT_OVERRIDE_METHOD.invoke(meta, false);
            } else {
                SET_GLINT_OVERRIDE_METHOD.invoke(meta, override);
            }
        } catch (ReflectiveOperationException ex) {
            if (!glintOverrideInvokeFailureLogged) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to apply enchantment glint override; disabling custom glint control.", ex);
                glintOverrideInvokeFailureLogged = true;
            }
        }
    }

    private boolean isRosegoldSwordMeta(ItemMeta meta) {
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String stripped = ChatColor.stripColor(meta.getDisplayName());
        return stripped != null && stripped.equalsIgnoreCase("Rosegold Sword");
    }

    private void resetAllPlayerGlints() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sanitizePlayerInventory(player);
        }
    }

    private void sanitizePlayerInventory(Player player) {
        if (player == null) return;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (sanitizeItem(stack)) {
                player.getInventory().setItem(slot, stack);
            }
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            if (sanitizeItem(armor[i])) {
                armorChanged = true;
            }
        }
        if (armorChanged) {
            player.getInventory().setArmorContents(armor);
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (sanitizeItem(offHand)) {
            player.getInventory().setItemInOffHand(offHand);
        }
        player.updateInventory();
    }

    private boolean sanitizeItem(ItemStack item) {
        if (item == null) return false;
        int tier = getEnchantedLevel(item);
        final int snapshot = tier;
        final boolean[] modified = {false};
        modifyMeta(item, meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            boolean touched = false;
            if (pdc.has(glintExpiryKey, PersistentDataType.LONG)) {
                pdc.remove(glintExpiryKey);
                touched = true;
            }
            if (snapshot <= 0) {
                setGlintOverride(meta, null);
                touched = true;
            } else if (snapshot >= 3) {
                setGlintOverride(meta, true);
                touched = true;
            } else {
                setGlintOverride(meta, false);
                touched = true;
            }
            modified[0] = touched;
        });
        if (snapshot == 0 && ItemRegistry.isRosegoldSword(item)) {
            ItemRegistry.applyRosegoldSwordBonus(item);
            modified[0] = true;
        }
        return modified[0];
    }

    private String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(level);
        };
    }
}
