package goat.projectLinearity.subsystems.culinary;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CulinaryCauldron:
 *
 * Allows players to use a cauldron as a cooking station. The player right-clicks the cauldron with
 * a specific item (defined in the recipes map) to start a cooking process that involves stirring.
 * After a short animation (stirring), the raw input turns into the output item defined in the recipe.
 */
public class CulinaryCauldron implements Listener {

    private JavaPlugin plugin;

    // A map of input item to output item recipes.
    // For simplicity, we compare by Material and optional display name.
    // In a more robust system, you'd consider NBT or other checks.
    private Map<ItemStack, ItemStack> recipes = new HashMap<>();

    // Track currently "in-progress" cauldrons to avoid multiple simultaneous uses
    private Set<Location> activeCauldrons = new HashSet<>();

    public CulinaryCauldron(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initializeRecipes();
    }

    /**
     * Define your recipes here. Input -> Output
     * Example: Putting a CARROT in the cauldron turns into a RABBIT_STEW (just an example).
     */
    private void initializeRecipes() {
        ItemStack milk = new ItemStack(Material.MILK_BUCKET, 1);
        ItemStack butter = ItemRegistry.getButter();
        recipes.put(milk, butter);


        ItemStack wheat = new ItemStack(Material.WHEAT, 1);
        ItemStack dough = ItemRegistry.getDough();
        recipes.put(wheat, dough);

        // ─── your new cocoa → chocolate recipe ────────────────────────────
        ItemStack cocoa      = new ItemStack(Material.COCOA_BEANS, 1);
        ItemStack chocolate  = ItemRegistry.getChocolate();  // ← your custom item
        recipes.put(cocoa, chocolate);

        ItemStack sugarcane      = new ItemStack(Material.SUGAR_CANE, 1);
        ItemStack rum  = ItemRegistry.getRum();  // ← your custom item
        recipes.put(sugarcane, rum);
    }

    /**
     * Event handler for right-clicking a cauldron.
     * If the player right-clicks a cauldron with an input item found in our recipe list,
     * start the stirring animation and process.
     */
    @EventHandler
    public void onCauldronInteract(PlayerInteractEvent event) {
//        Bukkit.broadcastMessage(event.getClickedBlock().getType() + "");
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Location cauldronLoc = event.getClickedBlock().getLocation();

        // Check if this cauldron is already in use
        if (activeCauldrons.contains(cauldronLoc)) {
            player.sendMessage(ChatColor.RED + "This cauldron is already in use!");
            return;
        }

        // Check if the item in hand matches any recipe input
        ItemStack matchingInput = getMatchingRecipeInput(hand);
        if (matchingInput == null) {
            player.sendMessage(ChatColor.RED + "You can't cook that here.");
            return;
        }

        // Consume one item from the stack
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() == 0) {
            player.getInventory().setItemInMainHand(null);
        }

