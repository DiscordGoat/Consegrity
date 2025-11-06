package goat.projectLinearity.subsystems.culinary;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CulinaryCauldron handles the custom cauldron catalogue that lets players
 * trade basic materials for curated culinary ingredients.
 */
public class CulinaryCauldron implements Listener {

    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "Culinary Cauldron";
    private static final int MENU_SIZE = 27;
    private static final int[] OPTION_SLOTS = {10, 12, 14, 16};

    private final JavaPlugin plugin;
    private final List<CauldronRecipe> recipes = new ArrayList<>();
    private final Set<Location> activeCauldrons = new HashSet<>();
    private final ItemStack filler;

    public CulinaryCauldron(JavaPlugin plugin) {
        this.plugin = plugin;
        this.filler = createFiller();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initializeRecipes();
    }

    private void initializeRecipes() {
        recipes.clear();
        recipes.add(new CauldronRecipe(
                ItemRegistry::getButter,
                new CauldronCost(Material.MILK_BUCKET, "Milk Bucket")
        ));
        recipes.add(new CauldronRecipe(
                ItemRegistry::getDough,
                new CauldronCost(Material.WHEAT, "Wheat")
        ));
        recipes.add(new CauldronRecipe(
                ItemRegistry::getChocolate,
                new CauldronCost(Material.COCOA_BEANS, "Cocoa Beans")
        ));
        recipes.add(new CauldronRecipe(
                ItemRegistry::getRum,
                new CauldronCost(Material.SUGAR_CANE, "Sugar Cane")
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCauldronInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!isCauldron(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (shouldDeferToVanilla(mainHand.getType())) {
            return;
        }

        Location cauldronLoc = block.getLocation();
        if (activeCauldrons.contains(cauldronLoc)) {
            player.sendMessage(ChatColor.RED + "This cauldron is already brewing something!");
            return;
        }

        event.setCancelled(true);
        openCatalogue(player, cauldronLoc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CauldronMenuHolder)) {
            return;
        }
        for (int raw : event.getRawSlots()) {
            if (raw < top.getSize()) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CauldronMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This cauldron menu belongs to someone else.");
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= top.getSize()) {
            return;
        }

        CauldronRecipe recipe = holder.getRecipe(rawSlot);
        if (recipe == null) {
            return;
        }

        Location cauldronLoc = holder.getCauldronLocation();
        if (cauldronLoc == null) {
            player.closeInventory();
            return;
        }
        if (!isCauldron(cauldronLoc.getBlock().getType())) {
            player.sendMessage(ChatColor.RED + "The cauldron is no longer here.");
            player.closeInventory();
            return;
        }
        if (activeCauldrons.contains(cauldronLoc)) {
            player.sendMessage(ChatColor.RED + "This cauldron is already brewing something!");
            return;
        }
        if (!recipe.consumeCost(player)) {
            player.sendMessage(ChatColor.RED + "You need a " + ChatColor.YELLOW + recipe.getCostDisplayName()
                    + ChatColor.RED + " to activate this recipe.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.9f);
            return;
        }

        player.closeInventory();
        activeCauldrons.add(cauldronLoc);
        player.updateInventory();
        startStirringAnimation(cauldronLoc, recipe, player);
    }

    private void openCatalogue(Player player, Location cauldronLoc) {
        CauldronMenuHolder holder = new CauldronMenuHolder(player.getUniqueId(), cauldronLoc);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, MENU_TITLE);
        fillInventory(inventory, filler);

        int max = Math.min(recipes.size(), OPTION_SLOTS.length);
        for (int i = 0; i < max; i++) {
            CauldronRecipe recipe = recipes.get(i);
            int slot = OPTION_SLOTS[i];
            inventory.setItem(slot, recipe.createIcon());
            holder.register(slot, recipe);
        }

        player.openInventory(inventory);
        player.sendMessage(ChatColor.GOLD + "[Culinary] " + ChatColor.GRAY + "Select an ingredient to brew.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
    }

    private void startStirringAnimation(Location cauldronLoc, CauldronRecipe recipe, Player player) {
        Location standLoc = cauldronLoc.clone().add(0.5, 0.4, 0.5);
        ArmorStand stirStand = (ArmorStand) standLoc.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stirStand.setInvisible(true);
        stirStand.setMarker(true);
        stirStand.setGravity(false);
        stirStand.setInvulnerable(true);
        stirStand.setCustomNameVisible(false);
        stirStand.setArms(true);
        stirStand.setSmall(true);

        stirStand.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SHOVEL, 1));
        stirStand.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));

