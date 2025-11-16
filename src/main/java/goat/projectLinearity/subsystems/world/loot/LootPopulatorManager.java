package goat.projectLinearity.subsystems.world.loot;

import goat.projectLinearity.subsystems.world.loot.LootRegistry.StructureLootDefinition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
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
    private final NamespacedKey templateIdKey;
    private final NamespacedKey structureIdKey;

    public LootPopulatorManager(JavaPlugin plugin, LootRegistry lootRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lootRegistry = Objects.requireNonNull(lootRegistry, "lootRegistry");
        this.templateIdKey = new NamespacedKey(plugin, "loot_template_id");
        this.structureIdKey = new NamespacedKey(plugin, "loot_structure_key");
    }

    public void handleStructurePlacement(String structureKey, Location origin, int bounds) {
        populateStructure(structureKey, origin, bounds, StructureLootDefinition::pickInventories);
    }

    /**
     * Populates nearby chests using loot templates gathered from every registered structure definition.
     */
    public void handleStructurePlacementWithGlobalLoot(String structureKey, Location origin, int bounds) {
        populateStructure(structureKey, origin, bounds, this::pickTemplatesFromAllDefinitions);
    }

    private void populateStructure(String structureKey, Location origin, int bounds, TemplateSelector templateSelector) {
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

        List<TargetInventory> targetInventories = new ArrayList<>();
        Map<Inventory, TargetInventory> seenInventories = new IdentityHashMap<>();
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
                    TargetInventory target = seenInventories.get(inventory);
                    if (target == null) {
                        target = new TargetInventory(inventory);
                        seenInventories.put(inventory, target);
                        targetInventories.add(target);
                    }
                    target.addBlock(block);

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

        List<LootRegistry.LootInventory> templates = templateSelector.select(definition, targetInventories.size(), random);
        for (int i = 0; i < targetInventories.size(); i++) {
            TargetInventory target = targetInventories.get(i);
            LootRegistry.LootInventory template = templates.get(Math.min(i, templates.size() - 1));
            applyTemplate(target, template, structureKey);
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

    private void applyTemplate(TargetInventory target, LootRegistry.LootInventory template, String structureKey) {
        ItemStack[] contents = cloneInventoryContents(template.contents(), target.inventory.getSize());
        target.inventory.setContents(contents);
        tagInventoryBlocks(target, template.describe(), structureKey);
    }

    private List<LootRegistry.LootInventory> pickTemplatesFromAllDefinitions(LootRegistry.StructureLootDefinition fallback,
                                                                             int count,
                                                                             Random random) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<LootRegistry.LootInventory> pool = new ArrayList<>();
        for (LootRegistry.StructureLootDefinition definition : lootRegistry.getDefinitions()) {
            pool.addAll(definition.getInventories());
        }
        if (pool.isEmpty()) {
            return fallback == null ? Collections.emptyList() : fallback.pickInventories(count, random);
        }
        Collections.shuffle(pool, random);
        List<LootRegistry.LootInventory> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            LootRegistry.LootInventory base;
            if (i < pool.size()) {
                base = pool.get(i);
            } else {
                base = pool.get(random.nextInt(pool.size()));
            }
            results.add(base);
        }
        return results;
    }

    private ItemStack[] cloneInventoryContents(ItemStack[] source, int targetSize) {
        if (source == null) {
            source = new ItemStack[0];
        }
        ItemStack[] clone = new ItemStack[targetSize];
        int limit = Math.min(targetSize, source.length);
        for (int i = 0; i < limit; i++) {
            ItemStack item = source[i];
            clone[i] = item == null ? null : item.clone();
        }
        return clone;
    }

    private void tagInventoryBlocks(TargetInventory target, String templateDescriptor, String structure) {
        if (target.blocks.isEmpty()) {
            return;
        }
        String normalizedStructure = structure == null ? "" : structure.toLowerCase(Locale.ROOT);
        for (Block block : target.blocks) {
            BlockState state = block.getState();
            if (!(state instanceof TileState tileState)) {
                continue;
            }
            PersistentDataContainer container = tileState.getPersistentDataContainer();
            if (templateDescriptor == null) {
                container.remove(templateIdKey);
            } else {
                container.set(templateIdKey, PersistentDataType.STRING, templateDescriptor);
            }
            if (normalizedStructure.isBlank()) {
                container.remove(this.structureIdKey);
            } else {
                container.set(this.structureIdKey, PersistentDataType.STRING, normalizedStructure);
            }
            tileState.update(true, false);
        }
    }

    @FunctionalInterface
    private interface TemplateSelector {
        List<LootRegistry.LootInventory> select(LootRegistry.StructureLootDefinition definition, int count, Random random);
    }

    private static final class TargetInventory {
        private final Inventory inventory;
        private final List<Block> blocks = new ArrayList<>();

        private TargetInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        private void addBlock(Block block) {
            if (block != null) {
                blocks.add(block);
            }
        }
    }
}
