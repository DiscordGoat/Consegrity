package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
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
        final double waterFrac = 0.40;
        final double scale = 34.0;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
                int topY = topYGrid[lx][lz];
                int wx = baseX + lx, wz = baseZ + lz;
                double n = valueNoise2(seed ^ 0x5A4D00A1L, (double) wx / scale, (double) wz / scale);
                boolean water = (n < waterFrac);
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
            double n = valueNoise2(seed ^ 0x5A4D00A1L, (double) wx / scale, (double) wz / scale);
            if (n < waterFrac) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.18) continue;
            growSimpleTree(data, lx, topY + 1, lz, Material.OAK_LOG, Material.OAK_LEAVES, new Random(rng.nextLong()));
            if (rng.nextBoolean()) addVinesAround(data, lx, topY + 1, lz, rng);
        }
    }
}

