package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class CentralSector extends SectorBase {
    private static final int SEA_LEVEL = 153; // keep consistent with generator

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0xC3A1A15AL, (double) wx / 180.0, (double) wz / 180.0);
        double h2 = valueNoise2(seed ^ 0xC3A1A15BL, (double) wx / 64.0, (double) wz / 64.0);
        double h = (h1 * 0.7 + h2 * 0.3) * 2.0 - 1.0;
        int base = 159;
        int amp = 9;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        if (centralMaskGrid == null) return;
        placeCentralTrees(world, data, seed, chunkX, chunkZ, topYGrid, centralMaskGrid);
        placeCentralGrass(world, data, seed, chunkX, chunkZ, topYGrid, centralMaskGrid);
    }

    private void placeCentralTrees(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, double[][] centralMaskGrid) {
        Random rng = new Random(hash(seed, chunkX, 123L, chunkZ, 466661L));
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (centralMaskGrid[lx][lz] < 0.6) continue;
                int y = topYGrid[lx][lz];
                if (y <= 159) continue;
                int h = y;
                int countLower = 0;
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = lx + dx, nz = lz + dz;
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                        int hy = topYGrid[nx][nz];
                        if (hy < y) countLower++;
                    }
                }
                boolean isHill = countLower >= 4;
                if (!isHill || rng.nextDouble() > 0.25) continue;
                Material wood = rng.nextDouble() < 0.9 ? Material.OAK_LOG : Material.BIRCH_LOG;
                Material leaves = (wood == Material.OAK_LOG) ? Material.OAK_LEAVES : Material.BIRCH_LEAVES;
                growSimpleTree(data, lx, h + 1, lz, wood, leaves, rng);
            }
        }
    }

    private void placeCentralGrass(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, double[][] centralMaskGrid) {
        java.util.SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 206158430L);
        Material grassPlant = pickGrassPlant();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (centralMaskGrid[lx][lz] < 0.5) continue;
                int topY = topYGrid[lx][lz];
                if (topY <= 157) continue;
                Material ground = safeType(data, lx, topY, lz);
                Material above = safeType(data, lx, topY + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    double roll = rng.nextDouble();
                    if (roll < 0.03) {
                        Material flower = rng.nextDouble() < 0.55 ? Material.POPPY : Material.DANDELION;
                        data.setBlock(lx, topY + 1, lz, flower);
                    } else if (roll < 0.11) {
                        data.setBlock(lx, topY + 1, lz, grassPlant);
                    }
                }
            }
        }
    }
}

