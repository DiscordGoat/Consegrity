package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
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
            int r = 2 + rng.nextInt(2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                    int xx = cx + dx, zz = cz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    if (safeType(data, xx, canopyY, zz) == Material.AIR) data.setBlock(xx, canopyY, zz, Material.ACACIA_LEAVES);
                    if (rng.nextDouble() < 0.35 && safeType(data, xx, canopyY + 1, zz) == Material.AIR)
                        data.setBlock(xx, canopyY + 1, zz, Material.ACACIA_LEAVES);
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

