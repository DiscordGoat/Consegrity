/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.entity.EntityType
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.world.ChunkLoadEvent
 */
package goat.projectLinearity.world;

import goat.projectLinearity.world.ConsegrityRegions;
import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ConsegritySpawnListener
implements Listener {
    private static final String WORLD_NAME = "Consegrity";
    private static final int SEA_LEVEL = 154;

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world;
        block14: {
            world = event.getWorld();
            if (!WORLD_NAME.equals(world.getName())) {
                return;
            }
            try {
                if (!event.isNewChunk()) {
                    return;
                }
            }
            catch (Throwable ignore) {
                if ((world.getFullTime() & 3L) == 0L) break block14;
                return;
            }
        }
        Chunk chunk = event.getChunk();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        long pair = (long)baseX << 32 ^ (long)baseZ & 0xFFFFFFFFL;
        Random rng = new Random(world.getSeed() ^ pair ^ 0xF15EF15EL);
        int fishGroups = 0;
        int squidGroups = 0;
        int fishCap = 2 + rng.nextInt(2);
        int squidCap = 1 + (rng.nextDouble() < 0.25 ? 1 : 0);
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                double oz;
                double oy;
                double ox;
                int i;
                int count;
                double nearChance;
                int topKelpY = -1;
                for (int y = 153; y >= world.getMinHeight(); --y) {
                    Material m = chunk.getBlock(lx, y, lz).getType();
                    if (m == Material.KELP || m == Material.KELP_PLANT) {
                        topKelpY = y;
                        break;
                    }
                    if (m != Material.WATER && m != Material.KELP_PLANT && m != Material.KELP) break;
                }
                if (topKelpY == -1) continue;
                int depth = 154 - topKelpY;
                double d = nearChance = depth >= 10 ? 0.45 : 0.25;
                if (fishGroups < fishCap && rng.nextDouble() < nearChance) {
                    count = 2 + rng.nextInt(4);
                    for (i = 0; i < count; ++i) {
                        ox = (double)(baseX + lx) + (rng.nextDouble() - 0.5) * 6.0;
                        oy = Math.min(153, topKelpY + 1 + rng.nextInt(Math.max(1, Math.min(6, depth - 1))));
                        oz = (double)(baseZ + lz) + (rng.nextDouble() - 0.5) * 6.0;
                        EntityType type = this.pickFishType(rng, depth);
                        world.spawnEntity(world.getBlockAt((int)Math.floor(ox), (int)Math.floor(oy), (int)Math.floor(oz)).getLocation().add(0.5, 0.0, 0.5), type);
                    }
                    ++fishGroups;
                }
                if (squidGroups < squidCap && depth >= 12 && rng.nextDouble() < 0.18) {
                    count = 1 + rng.nextInt(2);
                    for (i = 0; i < count; ++i) {
                        ox = (double)(baseX + lx) + (rng.nextDouble() - 0.5) * 8.0;
                        oy = Math.max(topKelpY - 2, topKelpY - rng.nextInt(Math.min(10, depth - 2)));
                        oz = (double)(baseZ + lz) + (rng.nextDouble() - 0.5) * 8.0;
                        world.spawnEntity(world.getBlockAt((int)Math.floor(ox), (int)Math.floor(oy), (int)Math.floor(oz)).getLocation().add(0.5, 0.0, 0.5), EntityType.SQUID);
                    }
                    ++squidGroups;
                }
                if (fishGroups < fishCap || squidGroups < squidCap) continue;
                return;
            }
        }
        int goatCap = 1 + (rng.nextDouble() < 0.2 ? 1 : 0);
        int goats = 0;
        for (int attempts = 0; attempts < 24 && goats < goatCap; ++attempts) {
            Material surface;
            int lz;
            int wz;
            int lx = rng.nextInt(16);
            int wx = baseX + lx;
            ConsegrityRegions.Region region = ConsegrityRegions.regionAt(world, wx, wz = baseZ + (lz = rng.nextInt(16)));
            if (region != ConsegrityRegions.Region.MOUNTAIN) continue;
            int yTop = -1;
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); --y) {
                Material m = chunk.getBlock(lx, y, lz).getType();
                if (m == Material.AIR) continue;
                yTop = y;
                break;
            }
            if (yTop < 200 || (surface = chunk.getBlock(lx, yTop, lz).getType()) != Material.SNOW_BLOCK || rng.nextDouble() > 0.03) continue;
            world.spawnEntity(chunk.getBlock(lx, yTop + 1, lz).getLocation().add(0.5, 0.0, 0.5), EntityType.GOAT);
            ++goats;
        }
    }

    private EntityType pickFishType(Random rng, int depth) {
        double r = rng.nextDouble();
        if (depth >= 18) {
            if (r < 0.65) {
                return EntityType.COD;
            }
            if (r < 0.9) {
                return EntityType.SALMON;
            }
            return EntityType.TROPICAL_FISH;
        }
        if (r < 0.6) {
            return EntityType.COD;
        }
        if (r < 0.8) {
            return EntityType.TROPICAL_FISH;
        }
        return EntityType.SALMON;
    }
}

