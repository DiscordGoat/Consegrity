package goat.projectLinearity.subsystems.world.sector;

import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.block.data.type.PinkPetals;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class CherrySector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0x7C3EE111L, (double) wx / 260.0, (double) wz / 260.0);
        double h2 = valueNoise2(seed ^ 0x7C3EE112L, (double) wx / 88.0, (double) wz / 88.0);
        double h3 = valueNoise2(seed ^ 0x7C3EE113L, (double) wx / 34.0, (double) wz / 34.0);
        double h = (h1 * 0.52 + h2 * 0.32 + h3 * 0.16) * 2.0 - 1.0;
        int base = 177;
        int amp = 26;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 3303206417L);
        boolean[][] treeBases = new boolean[16][16];

        int treeAttempts = 4; // reduced density
        for (int i = 0; i < treeAttempts; ++i) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY) continue;
            int groundY = topYGrid[lx][lz];
            if (groundY < 160 || groundY > 205) continue;
            if (slopeGrid(topYGrid, lx, lz) > 4) continue;
            Material ground = safeType(data, lx, groundY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT && ground != Material.STONE) continue;
            int height = 5 + rng.nextInt(3);
            placeCherryTree(data, lx, groundY + 1, lz, height, rng);
            treeBases[lx][lz] = true;
        }

        // flower carpet around bases
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY) continue;
                int groundY = topYGrid[lx][lz];
                if (groundY < 156 || groundY > 210) continue;
                if (slopeGrid(topYGrid, lx, lz) > 4) continue;
                Material ground = safeType(data, lx, groundY, lz);
                Material above = safeType(data, lx, groundY + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    double chance = 0.08;
                    if (rng.nextDouble() < chance) {
                        try {
                            BlockData petals = Bukkit.createBlockData(Material.PINK_PETALS);
                            ((PinkPetals) petals).setFlowerAmount(1 + rng.nextInt(4));
                            data.setBlock(lx, groundY + 1, lz, petals);
                        } catch (Throwable ignore) {
                            data.setBlock(lx, groundY + 1, lz, Material.AZURE_BLUET);
                        }
                    }
                }
            }
        }

        // Sprout wild carrots on grassy patches
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY) continue;
                int groundY = topYGrid[lx][lz];
                Material ground = safeType(data, lx, groundY, lz);
                if (ground != Material.GRASS_BLOCK) continue;
                if (safeType(data, lx, groundY + 1, lz) != Material.AIR) continue;
                if (rng.nextDouble() > 0.003) continue;

                BlockData farmland = Material.FARMLAND.createBlockData();
                if (farmland instanceof Farmland tilled) {
                    tilled.setMoisture(tilled.getMaximumMoisture());
                    farmland = tilled;
                }
                data.setBlock(lx, groundY, lz, farmland);

                if (groundY - 1 >= world.getMinHeight() && safeType(data, lx, groundY - 1, lz) == Material.AIR) {
                    data.setBlock(lx, groundY - 1, lz, Material.DIRT);
                }

                BlockData carrot = Material.CARROTS.createBlockData();
                if (carrot instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    carrot = ageable;
                }
                data.setBlock(lx, groundY + 1, lz, carrot);
                topYGrid[lx][lz] = groundY + 1;
            }
        }
    }

    private void placeCherryTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height, SplittableRandom rng) {
        for (int i = 0; i < height; ++i) data.setBlock(lx, y + i, lz, Material.CHERRY_LOG);
        int top = y + height - 1;
        // simple irregular canopy with petals
        int r = 3;
        org.bukkit.block.data.BlockData cherryLeaves = persistentLeaves(Material.CHERRY_LEAVES, 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                int xx = lx + dx, zz = lz + dz;
                int yy = top + (rng.nextDouble() < 0.2 ? 1 : 0);
                if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                Material existing = safeType(data, xx, yy, zz);
                if (existing == Material.AIR) data.setBlock(xx, yy, zz, cherryLeaves);
                if (rng.nextDouble() < 0.15 && yy - 1 >= 0 && safeType(data, xx, yy - 1, zz) == Material.AIR) {
                    try {
                        BlockData petals = Bukkit.createBlockData(Material.PINK_PETALS);
                        ((PinkPetals) petals).setFlowerAmount(1 + rng.nextInt(4));
                        data.setBlock(xx, yy - 1, zz, petals);
                    } catch (Throwable ignore) {}
                }
            }
        }
        // optional sideways branch
        if (rng.nextDouble() < 0.5) {
            int dir = rng.nextInt(4);
            int ox = dir == 0 ? 1 : (dir == 1 ? -1 : 0);
            int oz = dir == 2 ? 1 : (dir == 3 ? -1 : 0);
            int branchY = top - 1;
            int bx = lx + ox, bz = lz + oz;
            if (bx >= 0 && bx <= 15 && bz >= 0 && bz <= 15) {
                data.setBlock(bx, branchY, bz, Material.CHERRY_LOG);
                // orient if supported
                try { BlockData bd = Bukkit.createBlockData(Material.CHERRY_LOG); ((Orientable) bd).setAxis(ox != 0 ? org.bukkit.Axis.X : org.bukkit.Axis.Z); data.setBlock(bx, branchY, bz, bd); } catch (Throwable ignore) {}
            }
        }
    }
}
