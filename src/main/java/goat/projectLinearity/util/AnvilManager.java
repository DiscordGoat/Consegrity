package goat.projectLinearity.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class AnvilManager implements Listener {

    private static final String TITLE = ChatColor.DARK_GRAY + "Anvil";
    private static final int SLOT_BASE = 10;
    private static final int SLOT_MATERIAL = 13;
    private static final int SLOT_CONFIRM = 16;

    private static final Set<Material> ANVILS = EnumSet.of(
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL
    );

    private final JavaPlugin plugin;
    private final ItemStack filler;
    private final ItemStack confirm;
    private final EnchantedManager enchantedManager;

    private static final Map<Material, Supplier<ItemStack>> ROSEGOLD_TARGETS = Map.ofEntries(
            Map.entry(Material.GOLDEN_SWORD, ItemRegistry::getRosegoldSword),
            Map.entry(Material.GOLDEN_PICKAXE, ItemRegistry::getRosegoldPickaxe),
            Map.entry(Material.GOLDEN_AXE, ItemRegistry::getRosegoldAxe),
            Map.entry(Material.GOLDEN_SHOVEL, ItemRegistry::getRosegoldShovel),
            Map.entry(Material.GOLDEN_HOE, ItemRegistry::getRosegoldHoe),
            Map.entry(Material.GOLDEN_HELMET, ItemRegistry::getRosegoldHelmet),
            Map.entry(Material.GOLDEN_CHESTPLATE, ItemRegistry::getRosegoldChestplate),
            Map.entry(Material.GOLDEN_LEGGINGS, ItemRegistry::getRosegoldLeggings),
            Map.entry(Material.GOLDEN_BOOTS, ItemRegistry::getRosegoldBoots)
    );

    public AnvilManager(JavaPlugin plugin, EnchantedManager enchantedManager) {
        this.plugin = plugin;
        this.enchantedManager = enchantedManager;
        this.filler = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        this.confirm = createPane(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Confirm");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openAnvil(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 27, TITLE);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == SLOT_BASE || i == SLOT_MATERIAL) continue;
            if (i == SLOT_CONFIRM) {
                inventory.setItem(i, confirm.clone());
            } else {
                inventory.setItem(i, filler.clone());
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Material type = event.getClickedBlock().getType();
        if (!ANVILS.contains(type)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTask(plugin, () -> openAnvil(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!(view.getTopInventory().getHolder() instanceof Holder)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < view.getTopInventory().getSize()) {
            // interacting with top inventory
            if (rawSlot == SLOT_CONFIRM) {
                event.setCancelled(true);
                handleConfirm(event.getWhoClicked());
                return;
            }

            if (rawSlot != SLOT_BASE && rawSlot != SLOT_MATERIAL) {
                event.setCancelled(true);
                return;
            }
        } else {
            // shift-click from player inventory
            if (event.isShiftClick()) {
                event.setCancelled(true);
                shiftIntoSlots(view, event.getCurrentItem());
            }
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            ItemStack current = event.getCurrentItem();
            shiftIntoSlots(view, current);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot == SLOT_CONFIRM || (slot != SLOT_BASE && slot != SLOT_MATERIAL)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        if (!(view.getTopInventory().getHolder() instanceof Holder)) return;
        Inventory top = view.getTopInventory();
        for (int slot : new int[]{SLOT_BASE, SLOT_MATERIAL}) {
            ItemStack item = top.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                top.setItem(slot, null);
                Player player = (Player) event.getPlayer();
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                leftover.values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
            }
        }
    }

    private void handleConfirm(org.bukkit.entity.HumanEntity clicker) {
        if (!(clicker instanceof Player player)) {
            return;
        }
        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();
        ItemStack base = top.getItem(SLOT_BASE);
        ItemStack material = top.getItem(SLOT_MATERIAL);

        top.setItem(SLOT_CONFIRM, confirm.clone());

        boolean success = false;
        if (base != null && material != null) {
            success = attemptDurabilityRepair(player, base, material, top, SLOT_MATERIAL)
                    || attemptDurabilityRepair(player, material, base, top, SLOT_BASE)
                    || attemptGildIncrease(player, base, material, top, SLOT_MATERIAL)
                    || attemptGildIncrease(player, material, base, top, SLOT_BASE)
                    || attemptRosegoldConversion(player, base, material, top, SLOT_BASE, SLOT_MATERIAL)
                    || attemptRosegoldConversion(player, material, base, top, SLOT_MATERIAL, SLOT_BASE)
                    || attemptGoldenDurability(player, base, material, top, SLOT_BASE, SLOT_MATERIAL)
                    || attemptGoldenDurability(player, material, base, top, SLOT_MATERIAL, SLOT_BASE)
                    || attemptEnchantedUpgrade(player, base, material, top, SLOT_MATERIAL, SLOT_BASE)
                    || attemptEnchantedUpgrade(player, material, base, top, SLOT_BASE, SLOT_MATERIAL);
        }

        if (!success) {
            player.sendMessage(ChatColor.RED + "Those items cannot be combined here.");
        }
    }

    private boolean attemptDurabilityRepair(Player player, ItemStack tool, ItemStack resource, Inventory top, int resourceSlot) {
        if (tool == null || resource == null) return false;
        Material resType = resource.getType();
        if (resType != Material.IRON_INGOT && resType != Material.IRON_BLOCK) return false;

        CustomDurabilityManager durabilityManager = CustomDurabilityManager.getInstance();
        if (durabilityManager == null) return false;
        if (tool.getType().getMaxDurability() <= 0) return false;

        durabilityManager.ensureTracking(tool);
        int max = durabilityManager.getMaxDurability(tool);
        int current = durabilityManager.getCurrentDurability(tool);
        int missing = max - current;
        if (missing <= 0) return false;

        int restore = resType == Material.IRON_BLOCK ? 900 : 100;
        int applied = Math.min(restore, missing);
        if (applied <= 0) return false;

        durabilityManager.repair(tool, applied);
        consumeItem(top, resourceSlot, 1);
        player.sendMessage(ChatColor.GREEN + "Your item was repaired by " + applied + " durability.");
        playAnvilSound(player);
        return true;
    }

    private boolean attemptGildIncrease(Player player, ItemStack heirloom, ItemStack resource, Inventory top, int resourceSlot) {
        if (heirloom == null || resource == null) return false;
        Material resType = resource.getType();
        if (resType != Material.GOLD_INGOT && resType != Material.GOLD_BLOCK) return false;

        HeirloomManager manager = HeirloomManager.getInstance();
        if (manager == null) return false;
        if (!manager.isHeirloom(heirloom)) return false;

        int current = manager.getGild(heirloom);
        int max = manager.getMaxGild(heirloom);
        if (max <= 0) {
            max = resType == Material.GOLD_BLOCK ? 250 : 50;
        }
        if (current >= max) return false;

        int increase = resType == Material.GOLD_BLOCK ? 250 : 50;
        int newAmount = Math.min(max, current + increase);
        manager.setGild(heirloom, newAmount, max);
        consumeItem(top, resourceSlot, 1);
        player.sendMessage(ChatColor.GOLD + "Heirloom gild increased to " + newAmount + "/" + max + ".");
        playAnvilSound(player);
        return true;
    }

    private boolean attemptRosegoldConversion(Player player, ItemStack tool, ItemStack resource, Inventory top, int toolSlot, int resourceSlot) {
        if (tool == null || resource == null) return false;
        Supplier<ItemStack> supplier = ROSEGOLD_TARGETS.get(tool.getType());
        if (supplier == null) return false;
        ItemStack template = supplier.get();
        if (template == null) return false;
        if (!isRosegoldIngot(resource)) return false;

        CustomDurabilityManager durabilityManager;
        try {
            durabilityManager = CustomDurabilityManager.getInstance();
        } catch (RuntimeException ex) {
            return false;
        }

        int newMax = durabilityManager.getMaxDurability(template);
        if (newMax <= 0) return false;

        int originalMax = Math.max(1, durabilityManager.getMaxDurability(tool));
        int originalCurrent = durabilityManager.getCurrentDurability(tool);
        double ratio = originalMax > 0 ? (double) originalCurrent / originalMax : 1.0;
        int adjustedCurrent = (int) Math.round(ratio * newMax);
        if (adjustedCurrent < 0) adjustedCurrent = 0;
        if (adjustedCurrent > newMax) adjustedCurrent = newMax;

        ItemStack converted = tool.clone();
        ItemMeta convertedMeta = converted.getItemMeta();
        ItemMeta templateMeta = template.getItemMeta();
        if (convertedMeta != null && templateMeta != null && templateMeta.hasDisplayName()) {
            convertedMeta.setDisplayName(templateMeta.getDisplayName());
            converted.setItemMeta(convertedMeta);
        }

        if (converted.getType() == Material.GOLDEN_SWORD) {
            ItemRegistry.applyRosegoldSwordBonus(converted);
        }

        durabilityManager.removeGoldenDurability(converted);
        durabilityManager.setCustomDurability(converted, adjustedCurrent, newMax);

        top.setItem(toolSlot, converted);
        consumeItem(top, resourceSlot, 1);
        playAnvilSound(player);
        ItemMeta resultMeta = converted.getItemMeta();
        String label = (resultMeta != null && resultMeta.hasDisplayName())
                ? resultMeta.getDisplayName()
                : ChatColor.LIGHT_PURPLE + "Rosegold Item";
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Forged " + label + ChatColor.LIGHT_PURPLE + " with strengthened durability.");
        return true;
    }

    private boolean isRosegoldIngot(ItemStack item) {
        if (item == null) return false;
        ItemStack reference = ItemRegistry.getRosegoldIngot();
        return reference != null && item.isSimilar(reference);
    }

    private boolean attemptGoldenDurability(Player player, ItemStack heirloom, ItemStack tool, Inventory top, int heirloomSlot, int toolSlot) {
        if (heirloom == null || tool == null) return false;
        HeirloomManager manager = HeirloomManager.getInstance();
        CustomDurabilityManager durabilityManager = CustomDurabilityManager.getInstance();
        if (manager == null || durabilityManager == null) return false;

        if (!manager.isHeirloom(heirloom)) return false;
        int gild = manager.getGild(heirloom);
        if (gild <= 0) return false;

        if (tool.getType().getMaxDurability() <= 0) return false;
        durabilityManager.ensureTracking(tool);
        int max = durabilityManager.getMaxDurability(tool);
        int current = durabilityManager.getCurrentDurability(tool);
        if (current < max) return false;

        int existingGolden = durabilityManager.getGoldenDurability(tool);
        durabilityManager.setGoldenDurability(tool, existingGolden + gild);
        consumeItem(top, heirloomSlot, 1);
        player.sendMessage(ChatColor.GOLD + "Applied " + gild + " golden durability.");
        playAnvilSound(player);
        return true;
    }

    private boolean attemptEnchantedUpgrade(Player player, ItemStack target, ItemStack book, Inventory top, int bookSlot, int targetSlot) {
        if (enchantedManager == null) return false;
        if (target == null || book == null) return false;
        if (!enchantedManager.isEnchantable(target)) return false;
        if (!enchantedManager.isCustomEnchantedBook(book)) return false;

        int level = enchantedManager.getEnchantedLevel(target);
        if (level <= 0) {
            player.sendMessage(ChatColor.RED + "You must Enchant the item at a table first.");
            return true;
        }
        if (level >= 3) {
            player.sendMessage(ChatColor.RED + "That item is already Enchanted III.");
            return true;
        }

        boolean upgraded = enchantedManager.upgradeItem(target);
        if (!upgraded) {
            player.sendMessage(ChatColor.RED + "The book fails to empower that item.");
            return true;
        }

        top.setItem(targetSlot, target);
        consumeItem(top, bookSlot, 1);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Enchanted level increased to " + roman(enchantedManager.getEnchantedLevel(target)) + ".");
        return true;
    }

    private void consumeItem(Inventory top, int slot, int amount) {
        ItemStack stack = top.getItem(slot);
        if (stack == null) return;
        int remaining = stack.getAmount() - amount;
        if (remaining > 0) {
            stack.setAmount(remaining);
            top.setItem(slot, stack);
        } else {
            top.setItem(slot, null);
        }
    }

    private void shiftIntoSlots(InventoryView view, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Inventory top = view.getTopInventory();
        int[] targets = new int[]{SLOT_BASE, SLOT_MATERIAL};
        for (int slot : targets) {
            ItemStack existing = top.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                top.setItem(slot, item.clone());
                item.setAmount(0);
                return;
            }
        }
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private void playAnvilSound(Player player) {
        player.getWorld().playSound(
                player.getLocation(),
                org.bukkit.Sound.BLOCK_ANVIL_USE,
                1.0f,
                1.0f
        );
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
