package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import goat.projectLinearity.world.DeferredSpawnManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;
import java.util.SplittableRandom;

public class SwampSector extends SectorBase {
    private static final int SEA_LEVEL = 153;

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x51A7A11BL, (double) wx / 260.0, (double) wz / 260.0);
        double h2 = valueNoise2(seed ^ 0x51A7A11CL, (double) wx / 90.0, (double) wz / 90.0);
        double h = (h1 * 0.75 + h2 * 0.25) * 2.0 - 1.0;
        int base = SEA_LEVEL + 1; // ~154
        int amp = 2;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 271828183L);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        Material lily;
        try { lily = Material.valueOf("LILY_PAD"); } catch (Throwable t) { lily = Material.valueOf("WATER_LILY"); }
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
                int topY = topYGrid[lx][lz];
                int wx = baseX + lx, wz = baseZ + lz;
                boolean water = shouldPlaceWater(seed, wx, wz);
                if (water) {
                    data.setBlock(lx, topY, lz, Material.WATER);
                    if (rng.nextDouble() < 0.10 && safeType(data, lx, topY + 1, lz) == Material.AIR) {
                        try { data.setBlock(lx, topY + 1, lz, lily); } catch (Throwable ignore) {}
                    }
                } else {
                    Material g = safeType(data, lx, topY, lz);
                    if (g != Material.GRASS_BLOCK && g != Material.DIRT && g != Material.MUD) {
                        data.setBlock(lx, topY, lz, Material.GRASS_BLOCK);
                    }
                }
            }
        }

        // Rare oaks on paths
        for (int i = 0; i < 10; i++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
            int topY = topYGrid[lx][lz];
            int wx = baseX + lx, wz = baseZ + lz;
            if (shouldPlaceWater(seed, wx, wz)) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.18) continue;
            growSimpleTree(data, lx, topY + 1, lz, Material.OAK_LOG, Material.OAK_LEAVES, new Random(rng.nextLong()));
            if (rng.nextBoolean()) addVinesAround(data, lx, topY + 1, lz, rng);
        }

        maybeQueueCows(world, data, chunkX, chunkZ, topYGrid, regionGrid, rng);
        maybeQueueWitch(world, data, chunkX, chunkZ, topYGrid, regionGrid, rng);
    }

    private boolean shouldPlaceWater(long seed, int wx, int wz) {
        double coarse = valueNoise2(seed ^ 0x5A4D00A1L, (double) wx / 58.0, (double) wz / 58.0);
        double medium = valueNoise2(seed ^ 0x5A4D00B1L, (double) wx / 22.0, (double) wz / 22.0);
        double fine = valueNoise2(seed ^ 0x5A4D00C1L, (double) wx / 9.0, (double) wz / 9.0);
        double combined = coarse * 0.55 + medium * 0.30 + fine * 0.15;
        double threshold = 0.47 + (valueNoise2(seed ^ 0x5A4D00D1L, (double) wx / 110.0, (double) wz / 110.0) - 0.5) * 0.18;
        double rimMix = valueNoise2(seed ^ 0x5A4D00E1L, (double) wx / 6.0, (double) wz / 6.0) - 0.5;
        double deviation = Math.abs(combined - threshold);
        if (deviation < 0.05) {
            double blend = (0.05 - deviation) / 0.05;
            combined -= rimMix * 0.08 * blend;
        }
        return combined < threshold;
    }

    private void maybeQueueCows(World world, ChunkGenerator.ChunkData data, int chunkX, int chunkZ,
                                int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, SplittableRandom rng) {
        if (rng.nextDouble() >= 0.12) return;
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        int herd = 1 + rng.nextInt(2);
        for (int count = 0; count < herd; count++) {
            for (int attempts = 0; attempts < 8; attempts++) {
                int lx = rng.nextInt(16), lz = rng.nextInt(16);
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
                int y = topYGrid[lx][lz];
                Material surface = safeType(data, lx, y, lz);
                if (!isSolidSwampGround(surface)) continue;
                if (safeType(data, lx, y + 1, lz) != Material.AIR) continue;
                DeferredSpawnManager.queueEntitySpawn(world, chunkX, chunkZ,
                        baseX + lx + 0.5, y + 1, baseZ + lz + 0.5, EntityType.COW);
                break;
            }
        }
    }

    private void maybeQueueWitch(World world, ChunkGenerator.ChunkData data, int chunkX, int chunkZ,
                                 int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, SplittableRandom rng) {
        if (rng.nextInt(50) != 0) return;
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int attempts = 0; attempts < 12; attempts++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
            int y = topYGrid[lx][lz];
            Material surface = safeType(data, lx, y, lz);
            if (!isSolidSwampGround(surface)) continue;
            if (safeType(data, lx, y + 1, lz) != Material.AIR) continue;
            DeferredSpawnManager.queueEntitySpawn(world, chunkX, chunkZ,
                    baseX + lx + 0.5, y + 1, baseZ + lz + 0.5, EntityType.WITCH);
            break;
        }
    }

    private boolean isSolidSwampGround(Material material) {
        return material == Material.GRASS_BLOCK
                || material == Material.DIRT
                || material == Material.MUD
                || material == Material.MOSS_BLOCK
                || material == Material.MYCELIUM;
    }
}

