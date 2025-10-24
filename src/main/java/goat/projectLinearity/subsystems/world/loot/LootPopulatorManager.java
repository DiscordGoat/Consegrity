package goat.projectLinearity.subsystems.world.loot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Populates chests inside newly spawned structures using inventories registered via {@link LootRegistry}.
 */
public final class LootPopulatorManager {

    private static final int BASE_SCAN_RADIUS = 15; // minimum half-extent of the scan cube

    private final JavaPlugin plugin;
    private final LootRegistry lootRegistry;
    private final Random random = new Random();

    public LootPopulatorManager(JavaPlugin plugin, LootRegistry lootRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lootRegistry = Objects.requireNonNull(lootRegistry, "lootRegistry");
    }

    public void handleStructurePlacement(String structureKey, Location origin, int bounds) {
        if (structureKey == null || origin == null) {
            return;
        }

        LootRegistry.StructureLootDefinition definition = lootRegistry.get(structureKey);
        if (definition == null) {
            plugin.getLogger().fine(() -> "[LootPopulator] No loot definition for " + structureKey);
            return;
        }

        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        List<Inventory> targetInventories = new ArrayList<>();
        Map<Inventory, Boolean> seenInventories = new IdentityHashMap<>();
        int expectedChestBlocks = definition.getChestCountForKey(structureKey);
        boolean enforceLimit = expectedChestBlocks > 0;
        int foundChestBlocks = 0;

        int horizontalRadius = computeHorizontalRadius(bounds);
        int verticalRadius = computeVerticalRadius(bounds);

        int minX = origin.getBlockX() - horizontalRadius;
        int maxX = origin.getBlockX() + horizontalRadius;
        int minZ = origin.getBlockZ() - horizontalRadius;
        int maxZ = origin.getBlockZ() + horizontalRadius;
        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + verticalRadius);

        outer:
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
                        continue;
                    }
                    BlockState state = block.getState();
                    if (!(state instanceof Chest chest)) {
                        continue;
                    }

                    Inventory inventory = chest.getInventory();
                    if (!seenInventories.containsKey(inventory)) {
                        seenInventories.put(inventory, Boolean.TRUE);
                        targetInventories.add(inventory);
                    }

                    foundChestBlocks++;
                    if (enforceLimit && foundChestBlocks >= expectedChestBlocks) {
                        break outer;
                    }
                }
            }
        }

        if (targetInventories.isEmpty()) {
            return;
        }

        List<ItemStack[]> templates = definition.pickTemplates(targetInventories.size(), random);
        for (int i = 0; i < targetInventories.size(); i++) {
            Inventory inventory = targetInventories.get(i);
            ItemStack[] template = templates.get(Math.min(i, templates.size() - 1));
            applyTemplate(inventory, template);
        }

        if (enforceLimit && foundChestBlocks < expectedChestBlocks) {
            final int finalFoundChestBlocks = foundChestBlocks;
            plugin.getLogger().fine(() -> String.format(Locale.ROOT,
                    "[LootPopulator] Structure %s expected %d chests but found %d near %d,%d,%d",
                    structureKey, expectedChestBlocks, finalFoundChestBlocks,
                    origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()));
        }
    }

    private int computeHorizontalRadius(int bounds) {
        int halfSpan = (int) Math.ceil(Math.max(0, bounds) / 2.0);
        return Math.max(BASE_SCAN_RADIUS, halfSpan + 4); // small buffer accounts for paste offsets
    }

    private int computeVerticalRadius(int bounds) {
        int scaled = Math.max(8, bounds / 3); // tall builds (e.g., monuments) extend well above origin
        return Math.max(BASE_SCAN_RADIUS, scaled);
    }

    private void applyTemplate(Inventory inventory, ItemStack[] template) {
        int size = inventory.getSize();
        ItemStack[] contents = new ItemStack[size];
        int limit = Math.min(size, template.length);
        for (int i = 0; i < limit; i++) {
            ItemStack item = template[i];
            contents[i] = item == null ? null : item.clone();
        }
        inventory.setContents(contents);
    }
}
