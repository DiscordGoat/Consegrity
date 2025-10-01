package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public class MesaSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        // Keep mesa base relief similar to dunes/strata
        double h1 = valueNoise2(seed ^ 0xBADA11D5L, (double) wx / 300.0, (double) wz / 300.0);
        double h2 = valueNoise2(seed ^ 0xBADA11D6L, (double) wx / 96.0, (double) wz / 96.0);
        double h3 = valueNoise2(seed ^ 0xBADA11D7L, (double) wx / 36.0, (double) wz / 36.0);
        double baseNoise = (h1 * 0.55 + h2 * 0.30 + h3 * 0.15) * 2.0 - 1.0;
        // Flatter base plateau
        double base = 170.0 + baseNoise * 6.0;

        // Mountain overlay: match Jungle mountain frequency but twice the height, heavier erosion
        double field = 0.0;
        field += peakSum(seed, wx, wz, 140.0, 56.0, 36.0, 2.2, 0xA11L);
        field += peakSum(seed, wx, wz, 96.0, 36.0, 24.0, 2.0, 0xA12L);
        field += peakSum(seed, wx, wz, 64.0, 22.0, 14.0, 1.8, 0xA13L);

        double ridge = fbm2(seed ^ 0xCAFED00DL, (double) wx * 0.018, (double) wz * 0.018);
        double plains = (fbm2(seed ^ 0x600DD00EL, (double) wx * 0.02, (double) wz * 0.02) - 0.5) * 2.0 * 4.2;
        double micro = (fbm2(seed ^ 0x33AA55CCL, (double) wx * 0.06, (double) wz * 0.06) - 0.5) * 2.0 * 2.8;

        // Twice jungle mountain height (jungle uses 0.5 * (...)) and add strong erosion subtraction
        double rawMountains = (plains + micro + (ridge - 0.5) * 2.0 * 16.0 + field);
        double erosion = (fbm2(seed ^ 0xE0B0DE5L, (double) wx * 0.03, (double) wz * 0.03) - 0.5) * 2.0
                + (fbm2(seed ^ 0xE0B0DE6L, (double) wx * 0.008, (double) wz * 0.008) - 0.5) * 2.0;
        double mountains = 1.0 * rawMountains - erosion * 8.0; // stronger erosion

        int yCap = Math.min(360, Math.max(256, world.getMaxHeight() - 1));
        double H = base + mountains;
        if (H < 150.0) H = 150.0;
        if (H > yCap) H = yCap;
        return (int) Math.round(H);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                         int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        // Rare cactus/dead bush for mesa: 25% of desert cactus rate; dead bush also rare
        // Desert cactus rate is 0.5% per sandy block -> 0.125% here
        final double CACTUS_P = 0.005 * 0.25;      // 0.00125
        final double BUSH_P   = 0.005 * 0.25;      // rare dead bushes

        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.MESA) continue;
                int topY = topYGrid[lx][lz];
                if (topY <= world.getMinHeight() + 1) continue;
                Material ground = safeType(data, lx, topY, lz);
                boolean sandy = ground == Material.SAND || ground == Material.RED_SAND || ground == Material.SANDSTONE;
                if (!sandy) continue;
                double r = rngFor(seed, (chunkX << 4) + lx, (chunkZ << 4) + lz, 0x5A11L).nextDouble();
                if (r < CACTUS_P) {
                    if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
                    int maxH = Math.min(world.getMaxHeight() - 2, topY + 4);
                    int height = 1 + rngFor(seed, chunkX, chunkZ, 0xCACCAL).nextInt(3);
                    if (topY + height >= maxH) height = Math.max(1, maxH - topY - 1);
                    boolean clear = true;
                    for (int dy = 1; dy <= height; ++dy) {
                        if (safeType(data, lx, topY + dy, lz) != Material.AIR) { clear = false; break; }
                        Material e1 = safeType(data, lx + 1, topY + dy, lz);
                        Material e2 = safeType(data, lx - 1, topY + dy, lz);
                        Material e3 = safeType(data, lx, topY + dy, lz + 1);
                        Material e4 = safeType(data, lx, topY + dy, lz - 1);
                        if (e1 == Material.AIR && e2 == Material.AIR && e3 == Material.AIR && e4 == Material.AIR) continue;
                        clear = false; break;
                    }
                    if (!clear) continue;
                    for (int dy = 1; dy <= height; ++dy) data.setBlock(lx, topY + dy, lz, Material.CACTUS);
                } else if (r < CACTUS_P + BUSH_P) {
                    if (safeType(data, lx, topY + 1, lz) != Material.AIR) continue;
                    try { data.setBlock(lx, topY + 1, lz, Material.DEAD_BUSH); } catch (Throwable ignore) {}
                }
            }
        }
    }

    // Local helpers mirrored from JungleSector for mountain overlay/erosion
    private static double fbm2(long seed, double x, double z) {
        double sum = 0.0, amp = 1.0, freq = 1.0;
        for (int o = 0; o < 5; ++o) {
            sum += (valueNoise2(seed, x * freq, z * freq) - 0.5) * 2.0 * amp;
            amp *= 0.55; freq *= 1.9;
        }
        return clamp01(sum * 0.5 + 0.5);
    }

    private double peakSum(long seed, int wx, int wz, double cell, double baseRadius, double baseAmp, double power, long salt) {
        int ix = (int) Math.floor((double) wx / cell);
        int iz = (int) Math.floor((double) wz / cell);
        double sum = 0.0;
        for (int cx = ix - 1; cx <= ix + 1; ++cx) {
            for (int cz = iz - 1; cz <= iz + 1; ++cz) {
                double jx = (rand01(hash(seed, cx, 51L, cz, salt ^ 1L)) - 0.5) * (cell * 0.8);
                double jz = (rand01(hash(seed, cx, 87L, cz, salt ^ 2L)) - 0.5) * (cell * 0.8);
                double px = (double) cx * cell + jx, pz = (double) cz * cell + jz;
                double dx = (double) wx - px, dz = (double) wz - pz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double rScale = 0.7 + rand01(hash(seed, cx, 21L, cz, salt ^ 3L)) * 1.0;
                double radius = Math.max(6.0, baseRadius * rScale);
                if (dist >= radius) continue;
                double aScale = 0.6 + rand01(hash(seed, cx, 11L, cz, salt ^ 4L)) * 1.2;
                double amp = baseAmp * aScale;
                double t = 1.0 - dist / radius;
                sum += amp * Math.pow(t, power);
            }
        }
        return sum;
    }
}
