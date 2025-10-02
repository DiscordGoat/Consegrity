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
        // Only increase amplitude for land (non-water) cells
        boolean water = isSwampWater(seed, wx, wz);
        int amp = water ? 2 : 6;
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
                boolean water = isSwampWater(seed, wx, wz);
                if (water) {
                    // Indent water below nearby terrain instead of pasting on top
                    int surfaceY = Math.max(world.getMinHeight(), topY - 1);
                    // Clear any blocks above the water surface within this column to expose water
                    for (int yy = topY; yy > surfaceY; yy--) {
                        if (safeType(data, lx, yy, lz) != Material.AIR) {
                            data.setBlock(lx, yy, lz, Material.AIR);
                        }
                    }
                    // Place the water surface one block below original top
                    data.setBlock(lx, surfaceY, lz, Material.WATER);
                    if (rng.nextDouble() < 0.10 && safeType(data, lx, surfaceY + 1, lz) == Material.AIR) {
                        try { data.setBlock(lx, surfaceY + 1, lz, lily); } catch (Throwable ignore) {}
                    }
                    topYGrid[lx][lz] = surfaceY;

                    // Build grassy banks around water where water meets air, and add a gentle slope into terrain
                    int[][] dirs = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
                    for (int[] dxy : dirs) {
                        int nx = lx + dxy[0];
                        int nz = lz + dxy[1];
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                        if (regionGrid[nx][nz] != ConsegrityRegions.Region.SWAMP) continue;
                        // Only build a bank if this neighbor is air at bank height and not water
                        Material atBank = safeType(data, nx, surfaceY, nz);
                        Material aboveBank = safeType(data, nx, surfaceY + 1, nz);
                        if (atBank == Material.WATER) continue;
                        if (aboveBank != Material.AIR) continue;

                        // Place grass on the bank at surface height, with dirt support beneath if needed
                        data.setBlock(nx, surfaceY, nz, Material.GRASS_BLOCK);
                        if (surfaceY - 1 >= world.getMinHeight() && safeType(data, nx, surfaceY - 1, nz) == Material.AIR) {
                            data.setBlock(nx, surfaceY - 1, nz, Material.DIRT);
                        }
                        // Update topY for the bank cell
                        topYGrid[nx][nz] = Math.max(topYGrid[nx][nz], surfaceY);

                        // Extend grassy slopes both directions from the bank, descending in Y
                        int slopeLen = 2;
                        for (int sign = -1; sign <= 1; sign += 2) { // -1: toward water, +1: toward land
                            for (int step = 1; step <= slopeLen; step++) {
                                int sx = nx + dxy[0] * sign * step;
                                int sz = nz + dxy[1] * sign * step;
                                if (sx < 0 || sx > 15 || sz < 0 || sz > 15) break;
                                if (regionGrid[sx][sz] != ConsegrityRegions.Region.SWAMP) break;
                                int swx = baseX + sx, swz = baseZ + sz;
                                if (sign > 0 && isSwampWater(seed, swx, swz)) break; // don't slope into water on land side
                                int targetY = surfaceY - step; // descend from bank
                                if (targetY < world.getMinHeight()) break;
                                if (safeType(data, sx, targetY, sz) != Material.AIR) continue;
                                data.setBlock(sx, targetY, sz, Material.GRASS_BLOCK);
                                if (targetY - 1 >= world.getMinHeight() && safeType(data, sx, targetY - 1, sz) == Material.AIR) {
                                    data.setBlock(sx, targetY - 1, sz, Material.DIRT);
                                }
                                if (topYGrid[sx][sz] < targetY) topYGrid[sx][sz] = targetY;
                            }
                        }
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
            if (isSwampWater(seed, wx, wz)) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.18) continue;
            growSwampTree(data, lx, topY + 1, lz, Material.OAK_LOG, Material.OAK_LEAVES, new Random(rng.nextLong()));
            if (rng.nextBoolean()) addVinesAround(data, lx, topY + 1, lz, rng);
        }

        // Add some grass like the mountains, but only on land
        Material grassPlant = pickGrassPlant();
        for (int t = 0; t < 20; t++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
            int topY = topYGrid[lx][lz];
            int wx = baseX + lx, wz = baseZ + lz;
            if (isSwampWater(seed, wx, wz)) continue;
            Material ground = safeType(data, lx, topY, lz);
            if (ground == Material.WATER) continue; // Don't grow grass on water blocks
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT && ground != Material.MUD) continue;
            if (safeType(data, lx, topY + 1, lz) != Material.AIR) continue;
            data.setBlock(lx, topY + 1, lz, grassPlant);
        }
    }

    private void growSwampTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, Material log, Material leaves, Random rng) {
        int height = 5 + rng.nextInt(3); // Increased height by 1
        for (int i = 0; i < height; ++i) data.setBlock(lx, y + i, lz, log);
        int cy = y + height - 2; // Keep canopy at original height
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                int r2 = dx * dx + dz * dz;
                int x = lx + dx, z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15 || r2 > 6) continue;
                data.setBlock(x, cy, z, leaves);
                if (r2 <= 3) data.setBlock(x, cy + 1, z, leaves);
            }
        }
    }

    // Determine whether this swamp cell should be water based on blended noise fields
    private boolean isSwampWater(long seed, int wx, int wz) {
        final double waterFrac = 0.35;
        final double macroScale = 48.0;
        final double detailScale = 18.0;
        final double microScale = 8.0;

        double macro = valueNoise2(seed ^ 0x5A4D00A1L, (double) wx / macroScale, (double) wz / macroScale);
        double detail = valueNoise2(seed ^ 0x5A4D00A2L, (double) wx / detailScale, (double) wz / detailScale);
        double micro = valueNoise2(seed ^ 0x5A4D00A3L, (double) wx / microScale, (double) wz / microScale);
        double dryness = valueNoise2(seed ^ 0x6B8B45ADL, (double) wx / 110.0, (double) wz / 110.0);

        double blended = macro * 0.55 + detail * 0.30 + micro * 0.15;
        double threshold = waterFrac + (dryness - 0.5) * 0.18;
        double jitter = (micro - 0.5) * 0.08;

        boolean water = blended + jitter < threshold;
        if (!water) {
            double patch = valueNoise2(seed ^ 0x5A4D00A4L, (double) wx / 26.0, (double) wz / 26.0);
            if (patch < threshold - 0.10 && dryness < 0.62) {
                water = true;
            }
        }
        return water;
    }
}
