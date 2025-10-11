package goat.projectLinearity.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShelfManager implements Listener {
    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    // locKey -> stored ItemStack (the one slot)
    private final Map<String, ItemStack> shelfContents = new HashMap<>();
    // locKey -> armor stand UUID for the 3D display
    private final Map<String, UUID> displayStands = new HashMap<>();
    // locKey -> facing direction for persistence
    private final Map<String, String> shelfDirections = new HashMap<>();
    // flag to ensure shelves are loaded before we attempt to save
    private boolean hasLoaded = false;

    public ShelfManager(JavaPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "shelves.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); }
            catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadAllShelves();
    }

    public void onDisable() {
        saveAllShelves();
    }

    //=======================================================================
    // Persistence (copied pattern)
    //=======================================================================
    private void loadAllShelves() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String locKey : dataConfig.getKeys(false)) {
            ItemStack stored = dataConfig.getItemStack(locKey + ".item");
            String direction = dataConfig.getString(locKey + ".direction", "NORTH");

            Location loc = fromLocKey(locKey);
            World world = loc.getWorld();
            if (world == null) {
                plugin.getLogger().warning("[ShelfManager] Skipping " + locKey + " because world is null.");
                continue;
            }

            Block b = world.getBlockAt(loc);
            // Only load if there's still a closed oak trapdoor here
            if (b.getType() != Material.OAK_TRAPDOOR) continue;
            org.bukkit.block.data.type.TrapDoor trapData = (org.bukkit.block.data.type.TrapDoor) b.getBlockData();
            if (trapData.isOpen()) continue;

            shelfContents.put(locKey, stored);
            shelfDirections.put(locKey, direction);
            org.bukkit.block.BlockFace facing = org.bukkit.block.BlockFace.valueOf(direction);
            ArmorStand stand = spawnDisplayStand(loc, stored, facing);
            displayStands.put(locKey, stand.getUniqueId());
        }
        plugin.getLogger().info("[ShelfManager] Loaded " + shelfContents.size() + " shelf(s).");
        hasLoaded = true;
    }

    public void saveAllShelves() {
        if (!hasLoaded) return; // avoid overriding data before we've loaded existing shelves
        // clear old
        for (String key : dataConfig.getKeys(false)) {
            dataConfig.set(key, null);
        }
        // write current
        for (Map.Entry<String, ItemStack> e : shelfContents.entrySet()) {
            String locKey = e.getKey();
            dataConfig.set(locKey + ".item", e.getValue());
            dataConfig.set(locKey + ".direction", shelfDirections.get(locKey));
        }
        try { dataConfig.save(dataFile); }
        catch (IOException ex) { ex.printStackTrace(); }
        plugin.getLogger().info("[ShelfManager] Saved " + shelfContents.size() + " shelf(s).");
    }

    private String toLocKey(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    public Location fromLocKey(String key) {
        String[] p = key.split(":");
        World w = Bukkit.getWorld(p[0]);
        int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]), z = Integer.parseInt(p[3]);
        return new Location(w, x, y, z);
    }

    //=======================================================================
    // Block place / convert quartz → shelf slab
    //=======================================================================
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        String shelfName = ItemRegistry.getShelfItem()
                .getItemMeta()
                .getDisplayName();

        // bail if the item in hand has no meta or a different display name
        if (!inHand.hasItemMeta()
                || !inHand.getItemMeta().hasDisplayName()
                || !inHand.getItemMeta().getDisplayName().equals(shelfName)) {
            return;
        }
        e.setCancelled(false); // cancel quartz pillar placement
        Location loc = e.getBlockPlaced().getLocation();
        Player player = e.getPlayer();

        // Place a closed oak trapdoor instead, replacing the quartz pillar block the player placed


        // Set the trapdoor to face the wall the player is facing
        org.bukkit.block.BlockFace playerFacing = player.getFacing();

        String key = toLocKey(loc);
        shelfContents.put(key, null);  // empty shelf
        shelfDirections.put(key, playerFacing.toString()); // store direction
        ArmorStand stand = spawnDisplayStand(loc, null, playerFacing);
        displayStands.put(key, stand.getUniqueId());
    }

    //=======================================================================
    // Right-click to open 9-slot GUI with barriers
    //=======================================================================
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        // only closed oak trapdoor shelves
        if (b.getType() != Material.OAK_TRAPDOOR) return;
        org.bukkit.block.data.type.TrapDoor trapData = (org.bukkit.block.data.type.TrapDoor) b.getBlockData();
        if (trapData.isOpen()) return; // only closed trapdoors
        String key = toLocKey(b.getLocation());
        if (!shelfContents.containsKey(key)) return;

        e.setCancelled(true);
        Player player = e.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Simple logic:
        // 1. If player has no item -> open GUI
        // 2. If shelf is empty AND player has an item -> store item, don't open GUI
        // 3. Otherwise -> open GUI

        if (heldItem != null && heldItem.getType() != Material.AIR && shelfContents.get(key) == null) {
            // Store the item
            ItemStack itemToStore = heldItem.clone();
            shelfContents.put(key, itemToStore);

            // Remove from player's hand
            player.getInventory().setItemInMainHand(null);

            // Update display
            updateArmorStandDisplay(key);

            // Effects
            Location loc = b.getLocation().add(0.5, 0.5, 0.5);
            World world = loc.getWorld();
            if (world != null) {
                world.playSound(loc, Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.8f, 1.0f);
                world.spawnParticle(Particle.ENCHANT, loc, 10, 0.2, 0.2, 0.2, 0.05, itemToStore);
            }
            return; // Don't open GUI
        }

        // Open GUI in all other cases
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Shelf");

        // Fill slots 1-8 with barriers (GUI decoration)
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.setDisplayName(" ");
        barrier.setItemMeta(barrierMeta);

        for (int i = 1; i < 9; i++) {
            inv.setItem(i, barrier);
        }

        // Fill slot 0 with stored item if any
        ItemStack stored = shelfContents.get(key);
        if (stored != null) inv.setItem(0, stored.clone());

        player.openInventory(inv);
        invHolderMap.put(player, key);
    }

    private final Map<Player, String> invHolderMap = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(ChatColor.GOLD + "Shelf")) return;

        Player player = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();

        if (clickedInventory == null) return; // Clicked outside the inventory

        ItemStack clickedItem = e.getCurrentItem();

        // Allow clicks in the player's inventory (bottom inventory)
        if (clickedInventory != e.getView().getTopInventory()) {
            return; // Allow clicks in the player's inventory
        }

        int slot = e.getRawSlot();

        // Prevent moving GUI items (barriers in slots 1-8)
        if (isGuiItem(clickedItem) && slot >= 1 && slot <= 8) {
            e.setCancelled(true);
            return;
        }

        // Allow interaction only with slot 0 (the storage slot)
        if (slot != 0) {
            e.setCancelled(true);
        }
    }

    // Helper method to check if an item is a GUI decoration item
    private boolean isGuiItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.BARRIER) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals(" ");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(ChatColor.GOLD + "Shelf")) return;
        Player p = (Player)e.getPlayer();
        String key = invHolderMap.remove(p);
        if (key == null) return;

        ItemStack newStack = e.getInventory().getItem(0);
        shelfContents.put(key, (newStack!=null?newStack.clone():null));

        // Always update the armor stand display when inventory closes
        updateArmorStandDisplay(key);
    }

    // Helper method to update armor stand display
    private void updateArmorStandDisplay(String key) {
        UUID standId = displayStands.get(key);
        if (standId != null) {
            Entity ent = Bukkit.getEntity(standId);
            if (ent instanceof ArmorStand) {
                ArmorStand as = (ArmorStand)ent;
                ItemStack stored = shelfContents.get(key);
                as.getEquipment().setHelmet(stored!=null?stored.clone():null);
            }
        }
    }

    //=======================================================================
    // Left-click to extract (unless player holds an axe)
    //=======================================================================
    // Fix for the onLeftClick method to properly update the shelf contents
    // Fixed onLeftClick method to properly handle the last item in a stack
    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.OAK_TRAPDOOR) return;

        // only closed trapdoors
        org.bukkit.block.data.type.TrapDoor trapData = (org.bukkit.block.data.type.TrapDoor) b.getBlockData();
        if (trapData.isOpen()) return;

        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand != null && hand.getType().toString().endsWith("_AXE")) {
            // let blockBreak handle axes
            return;
        }

        e.setCancelled(true);
        String key = toLocKey(b.getLocation());
        ItemStack stored = shelfContents.get(key);
        if (stored == null || stored.getAmount() <= 0) return;

        // Check if this is the last item in the stack
        boolean isLastItem = (stored.getAmount() == 1);

        // prepare the single-output item first (before modifying the stored item)
        ItemStack output = stored.clone();
        output.setAmount(1);

        // NOW decrement stored count
        stored.setAmount(stored.getAmount() - 1);

        // If this was the last item, set the shelf contents to null
        if (isLastItem) {
            shelfContents.put(key, null);
        } else {
            shelfContents.put(key, stored);
        }

        updateArmorStandDisplay(key);

        // spawn effects & item entity
        Location spawnLoc = b.getLocation().add(0.5, 0.5, 0.5);
        World world = spawnLoc.getWorld();
        if (world == null) return;

        // particle burst
        world.spawnParticle(
                Particle.CLOUD,
                spawnLoc,    // center
                8,           // count
                0.2, 0.2, 0.2, // offset XYZ
                0.05         // extra speed/spread
        );

        // light pop sound
        world.playSound(
                spawnLoc,
                Sound.ENTITY_ITEM_PICKUP,
                0.5f,    // volume
                1.3f     // pitch
        );

        // drop the item and shoot it toward the player
        Item dropped = world.dropItem(spawnLoc, output);
        dropped.setPickupDelay(0); // No pickup delay

        // compute velocity from drop to player's eye height
        Vector toPlayer = p.getEyeLocation().toVector().subtract(spawnLoc.toVector()).normalize();
        dropped.setVelocity(toPlayer.multiply(0.6));

        // done — item will fly to you and then can be picked up
    }


    //=======================================================================
    // Breaking the slab with an axe drops the Shelf item + cleans up
    //=======================================================================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.OAK_TRAPDOOR) return;
        org.bukkit.block.data.type.TrapDoor trapData = (org.bukkit.block.data.type.TrapDoor) b.getBlockData();
        if (trapData.isOpen()) return; // only closed trapdoors

        String key = toLocKey(b.getLocation());
        // if this trapdoor isn’t one of our shelves, bail
        if (!shelfContents.containsKey(key)) return;

        // 1) drop the shelf item itself
        b.getWorld().dropItemNaturally(b.getLocation(), ItemRegistry.getShelfItem());
        e.setDropItems(false);

        // 2) drop any stored contents
        ItemStack stored = shelfContents.get(key);
        if (stored != null && stored.getAmount() > 0) {
            // clone so we don’t mutate the map entry
            ItemStack toDrop = stored.clone();
            b.getWorld().dropItemNaturally(b.getLocation(), toDrop);
        }

        // 3) cleanup all our internal state
        shelfContents.remove(key);
        shelfDirections.remove(key);

        UUID standId = displayStands.remove(key);
        if (standId != null) {
            Entity ent = Bukkit.getEntity(standId);
            if (ent != null) ent.remove();
        }
    }


    //=======================================================================
    // Helpers
    //=======================================================================
    private ArmorStand spawnDisplayStand(Location blockLoc, ItemStack stack, org.bukkit.block.BlockFace facing) {
        // Position the armor stand 1 block lower (at block level) and in front of the trapdoor
        Location standLoc = blockLoc.clone().add(0.5, -1.8, 0.5);

        // Calculate position offset and rotation based on trapdoor facing direction
        float yaw = 0.0f;
        double offsetX = 0, offsetZ = 0;

        switch (facing) {
            case NORTH: // trapdoor faces north wall, armor stand leans back (south)
                yaw = 180.0f;
                offsetZ = -0.3; // move slightly towards the wall
                break;
            case SOUTH: // trapdoor faces south wall, armor stand leans back (north)
                yaw = 0.0f;
                offsetZ = 0.3;
                break;
            case EAST: // trapdoor faces east wall, armor stand leans back (west)
                yaw = 270.0f;
                offsetX = 0.3;
                break;
            case WEST: // trapdoor faces west wall, armor stand leans back (east)
                yaw = 90.0f;
                offsetX = -0.3;
                break;
        }

        standLoc.add(offsetX, 0, offsetZ);
        standLoc.setYaw(yaw);

        ArmorStand stand = (ArmorStand)standLoc.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        // Lean back away from the player by tilting backwards
        stand.setHeadPose(new EulerAngle(Math.toRadians(15), 0, 0)); // positive angle leans back
        stand.getEquipment().setHelmet(stack!=null?stack.clone():null);
        return stand;
    }
    // Add these getter methods to your ShelfManager class

    /**
     * Get a map of all shelf contents (locKey -> ItemStack)
     * @return Map of shelf contents
     */
    public Map<String, ItemStack> getShelfContents() {
        return new HashMap<>(shelfContents);
    }

    /**
     * Get a map of all display stand UUIDs (locKey -> UUID)
     * @return Map of display stand UUIDs
     */
    public Map<String, UUID> getDisplayStands() {
        return new HashMap<>(displayStands);
    }

    /**
     * Remove a single item from the given shelf key and update its display.
     *
     * @param locKey location key of the shelf
     * @return the removed ItemStack or null if shelf empty
     */
    public ItemStack takeOneItem(String locKey) {
        ItemStack stored = shelfContents.get(locKey);
        if (stored == null || stored.getAmount() <= 0) {
            return null;
        }

        boolean last = stored.getAmount() == 1;
        ItemStack output = stored.clone();
        output.setAmount(1);

        stored.setAmount(stored.getAmount() - 1);
        if (last) {
            shelfContents.put(locKey, null);
        } else {
            shelfContents.put(locKey, stored);
        }

        // update display armor stand
        UUID standId = displayStands.get(locKey);
        if (standId != null) {
            Entity ent = Bukkit.getEntity(standId);
            if (ent instanceof ArmorStand as) {
                ItemStack remaining = shelfContents.get(locKey);
                as.getEquipment().setHelmet(remaining != null ? remaining.clone() : null);
            }
        }

        return output;
    }
}
