package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.SplittableRandom;

public class MesaSector extends SectorBase {
    public static final int VALLEY_FLOOR_Y = 166;
    private static final double PLATEAU_START_OFFSET = 100.0;
    private static final double PLATEAU_FLATTEN_FACTOR = 0.18;
    private static final double PLATEAU_NOISE = 2.5;
    private static final int FOREST_CELL = 48;

    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        double raw = mountainPlateauProfile(world, seed, wx, wz);
        double plateauStart = VALLEY_FLOOR_Y + PLATEAU_START_OFFSET;
        double height = raw;
        if (height > plateauStart) {
            double above = height - plateauStart;
            height = plateauStart + above * PLATEAU_FLATTEN_FACTOR;
            double wobble = (fbm2(seed ^ 0x5ED0BEEF, (double) wx * 0.02, (double) wz * 0.02) - 0.5) * 2.0 * PLATEAU_NOISE;
            height += wobble;
            height -= 10.0;
        }

        int yCap = Math.min(320, Math.max(256, world.getMaxHeight() - 1));
        if (height < VALLEY_FLOOR_Y) height = VALLEY_FLOOR_Y;
        if (height > yCap) height = yCap;
        return (int) Math.round(height);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                         int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        placePlateauForests(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        placeMesaFlora(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
    }

    private double mountainPlateauProfile(World world, long seed, int wx, int wz) {
        double base = VALLEY_FLOOR_Y + 12.0;
        double low = fbm2(seed ^ 0xA1B2C3D4L, (double) wx * 0.003, (double) wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = -0.22, t1 = 0.28;
        double mask = clamp01((mountainBand - t0) / (t1 - t0));

        double field = 0.0;
        field += peakSum(seed, wx, wz, 176.0, 60.0, 44.0, 2.2, 0xA11L) * 1.25;
        field += peakSum(seed, wx, wz, 112.0, 44.0, 32.0, 2.0, 0xA12L) * 1.15;
        field += peakSum(seed, wx, wz, 72.0, 28.0, 20.0, 1.9, 0xA13L) * 1.1;
        field += peakSum(seed, wx, wz, 48.0, 18.0, 14.0, 1.75, 0xA14L);

        double ridge = fbm2(seed ^ 0xCAFED00DL, (double) wx * 0.02, (double) wz * 0.02);
        double plains = (fbm2(seed ^ 0x600DD00EL, (double) wx * 0.02, (double) wz * 0.02) - 0.5) * 2.0 * 3.2;
        double between = (fbm2(seed ^ 0x77EEDD11L, (double) wx * 0.01, (double) wz * 0.01) - 0.5) * 2.0;
        double betweenAmp = 16.0 * (1.0 - mask * 0.8);
        double micro = (fbm2(seed ^ 0x33AA55CCL, (double) wx * 0.064, (double) wz * 0.064) - 0.5) * 2.0 * 2.2;

        double baseH = base + plains + between * betweenAmp + micro;
        baseH += (ridge - 0.5) * 2.0 * 16.0 * mask;
        baseH += field;
        baseH += fbm2(seed ^ 0xE01234L, (double) wx * 0.004, (double) wz * 0.004) * 36.0 * mask;

        double maskFactor = 0.25 + mask * 0.9;
        if (maskFactor > 1.0) maskFactor = 1.0;
        baseH = VALLEY_FLOOR_Y + (baseH - VALLEY_FLOOR_Y) * maskFactor;

        // Encourage valley floors to settle toward the valley plane
        double valleyPull = clamp01(1.0 - Math.max(0.0, baseH - (VALLEY_FLOOR_Y + 18.0)) / 26.0);
        baseH = lerp(baseH, VALLEY_FLOOR_Y, valleyPull * 0.35);

        if (baseH < VALLEY_FLOOR_Y) baseH = VALLEY_FLOOR_Y;
        return baseH;
    }

    private void placePlateauForests(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                                     int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minCellX = Math.floorDiv(baseX, FOREST_CELL);
        int maxCellX = Math.floorDiv(baseX + 15, FOREST_CELL);
        int minCellZ = Math.floorDiv(baseZ, FOREST_CELL);
        int maxCellZ = Math.floorDiv(baseZ + 15, FOREST_CELL);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                if (!cellHasForest(seed, cellX, cellZ)) continue;
                int anchorX = forestAnchorX(seed, cellX, cellZ);
                int anchorZ = forestAnchorZ(seed, cellX, cellZ);
                if (anchorX < baseX || anchorX > baseX + 15 || anchorZ < baseZ || anchorZ > baseZ + 15) continue;
                int lx = anchorX - baseX;
                int lz = anchorZ - baseZ;
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.MESA) continue;
                int topY = topYGrid[lx][lz];
                if (topY <= VALLEY_FLOOR_Y + 1) continue;
                if (slopeGrid(topYGrid, lx, lz) > 1) continue;

                SplittableRandom rng = rngFor(seed, anchorX, anchorZ, 0xD45A0L);
                int trees = 2 + rng.nextInt(3);
                for (int i = 0; i < trees; i++) {
                    int ox = rng.nextInt(7) - 3;
                    int oz = rng.nextInt(7) - 3;
                    int tx = lx + ox;
                    int tz = lz + oz;
                    if (tx < 1 || tx > 14 || tz < 1 || tz > 14) continue;
                    if (regionGrid[tx][tz] != ConsegrityRegions.Region.MESA) continue;
                    int ty = topYGrid[tx][tz];
                    if (ty <= VALLEY_FLOOR_Y) continue;
                    if (Math.abs(ty - topY) > 2) continue;
                    if (slopeGrid(topYGrid, tx, tz) > 1) continue;
                    Material ground = safeType(data, tx, ty, tz);
                    if (!ground.name().contains("TERRACOTTA")) continue;
                    if (safeType(data, tx, ty + 1, tz) != Material.AIR) continue;

                    data.setBlock(tx, ty, tz, Material.COARSE_DIRT);
                    growSimpleTree(data, tx, ty + 1, tz, Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES, rng.split());
                }
            }
        }
    }

    private boolean cellHasForest(long seed, int cellX, int cellZ) {
        double r = rand01(hash(seed, cellX, 0L, cellZ, 0x50F357L));
        return r < 0.5;
    }

    private int forestAnchorX(long seed, int cellX, int cellZ) {
        long h = hash(seed, cellX, 0L, cellZ, 0xF04571L);
        double offset = (rand01(h) - 0.5) * (FOREST_CELL - 16);
        return cellX * FOREST_CELL + FOREST_CELL / 2 + (int) Math.round(offset);
    }

    private int forestAnchorZ(long seed, int cellX, int cellZ) {
        long h = hash(seed, cellZ, 0L, cellX, 0xF04572L);
        double offset = (rand01(h) - 0.5) * (FOREST_CELL - 16);
        return cellZ * FOREST_CELL + FOREST_CELL / 2 + (int) Math.round(offset);
    }

    private void placeMesaFlora(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                                int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
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
