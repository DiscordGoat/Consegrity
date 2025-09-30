package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;
import java.util.SplittableRandom;

// Shared helpers for sector implementations.
abstract class SectorBase implements Sector {

    // --- Math/Noise utils (copied from generator) ---
    protected static long hash(long seed, long x, long y, long z, long salt) {
        long h = seed ^ salt;
        h ^= x * -7046029288634856825L;
        h ^= y * -4417276706812531889L;
        h ^= z * 1609587929392839161L;
        h ^= h >>> 27;
        h *= -7723592293110705685L;
        h ^= h >>> 31;
        return h;
    }

    protected static double rand01(long h) {
        long v = h >>> 11 & 0x1FFFFFFFFFFFFFL;
        return (double) v / 9.007199254740992E15;
    }

    protected static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    protected static double fade(double t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }
    protected static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    protected static double smooth01(double x) { return x * x * (3.0 - 2.0 * x); }

    protected static double valueAt(long seed, int xi, int yi, int zi) {
        long h = hash(seed, xi, yi, zi, 305419896L);
        long v = h >>> 11 & 0x1FFFFFFFFFFFFFL;
        return (double) v / 9.007199254740992E15;
    }

    protected static double valueNoise2(long seed, double x, double y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double tx = fade(x - (double) x0);
        double ty = fade(y - (double) y0);
        double c00 = valueAt(seed, x0, y0, 0);
        double c10 = valueAt(seed, x1, y0, 0);
        double c01 = valueAt(seed, x0, y1, 0);
        double c11 = valueAt(seed, x1, y1, 0);
        double x0v = lerp(c00, c10, tx);
        double x1v = lerp(c01, c11, tx);
        return lerp(x0v, x1v, ty);
    }

    protected static SplittableRandom rngFor(long seed, long x, long z, long salt) {
        long mixed = hash(seed, x, 0L, z, salt);
        return new SplittableRandom(mixed);
    }

    // --- Block/terrain helpers (copied from generator) ---
    protected Material safeType(ChunkGenerator.ChunkData data, int lx, int y, int lz) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) return Material.AIR;
        try { return data.getType(lx, y, lz); } catch (Throwable t) { return Material.AIR; }
    }

    protected int slopeGrid(int[][] topY, int lx, int lz) {
        int y0 = topY[lx][lz];
        int max = 0;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (dx == 0 && dz == 0) continue;
                int x = lx + dx, z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                int y1 = topY[x][z];
                max = Math.max(max, Math.abs(y1 - y0));
            }
        }
        return max;
    }

    protected void makeIceSpike(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height, int radius, SplittableRandom rng) {
        for (int dy = 0; dy < height; dy++) {
            int yy = y + dy;
            if (yy >= y + height) break;
            Material m = (rng.nextDouble() < 0.15) ? Material.BLUE_ICE : Material.PACKED_ICE;
            data.setBlock(lx, yy, lz, m);
            int r = (dy < 2) ? (radius + 1) : (dy < height / 3 ? radius : (dy > height * 2 / 3 ? 0 : (radius - 1)));
            if (r < 0) r = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (Math.abs(dx) + Math.abs(dz) > r) continue;
                    int xx = lx + dx, zz = lz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    if (rng.nextDouble() < 0.75) data.setBlock(xx, yy, zz, Material.PACKED_ICE);
                }
            }
        }
        data.setBlock(lx, y + height, lz, Material.BLUE_ICE);
    }

    protected void placeSpruceSimple(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height) {
        Material ground = data.getType(lx, Math.max(0, y - 1), lz);
        String gn = ground != null ? ground.name() : "";
        if (gn.endsWith("_LEAVES") || gn.endsWith("_LOG") || gn.contains("LEAVES") || gn.contains("LOG")) return;
        for (int i = 0; i < height; ++i) data.setBlock(lx, y + i, lz, Material.SPRUCE_LOG);
        int top = y + height - 1;
        int radius = Math.max(1, height / 4);
        for (int ry = 0; ry <= radius; ++ry) {
            int r = Math.max(1, radius - ry);
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                    int xx = lx + dx, zz = lz + dz, yy = top - ry;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || data.getType(xx, yy, zz) != Material.AIR) continue;
                    data.setBlock(xx, yy, zz, Material.SPRUCE_LEAVES);
                }
            }
        }
        if (top + 1 < y + height + 8) data.setBlock(lx, top + 1, lz, Material.SPRUCE_LEAVES);
    }

    protected void growSimpleTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, Material log, Material leaves, Random rng) {
        int height = 4 + rng.nextInt(3);
        for (int i = 0; i < height; ++i) data.setBlock(lx, y + i, lz, log);
        int cy = y + height - 1;
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

    protected Material pickGrassPlant() {
        try { return Material.valueOf("SHORT_GRASS"); } catch (Throwable ignore) {
            try { return Material.valueOf("TALL_GRASS"); } catch (Throwable ignore2) {
                try { return Material.valueOf("GRASS"); } catch (Throwable ignore3) {
                    return Material.FERN;
                }
            }
        }
    }

    protected void addVinesAround(ChunkGenerator.ChunkData data, int x, int y, int z, SplittableRandom rng) {
        try {
            for (int side = 0; side < 4; side++) {
                if (rng.nextDouble() > 0.6) continue;
                int dx = (side == 0) ? 1 : (side == 1 ? -1 : 0);
                int dz = (side == 2) ? 1 : (side == 3 ? -1 : 0);
                BlockFace face = (side == 0) ? BlockFace.WEST : (side == 1 ? BlockFace.EAST : (side == 2 ? BlockFace.NORTH : BlockFace.SOUTH));
                int len = 1 + rng.nextInt(3);
                int yy = y + 2;
                for (int k = 0; k < len; k++) {
                    int xx = x + dx, zz = z + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) break;
                    if (safeType(data, xx, yy - k, zz) != Material.AIR) continue;
                    BlockData bd = Bukkit.createBlockData(Material.VINE);
                    try { ((MultipleFacing) bd).setFace(face, true); } catch (Throwable ignore) {}
                    try { data.setBlock(xx, yy - k, zz, bd); } catch (Throwable t) { data.setBlock(xx, yy - k, zz, Material.VINE); }
                }
            }
        } catch (Throwable ignore) { }
    }
}

