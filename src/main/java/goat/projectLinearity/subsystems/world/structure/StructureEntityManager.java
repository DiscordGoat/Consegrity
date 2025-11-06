package goat.projectLinearity.subsystems.world.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns configured entities around structure paste origins.
 */
public final class StructureEntityManager {

    private static final int MAX_SPAWN_ATTEMPTS = 12;

    private final JavaPlugin plugin;
    private final Map<StructureType, List<SpawnRule>> spawnRules = new EnumMap<>(StructureType.class);

    public StructureEntityManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void registerStructureEntities(StructureType type,
                                          EntityType entityType,
                                          int minCount,
                                          int maxCount,
                                          int radius,
                                          int tier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(entityType, "entityType");

        int safeMin = Math.max(0, minCount);
        int safeMax = Math.max(safeMin, maxCount);
        int safeRadius = Math.max(0, radius);
        int safeTier = Math.max(0, tier);

        SpawnRule rule = new SpawnRule(entityType, safeMin, safeMax, safeRadius, safeTier);
        spawnRules.computeIfAbsent(type, key -> new ArrayList<>()).add(rule);
    }

    public void handleStructurePlacement(String structureKey, Location origin) {
        if (structureKey == null || origin == null) {
            return;
        }
        StructureType type = StructureType.fromKey(structureKey);
        if (type == null) {
            return;
        }
        List<SpawnRule> rules = spawnRules.get(type);
        if (rules == null || rules.isEmpty()) {
            return;
        }

        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (SpawnRule rule : rules) {
            int count = rule.rollCount(random);
            if (count <= 0) {
                continue;
            }
            for (int i = 0; i < count; i++) {
                Location spawnLocation = pickSpawnLocation(origin, rule.radius(), random);
                if (spawnLocation == null) {
                    continue;
                }
                try {
                    world.spawnEntity(spawnLocation, rule.entityType());
                } catch (Exception ex) {
                    plugin.getLogger().warning("[StructureEntityManager] Failed to spawn "
                            + rule.entityType().name() + " near " + origin + ": " + ex.getMessage());
                }
            }
        }
    }

    private Location pickSpawnLocation(Location origin, int radius, ThreadLocalRandom random) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }

        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = radius <= 0 ? 0 : random.nextDouble() * radius;
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);

            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            candidate.add(0.0, 1.0, 0.0);
            if (isSpawnable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSpawnable(Location location) {
        Block block = location.getBlock();
        Material type = block.getType();
        if (type.isAir()) {
            Block below = block.getRelative(BlockFace.DOWN);
            return below.getType().isSolid();
        }
        return type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }

    private static final class SpawnRule {
        private final EntityType entityType;
        private final int min;
        private final int max;
        private final int radius;
        private final int tier;

        private SpawnRule(EntityType entityType, int min, int max, int radius, int tier) {
            this.entityType = entityType;
            this.min = min;
            this.max = max;
            this.radius = radius;
            this.tier = tier;
        }

        private int rollCount(ThreadLocalRandom random) {
            if (max <= min) {
                return min;
            }
            return random.nextInt(min, max + 1);
        }

        private EntityType entityType() {
            return entityType;
        }

        private int radius() {
            return radius;
        }
    }
}
