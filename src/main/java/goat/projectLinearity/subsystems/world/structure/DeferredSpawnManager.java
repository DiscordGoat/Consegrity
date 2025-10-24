package goat.projectLinearity.subsystems.world.structure;

import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * Defers entity spawning requested by sector rules to avoid heavy work inside generation callbacks.
 *
 * Current rules:
 * - Jungle: PANDA spawns at ~1 per 200 newly generated chunks.
 * - Savanna: For each of COW/SHEEP/PIG/CHICKEN, with 1/50 chance per new chunk,
 *   spawn a small group of 1-4.
 * - Swamp: WITCH spawns with 1/50 chance per new chunk.
 * - Desert: RABBIT spawns with 1/50 chance per new chunk, group size 1-4.
 *
 * Spawns are scheduled on chunk load and processed in small batches every few ticks.
 */
public final class DeferredSpawnManager implements Listener, Runnable {
    private static final String WORLD_NAME = "Consegrity";

    private static final int BATCH_BUDGET = 24;

    private final Queue<SpawnRequest> queue = new ArrayDeque<>();

    public DeferredSpawnManager() {}

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!WORLD_NAME.equals(world.getName())) return;

        boolean isNew;
        try {
            isNew = event.isNewChunk();
        } catch (Throwable t) {
            // Older APIs: lightly gate to avoid duplication
            isNew = (world.getFullTime() & 3) == 0;
        }
        if (!isNew) return;

        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();

        int centerX = (cx << 4) + 8;
        int centerZ = (cz << 4) + 8;

        ConsegrityRegions.Region region = ConsegrityRegions.regionAt(world, centerX, centerZ);

        long pair = (((long) (cx << 4)) << 32) ^ ((cz << 4) & 0xFFFFFFFFL);
        Random rng = new Random(world.getSeed() ^ pair ^ 0xC0FFEE99ABL);

        // Jungle pandas: ~1 per 200 chunks
        if (region == ConsegrityRegions.Region.JUNGLE) {
            if (rng.nextInt(200) == 0) {
                queue.add(new SpawnRequest(world.getName(), cx, cz, ConsegrityRegions.Region.JUNGLE, EntityType.PANDA, 1));
            }
        }

        // Savanna passive mobs: 1/50 chance per mob, group size 1-4
        if (region == ConsegrityRegions.Region.SAVANNAH) {
            scheduleSavanna(world, cx, cz, rng);
        }

        // Swamp witches: 1/50 chance per chunk
        if (region == ConsegrityRegions.Region.SWAMP) {
            scheduleSwamp(world, cx, cz, rng);
        }

        // Desert rabbits: 1/50 chance, group size 1-4
        if (region == ConsegrityRegions.Region.DESERT) {
            scheduleDesert(world, cx, cz, rng);
        }
    }

    private void scheduleSavanna(World world, int cx, int cz, Random rng) {
        ConsegrityRegions.Region required = ConsegrityRegions.Region.SAVANNAH;
        // For each mob, independent 1/50 chance, count 1-4
        if (rng.nextInt(50) == 0) queue.add(new SpawnRequest(world.getName(), cx, cz, required, EntityType.COW, 1 + rng.nextInt(4)));
        if (rng.nextInt(50) == 0) queue.add(new SpawnRequest(world.getName(), cx, cz, required, EntityType.SHEEP, 1 + rng.nextInt(4)));
        if (rng.nextInt(50) == 0) queue.add(new SpawnRequest(world.getName(), cx, cz, required, EntityType.PIG, 1 + rng.nextInt(4)));
        if (rng.nextInt(50) == 0) queue.add(new SpawnRequest(world.getName(), cx, cz, required, EntityType.CHICKEN, 1 + rng.nextInt(4)));
    }

    private void scheduleSwamp(World world, int cx, int cz, Random rng) {
        if (rng.nextInt(50) == 0) {
            queue.add(new SpawnRequest(world.getName(), cx, cz, ConsegrityRegions.Region.SWAMP, EntityType.WITCH, 1));
        }
    }

    private void scheduleDesert(World world, int cx, int cz, Random rng) {
        if (rng.nextInt(50) == 0) {
            queue.add(new SpawnRequest(world.getName(), cx, cz, ConsegrityRegions.Region.DESERT, EntityType.RABBIT, 1 + rng.nextInt(4)));
        }
    }

    @Override
    public void run() {
        int budget = BATCH_BUDGET;
        while (budget > 0 && !queue.isEmpty()) {
            SpawnRequest req = queue.poll();
            if (req == null) break;
            World world = Bukkit.getWorld(req.worldName);
            if (world == null) continue;

            Random rng = new Random(world.getSeed() ^ (((long) req.chunkX) << 21) ^ (((long) req.chunkZ) << 7) ^ 0x51A9ADL);
            for (int i = 0; i < req.count && budget > 0; i++, budget--) {
                trySpawnOne(world, req, rng);
            }
        }
    }

    private void trySpawnOne(World world, SpawnRequest req, Random rng) {
        int baseX = req.chunkX << 4;
        int baseZ = req.chunkZ << 4;

        for (int attempts = 0; attempts < 16; attempts++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            int wx = baseX + lx;
            int wz = baseZ + lz;

            if (ConsegrityRegions.regionAt(world, wx, wz) != req.requiredRegion) continue;

            // Find surface top Y in this column
            int yTop = -1;
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                Material m = world.getBlockAt(wx, y, wz).getType();
                if (m == Material.AIR) continue;
                yTop = y; break;
            }
            if (yTop <= world.getMinHeight()) continue;

            Material ground = world.getBlockAt(wx, yTop, wz).getType();
            Material above = world.getBlockAt(wx, yTop + 1, wz).getType();

            // Only on natural ground with air above
            if (!isValidGround(req.type, ground)) continue;
            if (above != Material.AIR) continue;

            Location loc = new Location(world, wx + 0.5, yTop + 1, wz + 0.5);
            try {
                world.spawnEntity(loc, req.type);
            } catch (Throwable ignore) {}
            return;
        }
    }

    private boolean isValidGround(EntityType type, Material ground) {
        if (ground == Material.GRASS_BLOCK || ground == Material.DIRT || ground == Material.SAND) return true;
        if (type == EntityType.WITCH) {
            return ground == Material.MUD || ground == Material.MANGROVE_ROOTS;
        }
        return false;
    }

    private static final class SpawnRequest {
        final String worldName;
        final int chunkX, chunkZ;
        final ConsegrityRegions.Region requiredRegion;
        final EntityType type;
        final int count;

        SpawnRequest(String worldName, int chunkX, int chunkZ, ConsegrityRegions.Region requiredRegion, EntityType type, int count) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.requiredRegion = requiredRegion;
            this.type = type;
            this.count = Math.max(1, count);
        }
    }
}
