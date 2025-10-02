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
            if (cx < 0 || cx > 15 || cz < 0 || cz > 15) continue;

            // Use Central-style canopy shape for savanna trees
            placeCentralStyleCanopy(data, persistentLeaves, cx, canopyY, cz);
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

    private void placeCentralStyleCanopy(ChunkGenerator.ChunkData data, BlockData leaves, int cx, int cy, int cz) {
        if (cy < 0) return;
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                int r2 = dx * dx + dz * dz;
                int xx = cx + dx;
                int zz = cz + dz;
                if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || r2 > 10) continue;
                data.setBlock(xx, cy, zz, leaves);
                if (r2 <= 5) data.setBlock(xx, cy + 1, zz, leaves);
            }
        }
    }

    private void placeSavannaCanopy(ChunkGenerator.ChunkData data, BlockData leaves, SplittableRandom rng, int cx, int cy, int cz, int dirX, int dirZ) {
        if (dirX == 0 && dirZ == 0) {
            dirX = 1;
            dirZ = 0;
        }
        placeLeavesLayer(data, leaves, cx, cy - 1, cz, PRIMARY_CANOPY_BELOW_BASE, dirX, dirZ);
        placeLeavesLayer(data, leaves, cx, cy, cz, PRIMARY_CANOPY_MAIN_BASE, dirX, dirZ);
        placeLeavesLayer(data, leaves, cx, cy + 1, cz, PRIMARY_CANOPY_TOP_BASE, dirX, dirZ);

        int backX = cx - dirX;
        int backZ = cz - dirZ;
        placeLeavesLayer(data, leaves, backX, cy - 1, backZ, SECONDARY_CANOPY_BELOW, dirX, dirZ);
        placeLeavesLayer(data, leaves, backX, cy, backZ, SECONDARY_CANOPY_MAIN, dirX, dirZ);
        placeLeavesLayer(data, leaves, backX, cy + 1, backZ, SECONDARY_CANOPY_TOP, dirX, dirZ);

        // Occasional dangling leaves below the canopy to mimic natural acacia droop.
        for (int[] offset : PRIMARY_CANOPY_DANGLE_BASE) {
            int dx = rotateX(offset[0], offset[1], dirX, dirZ);
            int dz = rotateZ(offset[0], offset[1], dirX, dirZ);
            if (rng.nextDouble() > 0.35) continue;
            int xx = cx + dx;
            int zz = cz + dz;
            int yy = cy - 2;
            if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || yy < 0) continue;
            if (safeType(data, xx, yy, zz) == Material.AIR) {
                data.setBlock(xx, yy, zz, leaves);
            }
        }
    }

    private void placeLeavesLayer(ChunkGenerator.ChunkData data, BlockData leaves, int cx, int cy, int cz, int[][] pattern, int dirX, int dirZ) {
        if (cy < 0) return;
        for (int[] offset : pattern) {
            int dx = rotateX(offset[0], offset[1], dirX, dirZ);
            int dz = rotateZ(offset[0], offset[1], dirX, dirZ);
            int xx = cx + dx;
            int zz = cz + dz;
            if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
            if (safeType(data, xx, cy, zz) == Material.AIR) {
                data.setBlock(xx, cy, zz, leaves);
            }
        }
    }

    private int rotateX(int x, int z, int dirX, int dirZ) {
        if (dirX == 1 && dirZ == 0) return x;
        if (dirX == -1 && dirZ == 0) return -x;
        if (dirX == 0 && dirZ == 1) return -z;
        if (dirX == 0 && dirZ == -1) return z;
        return x;
    }

    private int rotateZ(int x, int z, int dirX, int dirZ) {
        if (dirX == 1 && dirZ == 0) return z;
        if (dirX == -1 && dirZ == 0) return -z;
        if (dirX == 0 && dirZ == 1) return x;
        if (dirX == 0 && dirZ == -1) return -x;
        return z;
    }

    private static final int[][] PRIMARY_CANOPY_MAIN_BASE = {
            {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0},
            {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {2, 1}, {2, -1}, {3, 1}, {3, -1}, {4, 1}, {4, -1},
            {0, 2}, {0, -2}, {1, 2}, {1, -2}, {2, 2}, {2, -2}
    };

    private static final int[][] PRIMARY_CANOPY_TOP_BASE = {
            {1, 0}, {2, 0}, {3, 0}, {4, 0},
            {1, 1}, {1, -1}, {2, 1}, {2, -1}, {3, 1}, {3, -1}
    };

    private static final int[][] PRIMARY_CANOPY_BELOW_BASE = {
            {0, 0}, {1, 0}, {2, 0}, {3, 0},
            {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {2, 1}, {2, -1}, {3, 1}, {3, -1}
    };

    private static final int[][] PRIMARY_CANOPY_DANGLE_BASE = {
            {1, 0}, {2, 0},
            {1, 1}, {1, -1}
    };

    private static final int[][] SECONDARY_CANOPY_MAIN = {
            {0, 0}, {1, 0}, {-1, 0}, {-2, 0},
            {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static final int[][] SECONDARY_CANOPY_TOP = {
            {0, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}
    };

    private static final int[][] SECONDARY_CANOPY_BELOW = {
            {0, 0}, {-1, 0},
            {0, 1}, {0, -1}
    };
}