        // Start stirring animation and schedule the completion
        activeCauldrons.add(cauldronLoc);
        startStirringAnimation(cauldronLoc, matchingInput, player);
    }

    /**
     * Given the player's input item, check if it matches one of the recipes (ignoring quantity).
     */
    private ItemStack getMatchingRecipeInput(ItemStack input) {
        if (input == null || input.getType() == Material.AIR) return null;

        // For simplicity, just compare type and display name if present.
        // You may need a more robust check depending on your recipes.
        for (ItemStack key : recipes.keySet()) {
            if (compareItems(key, input)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Compare two items by their type and display name (if any).
     */
    private boolean compareItems(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) return false;

        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        String aName = (am != null && am.hasDisplayName()) ? am.getDisplayName() : null;
        String bName = (bm != null && bm.hasDisplayName()) ? bm.getDisplayName() : null;

        if (aName == null && bName == null) return true;
        if (aName != null && bName != null) return aName.equals(bName);

        return false;
    }

    /**
     * Start a stirring animation:
     * - Spawn an ArmorStand holding a stick above the cauldron.
     * - Play bubble particles and sounds.
     * - After a short delay, produce the output item and remove stands.
     */
    private void startStirringAnimation(Location cauldronLoc, ItemStack inputKey, Player player) {
        // Spawn an armor stand holding a stick to represent stirring
        Location standLoc = cauldronLoc.clone().add(0.5, 0.4, 0.5); // slightly above cauldron
        ArmorStand stirStand = (ArmorStand) standLoc.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stirStand.setInvisible(true);
        stirStand.setMarker(true);
        stirStand.setGravity(false);
        stirStand.setInvulnerable(true);
        stirStand.setCustomNameVisible(false);
        stirStand.setArms(true);
        stirStand.setSmall(true);

        // Give the stand a stick in its main hand
        stirStand.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SHOVEL, 1));
        // Tilt the stand's arm forward by about 100 degrees
        stirStand.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));

        // We'll run two tasks:
        // 1. Bubble and sound effects every 10 ticks.
        // 2. Rotation task that continuously rotates the ArmorStand slightly each tick.

        // Rotation task: rotate the ArmorStand slightly every tick
        BukkitTask rotationTask = new BukkitRunnable() {
            double angle = 0.0;

            @Override
            public void run() {
                if (!stirStand.isValid()) {
                    cancel();
                    return;
                }
                angle += 12.0; // rotate by 3 degrees per tick
                if (angle > 360.0) angle -= 360.0;
                Location loc = stirStand.getLocation();
                loc.setYaw((float) angle);
                stirStand.teleport(loc);
            }
        }.runTaskTimer(plugin, 0, 1);

        // Particle and sound effects: bubble particles + water sounds
        BukkitTask bubbleTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 80) { // ~4 seconds of stirring
                    this.cancel();
                    // Once done, stop rotation and finish cooking
                    rotationTask.cancel();
                    finishCooking(cauldronLoc, inputKey, player, stirStand);
                    return;
                }

                // Spawn bubble particles
                cauldronLoc.getWorld().spawnParticle(
                        Particle.BUBBLE_POP,
                        cauldronLoc.clone().add(0.5, 0.9, 0.5),
                        5, 0.2, 0.2, 0.2, 0.01
                );
                // Play some sounds
                cauldronLoc.getWorld().playSound(cauldronLoc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.5f, 1.0f);

                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10); // every half second
    }


    /**
     * Finish the cooking process:
     * - Remove stirring stand
     * - Drop the output item on top of the cauldron
     * - Clear the cauldron from activeCauldrons
     */
    /**
 * Finish the cooking process:
 * - Remove stirring stand
 * - Launch the output items towards the player
 * - Clear the cauldron from activeCauldrons
 */
private void finishCooking(Location cauldronLoc, ItemStack inputKey, Player player, ArmorStand stirStand) {
    if (stirStand != null && !stirStand.isDead()) {
        stirStand.remove();
    }

    // Retrieve the output from the recipes map
    ItemStack output = recipes.get(inputKey);
    if (output == null) {
        // If something is off and no recipe found, just return the input
        output = new ItemStack(inputKey);
    }

    // Play a completion sound and particles
    cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    cauldronLoc.getWorld().spawnParticle(
            Particle.WITCH,
            cauldronLoc.clone().add(0.5, 1.0, 0.5),
            20, 0.3, 0.3, 0.3, 0.05
    );

    // Calculate vector from cauldron to player
    Location spawnLoc = cauldronLoc.clone().add(0.5, 1.0, 0.5);
    org.bukkit.util.Vector direction = player.getLocation().toVector().subtract(spawnLoc.toVector());
    
    // Normalize and scale the vector for a nice arc
    direction.normalize().multiply(0.3);
    direction.setY(0.2); // Add some upward velocity for an arc effect
    
    // Drop multiple items with velocity towards the player
    for (int i = 0; i < 4; i++) {
        org.bukkit.entity.Item item = cauldronLoc.getWorld().dropItem(spawnLoc, output.clone());
        item.setVelocity(direction);
        
        // Add a slight random variation to each item's trajectory
        org.bukkit.util.Vector randomOffset = new org.bukkit.util.Vector(
                (Math.random() - 0.5) * 0.1,
                Math.random() * 0.1,
                (Math.random() - 0.5) * 0.1
        );
        item.setVelocity(item.getVelocity().add(randomOffset));
        
        // Make items not despawn as quickly
        item.setPickupDelay(10);
    }

    activeCauldrons.remove(cauldronLoc);
}

}
