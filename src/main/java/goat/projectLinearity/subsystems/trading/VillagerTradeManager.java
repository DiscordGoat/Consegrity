package goat.projectLinearity.subsystems.trading;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.enchanting.EnchantedManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class VillagerTradeManager implements Listener {

    private static final String TITLE = ChatColor.DARK_GREEN + "Villager Trades";
    private static final int INVENTORY_SIZE = 54;
    private static final int SLOT_MEAL = 20;
    private static final int SLOT_ENCHANT = 22;
    private static final int SLOT_COAL = 24;

    private final JavaPlugin plugin;
    private final EnchantedManager enchantedManager;
    private final ItemStack filler;
    private final Map<Integer, TradeType> tradeSlots;
    private final Random random = new Random();

    public VillagerTradeManager(ProjectLinearity plugin, EnchantedManager enchantedManager) {
        this.plugin = plugin;
        this.enchantedManager = enchantedManager;
        this.filler = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        this.tradeSlots = Map.of(
                SLOT_MEAL, TradeType.MEAL,
                SLOT_ENCHANT, TradeType.ENCHANT,
                SLOT_COAL, TradeType.COAL
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        EntityType type = event.getRightClicked().getType();
        if (type != EntityType.VILLAGER && type != EntityType.WANDERING_TRADER) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTask(plugin, () -> openInventory(player));
    }

    private void openInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), INVENTORY_SIZE, TITLE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
        inventory.setItem(SLOT_MEAL, createMealIcon());
        inventory.setItem(SLOT_ENCHANT, createEnchantIcon(player));
        inventory.setItem(SLOT_COAL, createCoalIcon());
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < top.getSize()) {
            event.setCancelled(true);
            TradeType type = tradeSlots.get(rawSlot);
            if (type == null) {
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            handleTrade(player, type);
            // Refresh icons that depend on player state (e.g. enchant cost)
            top.setItem(SLOT_ENCHANT, createEnchantIcon(player));
        } else if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        if (!(view.getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < view.getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleTrade(Player player, TradeType type) {
        switch (type) {
            case MEAL -> executeMealTrade(player);
            case ENCHANT -> executeEnchantTrade(player);
            case COAL -> executeCoalTrade(player);
        }
    }

    private void executeMealTrade(Player player) {
        int cost = 1;
        if (!hasEmeralds(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " Emerald to buy this.");
            return;
        }
        removeEmeralds(player, cost);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Enjoy your well balanced meal!");
    }

    private void executeEnchantTrade(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!enchantedManager.isEnchantable(hand)) {
            player.sendMessage(ChatColor.RED + "Hold an enchantable item in your main hand.");
            return;
        }
        int level = enchantedManager.getEnchantedLevel(hand);
        if (level >= 3) {
            player.sendMessage(ChatColor.RED + "That item is already Enchanted III.");
            return;
        }
        int targetLevel = level + 1;
        int cost = enchantCostForLevel(targetLevel);
        if (!hasEmeralds(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " Emerald" + (cost == 1 ? "" : "s") + " to buy this.");
            return;
        }
        removeEmeralds(player, cost);
        enchantedManager.applyTier(hand, targetLevel);
        player.updateInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Enchanted level increased to " + roman(targetLevel) + ".");
    }

    private void executeCoalTrade(Player player) {
        int cost = 1;
        if (!hasEmeralds(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " Emerald to buy this.");
            return;
        }
        removeEmeralds(player, cost);
        int amount = random.nextInt(8) + 1;
        ItemStack coal = new ItemStack(Material.COAL, amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(coal);
        leftover.values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "You received " + amount + " Coal.");
    }

    private int enchantCostForLevel(int targetLevel) {
        return switch (targetLevel) {
            case 1 -> 8;
            case 2 -> 10;
            case 3 -> 12;
            default -> 12;
        };
    }

    private ItemStack createMealIcon() {
        ItemStack item = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Well Balanced Meal");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cost: 1 Emerald");
            lore.add(ChatColor.GRAY + "Restores full hunger and saturation.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEnchantIcon(Player player) {
        ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchant");
            List<String> lore = new ArrayList<>();
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!enchantedManager.isEnchantable(hand)) {
                lore.add(ChatColor.RED + "Hold an enchantable item.");
            } else {
                int level = enchantedManager.getEnchantedLevel(hand);
                if (level >= 3) {
                    lore.add(ChatColor.RED + "Already Enchanted III.");
                } else {
                    int targetLevel = level + 1;
                    int cost = enchantCostForLevel(targetLevel);
                    lore.add(ChatColor.GRAY + "Next tier: " + ChatColor.LIGHT_PURPLE + roman(targetLevel));
                    lore.add(ChatColor.GRAY + "Cost: " + cost + " Emerald" + (cost == 1 ? "" : "s"));
                }
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack createCoalIcon() {
        ItemStack icon = new ItemStack(Material.COAL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Coal Bundle");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cost: 1 Emerald");
            lore.add(ChatColor.GRAY + "Receive 1-8 Coal.");
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private boolean hasEmeralds(Player player, int amount) {
        return countPlainEmeralds(player) >= amount;
    }

    private int countPlainEmeralds(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        int total = 0;
        for (ItemStack stack : contents) {
            if (isPlainEmerald(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean removeEmeralds(Player player, int amount) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && amount > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!isPlainEmerald(stack)) {
                continue;
            }
            int stackAmount = stack.getAmount();
            if (stackAmount > amount) {
                stack.setAmount(stackAmount - amount);
                amount = 0;
            } else {
                amount -= stackAmount;
                inventory.setItem(slot, null);
            }
        }
        player.updateInventory();
        return amount <= 0;
    }

    private boolean isPlainEmerald(ItemStack stack) {
        if (stack == null || stack.getType() != Material.EMERALD) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        if (meta.hasDisplayName()) {
            return false;
        }
        if (meta.hasEnchants()) {
            return false;
        }
        if (meta.hasCustomModelData()) {
            return false;
        }
        return true;
    }

    private ItemStack createPane(Material material, String title) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> Integer.toString(value);
        };
    }

    private enum TradeType {
        MEAL,
        ENCHANT,
        COAL
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
