package goat.projectLinearity.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.NumberConversions;

import java.util.Random;

public class ConsegritySpawnListener implements Listener {
    private static final String WORLD_NAME = "Consegrity";
    private static final int SEA_LEVEL = 154;

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!WORLD_NAME.equals(world.getName())) return;

        // Only decorate newly generated chunks to avoid re-spawning on reload
        try {
            if (!event.isNewChunk()) return;
        } catch (Throwable ignore) {
            // Older APIs: fall back to a light random gate to avoid duplication
            if ((world.getFullTime() & 3) != 0) return;
        }

        Chunk chunk = event.getChunk();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        long pair = (((long) baseX) << 32) ^ (baseZ & 0xFFFFFFFFL);
        Random rng = new Random(world.getSeed() ^ pair ^ 0xF15EF15EL);


        int fishGroups = 0;
        int squidGroups = 0;
        int fishCap = 2 + rng.nextInt(2);  // 2-3 groups per chunk
        int squidCap = 1 + (rng.nextDouble() < 0.25 ? 1 : 0); // 1-2 groups occasionally

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                // find topmost kelp block in the column
                int topKelpY = -1;
                for (int y = SEA_LEVEL - 1; y >= world.getMinHeight(); y--) {
                    Material m = chunk.getBlock(lx, y, lz).getType();
                    if (m == Material.KELP || m == Material.KELP_PLANT) { topKelpY = y; break; }
                    if (m != Material.WATER && m != Material.KELP_PLANT && m != Material.KELP) break;
                }
                if (topKelpY == -1) continue;

                int depth = SEA_LEVEL - topKelpY;
                double nearChance = depth >= 10 ? 0.45 : 0.25;

                // Fish groups near kelp
                if (fishGroups < fishCap && rng.nextDouble() < nearChance) {
                    int count = 2 + rng.nextInt(4); // 2-5 fish
                    for (int i = 0; i < count; i++) {
                        double ox = baseX + lx + (rng.nextDouble() - 0.5) * 6.0;
                        double oy = Math.min(SEA_LEVEL - 1, topKelpY + 1 + rng.nextInt(Math.max(1, Math.min(6, depth - 1))));
                        double oz = baseZ + lz + (rng.nextDouble() - 0.5) * 6.0;
                        EntityType type = pickFishType(rng, depth);
                        world.spawnEntity(world.getBlockAt((int) Math.floor(ox), (int) Math.floor(oy), (int) Math.floor(oz)).getLocation().add(0.5, 0.0, 0.5), type);
                    }
                    fishGroups++;
                }

                // Squid groups deeper
                if (squidGroups < squidCap && depth >= 12 && rng.nextDouble() < 0.18) {
                    int count = 1 + rng.nextInt(2);
                    for (int i = 0; i < count; i++) {
                        double ox = baseX + lx + (rng.nextDouble() - 0.5) * 8.0;
                        double oy = Math.max(topKelpY - 2, topKelpY - rng.nextInt(Math.min(10, depth - 2)));
                        double oz = baseZ + lz + (rng.nextDouble() - 0.5) * 8.0;
                        world.spawnEntity(world.getBlockAt((int) Math.floor(ox), (int) Math.floor(oy), (int) Math.floor(oz)).getLocation().add(0.5, 0.0, 0.5), EntityType.SQUID);
                    }
                    squidGroups++;
                }

                if (fishGroups >= fishCap && squidGroups >= squidCap) return;
            }
        }

        // Rarely spawn goats on high snow in Mountain sector (Y >= 200)
        int goatCap = 1 + (rng.nextDouble() < 0.20 ? 1 : 0); // 1, sometimes 2 per chunk
        int goats = 0;
        for (int attempts = 0; attempts < 24 && goats < goatCap; attempts++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            int wx = baseX + lx;
            int wz = baseZ + lz;
            // Quick region check first
            ConsegrityRegions.Region region = ConsegrityRegions.regionAt(world, wx, wz);
            if (region != ConsegrityRegions.Region.MOUNTAIN) continue;

            // Find surface top Y in this column
            int yTop = -1;
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                Material m = chunk.getBlock(lx, y, lz).getType();
                if (m == Material.AIR) continue;
                yTop = y; break;
            }
            if (yTop < 200) continue;
            Material surface = chunk.getBlock(lx, yTop, lz).getType();
            if (surface != Material.SNOW_BLOCK) continue;

            // Rare chance per valid site
            if (rng.nextDouble() > 0.03) continue; // ~3% chance
            world.spawnEntity(chunk.getBlock(lx, yTop + 1, lz).getLocation().add(0.5, 0.0, 0.5), EntityType.GOAT);
            goats++;
        }
    }

    private EntityType pickFishType(Random rng, int depth) {
        double r = rng.nextDouble();
        if (depth >= 18) {
            if (r < 0.65) return EntityType.COD;
            if (r < 0.90) return EntityType.SALMON;
            return EntityType.TROPICAL_FISH;
        } else {
            if (r < 0.60) return EntityType.COD;
            if (r < 0.80) return EntityType.TROPICAL_FISH;
            return EntityType.SALMON;
        }
    }

    
}
