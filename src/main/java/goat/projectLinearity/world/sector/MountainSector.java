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
    private static final double[] R1_SPLITS = new double[]{0.0, 0.175, 0.35, 0.65, 0.825, 1.0};

    // Massive Mountain (MM)
    private static final double MM_EFFECT_R = 150.0;

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        // Baseline mountain field
        double base = 155.0;
        double low = fbm2(seed ^ 0xA1B2C3D4L, (double) wx * 0.003, (double) wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = 0.12, t1 = 0.48;
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

        double baseTerrain = base + plains + between * betweenAmp + micro + (ridge - 0.5) * 2.0 * 20.0 * mask;
        double peakWeight = Math.pow(mask, 1.35);
        double baseH = baseTerrain + field * peakWeight;
        baseH += fbm2(seed ^ 0xE01234L, (double) wx * 0.004, (double) wz * 0.004) * 70.0 * peakWeight;

        int yCap = Math.min(400, Math.max(256, world.getMaxHeight() - 1));
        if (baseH < 150.0) baseH = 150.0;
        if (baseH > yCap) baseH = yCap;
        return (int) Math.floor(baseH);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        placeMountainTaigaEdge(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        placeGrass(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
    }

    // --- mountain features ---
    private void placeGrass(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 0x12345L);
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int t = 0; t < 20; t++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.MOUNTAIN) continue;
            int wx = baseX + lx, wz = baseZ + lz;
            int y = topYGrid[lx][lz];
            if (y > 220) continue; // No grass on high peaks

            Material ground = safeType(data, lx, y, lz);
            if (ground != Material.GRASS_BLOCK) continue;

            Material above = safeType(data, lx, y + 1, lz);
            if (above != Material.AIR) continue;

            data.setBlock(lx, y + 1, lz, Material.SHORT_GRASS);
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

            boolean nearAnotherTree = false;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int checkLX = lx + dx;
                    int checkLZ = lz + dz;
                    if (checkLX < 0 || checkLX >= 16 || checkLZ < 0 || checkLZ >= 16) continue;
                    int checkY = topYGrid[checkLX][checkLZ];
                    if (Math.abs(y - checkY) > 5) continue; // Optimization: only check plausible heights
                    Material mat = safeType(data, checkLX, checkY + 1, checkLZ);
                    if (mat.toString().endsWith("_LOG")) {
                        nearAnotherTree = true;
                        break;
                    }
                }
                if (nearAnotherTree) break;
            }
            if(nearAnotherTree) continue;

            int h = 5 + rng.nextInt(4);
            placeSpruceSimple(data, lx, y + 1, lz, h);
        }
    }

    // --- massif helpers ---
    private static class V2 { final double x, z; V2(double x, double z) { this.x = x; this.z = z; } }

    // Exclusion: carve a bedrock disk at the massif center
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