        BukkitTask rotationTask = new BukkitRunnable() {
            double angle = 0.0;

            @Override
            public void run() {
                if (!stirStand.isValid()) {
                    cancel();
                    return;
                }
                angle += 12.0;
                if (angle > 360.0) angle -= 360.0;
                Location loc = stirStand.getLocation();
                loc.setYaw((float) angle);
                stirStand.teleport(loc);
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 80) {
                    cancel();
                    rotationTask.cancel();
                    finishCooking(cauldronLoc, recipe, player, stirStand);
                    return;
                }
                cauldronLoc.getWorld().spawnParticle(
                        Particle.BUBBLE_POP,
                        cauldronLoc.clone().add(0.5, 0.9, 0.5),
                        5, 0.2, 0.2, 0.2, 0.01
                );
                cauldronLoc.getWorld().playSound(cauldronLoc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.5f, 1.0f);
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    private void finishCooking(Location cauldronLoc, CauldronRecipe recipe, Player player, ArmorStand stirStand) {
        try {
            if (stirStand != null && !stirStand.isDead()) {
                stirStand.remove();
            }
            ItemStack output = recipe.createResult();
            if (output == null) {
                return;
            }

            cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            cauldronLoc.getWorld().spawnParticle(
                    Particle.WITCH,
                    cauldronLoc.clone().add(0.5, 1.0, 0.5),
                    20, 0.3, 0.3, 0.3, 0.05
            );

            Location spawnLoc = cauldronLoc.clone().add(0.5, 1.0, 0.5);
            org.bukkit.util.Vector direction = player.getLocation().toVector().subtract(spawnLoc.toVector());
            direction.normalize().multiply(0.3);
            direction.setY(0.2);

            for (int i = 0; i < 4; i++) {
                org.bukkit.entity.Item item = cauldronLoc.getWorld().dropItem(spawnLoc, output.clone());
                item.setVelocity(direction);
                org.bukkit.util.Vector randomOffset = new org.bukkit.util.Vector(
                        (Math.random() - 0.5) * 0.1,
                        Math.random() * 0.1,
                        (Math.random() - 0.5) * 0.1
                );
                item.setVelocity(item.getVelocity().add(randomOffset));
                item.setPickupDelay(10);
            }
        } finally {
            activeCauldrons.remove(cauldronLoc);
        }
    }

    private ItemStack createFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private void fillInventory(Inventory inventory, ItemStack fillerItem) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillerItem.clone());
        }
    }

    private boolean shouldDeferToVanilla(Material material) {
        if (material == Material.AIR) {
            return false;
        }
        if (material == Material.BUCKET) {
            return true;
        }
        if (material.name().endsWith("_BUCKET")) {
            return true;
        }
        return material == Material.GLASS_BOTTLE
                || material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private boolean isCauldron(Material type) {
        return type == Material.CAULDRON
                || type == Material.WATER_CAULDRON
                || type == Material.LAVA_CAULDRON
                || type == Material.POWDER_SNOW_CAULDRON;
    }

    private static final class CauldronMenuHolder implements InventoryHolder {
        private final UUID owner;
        private final Location cauldronLocation;
        private final Map<Integer, CauldronRecipe> slotMapping = new HashMap<>();

        private CauldronMenuHolder(UUID owner, Location cauldronLocation) {
            this.owner = owner;
            this.cauldronLocation = cauldronLocation == null ? null : cauldronLocation.clone();
        }

        private void register(int slot, CauldronRecipe recipe) {
            slotMapping.put(slot, recipe);
        }

        private CauldronRecipe getRecipe(int slot) {
            return slotMapping.get(slot);
        }

        private boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        private Location getCauldronLocation() {
            return cauldronLocation == null ? null : cauldronLocation.clone();
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class CauldronRecipe {
        private final Supplier<ItemStack> resultSupplier;
        private final CauldronCost cost;

        private CauldronRecipe(Supplier<ItemStack> resultSupplier, CauldronCost cost) {
            this.resultSupplier = resultSupplier;
            this.cost = cost;
        }

        private ItemStack createIcon() {
            ItemStack icon = createResult();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + cost.getName());
            lore.add(ChatColor.GREEN + "Click to brew.");
            if (meta != null) {
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            return icon;
        }

        private ItemStack createResult() {
            return resultSupplier.get();
        }

        private boolean consumeCost(Player player) {
            return cost.consume(player);
        }

        private String getCostDisplayName() {
            return cost.getName();
        }
    }

    private static final class CauldronCost {
        private final Material material;
        private final String name;

        private CauldronCost(Material material, String name) {
            this.material = material;
            this.name = name;
        }

        private boolean consume(Player player) {
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (matches(stack)) {
                    decrementStack(inventory, slot, stack);
                    return true;
                }
            }
            ItemStack offhand = inventory.getItemInOffHand();
            if (matches(offhand)) {
                decrementOffhand(inventory, offhand);
                return true;
            }
            return false;
        }

        private boolean matches(ItemStack stack) {
            return stack != null && stack.getType() == material;
        }

        private void decrementStack(PlayerInventory inventory, int slot, ItemStack stack) {
            int amount = stack.getAmount();
            if (amount <= 1) {
                inventory.clear(slot);
            } else {
                stack.setAmount(amount - 1);
                inventory.setItem(slot, stack);
            }
        }

        private void decrementOffhand(PlayerInventory inventory, ItemStack stack) {
            int amount = stack.getAmount();
            if (amount <= 1) {
                inventory.setItemInOffHand(null);
            } else {
                stack.setAmount(amount - 1);
                inventory.setItemInOffHand(stack);
            }
        }

        private String getName() {
            return name;
        }
    }
}
