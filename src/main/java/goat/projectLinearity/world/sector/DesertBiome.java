package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class DesertBiome extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double h1 = valueNoise2(seed ^ 0xD3512E57L, (double) wx / 240.0, (double) wz / 240.0);
        double h2 = valueNoise2(seed ^ 0xD3512E58L, (double) wx / 80.0, (double) wz / 80.0);
        double h3 = valueNoise2(seed ^ 0xD3512E59L, (double) wx / 34.0, (double) wz / 34.0);
        double h = (h1 * 0.52 + h2 * 0.32 + h3 * 0.16) * 2.0 - 1.0;
        int base = 163;
        int amp = 18;
        return base + (int) Math.round(h * amp);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 3546184538L);
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.DESERT) continue;
                int topY = topYGrid[lx][lz];
                if (topY <= world.getMinHeight() + 1) continue;
                Material ground;
                try { ground = data.getType(lx, topY, lz); } catch (Throwable t) { ground = Material.SAND; }
                boolean sandy = ground == Material.SAND || ground == Material.RED_SAND || ground == Material.SANDSTONE;
                if (!sandy) continue;
                double r = rng.nextDouble();
                if (r < 0.005) {
                    if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
                    int maxH = Math.min(world.getMaxHeight() - 2, topY + 4);
                    int height = 1 + rng.nextInt(3);
                    if (topY + height >= maxH) height = Math.max(1, maxH - topY - 1);
                    boolean clear = true;
                    for (int dy = 1; dy <= height; ++dy) {
                        Material above2;
                        try { above2 = data.getType(lx, topY + dy, lz); } catch (Throwable t) { above2 = Material.AIR; }
                        if (above2 != Material.AIR) { clear = false; break; }
                        Material e1 = safeType(data, lx + 1, topY + dy, lz);
                        Material e2 = safeType(data, lx - 1, topY + dy, lz);
                        Material e3 = safeType(data, lx, topY + dy, lz + 1);
                        Material e4 = safeType(data, lx, topY + dy, lz - 1);
                        if (e1 == Material.AIR && e2 == Material.AIR && e3 == Material.AIR && e4 == Material.AIR) continue;
                        clear = false; break;
                    }
                    if (!clear) continue;
                    for (int dy = 1; dy <= height; ++dy) data.setBlock(lx, topY + dy, lz, Material.CACTUS);
                } else if (r < 0.01) {
                    Material above;
                    try { above = data.getType(lx, topY + 1, lz); } catch (Throwable t) { above = Material.AIR; }
                    if (above != Material.AIR) continue;
                    try { data.setBlock(lx, topY + 1, lz, Material.DEAD_BUSH); } catch (Throwable ignore) {}
                }
            }
        }
    }
}
