package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class SavannaSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x5A7A7A11L, (double) wx / 260.0, (double) wz / 260.0);
        double h2 = valueNoise2(seed ^ 0x5A7A7A12L, (double) wx / 90.0, (double) wz / 90.0);
        double h = (h1 * 0.65 + h2 * 0.35) * 2.0 - 1.0;
        int base = 165;
        int amp = 12;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 151587097L);
        BlockData persistentLeaves;
        try {
            persistentLeaves = Material.ACACIA_LEAVES.createBlockData();
        } catch (Throwable ignore) {
            persistentLeaves = Bukkit.createBlockData(Material.ACACIA_LEAVES);
        }
        if (persistentLeaves instanceof Leaves leavesData) {
            try { leavesData.setPersistent(true); } catch (Throwable ignore) { }
            try { leavesData.setDistance(1); } catch (Throwable ignore) { }
        }

        // Acacia-like sparse trees
        for (int i = 0; i < 4; i++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SAVANNAH) continue;
            int topY = topYGrid[lx][lz];
            if (topY < 155) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.35) continue;

            int height = 4 + rng.nextInt(3);
            int dir = rng.nextInt(4);
            int ox = dir == 0 ? 1 : (dir == 1 ? -1 : 0);
            int oz = dir == 2 ? 1 : (dir == 3 ? -1 : 0);
            int x = lx, z = lz;
            for (int dy = 0; dy < height; dy++) {
                int yy = topY + 1 + dy;
                data.setBlock(x, yy, z, Material.ACACIA_LOG);
                if (dy >= height - 2) { x += ox; z += oz; }
                if (x < 0 || x > 15 || z < 0 || z > 15) break;
                data.setBlock(x, yy, z, Material.ACACIA_LOG);
            }
            int canopyY = topY + height;
            int cx = x, cz = z;
            int canopyRadius = 2 + rng.nextInt(2);

            for (int dy = -1; dy <= 1; dy++) {
                int yy = canopyY + dy;
                int layerRadius = Math.max(1, canopyRadius - (dy == 0 ? 0 : 1));
                for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                    for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                        int xx = cx + dx, zz = cz + dz;
                        if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                        double distSq = dx * dx + dz * dz;
                        double maxDist = layerRadius * layerRadius + (dy == 0 ? 1.5 : 0.5);
                        if (distSq > maxDist) continue;
                        if (rng.nextDouble() < 0.15 && distSq > layerRadius * layerRadius - 1) continue;
                        if (safeType(data, xx, yy, zz) == Material.AIR) data.setBlock(xx, yy, zz, persistentLeaves);
                        if (dy == -1 && rng.nextDouble() < 0.3) {
                            int hangY = yy - 1;
                            if (hangY >= 0 && safeType(data, xx, hangY, zz) == Material.AIR) {
                                data.setBlock(xx, hangY, zz, persistentLeaves);
                            }
                        }
                    }
                }
            }

            // Small cap on the top to smooth silhouettes
            int topCapY = canopyY + 2;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 1) continue;
                    int xx = cx + dx, zz = cz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    if (safeType(data, xx, topCapY, zz) == Material.AIR && rng.nextDouble() < 0.6) {
                        data.setBlock(xx, topCapY, zz, persistentLeaves);
                    }
                }
            }
        }

        // Thick grasses
        Material grassPlant = pickGrassPlant();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SAVANNAH) continue;
                int topY = topYGrid[lx][lz];
                Material ground = safeType(data, lx, topY, lz);
                Material above = safeType(data, lx, topY + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    if (rng.nextDouble() < 0.4) data.setBlock(lx, topY + 1, lz, grassPlant);
                }
            }
        }
    }
}

