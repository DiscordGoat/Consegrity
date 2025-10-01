package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class MountainSector extends SectorBase {
    // Constants mirrored from generator
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    private static final int SEA_LEVEL = 153;
    private static final double[] R1_SPLITS = new double[]{0.0, 0.2, 0.4, 0.6, 0.8, 1.0};

    // Massive Mountain (MM)
    private static final int MM_GROUND_Y = 160;
    private static final double MM_EFFECT_R = 150.0;
    private static final double MM_CORE_R = 26.0;
    private static final double MM_CROWN_IN = 18.0;
    private static final double MM_CROWN_OUT = 36.0;
    private static final double MM_RANGE_CELL = 180.0;
    private static final double MM_VALLEY_R0 = 60.0;
    private static final double MM_VALLEY_R1 = 110.0;

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        // Baseline mountain field
        double base = 155.0;
        double low = fbm2(seed ^ 0xA1B2C3D4L, (double) wx * 0.003, (double) wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = 0.0, t1 = 0.38;
        double mask = clamp01((mountainBand - t0) / (t1 - t0));

        double field = 0.0;
        field += peakSum(seed, wx, wz, 176.0, 70.0, 64.0, 2.4, 2576L);
        field += peakSum(seed, wx, wz, 112.0, 46.0, 44.0, 2.1, 2832L);
        field += peakSum(seed, wx, wz, 72.0, 30.0, 28.0, 1.9, 3088L);
        field += peakSum(seed, wx, wz, 48.0, 20.0, 16.0, 1.75, 3344L);

        double ridge = fbm2(seed ^ 0xB3C4D5E6L, (double) wx * 0.02, (double) wz * 0.02);
        double plains = (fbm2(seed ^ 0x600DD00EL, (double) wx * 0.02, (double) wz * 0.02) - 0.5) * 2.0 * 4.8;
        double between = (fbm2(seed ^ 0x77EEDD11L, (double) wx * 0.01, (double) wz * 0.01) - 0.5) * 2.0;
        double betweenAmp = 18.0 * (1.0 - mask * 0.85);
        double micro = (fbm2(seed ^ 0x33AA55CCL, (double) wx * 0.06, (double) wz * 0.06) - 0.5) * 2.0 * 2.6;

        double baseH = base + plains + between * betweenAmp + micro + (ridge - 0.5) * 2.0 * 20.0 * mask + field;
        baseH += fbm2(seed ^ 0xE01234L, (double) wx * 0.004, (double) wz * 0.004) * 70.0 * mask;

        int yCap = Math.min(400, Math.max(256, world.getMaxHeight() - 1));
        double mmW = massiveMask(seed, wx, wz);
        if (mmW > 0.0) {
            V2 c = massiveMountainCenter(seed);
            double dx = (double) wx - c.x, dz = (double) wz - c.z;
            double d = Math.hypot(dx, dz);

            double rise = Math.pow(clamp01(1.0 - d / (MM_EFFECT_R)), 1.45);
            double H = MM_GROUND_Y + rise * (yCap - MM_GROUND_Y);
            if (d <= MM_CORE_R) {
                H = Math.max(H, yCap - 6);
            }
            if (d > MM_CROWN_IN && d < MM_CROWN_OUT) {
                double t = 1.0 - Math.abs((d - (MM_CROWN_IN + MM_CROWN_OUT) * 0.5) / ((MM_CROWN_OUT - MM_CROWN_IN) * 0.5));
                double spike = (valueNoise2(seed ^ 0x51ABCDL, (double) wx / 6.0, (double) wz / 6.0) - 0.5) * 2.0;
                H += Math.max(0.0, t) * (10.0 + spike * 14.0);
            }
            double step = 5.5;
            H = Math.floor(H / step) * step + (H - Math.floor(H / step) * step) * 0.35;
            if (d > MM_VALLEY_R0 && d < MM_VALLEY_R1) {
                double vt = 1.0 - Math.abs((d - (MM_VALLEY_R0 + MM_VALLEY_R1) * 0.5) / ((MM_VALLEY_R1 - MM_VALLEY_R0) * 0.5));
                H = lerp(H, MM_GROUND_Y, smooth01(clamp01(vt)) * 0.85);
            }
            double ring = clamp01(1.0 - Math.abs(d - (MM_VALLEY_R1 + MM_EFFECT_R) * 0.5) / (MM_EFFECT_R * 0.5));
            if (ring > 0.0) {
                double rr = (fbm2(seed ^ 0xA55A5AL, (double) wx / MM_RANGE_CELL, (double) wz / MM_RANGE_CELL) - 0.5) * 2.0;
                H += ring * rr * 60.0;
            }
            {
                double e1 = fbm2(seed ^ 0xE0B0DE5L, (double) wx * 0.02, (double) wz * 0.02);
                double e2 = fbm2(seed ^ 0xE0B0DE6L, (double) wx * 0.005, (double) wz * 0.005);
                double er = clamp01(e1 * 0.65 + e2 * 0.35);
                double u = clamp01(d / Math.max(1.0, MM_EFFECT_R));
                double wall = smooth01(1.0 - Math.abs(u - 0.5) / 0.5);
                double heightMask = smooth01(clamp01((H - MM_GROUND_Y) / Math.max(1.0, (yCap - MM_GROUND_Y))));
                double erosion = Math.pow(er, 1.2) * 18.0 * wall * heightMask;
                H = Math.max(MM_GROUND_Y, H - erosion);
            }
            baseH = Math.max(baseH, H);
        }
        if (baseH < 150.0) baseH = 150.0;
        if (baseH > yCap) baseH = yCap;
        return (int) Math.floor(baseH);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        placeMountainRivers(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        placeMountainTaigaEdge(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
    }

    // --- mountain features ---
    private void placeMountainRivers(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        int riverY = Math.max(MM_GROUND_Y, SEA_LEVEL);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.MOUNTAIN) continue;
                int wx = baseX + lx, wz = baseZ + lz;
                double vm = valleyMask(seed, wx, wz);
                if (vm < 0.65) continue;
                int flatY = riverY;
                data.setRegion(lx, Math.max(world.getMinHeight(), flatY - 2), lz, lx + 1, flatY, lz + 1, Material.STONE);
                data.setBlock(lx, flatY, lz, Material.WATER);
                if (safeType(data, lx, flatY + 1, lz) == Material.AIR && vm > 0.85) {
                    data.setBlock(lx, flatY + 1, lz, Material.WATER);
                }
                topYGrid[lx][lz] = flatY;
            }
        }
    }

    private void placeMountainTaigaEdge(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 0x55AA33L);
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int t = 0; t < 12; t++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.MOUNTAIN) continue;
            int wx = baseX + lx, wz = baseZ + lz;
            double edge = taigaEdgeMask(seed, wx, wz);
            if (edge < 0.6) continue;
            if (slopeGrid(topYGrid, lx, lz) > 3) continue;
            int y = topYGrid[lx][lz];
            Material g = safeType(data, lx, y, lz);
            if (g != Material.GRASS_BLOCK && g != Material.DIRT && g != Material.SNOW_BLOCK && g != Material.STONE) continue;
            // avoid reusing spots that already grew a tree (logs or other blocks above ground)
            Material above = safeType(data, lx, y + 1, lz);
            if (above != Material.AIR) continue;
            if (rng.nextDouble() < 0.45 && y + 2 < world.getMaxHeight() - 1) {
                data.setBlock(lx, y + 1, lz, Material.DIRT);
                data.setBlock(lx, y + 2, lz, Material.GRASS_BLOCK);
                topYGrid[lx][lz] = y + 2;
                y += 2;
            }
            int h = 5 + rng.nextInt(4);
            placeSpruceSimple(data, lx, y + 1, lz, h);
        }
    }

    // --- massif helpers ---
    private static class V2 { final double x, z; V2(double x, double z) { this.x = x; this.z = z; } }

    private V2 massiveMountainCenter(long seed) {
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        double rotR1 = wrap01(baseRot);
        double a0 = R1_SPLITS[2], a1 = R1_SPLITS[3];
        double uMid = wrap01((a0 + a1) * 0.5 + rotR1);
        double ang = uMid * Math.PI * 2.0 - Math.PI;
        double rMid = (120.0 + 520.0) * 0.5; // ring1 mid radius (mirrors generator)
        double jx = (rand01(hash(seed, 9001L, 0L, 0L, 0xCAFEBABEL)) - 0.5) * 40.0;
        double jz = (rand01(hash(seed, 9002L, 0L, 0L, 0xCAFED00DL)) - 0.5) * 40.0;
        double x = CENTER_X + Math.cos(ang) * rMid + jx;
        double z = CENTER_Z + Math.sin(ang) * rMid + jz;
        return new V2(x, z);
    }

    private boolean inMountainSector(long seed, int wx, int wz) {
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        double rotR1 = wrap01(baseRot);
        double u = wrap01(angle01Warped(seed, wx, wz) + rotR1);
        int idx = arcIndex(u, R1_SPLITS);
        return idx == 2; // mountains wedge index in ring1
    }

    private double massiveMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;
        V2 c = massiveMountainCenter(seed);
        double dx = wx - c.x, dz = wz - c.z;
        double d = Math.hypot(dx, dz);
        double radial = smooth01(clamp01(1.0 - d / MM_EFFECT_R));
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        double rotR1 = wrap01(baseRot);
        double u = wrap01(angle01Warped(seed, wx, wz) + rotR1);
        double a0 = R1_SPLITS[2], a1 = R1_SPLITS[3];
        double uMid = wrap01((a0 + a1) * 0.5 + rotR1);
        double diff = Math.abs(u - uMid);
        if (diff > 0.5) diff = 1.0 - diff;
        double halfWidth = Math.max(1e-6, (a1 - a0) * 0.5);
        double angNorm = clamp01(1.0 - diff / halfWidth);
        double angular = smooth01(Math.pow(angNorm, 0.9));
        return radial * angular;
    }

    private double valleyMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;
        V2 c = massiveMountainCenter(seed);
        double d = Math.hypot(wx - c.x, wz - c.z);
        double mid = (MM_VALLEY_R0 + MM_VALLEY_R1) * 0.5;
        double half = (MM_VALLEY_R1 - MM_VALLEY_R0) * 0.5;
        double t = 1.0 - Math.abs((d - mid) / half);
        return clamp01(smooth01(Math.max(0.0, t)));
    }

    private double taigaEdgeMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;
        V2 c = massiveMountainCenter(seed);
        double d = Math.hypot(wx - c.x, wz - c.z);
        double t = clamp01((d - (MM_EFFECT_R * 0.70)) / (MM_EFFECT_R * 0.25));
        return smooth01(t);
    }

    // --- small helpers ---
    private static double wrap01(double u) { return u - Math.floor(u); }

    private double angle01Warped(long seed, int wx, int wz) {
        double ang = Math.atan2(wz - CENTER_Z, wx - CENTER_X);
        double n1 = valueNoise2(seed ^ 0xA11C1EEDL, (double) wx / 180.0, (double) wz / 180.0);
        double n2 = valueNoise2(seed ^ 0xB22C2EEDL, (double) wx / 64.0, (double) wz / 64.0);
        double jitter = (n1 * 0.65 + n2 * 0.35 - 0.5) * 2.0 * 0.35;
        return ((ang + jitter * 0.3490658503988659) + Math.PI) / (Math.PI * 2);
    }

    private static int arcIndex(double u, double[] splits) {
        int n = splits.length - 1;
        if (n <= 0) return 0;
        double uu = wrap01(u);
        for (int i = 0; i < n; ++i) {
            if (uu >= splits[i] && uu < splits[i + 1]) return i;
        }
        return n - 1;
    }

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

