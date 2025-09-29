/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Biome
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Snow
 *  org.bukkit.generator.ChunkGenerator
 *  org.bukkit.generator.ChunkGenerator$BiomeGrid
 *  org.bukkit.generator.ChunkGenerator$ChunkData
 */
package goat.projectLinearity.libs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.generator.ChunkGenerator;

public class ArcticChunkGenerator
extends ChunkGenerator {
    private static final int AUDIT_Y_MIN = -192;
    private static final int AUDIT_Y_MAX = 320;
    private static final int CAVE_MIN_Y = -192;
    private static final int CAVE_MAX_Y = 260;
    private static final int MAX_CARVE_RADIUS = 12;
    private static final int W_COAL = 40;
    private static final int W_IRON = 20;
    private static final int W_GOLD = 10;
    private static final int W_DIAMOND = 1;
    private static final int W_COPPER = 2;
    private static final int W_EMERALD = 5;
    private static final int W_REDSTONE = 8;
    private static final int W_LAPIS = 6;
    private static final int OCEAN_SEA_LEVEL = 154;
    private static final int MOUNTAIN_Y = 200;
    private static final int OCEAN_FROM_MOUNTAIN_DIST = 160;
    private static final int OCEAN_COAST_WIDTH = 56;
    private static final int MOUNTAIN_SEARCH_RADIUS = 224;
    private static final int SHORE_JITTER_AMPLITUDE = 12;
    private static final int BASE_HALO = 64;
    private static final int MAX_HALO = Math.min(224, Math.max(64, 160));
    private static final int CHAMFER_AXIS = 5;
    private static final int CHAMFER_DIAGONAL = 7;
    private static final int CHAMFER_INF = 0x1FFFFFFF;
    private static final ThreadLocal<boolean[][]> TL_MOUNTAIN = ThreadLocal.withInitial(() -> new boolean[16 + MAX_HALO * 2][16 + MAX_HALO * 2]);
    private static final ThreadLocal<int[][]> TL_DIST = ThreadLocal.withInitial(() -> new int[16 + MAX_HALO * 2][16 + MAX_HALO * 2]);
    private static final int MIN_BUNGALOW_DIST = 100;
    private static final int BUNGALOW_RESERVE_RADIUS = 9;
    private static final int MIN_DIST_TO_MOUNTAIN = 120;
    public final List<Reserve> reservedFlats = Collections.synchronizedList(new ArrayList());
    public final List<Location> bungalowQueue = Collections.synchronizedList(new ArrayList());
    public final List<Location> placedBungalows = Collections.synchronizedList(new ArrayList());
    public final List<Location> capsuleQueue = Collections.synchronizedList(new ArrayList());
    public final List<Location> ctmQueue = Collections.synchronizedList(new ArrayList());
    private boolean ctmQueued = false;
    private static ArcticChunkGenerator instance;
    private static final double PEAK_CHANCE = 0.008333333333333333;
    private static final int PEAK_MIN_Y = 200;
    private static final int PEAK_MAX_Y = 280;
    private static final int PEAK_INFLUENCE = 96;

    public ArcticChunkGenerator() {
        instance = this;
    }

    public static ArcticChunkGenerator getInstance() {
        return instance;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void registerBungalow(Location loc) {
        List<Location> list = this.placedBungalows;
        synchronized (list) {
            this.placedBungalows.add(loc);
        }
    }

    private boolean canPlaceBungalow(World world, ChunkGenerator.ChunkData data, int lx, int lz, int wx, int wz) {
        int total;
        int baseY = this.getHighestBlockY(data, world, lx, lz);
        if (baseY < 154 || baseY > 158) {
            return false;
        }
        if (this.hasWaterNearby(data, world, lx, lz, 6, 145, 158)) {
            return false;
        }
        int minY = baseY;
        int maxY = baseY;
        HashMap<Integer, Integer> heightCounts = new HashMap<Integer, Integer>();
        for (int dx = -7; dx <= 7; ++dx) {
            for (int dz = -7; dz <= 7; ++dz) {
                int x = lx + dx & 0xF;
                int z = lz + dz & 0xF;
                int y = this.getHighestBlockY(data, world, x, z);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                heightCounts.merge(y, 1, Integer::sum);
                if (y < 154 || y > 158) {
                    return false;
                }
                Material topType = data.getType(x, y, z);
                if (topType != Material.SNOW && topType != Material.SNOW_BLOCK && topType != Material.DIRT) {
                    return false;
                }
                Material below = data.getType(x, y - 1, z);
                if (below != Material.WATER && below != Material.SAND) continue;
                return false;
            }
        }
        if (maxY - minY > 2) {
            return false;
        }
        int dominant = heightCounts.values().stream().max(Integer::compareTo).orElse(0);
        if ((double)dominant / (double)(total = heightCounts.values().stream().mapToInt(i -> i).sum()) < 0.65) {
            return false;
        }
        return !(this.distanceToNearestPeak(world.getSeed(), wx, wz, 16) < 120.0);
    }

    private boolean isRegionAnchor(int chunkX, int chunkZ) {
        return chunkX % 8 == 0 && chunkZ % 8 == 0;
    }

    public Location findBungalowSpot(World world, ChunkGenerator.ChunkData data, int chunkX, int chunkZ) {
        int radius = 4;
        int baseX = (chunkX << 4) + 8;
        int baseZ = (chunkZ << 4) + 8;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                int y;
                Location candidate;
                int wx = baseX + (dx << 4);
                int lx = wx & 0xF;
                int wz = baseZ + (dz << 4);
                int lz = wz & 0xF;
                if (!this.canPlaceBungalow(world, data, lx, lz, wx, wz) || !this.isFarEnough(candidate = new Location(world, (double)wx, (double)(y = this.getHighestBlockY(data, world, lx, lz)), (double)wz))) continue;
                this.reserveFlatArea(candidate, y, 9);
                this.flattenForBungalowNow(data, world, lx, lz, y, 9);
                return candidate;
            }
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isFarEnough(Location loc) {
        List<Location> list = this.placedBungalows;
        synchronized (list) {
            for (Location other : this.placedBungalows) {
                if (!other.getWorld().equals((Object)loc.getWorld()) || !(other.distanceSquared(loc) < 10000.0)) continue;
                return false;
            }
        }
        return true;
    }

    private void flattenForBungalowNow(ChunkGenerator.ChunkData data, World world, int cx, int cz, int y, int r) {
        int yTop = Math.min(world.getMaxHeight() - 1, 320);
        for (int dx = -r; dx <= r; ++dx) {
            for (int dz = -r; dz <= r; ++dz) {
                int lx = cx + dx;
                int lz = cz + dz;
                if (lx < 0 || lx > 15 || lz < 0 || lz > 15 || dx * dx + dz * dz > r * r) continue;
                for (int yy = 150; yy <= yTop; ++yy) {
                    if (yy < y) {
                        data.setBlock(lx, yy, lz, Material.DIRT);
                        continue;
                    }
                    if (yy == y) {
                        data.setBlock(lx, yy, lz, Material.SNOW_BLOCK);
                        continue;
                    }
                    data.setBlock(lx, yy, lz, Material.AIR);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ChunkGenerator.ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biome) {
        ChunkGenerator.ChunkData data = this.createChunkData(world);
        long seed = world.getSeed();
        int SUBLAND_MIN = 150;
        int yFillMin = Math.max(world.getMinHeight(), -192);
        int yFillMax = Math.min(world.getMaxHeight() - 1, 149);
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = yFillMin; y <= yFillMax; ++y) {
                    data.setBlock(x, y, z, Material.STONE);
                }
            }
        }
        this.generateMountains(world, data, seed, chunkX, chunkZ, biome);
        this.placeOres(world, data, seed, chunkX, chunkZ);
        this.carveCavesV2(world, data, seed, chunkX, chunkZ);
        this.placeTaigaClusters(world, data, seed, chunkX, chunkZ, biome);
        if (this.isRegionAnchor(chunkX, chunkZ)) {
            Bukkit.getLogger().info("[Bungalow] Checking region anchor (" + chunkX + "," + chunkZ + ")");
            Location spot = this.findBungalowSpot(world, data, chunkX, chunkZ);
            if (spot != null) {
                List<Location> list = this.bungalowQueue;
                synchronized (list) {
                    this.bungalowQueue.add(spot);
                }
                Bukkit.getLogger().info("[Bungalow] Added bungalow to queue at " + String.valueOf(spot));
            }
        }
        this.placeBoulders(world, data, seed, chunkX, chunkZ, biome);
        this.placeBedrock(world, data, seed, chunkX, chunkZ);
        this.postProcessSandAndDirt(world, data);
        return data;
    }

    private int getHighestBlockY(ChunkGenerator.ChunkData data, World world, int lx, int lz) {
        int yTop = Math.min(world.getMaxHeight() - 1, 320);
        int yBottom = Math.max(world.getMinHeight(), -192);
        for (int y = yTop; y >= yBottom; --y) {
            Material m = data.getType(lx, y, lz);
            if (m == Material.AIR) continue;
            return y;
        }
        return yBottom;
    }

    private boolean hasWaterNearby(ChunkGenerator.ChunkData data, World world, int lx, int lz, int radius, int yMin, int yMax) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; ++dx) {
            int x = lx + dx;
            if (x < 0 || x > 15) continue;
            int dx2 = dx * dx;
            for (int dz = -radius; dz <= radius; ++dz) {
                int z;
                if (dx2 + dz * dz > r2 || (z = lz + dz) < 0 || z > 15) continue;
                for (int y = yMin; y <= yMax; ++y) {
                    try {
                        if (data.getType(x, y, z) != Material.WATER) continue;
                        return true;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean insideReserve(int wx, int wz) {
        List<Reserve> list = this.reservedFlats;
        synchronized (list) {
            for (Reserve r : this.reservedFlats) {
                int dx = wx - r.x;
                int dz = wz - r.z;
                if (dx * dx + dz * dz > r.r * r.r) continue;
                return true;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private int reservedHeightAt(int wx, int wz, int def) {
        List<Reserve> list = this.reservedFlats;
        synchronized (list) {
            for (Reserve r : this.reservedFlats) {
                int dx = wx - r.x;
                int dz = wz - r.z;
                if (dx * dx + dz * dz > r.r * r.r) continue;
                return r.y;
            }
        }
        return def;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void reserveFlatArea(Location center, int y, int radius) {
        List<Reserve> list = this.reservedFlats;
        synchronized (list) {
            this.reservedFlats.add(new Reserve(center.getBlockX(), center.getBlockZ(), y, radius));
        }
    }

    private void placeBedrock(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        int yMin = Math.max(world.getMinHeight(), -64);
        int yMax = Math.min(world.getMaxHeight() - 1, -60);
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;
                for (int y = yMin; y <= yMax; ++y) {
                    double r;
                    double chance = y <= -64 ? 1.0 : (y == -63 ? 0.75 : (y == -62 ? 0.5 : (y == -61 ? 0.25 : 0.0)));
                    if (chance <= 0.0 || !((r = ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, worldX, y, worldZ, 3016886813L))) < chance)) continue;
                    data.setBlock(x, y, z, Material.BEDROCK);
                }
            }
        }
    }

    private void carveCaves(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        int yMax;
        int REGION = 64;
        int yMin = Math.max(-192, -192);
        if (yMin > (yMax = Math.min(260, 320))) {
            return;
        }
        int chunkBaseX = chunkX << 4;
        int chunkBaseZ = chunkZ << 4;
        int rx0 = Math.floorDiv(chunkBaseX - 1, 64);
        int rz0 = Math.floorDiv(chunkBaseZ - 1, 64);
        int rx1 = Math.floorDiv(chunkBaseX + 16, 64);
        int rz1 = Math.floorDiv(chunkBaseZ + 16, 64);
        for (int rx = rx0; rx <= rx1; ++rx) {
            for (int rz = rz0; rz <= rz1; ++rz) {
                long rseed = ArcticChunkGenerator.hash(seed, rx, rz, 0L, 1475271855L);
                Random rr = new Random(rseed);
                int worms = 2 + rr.nextInt(2);
                for (int i = 0; i < worms; ++i) {
                    double x = (double)(rx * 64) + rr.nextDouble() * 64.0;
                    double z = (double)(rz * 64) + rr.nextDouble() * 64.0;
                    int y = yMin + rr.nextInt(yMax - yMin + 1);
                    double yaw = rr.nextDouble() * Math.PI * 2.0;
                    double pitch = (rr.nextDouble() - 0.5) * 0.3;
                    int steps = 200 + rr.nextInt(100);
                    double stepLen = 1.5;
                    double rMin = 1.0;
                    double rMax = 5.0;
                    double rPhase = rr.nextDouble() * Math.PI * 2.0;
                    double rFreq = 0.05 + rr.nextDouble() * 0.05;
                    for (int s = 0; s < steps; ++s) {
                        x += Math.cos(yaw) * stepLen;
                        z += Math.sin(yaw) * stepLen;
                        if ((y += (int)Math.round(Math.sin(pitch))) < yMin + 1) {
                            y = yMin + 1;
                            pitch = Math.abs(pitch) * 0.5;
                        }
                        if (y > yMax - 1) {
                            y = yMax - 1;
                            pitch = -Math.abs(pitch) * 0.5;
                        }
                        yaw += (rr.nextDouble() - 0.5) * 0.18;
                        if ((pitch += (rr.nextDouble() - 0.5) * 0.08) < -0.6) {
                            pitch = -0.6;
                        }
                        if (pitch > 0.6) {
                            pitch = 0.6;
                        }
                        double radiusBase = rMin + rr.nextDouble() * (rMax - rMin);
                        double rJitter = 0.5 * Math.sin(rPhase + (double)s * rFreq);
                        double radius = Math.max(0.8, Math.min(6.0, radiusBase + rJitter));
                        int ix = (int)Math.floor(x);
                        int iy = y;
                        int iz = (int)Math.floor(z);
                        int minX = Math.max(ix - (int)Math.ceil(radius), chunkBaseX);
                        int maxX = Math.min(ix + (int)Math.ceil(radius), chunkBaseX + 15);
                        int minZ = Math.max(iz - (int)Math.ceil(radius), chunkBaseZ);
                        int maxZ = Math.min(iz + (int)Math.ceil(radius), chunkBaseZ + 15);
                        int minY = Math.max(iy - (int)Math.ceil(radius), yMin);
                        int maxY = Math.min(iy + (int)Math.ceil(radius), yMax);
                        for (int wx = minX; wx <= maxX; ++wx) {
                            int lx = wx - chunkBaseX;
                            for (int wz = minZ; wz <= maxZ; ++wz) {
                                int lz = wz - chunkBaseZ;
                                for (int wy = minY; wy <= maxY; ++wy) {
                                    double dz;
                                    double dy;
                                    double dx;
                                    if (wy <= -192 || !((dx = (double)wx - x) * dx + (dy = (double)(wy - y)) * dy + (dz = (double)wz - z) * dz <= radius * radius)) continue;
                                    data.setBlock(lx, wy, lz, Material.AIR);
                                }
                            }
                        }
                        if (rr.nextInt(40) != 0) continue;
                        double roomR = Math.min(6.0, radius + 2.5);
                        int ix2 = (int)Math.floor(x);
                        int iy2 = y;
                        int iz2 = (int)Math.floor(z);
                        int minX2 = Math.max(ix2 - (int)Math.ceil(roomR), chunkBaseX);
                        int maxX2 = Math.min(ix2 + (int)Math.ceil(roomR), chunkBaseX + 15);
                        int minZ2 = Math.max(iz2 - (int)Math.ceil(roomR), chunkBaseZ);
                        int maxZ2 = Math.min(iz2 + (int)Math.ceil(roomR), chunkBaseZ + 15);
                        int minY2 = Math.max(iy2 - (int)Math.ceil(roomR), yMin);
                        int maxY2 = Math.min(iy2 + (int)Math.ceil(roomR), yMax);
                        for (int wx2 = minX2; wx2 <= maxX2; ++wx2) {
                            int lx2 = wx2 - chunkBaseX;
                            for (int wz2 = minZ2; wz2 <= maxZ2; ++wz2) {
                                int lz2 = wz2 - chunkBaseZ;
                                for (int wy2 = minY2; wy2 <= maxY2; ++wy2) {
                                    double dz2;
                                    double dy2;
                                    double dx2;
                                    if (wy2 <= -61 || !((dx2 = (double)wx2 - x) * dx2 + (dy2 = (double)(wy2 - y)) * dy2 + (dz2 = (double)wz2 - z) * dz2 <= roomR * roomR)) continue;
                                    data.setBlock(lx2, wy2, lz2, Material.AIR);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int surfaceY(ChunkGenerator.ChunkData data, int lx, int lz, int top) {
        for (int y = Math.min(top, 320); y >= 0; --y) {
            try {
                Material m = data.getType(lx, y, lz);
                if (m == Material.AIR || m == Material.SNOW) continue;
                return y;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return -1;
    }

    private int slopeAt(ChunkGenerator.ChunkData data, int lx, int lz, int top) {
        int y0 = this.surfaceY(data, lx, lz, top);
        if (y0 < 0) {
            return 0;
        }
        int maxDiff = 0;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int y1;
                if (dx == 0 && dz == 0) continue;
                int x = lx + dx;
                int z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15 || (y1 = this.surfaceY(data, x, z, top)) < 0) continue;
                maxDiff = Math.max(maxDiff, Math.abs(y1 - y0));
            }
        }
        return maxDiff;
    }

    private void placeTreeSpruce(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height) {
        try {
            Material ground = data.getType(lx, Math.max(0, y - 1), lz);
            if (this.isLeavesOrLog(ground)) {
                return;
            }
        }
        catch (Throwable ground) {
            // empty catch block
        }
        for (int i = 0; i < height; ++i) {
            data.setBlock(lx, y + i, lz, Material.SPRUCE_LOG);
        }
        int top = y + height - 1;
        int radius = Math.max(1, height / 4);
        for (int ry = 0; ry <= radius; ++ry) {
            int r = Math.max(1, radius - ry);
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                    int xx = lx + dx;
                    int zz = lz + dz;
                    int yy = top - ry;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || yy < y || yy > 320 || data.getType(xx, yy, zz) != Material.AIR) continue;
                    data.setBlock(xx, yy, zz, Material.SPRUCE_LEAVES);
                }
            }
        }
        if (top + 1 <= 320) {
            data.setBlock(lx, top + 1, lz, Material.SPRUCE_LEAVES);
        }
    }

    private boolean isLeavesOrLog(Material m) {
        if (m == null) {
            return false;
        }
        String n = m.name();
        return n.endsWith("_LEAVES") || n.endsWith("_LOG") || n.contains("LEAVES") || n.contains("LOG");
    }

    private void placeTaigaClusters(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biome) {
        int chunkBaseX = chunkX << 4;
        int chunkBaseZ = chunkZ << 4;
        double clusterMask = this.fbm2(seed ^ 0x7AE3C15DL, (double)chunkX * 0.13, (double)chunkZ * 0.13);
        if (clusterMask < 0.55) {
            return;
        }
        int clusters = 1 + (int)Math.floor((clusterMask - 0.55) * 6.0);
        Random rng = new Random(ArcticChunkGenerator.hash(seed, chunkX, 77L, chunkZ, 1365164254L));
        for (int c = 0; c < clusters; ++c) {
            Material top2;
            int slope;
            boolean onPeak;
            int cz;
            int cx = rng.nextInt(16);
            int baseY = this.surfaceY(data, cx, cz = rng.nextInt(16), 220);
            if (baseY < 165) continue;
            boolean bl = onPeak = baseY >= 205;
            if (!onPeak && baseY > 195 || (slope = this.slopeAt(data, cx, cz, 220)) < 2 || slope > 7) continue;
            int trees = onPeak ? 3 + rng.nextInt(4) : 5 + rng.nextInt(6);
            for (int t = 0; t < trees; ++t) {
                int lx = cx + rng.nextInt(7) - 3;
                int lz = cz + rng.nextInt(7) - 3;
                if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
                int y = this.surfaceY(data, lx, lz, 220);
                try {
                    Biome b = biome.getBiome(lx, lz);
                    if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                        continue;
                    }
                }
                catch (Throwable b) {
                    // empty catch block
                }
                if (onPeak ? y < 200 || y > 220 : y < 165 || y > 195) continue;
                Material top = data.getType(lx, y, lz);
                if (this.isLeavesOrLog(top) || top != Material.GRASS_BLOCK && top != Material.DIRT && top != Material.SNOW_BLOCK && top != Material.STONE) continue;
                int h = 5 + rng.nextInt(5);
                this.placeTreeSpruce(data, lx, y + 1, lz, h);
            }
            if (!(rng.nextDouble() < 0.2)) continue;
            int lx = cx + rng.nextInt(11) - 5;
            int lz = cz + rng.nextInt(11) - 5;
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            int y = this.surfaceY(data, lx, lz, 220);
            try {
                Biome b = biome.getBiome(lx, lz);
                if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                    continue;
                }
            }
            catch (Throwable b) {
                // empty catch block
            }
            if (this.isLeavesOrLog(top2 = data.getType(lx, y, lz)) || (y < 165 || y > 195) && (y < 205 || y > 220)) continue;
            this.placeTreeSpruce(data, lx, y + 1, lz, 6 + rng.nextInt(3));
        }
        for (int t = 0; t < 6; ++t) {
            Material top;
            int lx = 1 + rng.nextInt(14);
            int lz = 1 + rng.nextInt(14);
            int y = this.surfaceY(data, lx, lz, 220);
            try {
                Biome b = biome.getBiome(lx, lz);
                if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                    continue;
                }
            }
            catch (Throwable b) {
                // empty catch block
            }
            if (this.isLeavesOrLog(top = data.getType(lx, y, lz)) || y < 160 || y > 175 || this.slopeAt(data, lx, lz, 220) > 3) continue;
            this.placeTreeSpruce(data, lx, y + 1, lz, 6 + rng.nextInt(5));
        }
    }

    private void placeBoulders(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biome) {
        Random rng = new Random(ArcticChunkGenerator.hash(seed, chunkX, 184594925L, chunkZ, 3735924912L));
        if (rng.nextDouble() >= 0.4) {
            return;
        }
        ArrayList<int[]> placedCenters = new ArrayList<int[]>();
        int rocks = rng.nextInt(5);
        for (int i = 0; i < rocks; ++i) {
            int lz;
            int lx = 2 + rng.nextInt(12);
            int y = this.surfaceY(data, lx, lz = 2 + rng.nextInt(12), 220);
            if (y < 150 || y > 165 || this.slopeAt(data, lx, lz, 220) > 2) continue;
            try {
                Biome b = biome.getBiome(lx, lz);
                if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                    continue;
                }
            }
            catch (Throwable b) {
                // empty catch block
            }
            try {
                Material topMat = this.highestNonAir(data, world, lx, lz);
                if (topMat == Material.WATER) {
                    continue;
                }
            }
            catch (Throwable topMat) {
                // empty catch block
            }
            if (this.isTooCloseToCenters(placedCenters, lx, lz, 100)) continue;
            int r = 1 + rng.nextInt(2);
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    for (int dy = 0; dy <= r; ++dy) {
                        if (dx * dx + dz * dz + dy * dy > r * r) continue;
                        int x = lx + dx;
                        int z = lz + dz;
                        int yy = y + 1 + dy;
                        if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                        Material m = Material.STONE;
                        double roll = rng.nextDouble();
                        if (roll < 0.1) {
                            m = Material.COAL_ORE;
                        } else if (roll < 0.16) {
                            m = Material.IRON_ORE;
                        } else if (roll < 0.165) {
                            m = Material.GOLD_ORE;
                        }
                        data.setBlock(x, yy, z, m);
                    }
                }
            }
            placedCenters.add(new int[]{lx, lz});
        }
        int iceRocks = rng.nextInt(2);
        for (int i = 0; i < iceRocks; ++i) {
            int lz;
            int lx = 2 + rng.nextInt(12);
            int y = this.surfaceY(data, lx, lz = 2 + rng.nextInt(12), 220);
            if (y < 150 || y > 170 || this.slopeAt(data, lx, lz, 220) > 2) continue;
            try {
                Biome b = biome.getBiome(lx, lz);
                if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                    continue;
                }
            }
            catch (Throwable b) {
                // empty catch block
            }
            try {
                Material topMat = this.highestNonAir(data, world, lx, lz);
                if (topMat == Material.WATER) {
                    continue;
                }
            }
            catch (Throwable topMat) {
                // empty catch block
            }
            if (this.isTooCloseToCenters(placedCenters, lx, lz, 100)) continue;
            int r = 1 + rng.nextInt(2);
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    for (int dy = 0; dy <= r; ++dy) {
                        if (dx * dx + dz * dz + dy * dy > r * r) continue;
                        int x = lx + dx;
                        int z = lz + dz;
                        int yy = y + 1 + dy;
                        if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                        Material m = rng.nextDouble() < 0.2 ? Material.BLUE_ICE : Material.PACKED_ICE;
                        data.setBlock(x, yy, z, m);
                    }
                }
            }
            placedCenters.add(new int[]{lx, lz});
        }
    }

    private boolean isTooCloseToCenters(List<int[]> centers, int x, int z, int minDistSq) {
        for (int[] c : centers) {
            int dz;
            int dx = x - c[0];
            if (dx * dx + (dz = z - c[1]) * dz >= minDistSq) continue;
            return true;
        }
        return false;
    }

    private Material highestNonAir(ChunkGenerator.ChunkData data, World world, int lx, int lz) {
        int yTop = Math.min(world.getMaxHeight() - 1, 320);
        int yBottom = Math.max(world.getMinHeight(), -192);
        for (int y = yTop; y >= yBottom; --y) {
            Material m = data.getType(lx, y, lz);
            if (m == Material.AIR) continue;
            return m;
        }
        return Material.AIR;
    }

    protected void placeOres(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        Random rng = new Random(ArcticChunkGenerator.hash(seed, chunkX, chunkZ, 0L, 3729846285L));
        for (int i = 0; i < 72; ++i) {
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.COAL_ORE, 40, -64, 150, Bias.TOP, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.IRON_ORE, 20, -64, 150, Bias.UNIFORM, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.GOLD_ORE, 10, -64, 150, Bias.TRIANGULAR, -50, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.LAPIS_ORE, 6, -64, 150, Bias.BOTTOM, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.REDSTONE_ORE, 8, -64, 150, Bias.BOTTOM, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.EMERALD_ORE, 5, -64, 150, Bias.UNIFORM, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.DIAMOND_ORE, 1, -64, -10, Bias.BOTTOM, 0, false);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.OBSIDIAN, 1, -64, -40, Bias.BOTTOM, 0, true);
            this.oreAttempts(world, data, rng, chunkX, chunkZ, Material.COPPER_ORE, 2, -32, 150, Bias.UNIFORM, 0, false);
        }
    }

    private void oreAttempts(World world, ChunkGenerator.ChunkData data, Random rng, int chunkX, int chunkZ, Material ore, int weight, int minY, int maxY, Bias bias, int peakY, boolean isObsidian) {
        int yMax;
        int yMin = Math.max(-192, minY);
        if (yMin > (yMax = Math.min(320, maxY))) {
            return;
        }
        double chance = Math.min(1.0, (double)weight * 0.02);
        if (rng.nextDouble() >= chance) {
            return;
        }
        int clusters = 1 + (weight >= 20 && rng.nextDouble() < 0.25 ? 1 : 0);
        for (int c = 0; c < clusters; ++c) {
            int i;
            int ax = rng.nextInt(15);
            int az = rng.nextInt(15);
            int ay = this.biasedY(rng, yMin, yMax - 1, bias, peakY);
            int wx = (chunkX << 4) + ax;
            int wz = (chunkZ << 4) + az;
            long salt = (long)ore.ordinal() * -7046029254386353131L + (long)c;
            Random local = new Random(ArcticChunkGenerator.hash(world.getSeed(), wx, ay, wz, salt));
            int[][] positions = new int[8][3];
            int idx = 0;
            for (int dx = 0; dx <= 1; ++dx) {
                for (int dy = 0; dy <= 1; ++dy) {
                    for (int dz = 0; dz <= 1; ++dz) {
                        positions[idx][0] = ax + dx;
                        positions[idx][1] = ay + dy;
                        positions[idx][2] = az + dz;
                        ++idx;
                    }
                }
            }
            for (int i2 = positions.length - 1; i2 > 0; --i2) {
                int j = local.nextInt(i2 + 1);
                int[] tmp = positions[i2];
                positions[i2] = positions[j];
                positions[j] = tmp;
            }
            double p = isObsidian ? 0.05 : 0.2;
            int placed = 0;
            boolean[] chosen = new boolean[8];
            for (int i3 = 0; i3 < 8; ++i3) {
                if (!(local.nextDouble() < p)) continue;
                chosen[i3] = true;
            }
            int chosenCount = 0;
            for (boolean b : chosen) {
                if (!b) continue;
                ++chosenCount;
            }
            for (i = 0; i < 8 && chosenCount < 3; ++i) {
                if (chosen[i]) continue;
                chosen[i] = true;
                ++chosenCount;
            }
            for (i = 0; i < 8; ++i) {
                Material current;
                if (!chosen[i]) continue;
                int lx = positions[i][0];
                int ly = positions[i][1];
                int lz = positions[i][2];
                if (ly < yMin || ly > yMax) continue;
                try {
                    current = data.getType(lx, ly, lz);
                }
                catch (Throwable t) {
                    current = Material.AIR;
                }
                if (current != Material.STONE) continue;
                data.setBlock(lx, ly, lz, ore);
                ++placed;
            }
        }
    }

    private int biasedY(Random rng, int yMin, int yMax, Bias bias, int peakY) {
        if (yMax == yMin) {
            return yMin;
        }
        for (int attempts = 0; attempts < 8; ++attempts) {
            double w;
            int y = yMin + rng.nextInt(yMax - yMin + 1);
            switch (bias.ordinal()) {
                case 2: {
                    return y;
                }
                case 0: {
                    w = this.triangularWeight(y, yMin, yMax, yMax);
                    break;
                }
                case 1: {
                    w = this.triangularWeight(y, yMin, yMax, yMin);
                    break;
                }
                default: {
                    w = this.triangularWeight(y, yMin, yMax, peakY);
                }
            }
            if (!(rng.nextDouble() < w)) continue;
            return y;
        }
        return yMin + rng.nextInt(yMax - yMin + 1);
    }

    private double triangularWeight(int y, int yMin, int yMax, int peakY) {
        if (yMax == yMin) {
            return 1.0;
        }
        double range = yMax - yMin;
        double d = 1.0 - Math.min(1.0, (double)Math.abs(y - peakY) / (range * 0.5));
        return Math.max(0.05, d);
    }

    private static long hash(long seed, long x, long y, long z, long salt) {
        long h = seed ^ salt;
        h ^= x * -7046029288634856825L;
        h ^= y * -4417276706812531889L;
        h ^= z * 1609587929392839161L;
        h ^= h >>> 27;
        h *= -7723592293110705685L;
        h ^= h >>> 31;
        return h;
    }

    private static double random01(long h) {
        long v = h >>> 11 & 0x1FFFFFFFFFFFFFL;
        return (double)v / 9.007199254740992E15;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double valueAt(long seed, int xi, int yi, int zi) {
        return ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, xi, yi, zi, 305419896L));
    }

    private static double valueNoise3(long seed, double x, double y, double z) {
        int x0 = (int)Math.floor(x);
        int y0 = (int)Math.floor(y);
        int z0 = (int)Math.floor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;
        double tx = ArcticChunkGenerator.fade(x - (double)x0);
        double ty = ArcticChunkGenerator.fade(y - (double)y0);
        double tz = ArcticChunkGenerator.fade(z - (double)z0);
        double c000 = ArcticChunkGenerator.valueAt(seed, x0, y0, z0);
        double c100 = ArcticChunkGenerator.valueAt(seed, x1, y0, z0);
        double c010 = ArcticChunkGenerator.valueAt(seed, x0, y1, z0);
        double c110 = ArcticChunkGenerator.valueAt(seed, x1, y1, z0);
        double c001 = ArcticChunkGenerator.valueAt(seed, x0, y0, z1);
        double c101 = ArcticChunkGenerator.valueAt(seed, x1, y0, z1);
        double c011 = ArcticChunkGenerator.valueAt(seed, x0, y1, z1);
        double c111 = ArcticChunkGenerator.valueAt(seed, x1, y1, z1);
        double x00 = ArcticChunkGenerator.lerp(c000, c100, tx);
        double x10 = ArcticChunkGenerator.lerp(c010, c110, tx);
        double x01 = ArcticChunkGenerator.lerp(c001, c101, tx);
        double x11 = ArcticChunkGenerator.lerp(c011, c111, tx);
        double y0v = ArcticChunkGenerator.lerp(x00, x10, ty);
        double y1v = ArcticChunkGenerator.lerp(x01, x11, ty);
        return ArcticChunkGenerator.lerp(y0v, y1v, tz);
    }

    private static double valueNoise2(long seed, double x, double y) {
        int x0 = (int)Math.floor(x);
        int y0 = (int)Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double tx = ArcticChunkGenerator.fade(x - (double)x0);
        double ty = ArcticChunkGenerator.fade(y - (double)y0);
        double c00 = ArcticChunkGenerator.valueAt(seed, x0, y0, 0);
        double c10 = ArcticChunkGenerator.valueAt(seed, x1, y0, 0);
        double c01 = ArcticChunkGenerator.valueAt(seed, x0, y1, 0);
        double c11 = ArcticChunkGenerator.valueAt(seed, x1, y1, 0);
        double x0v = ArcticChunkGenerator.lerp(c00, c10, tx);
        double x1v = ArcticChunkGenerator.lerp(c01, c11, tx);
        return ArcticChunkGenerator.lerp(x0v, x1v, ty);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp01(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }

    private void generateMountains(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biome) {
        int lx;
        int x;
        int chunkBaseX = chunkX << 4;
        int chunkBaseZ = chunkZ << 4;
        int worldMaxY = Math.min(world.getMaxHeight() - 1, 320);
        int rChunks = 6;
        class Peak {
            int cx;
            int cz;
            int x;
            int z;
            int h;

            Peak(ArcticChunkGenerator this$0, int cx, int cz, int x, int z, int h) {
                this.cx = cx;
                this.cz = cz;
                this.x = x;
                this.z = z;
                this.h = h;
            }
        }
        ArrayList<Peak> peaks = new ArrayList<Peak>();
        for (int cx = chunkX - rChunks; cx <= chunkX + rChunks; ++cx) {
            for (int cz = chunkZ - rChunks; cz <= chunkZ + rChunks; ++cz) {
                if (!this.hasPeak(seed, cx, cz)) continue;
                int[] p = this.peakParams(seed, cx, cz);
                int px = p[0];
                int pz = p[1];
                long hh = ArcticChunkGenerator.hash(seed, cx, 4L, cz, 3203383550L);
                int ph = 240 + (int)Math.floor(ArcticChunkGenerator.random01(hh) * 61.0);
                int minWX = chunkBaseX - 96;
                int maxWX = chunkBaseX + 15 + 96;
                int minWZ = chunkBaseZ - 96;
                int maxWZ = chunkBaseZ + 15 + 96;
                if (px < minWX || px > maxWX || pz < minWZ || pz > maxWZ) continue;
                peaks.add(new Peak(this, cx, cz, px, pz, ph));
                long baseHash = ArcticChunkGenerator.hash(seed, cx, 2L, cz, 3241507233L);
                int plateauCount = (int)Math.floor(ArcticChunkGenerator.random01(baseHash) * 9.0);
                for (int i = 0; i < plateauCount; ++i) {
                    long ih = ArcticChunkGenerator.hash(seed, cx, 3 + i, cz, 2780087250L);
                    double angle = ArcticChunkGenerator.random01(ih) * Math.PI * 2.0;
                    double dist = 24.0 + ArcticChunkGenerator.random01(ih ^ 0x9E3779B97F4A7C15L) * 72.0;
                    int cpx = px + (int)Math.round(Math.cos(angle) * dist);
                    int cpz = pz + (int)Math.round(Math.sin(angle) * dist);
                    int ch = 200 + (int)Math.floor(ArcticChunkGenerator.random01(ih ^ 0xDEADBEE1L) * 61.0);
                    if (cpx < minWX || cpx > maxWX || cpz < minWZ || cpz > maxWZ) continue;
                    peaks.add(new Peak(this, cx, cz, cpx, cpz, ch));
                }
            }
        }
        double[][] surf = new double[16][16];
        boolean longPlains = false;
        int centerX = chunkBaseX + 8;
        int centerZ = chunkBaseZ + 8;
        int radius = 224;
        int step = 64;
        int total = 0;
        int plains = 0;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int sx = centerX + dx;
                int sz = centerZ + dz;
                double lowS = this.fbm2(seed ^ 0xA1B2C3D4L, (double)sx * 0.003, (double)sz * 0.003);
                double bandS = (lowS - 0.5) * 2.0;
                if (bandS < 0.15) {
                    ++plains;
                }
                ++total;
            }
        }
        if (total > 0 && (double)plains / (double)total > 0.8) {
            longPlains = true;
        }
        for (int lx2 = 0; lx2 < 16; ++lx2) {
            for (int lz = 0; lz < 16; ++lz) {
                int wx = chunkBaseX + lx2;
                int wz = chunkBaseZ + lz;
                double base = 155.0;
                double low = this.fbm2(seed ^ 0xA1B2C3D4L, (double)wx * 0.003, (double)wz * 0.003);
                double mountainBand = (low - 0.5) * 2.0;
                double t0 = 0.15;
                double t1 = 0.45;
                if (longPlains) {
                    t0 -= 0.06;
                    t1 -= 0.06;
                }
                double mask = ArcticChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
                mask *= mask;
                double cellSize = 288.0;
                int nx = (int)Math.round((double)wx / 288.0);
                int nz = (int)Math.round((double)wz / 288.0);
                double jx = (ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, nx, 51L, nz, 10601445L)) - 0.5) * 100.8;
                double jz = (ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, nx, 87L, nz, 11719926L)) - 0.5) * 100.8;
                double cx = (double)nx * 288.0 + jx;
                double cz = (double)nz * 288.0 + jz;
                double ddx = (double)wx - cx;
                double ddz = (double)wz - cz;
                double d = Math.sqrt(ddx * ddx + ddz * ddz);
                double R = 144.0;
                double w = ArcticChunkGenerator.clamp01(1.0 - d / R);
                double evenBoost = (w - 0.5) * 0.2;
                mask = ArcticChunkGenerator.clamp01(mask + evenBoost);
                double baseLift = mask * 60.0;
                double ridge = this.fbm2(seed ^ 0xB3C4D5E6L, (double)wx * 0.02, (double)wz * 0.02);
                double ridgeAdd = (ridge - 0.5) * 2.0 * 10.0;
                ridgeAdd *= mask;
                double edge = ArcticChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
                double edgeTaper = edge * edge * (3.0 - 2.0 * edge);
                double plains2 = (this.fbm2(seed ^ 0x600DD00EL, (double)wx * 0.02, (double)wz * 0.02) - 0.5) * 2.0;
                double h = base + plains2 + (baseLift *= edgeTaper) + (ridgeAdd *= edgeTaper);
                double erosion = this.fbm2(seed ^ 0xE01234L, (double)wx * 0.004, (double)wz * 0.004);
                if ((h += erosion * erosion * 40.0 * mask) > 190.0) {
                    double n = this.fbm2(seed ^ 0xC0FFEE1L, (double)wx * 0.02, (double)wz * 0.02);
                    double ridged = 1.0 - Math.abs(n * 2.0 - 1.0);
                    double gain = Math.min(1.0, (h - 190.0) / Math.max(1.0, (double)worldMaxY - 190.0));
                    h += Math.pow(gain, 1.4) * ridged * 20.0;
                }
                if (h > (double)worldMaxY) {
                    h = worldMaxY;
                }
                if (h < 150.0) {
                    h = 150.0;
                }
                surf[lx2][lz] = h;
            }
        }
        for (int x2 = 0; x2 < 16; ++x2) {
            for (int z = 0; z < 16; ++z) {
                try {
                    biome.setBiome(x2, z, Biome.SNOWY_SLOPES);
                    continue;
                }
                catch (Throwable wx) {
                    // empty catch block
                }
            }
        }
        double[][] distToMtn = this.computeMountainDistances(world, seed, surf, chunkBaseX, chunkBaseZ, worldMaxY, 224);
        double[][] threshold = new double[16][16];
        double[][] oceanRaw = new double[16][16];
        double[][] oceanMask = new double[16][16];
        for (int lx3 = 0; lx3 < 16; ++lx3) {
            for (int lz = 0; lz < 16; ++lz) {
                int wx = chunkBaseX + lx3;
                int wz = chunkBaseZ + lz;
                double shoreJitter = (this.fbm2(seed ^ 0x7A1B2C3DL, (double)wx * 0.01, (double)wz * 0.01) - 0.5) * 24.0;
                threshold[lx3][lz] = 160.0 + shoreJitter;
                double d = distToMtn[lx3][lz];
                double raw = (d - threshold[lx3][lz]) / 56.0;
                raw = Double.isFinite(raw) ? ArcticChunkGenerator.clamp01(0.5 + raw) : 1.0;
                oceanRaw[lx3][lz] = raw;
            }
        }
        double[][] buf = new double[16][16];
        for (int pass = 0; pass < 3; ++pass) {
            for (int lx4 = 0; lx4 < 16; ++lx4) {
                for (int lz = 0; lz < 16; ++lz) {
                    int numW = 0;
                    double sum = 0.0;
                    for (int ox = -1; ox <= 1; ++ox) {
                        for (int oz = -1; oz <= 1; ++oz) {
                            x = lx4 + ox;
                            int z = lz + oz;
                            if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                            int wgt = ox == 0 && oz == 0 ? 4 : (Math.abs(ox) + Math.abs(oz) == 2 ? 1 : 2);
                            sum += oceanRaw[x][z] * (double)wgt;
                            numW += wgt;
                        }
                    }
                    buf[lx4][lz] = sum / (double)Math.max(1, numW);
                }
            }
            double[][] tmp = oceanRaw;
            oceanRaw = buf;
            buf = tmp;
        }
        for (int lx5 = 0; lx5 < 16; ++lx5) {
            System.arraycopy(oceanRaw[lx5], 0, oceanMask[lx5], 0, 16);
        }
        double[][] icebergDelta = new double[16][16];
        boolean[][] iceSheet = new boolean[16][16];
        for (int lx6 = 0; lx6 < 16; ++lx6) {
            for (int lz = 0; lz < 16; ++lz) {
                boolean deep;
                boolean isOceanHere;
                int wx = chunkBaseX + lx6;
                int wz = chunkBaseZ + lz;
                double oceanFactor = oceanMask[lx6][lz];
                boolean bl = isOceanHere = oceanFactor >= 0.5;
                if (!isOceanHere) continue;
                double over = Math.max(0.0, distToMtn[lx6][lz] - threshold[lx6][lz]);
                double depth = 2.0 + Math.min(40.0, over * 0.25);
                boolean bl2 = deep = depth >= 22.0;
                if (!deep) continue;
                boolean deepNeighborhood = true;
                block39: for (int dx = -6; dx <= 6 && deepNeighborhood; ++dx) {
                    for (int dz = -6; dz <= 6; ++dz) {
                        int x3 = lx6 + dx;
                        int z = lz + dz;
                        if (x3 < 0 || x3 > 15 || z < 0 || z > 15) continue;
                        if (oceanMask[x3][z] < 0.6) {
                            deepNeighborhood = false;
                            continue block39;
                        }
                        double overN = Math.max(0.0, distToMtn[x3][z] - threshold[x3][z]);
                        double dN = 2.0 + Math.min(40.0, overN * 0.25);
                        if (!(dN < 18.0)) continue;
                        deepNeighborhood = false;
                        continue block39;
                    }
                }
                if (!deepNeighborhood) continue;
                double chance = 0.004 + 0.006 * ArcticChunkGenerator.clamp01((depth - 22.0) / 20.0);
                double roll = ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, 30323687L, wz, 2857704673L));
                if (roll > chance) continue;
                int coreR = 4 + (int)Math.floor(ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, 3L, wz, 200208657L)) * 4.0);
                int coreH = 6 + (int)Math.floor(ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, 5L, wz, 13504768L)) * 7.0);
                int sheetR = coreR + 9;
                for (int dx = -sheetR; dx <= sheetR; ++dx) {
                    for (int dz = -sheetR; dz <= sheetR; ++dz) {
                        int x4 = lx6 + dx;
                        int z = lz + dz;
                        if (x4 < 0 || x4 > 15 || z < 0 || z > 15) continue;
                        double r = Math.hypot(dx, dz);
                        if (r <= (double)coreR) {
                            double noise;
                            double t = ArcticChunkGenerator.clamp01(1.0 - r / (double)coreR);
                            double h = (double)coreH * (t * t) * (0.9 + (noise = (this.fbm2(seed ^ 0x11C3B3E7L, (double)(wx + dx) * 0.08, (double)(wz + dz) * 0.08) - 0.5) * 0.35));
                            if (!(h > icebergDelta[x4][z])) continue;
                            icebergDelta[x4][z] = h;
                            continue;
                        }
                        if (!(r <= (double)sheetR)) continue;
                        double fall = ArcticChunkGenerator.clamp01(1.0 - (r - (double)coreR) / (double)(sheetR - coreR));
                        double n = this.fbm2(seed ^ 0x55C1C35EL, (double)(wx + dx) * 0.1, (double)(wz + dz) * 0.1);
                        if (!(n * fall > 0.45)) continue;
                        iceSheet[x4][z] = true;
                    }
                }
            }
        }
        boolean[][] grown = new boolean[16][16];
        for (lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                grown[lx][lz] = iceSheet[lx][lz];
            }
        }
        for (lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                if (iceSheet[lx][lz]) continue;
                int neighbors = 0;
                for (int ox = -1; ox <= 1; ++ox) {
                    for (int oz = -1; oz <= 1; ++oz) {
                        if (ox == 0 && oz == 0) continue;
                        x = lx + ox;
                        int z = lz + oz;
                        if (x < 0 || x > 15 || z < 0 || z > 15 || !iceSheet[x][z]) continue;
                        ++neighbors;
                    }
                }
                if (neighbors < 3) continue;
                grown[lx][lz] = true;
            }
        }
        for (lx = 0; lx < 16; ++lx) {
            System.arraycopy(grown[lx], 0, iceSheet[lx], 0, 16);
        }
        for (int lx7 = 0; lx7 < 16; ++lx7) {
            for (int lz = 0; lz < 16; ++lz) {
                int y;
                boolean nearShore;
                double oceanFactor;
                int wx = chunkBaseX + lx7;
                int wz = chunkBaseZ + lz;
                int reservedY = this.reservedHeightAt(wx, wz, Integer.MIN_VALUE);
                if (reservedY != Integer.MIN_VALUE) {
                    surf[lx7][lz] = Math.max(150, Math.min(reservedY, worldMaxY));
                }
                boolean isOcean = (oceanFactor = oceanMask[lx7][lz]) >= 0.5;
                boolean bl = nearShore = oceanFactor > 0.4 && oceanFactor < 0.75;
                if (nearShore && surf[lx7][lz] > 154.0) {
                    surf[lx7][lz] = 154.0;
                }
                int finalSurf = (int)Math.floor(Math.max(150.0, Math.min(surf[lx7][lz], (double)worldMaxY)));
                double distPeak = this.distanceToNearestPeak(seed, wx, wz, 16);
                if (!nearShore && Double.isFinite(distPeak) && finalSurf >= 155 && finalSurf <= 200) {
                    double rSlope = 80.0;
                    double rHill = 140.0;
                    double rPlains = 220.0;
                    if (distPeak > rSlope && distPeak <= rHill) {
                        double t = (distPeak - rSlope) / (rHill - rSlope);
                        s = 1.0 - ArcticChunkGenerator.fade(Math.max(0.0, Math.min(1.0, t)));
                        double n = this.fbm2(seed ^ 0x1A1B551L, (double)wx * 0.02, (double)wz * 0.02);
                        int delta = (int)Math.round((n - 0.5) * 20.0 * s);
                        finalSurf = Math.max(150, Math.min(worldMaxY, finalSurf + delta));
                    } else if (distPeak > rHill && distPeak <= rPlains) {
                        double t = (distPeak - rHill) / (rPlains - rHill);
                        s = ArcticChunkGenerator.fade(Math.max(0.0, Math.min(1.0, t)));
                        int plainsHeight = 156;
                        double blended = (double)finalSurf * (1.0 - s) + (double)plainsHeight * s;
                        finalSurf = (int)Math.round(blended);
                    } else if (distPeak > rPlains) {
                        double t = Math.min(1.0, (distPeak - rPlains) / 100.0);
                        s = ArcticChunkGenerator.fade(Math.max(0.0, t));
                        double n = this.fbm2(seed ^ 0x2A1B552L, (double)wx * 0.02, (double)wz * 0.02);
                        int delta = (int)Math.round((n - 0.5) * 20.0 * (0.5 + 0.5 * s));
                        finalSurf = Math.max(150, Math.min(worldMaxY, finalSurf + delta));
                    }
                }
                if (finalSurf < 150) continue;
                finalSurf = Math.min(finalSurf, worldMaxY);
                if (!(isOcean || finalSurf < 170 || Double.isFinite(distPeak) && !(distPeak > 80.0) || finalSurf >= 195)) {
                    int lx0 = Math.max(0, lx7 - 1);
                    int lx1 = Math.min(15, lx7 + 1);
                    int lz0 = Math.max(0, lz - 1);
                    int lz1 = Math.min(15, lz + 1);
                    double sMax = 0.0;
                    double h0 = surf[lx7][lz];
                    for (int ax = lx0; ax <= lx1; ++ax) {
                        for (int az = lz0; az <= lz1; ++az) {
                            if (ax == lx7 && az == lz) continue;
                            sMax = Math.max(sMax, Math.abs(surf[ax][az] - h0));
                        }
                    }
                    double slopeStrength = ArcticChunkGenerator.clamp01(sMax / 12.0);
                    if (slopeStrength > 0.0) {
                        double n = this.fbm2(seed ^ 0xC1A05C1AL, (double)wx * 0.06, (double)wz * 0.06);
                        int delta = (int)Math.round((n - 0.5) * 2.0 * (2.0 + 6.0 * slopeStrength));
                        finalSurf = Math.max(150, Math.min(worldMaxY, finalSurf + delta));
                    }
                }
                if (isOcean) {
                    double r;
                    int yStart;
                    int y2;
                    double bedNoise;
                    double dNoise;
                    double over = Math.max(0.0, distToMtn[lx7][lz] - threshold[lx7][lz]);
                    double depth = 2.0 + Math.min(40.0, over * 0.25);
                    int floorY = (int)Math.round(154.0 - depth + (dNoise = (this.fbm2(seed ^ 0x12345ABCL, (double)wx * 0.03, (double)wz * 0.03) - 0.5) * 6.0) + (bedNoise = (this.fbm2(seed ^ 0xBEDBEDL, (double)wx * 0.08, (double)wz * 0.08) - 0.5) * (oceanFactor < 0.7 ? 2.0 : 4.0)));
                    if (floorY < 130) {
                        floorY = 130;
                    }
                    if (floorY > 153) {
                        floorY = 153;
                    }
                    for (y2 = yStart = Math.max(120, Math.min(floorY - 6, 154)); y2 <= floorY; ++y2) {
                        data.setBlock(lx7, y2, lz, Material.DEEPSLATE);
                    }
                    if (oceanFactor < 0.7) {
                        int sandLayers = 1 + (int)Math.round((0.7 - oceanFactor) * 3.0);
                        sandLayers = Math.min(sandLayers, Math.max(0, 154 - (floorY + 1)));
                        for (int i = 0; i < sandLayers; ++i) {
                            int y3 = floorY - i;
                            if (y3 < yStart) continue;
                            data.setBlock(lx7, y3, lz, Material.SAND);
                        }
                    }
                    for (y2 = floorY + 1; y2 <= 154; ++y2) {
                        data.setBlock(lx7, y2, lz, Material.WATER);
                    }
                    int clearTop = Math.min(162, worldMaxY);
                    for (int y4 = 155; y4 <= clearTop; ++y4) {
                        data.setBlock(lx7, y4, lz, Material.AIR);
                    }
                    int icebergUp = (int)Math.floor(icebergDelta[lx7][lz]);
                    if (icebergUp > 0) {
                        int base;
                        int topIce = Math.min(worldMaxY, 154 + icebergUp);
                        int subExtra = (int)Math.floor(this.fbm2(seed ^ 0x42CEB1L, (double)wx * 0.07, (double)wz * 0.07) * 4.0);
                        for (int y5 = base = Math.max(floorY + 1, 146 - subExtra); y5 <= topIce; ++y5) {
                            boolean underwater = y5 < 154;
                            double roll = ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, y5, wz, 0x1CE1CE1L));
                            Material mIce = underwater ? (y5 <= base + 1 ? Material.BLUE_ICE : (roll < 0.4 ? Material.BLUE_ICE : Material.PACKED_ICE)) : (roll < 0.15 ? Material.BLUE_ICE : Material.PACKED_ICE);
                            data.setBlock(lx7, y5, lz, mIce);
                        }
                    }
                    if (iceSheet[lx7][lz]) {
                        try {
                            if (data.getType(lx7, 154, lz) == Material.WATER) {
                                data.setBlock(lx7, 154, lz, Material.ICE);
                            }
                        }
                        catch (Throwable topIce) {
                            // empty catch block
                        }
                    }
                    if (icebergUp == 0 && floorY + 5 < 154 && (r = ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, floorY, wz, 161987806L))) < 0.08) {
                        int y6;
                        int maxLen = Math.min(12, 154 - (floorY + 1));
                        int len = 3 + (int)Math.floor(r * (double)maxLen);
                        for (int i = 0; i < len && (y6 = floorY + 1 + i) < 154; ++i) {
                            data.setBlock(lx7, y6, lz, i == len - 1 ? Material.KELP : Material.KELP_PLANT);
                        }
                    }
                    if (oceanMask[lx7][lz] < 0.7) {
                        int y7;
                        for (y7 = Math.min(153, worldMaxY); y7 >= 140 && data.getType(lx7, y7, lz) == Material.WATER; --y7) {
                        }
                        if (y7 >= 140 && data.getType(lx7, y7, lz) != Material.AIR) {
                            data.setBlock(lx7, y7, lz, Material.SAND);
                        }
                    }
                    try {
                        if (over < 12.0) {
                            biome.setBiome(lx7, lz, Biome.BEACH);
                            continue;
                        }
                        if (depth >= 24.0) {
                            biome.setBiome(lx7, lz, Biome.DEEP_OCEAN);
                            continue;
                        }
                        biome.setBiome(lx7, lz, Biome.OCEAN);
                    }
                    catch (Throwable throwable) {}
                    continue;
                }
                for (y = 150; y <= finalSurf; ++y) {
                    data.setBlock(lx7, y, lz, Material.STONE);
                }
                for (y = 150; y <= Math.min(155, finalSurf); ++y) {
                    data.setBlock(lx7, y, lz, Material.DIRT);
                }
                double s = (oceanFactor - 0.25) / 0.55;
                if (s > 0.0 && s < 1.0 && finalSurf <= 166) {
                    int y8;
                    s = ArcticChunkGenerator.clamp01(s);
                    if (finalSurf > 154) {
                        finalSurf = 154;
                    }
                    int sandLayers = 4 + (int)Math.round(5.0 * (1.0 - s));
                    for (int i = 0; i < sandLayers && (y8 = finalSurf - i) >= 150; ++i) {
                        data.setBlock(lx7, y8, lz, Material.SAND);
                    }
                    try {
                        biome.setBiome(lx7, lz, Biome.BEACH);
                    }
                    catch (Throwable i) {}
                } else {
                    boolean rocky = false;
                    if (finalSurf >= 185 && (!Double.isFinite(distPeak) || distPeak > 80.0)) {
                        double nRock = this.fbm2(seed ^ 0x51A0BEF5L, (double)wx * 0.05, (double)wz * 0.05);
                        boolean bl3 = rocky = nRock > 0.65;
                    }
                    if (finalSurf > 180 && rocky) {
                        data.setBlock(lx7, finalSurf, lz, Material.STONE);
                    } else if (finalSurf > 180) {
                        data.setBlock(lx7, finalSurf, lz, Material.SNOW_BLOCK);
                    } else if (finalSurf < 200) {
                        data.setBlock(lx7, finalSurf, lz, Material.SNOW_BLOCK);
                        int dirtMin = 3 + (int)Math.floor(ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, wx, finalSurf, wz, 3517436327L)) * 3.0);
                        int dirtLayers = Math.min(Math.min(6, dirtMin), finalSurf - 150);
                        for (int i = 1; i <= dirtLayers; ++i) {
                            data.setBlock(lx7, finalSurf - i, lz, Material.DIRT);
                        }
                    } else {
                        data.setBlock(lx7, finalSurf, lz, Material.STONE);
                    }
                    int ySnow = finalSurf + 1;
                    if (ySnow <= worldMaxY) {
                        try {
                            double drift = this.fbm2(seed ^ 0x5A0BD123L, (double)wx * 0.03, (double)wz * 0.03);
                            int layers = 1 + (int)Math.floor(Math.max(0.0, (drift - 0.4) * 6.0));
                            if (layers < 1) {
                                layers = 1;
                            }
                            if (layers > 8) {
                                layers = 8;
                            }
                            Snow snow = (Snow)Bukkit.createBlockData((Material)Material.SNOW);
                            snow.setLayers(layers);
                            data.setBlock(lx7, ySnow, lz, (BlockData)snow);
                        }
                        catch (Throwable ignore) {
                            try {
                                data.setBlock(lx7, ySnow, lz, Material.SNOW);
                            }
                            catch (Throwable throwable) {
                                // empty catch block
                            }
                        }
                        try {
                            if (data.getType(lx7, finalSurf, lz) == Material.GRASS_BLOCK) {
                                Snow one = (Snow)Bukkit.createBlockData((Material)Material.SNOW);
                                one.setLayers(1);
                                data.setBlock(lx7, ySnow, lz, (BlockData)one);
                            }
                        }
                        catch (Throwable one) {
                            // empty catch block
                        }
                    }
                    if (!(s > 0.0) || !(s < 1.0) || finalSurf > 166) {
                        try {
                            if (finalSurf <= 162) {
                                biome.setBiome(lx7, lz, Biome.SNOWY_PLAINS);
                            } else if (finalSurf >= 185) {
                                biome.setBiome(lx7, lz, Biome.SNOWY_SLOPES);
                            } else {
                                biome.setBiome(lx7, lz, Biome.SNOWY_TAIGA);
                            }
                        }
                        catch (Throwable one) {
                            // empty catch block
                        }
                    }
                }
                boolean waterAdj = false;
                if (154 <= worldMaxY) {
                    int[][] dirs;
                    for (int[] d : dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = lx7 + d[0];
                        int nz = lz + d[1];
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                        try {
                            if (data.getType(nx, 154, nz) != Material.WATER) continue;
                            waterAdj = true;
                            break;
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                    }
                }
                if (waterAdj) {
                    int y9 = Math.min(finalSurf, 154);
                    data.setBlock(lx7, y9, lz, Material.SAND);
                    for (int i = 1; i <= 3 && y9 - i >= 150; ++i) {
                        data.setBlock(lx7, y9 - i, lz, Material.SAND);
                    }
                    try {
                        biome.setBiome(lx7, lz, Biome.BEACH);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                this.fixStoneAboveSand(data, lx7, lz, 150, finalSurf);
            }
        }
    }

    private boolean hasPeak(long seed, int chunkX, int chunkZ) {
        double r = ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, chunkX, 0L, chunkZ, 3237998257L));
        return r < 0.008333333333333333;
    }

    private int[] peakParams(long seed, int chunkX, int chunkZ) {
        long h = ArcticChunkGenerator.hash(seed, chunkX, 1L, chunkZ, 3737181713L);
        int cx = (chunkX << 4) + 2 + (int)Math.floor(ArcticChunkGenerator.random01(h) * 12.0);
        int cz = (chunkZ << 4) + 2 + (int)Math.floor(ArcticChunkGenerator.random01(h ^ 0x9E3779B97F4A7C15L) * 12.0);
        int hMin = 200;
        int hMax = 280;
        int peakY = hMin + (int)Math.floor(ArcticChunkGenerator.random01(h ^ 0xA55A55A5L) * (double)(hMax - hMin + 1));
        return new int[]{cx, cz, peakY};
    }

    private double distanceToNearestPeak(long seed, int wx, int wz, int searchRadiusChunks) {
        int chunkX = Math.floorDiv(wx, 16);
        int chunkZ = Math.floorDiv(wz, 16);
        double bestDist2 = Double.MAX_VALUE;
        for (int cx = chunkX - searchRadiusChunks; cx <= chunkX + searchRadiusChunks; ++cx) {
            for (int cz = chunkZ - searchRadiusChunks; cz <= chunkZ + searchRadiusChunks; ++cz) {
                int pz;
                double dz;
                int[] p;
                int px;
                double dx;
                double d2;
                if (!this.hasPeak(seed, cx, cz) || !((d2 = (dx = (double)(px = (p = this.peakParams(seed, cx, cz))[0]) - (double)wx) * dx + (dz = (double)(pz = p[1]) - (double)wz) * dz) < bestDist2)) continue;
                bestDist2 = d2;
            }
        }
        return bestDist2 == Double.MAX_VALUE ? Double.POSITIVE_INFINITY : Math.sqrt(bestDist2);
    }

    private double fbm2(long seed, double x, double z) {
        double sum = 0.0;
        double amp = 1.0;
        double freq = 1.0;
        for (int o = 0; o < 5; ++o) {
            sum += (ArcticChunkGenerator.valueNoise2(seed, x * freq, z * freq) - 0.5) * 2.0 * amp;
            amp *= 0.55;
            freq *= 1.9;
        }
        return ArcticChunkGenerator.clamp01(sum * 0.5 + 0.5);
    }

    private double[][] computeMountainDistances(World world, long seed, double[][] surf, int chunkBaseX, int chunkBaseZ, int worldMaxY, int maxDist) {
        int v;
        int x;
        int lx;
        int halo = Math.min(MAX_HALO, maxDist);
        int size = 16 + halo * 2;
        int off = halo;
        boolean[][] mountain = TL_MOUNTAIN.get();
        int[][] dist = TL_DIST.get();
        for (int x2 = 0; x2 < size; ++x2) {
            Arrays.fill(mountain[x2], 0, size, false);
            Arrays.fill(dist[x2], 0, size, 0x1FFFFFFF);
        }
        boolean any = false;
        for (int gx = -halo; gx < 16 + halo; ++gx) {
            for (int gz = -halo; gz < 16 + halo; ++gz) {
                double h;
                if (gx >= 0 && gx < 16 && gz >= 0 && gz < 16) {
                    h = surf[gx][gz];
                } else {
                    int wx = chunkBaseX + gx;
                    int wz = chunkBaseZ + gz;
                    h = this.surfaceApprox(world, seed, wx, wz, worldMaxY);
                }
                if (!(h >= 200.0)) continue;
                mountain[gx + off][gz + off] = true;
                dist[gx + off][gz + off] = 0;
                any = true;
            }
        }
        double[][] result = new double[16][16];
        if (!any) {
            for (lx = 0; lx < 16; ++lx) {
                Arrays.fill(result[lx], Double.POSITIVE_INFINITY);
            }
            return result;
        }
        for (x = 0; x < size; ++x) {
            for (int z = 0; z < size; ++z) {
                v = dist[x][z];
                if (v == 0) continue;
                if (x > 0) {
                    v = Math.min(v, dist[x - 1][z] + 5);
                }
                if (z > 0) {
                    v = Math.min(v, dist[x][z - 1] + 5);
                }
                if (x > 0 && z > 0) {
                    v = Math.min(v, dist[x - 1][z - 1] + 7);
                }
                if (x < size - 1 && z > 0) {
                    v = Math.min(v, dist[x + 1][z - 1] + 7);
                }
                dist[x][z] = v;
            }
        }
        for (x = size - 1; x >= 0; --x) {
            for (int z = size - 1; z >= 0; --z) {
                v = dist[x][z];
                if (x < size - 1) {
                    v = Math.min(v, dist[x + 1][z] + 5);
                }
                if (z < size - 1) {
                    v = Math.min(v, dist[x][z + 1] + 5);
                }
                if (x < size - 1 && z < size - 1) {
                    v = Math.min(v, dist[x + 1][z + 1] + 7);
                }
                if (x > 0 && z < size - 1) {
                    v = Math.min(v, dist[x - 1][z + 1] + 7);
                }
                dist[x][z] = v;
            }
        }
        for (lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                v = dist[lx + off][lz + off];
                double d = v >= 0x1FFFFFFF ? (double)maxDist : Math.min((double)maxDist, (double)v / 5.0);
                result[lx][lz] = d;
            }
        }
        return result;
    }

    private int surfaceApprox(World world, long seed, int wx, int wz, int worldMaxY) {
        double base = 155.0;
        double low = this.fbm2(seed ^ 0xA1B2C3D4L, (double)wx * 0.003, (double)wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = 0.15;
        double t1 = 0.45;
        double mask = ArcticChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
        double cellSize = 288.0;
        int nx = (int)Math.round((double)wx / 288.0);
        int nz = (int)Math.round((double)wz / 288.0);
        double jx = (ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, nx, 51L, nz, 10601445L)) - 0.5) * 100.8;
        double jz = (ArcticChunkGenerator.random01(ArcticChunkGenerator.hash(seed, nx, 87L, nz, 11719926L)) - 0.5) * 100.8;
        double cx = (double)nx * 288.0 + jx;
        double cz = (double)nz * 288.0 + jz;
        double ddx = (double)wx - cx;
        double ddz = (double)wz - cz;
        double d = Math.sqrt(ddx * ddx + ddz * ddz);
        double R = 144.0;
        double w = ArcticChunkGenerator.clamp01(1.0 - d / R);
        double evenBoost = (w - 0.5) * 0.2;
        mask = ArcticChunkGenerator.clamp01(mask + evenBoost);
        double baseLift = mask * 60.0;
        double ridge = this.fbm2(seed ^ 0xB3C4D5E6L, (double)wx * 0.02, (double)wz * 0.02);
        double ridgeAdd = (ridge - 0.5) * 2.0 * 10.0;
        double edge = ArcticChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
        double edgeTaper = edge * edge * (3.0 - 2.0 * edge);
        double plains = (this.fbm2(seed ^ 0x600DD00EL, (double)wx * 0.02, (double)wz * 0.02) - 0.5) * 2.0;
        double h = base + plains + (baseLift *= edgeTaper) * mask + (ridgeAdd *= edgeTaper) * mask;
        double erosion = this.fbm2(seed ^ 0xE01234L, (double)wx * 0.004, (double)wz * 0.004);
        if ((h += erosion * erosion * 40.0 * mask) > (double)worldMaxY) {
            h = worldMaxY;
        }
        if (h < 150.0) {
            h = 150.0;
        }
        return (int)Math.floor(h);
    }

    protected void carveCavesV2(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        int yMax;
        int REGION = 64;
        int MAX_TASKS_PER_REGION = 10;
        double INTERSECT_CHANCE = 0.2;
        int yMin = Math.max(-192, -192);
        if (yMin > (yMax = Math.min(260, 320))) {
            return;
        }
        int chunkBaseX = chunkX << 4;
        int chunkBaseZ = chunkZ << 4;
        double MAX_TRAVEL = 641.6;
        int cellX0 = Math.floorDiv(chunkBaseX, 64);
        int cellZ0 = Math.floorDiv(chunkBaseZ, 64);
        int reachCells = (int)Math.ceil(10.025) + 1;
        for (int rx = cellX0 - reachCells; rx <= cellX0 + reachCells; ++rx) {
            block13: for (int rz = cellZ0 - reachCells; rz <= cellZ0 + reachCells; ++rz) {
                if (!ArcticChunkGenerator.cellMayTouchChunk(rx, rz, 64, chunkX, chunkZ, 641.6)) continue;
                long rseed = ArcticChunkGenerator.hash(seed, rx, rz, 0L, 1475271855L);
                Random rr = new Random(rseed);
                ArrayDeque<CaveTask> queue = new ArrayDeque<CaveTask>();
                int created = 0;
                int worms = 2 + rr.nextInt(2);
                for (int i = 0; i < worms && created < 10; ++created, ++i) {
                    double sx = (double)(rx * 64) + rr.nextDouble() * 64.0;
                    double sz = (double)(rz * 64) + rr.nextDouble() * 64.0;
                    int sy = yMin + rr.nextInt(yMax - yMin + 1);
                    double yaw = rr.nextDouble() * Math.PI * 2.0;
                    double pitch = (rr.nextDouble() - 0.5) * 0.4;
                    int steps = 220 + rr.nextInt(100);
                    queue.add(new CaveTask(CaveType.WORM, sx, sy, sz, yaw, pitch, steps));
                }
                while (!queue.isEmpty()) {
                    CaveTask t = (CaveTask)queue.pollFirst();
                    switch (t.type.ordinal()) {
                        case 0: {
                            StepResult res = this.carveWorm(data, rr, t, chunkBaseX, chunkBaseZ, yMin, yMax, 1.6, 1.0, 4.5, true, true);
                            if (res != null && rr.nextDouble() < 0.2 && created < 10) {
                                CaveType inter = this.pickIntersector(rr);
                                switch (inter.ordinal()) {
                                    case 2: {
                                        queue.add(new CaveTask(CaveType.ARENA, res.x, res.y, res.z, res.yaw, 0.0, 1));
                                        ++created;
                                        break;
                                    }
                                    case 3: {
                                        double py = -0.6 + rr.nextDouble() * 0.2;
                                        queue.add(new CaveTask(CaveType.STAIRCASE, res.x, res.y, res.z, res.yaw, py, 140));
                                        ++created;
                                        break;
                                    }
                                    case 4: {
                                        queue.add(new CaveTask(CaveType.OMINOUS_PASSAGE, res.x, res.y, res.z, res.yaw, -1.0, 80 + rr.nextInt(60)));
                                        ++created;
                                        break;
                                    }
                                }
                            }
                            if (res == null) break;
                            this.queueCapsule(world, res, chunkX, chunkZ);
                            this.queueCtm(world, res, chunkX, chunkZ);
                            if (res.y > 165) break;
                            this.dentTowardsAir(data, chunkBaseX, chunkBaseZ, res.x, res.y, res.z, yMin, yMax);
                            break;
                        }
                        case 1: {
                            StepResult res = this.carveWorm(data, rr, t, chunkBaseX, chunkBaseZ, yMin, yMax, 1.9, 1.5, 6.5, false, false);
                            if (res == null) break;
                            this.queueCapsule(world, res, chunkX, chunkZ);
                            this.queueCtm(world, res, chunkX, chunkZ);
                            if (res.y > 165) break;
                            this.dentTowardsAir(data, chunkBaseX, chunkBaseZ, res.x, res.y, res.z, yMin, yMax);
                            break;
                        }
                        case 2: {
                            this.carveArenaAndSpur(data, rr, t, queue, created, 10, yMin, yMax, chunkBaseX, chunkBaseZ);
                            created = Math.min(10, created + 1);
                            break;
                        }
                        case 3: {
                            StepResult res = this.carveWorm(data, rr, t, chunkBaseX, chunkBaseZ, yMin, yMax, 1.5, 1.0, 4.0, true, true);
                            if (res == null) break;
                            this.queueCapsule(world, res, chunkX, chunkZ);
                            this.queueCtm(world, res, chunkX, chunkZ);
                            if (res.y > 165) break;
                            this.dentTowardsAir(data, chunkBaseX, chunkBaseZ, res.x, res.y, res.z, yMin, yMax);
                            break;
                        }
                        case 4: {
                            this.carveOminousPassage(data, rr, t, queue, created, 10, yMin, yMax, chunkBaseX, chunkBaseZ);
                        }
                    }
                    if (created < 10) continue;
                    continue block13;
                }
            }
        }
    }

    private static boolean cellMayTouchChunk(int cx, int cz, int cellSize, int chunkX, int chunkZ, double reach) {
        double dx;
        int cellMinX = cx * cellSize;
        int cellMaxX = cellMinX + cellSize;
        int cellMinZ = cz * cellSize;
        int cellMaxZ = cellMinZ + cellSize;
        int chMinX = chunkX << 4;
        int chMaxX = chMinX + 16;
        int chMinZ = chunkZ << 4;
        int chMaxZ = chMinZ + 16;
        double d = cellMaxX < chMinX ? (double)(chMinX - cellMaxX) : (dx = cellMinX > chMaxX ? (double)(cellMinX - chMaxX) : 0.0);
        double dz = cellMaxZ < chMinZ ? (double)(chMinZ - cellMaxZ) : (cellMinZ > chMaxZ ? (double)(cellMinZ - chMaxZ) : 0.0);
        return Math.hypot(dx, dz) <= reach;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void queueCapsule(World world, StepResult res, int chunkX, int chunkZ) {
        int endChunkX = Math.floorDiv((int)Math.floor(res.x), 16);
        int endChunkZ = Math.floorDiv((int)Math.floor(res.z), 16);
        if (endChunkX != chunkX || endChunkZ != chunkZ) {
            return;
        }
        Location loc = new Location(world, Math.floor(res.x), (double)res.y, Math.floor(res.z));
        List<Location> list = this.capsuleQueue;
        synchronized (list) {
            this.capsuleQueue.add(loc);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void queueCtm(World world, StepResult res, int chunkX, int chunkZ) {
        if (this.ctmQueued) {
            return;
        }
        int endChunkX = Math.floorDiv((int)Math.floor(res.x), 16);
        int endChunkZ = Math.floorDiv((int)Math.floor(res.z), 16);
        if (endChunkX != chunkX || endChunkZ != chunkZ) {
            return;
        }
        Location loc = new Location(world, Math.floor(res.x), (double)res.y, Math.floor(res.z));
        List<Location> list = this.ctmQueue;
        synchronized (list) {
            this.ctmQueue.add(loc);
        }
        this.ctmQueued = true;
    }

    private CaveType pickIntersector(Random rr) {
        double d = rr.nextDouble();
        if (d < 0.5) {
            return CaveType.OMINOUS_PASSAGE;
        }
        if (d < 0.8) {
            return CaveType.ARENA;
        }
        return CaveType.STAIRCASE;
    }

    private StepResult carveWorm(ChunkGenerator.ChunkData data, Random rr, CaveTask t, int chunkBaseX, int chunkBaseZ, int yMin, int yMax, double stepLen, double rMin, double rMax, boolean shorter, boolean chaos) {
        double x = t.x;
        double z = t.z;
        double yaw = t.yaw;
        double pitch = t.pitch;
        int y = t.y;
        double rPhase = rr.nextDouble() * Math.PI * 2.0;
        double rFreq = 0.05 + rr.nextDouble() * 0.05;
        for (int s = 0; s < t.steps; ++s) {
            x += Math.cos(yaw) * stepLen;
            z += Math.sin(yaw) * stepLen;
            y += (int)Math.round(Math.sin(pitch));
            if (chaos) {
                yaw += (rr.nextDouble() - 0.5) * 0.6;
                pitch += (rr.nextDouble() - 0.5) * 0.3;
                if (rr.nextDouble() < 0.07) {
                    yaw += (rr.nextDouble() - 0.5) * Math.PI;
                }
            } else {
                yaw += (rr.nextDouble() - 0.5) * 0.4;
                pitch += (rr.nextDouble() - 0.5) * 0.2;
                if (rr.nextDouble() < 0.05) {
                    yaw += (rr.nextDouble() - 0.5) * Math.PI;
                }
            }
            if (y < yMin + 1) {
                y = yMin + 1;
                pitch = Math.abs(pitch) * 0.5;
            }
            if (y > yMax - 1) {
                y = yMax - 1;
                pitch = -Math.abs(pitch) * 0.5;
            }
            if (pitch < -1.0) {
                pitch = -1.0;
            }
            if (pitch > 1.0) {
                pitch = 1.0;
            }
            double baseR = rMin + rr.nextDouble() * (rMax - rMin);
            if (shorter) {
                baseR = Math.max(0.8, baseR - 1.0);
            }
            double radius = Math.max(0.8, Math.min(6.0, baseR + 0.5 * Math.sin(rPhase + (double)s * rFreq)));
            this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, radius, yMin, yMax);
            if (!(rr.nextDouble() < 0.01)) continue;
            this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, 6.0 + (double)rr.nextInt(7), yMin, yMax);
        }
        int overshoot = 6 + rr.nextInt(10);
        double tailPitch = pitch * 0.6;
        for (int s = 0; s < overshoot; ++s) {
            x += Math.cos(yaw) * stepLen;
            z += Math.sin(yaw) * stepLen;
            y += (int)Math.round(Math.sin(tailPitch) * 0.5);
            yaw += (rr.nextDouble() - 0.5) * 0.1;
            tailPitch *= 0.9;
            double baseR = rMin + rr.nextDouble() * (rMax - rMin);
            double taper = 0.8 - 0.5 * ((double)s / (double)Math.max(1, overshoot - 1));
            double radius = Math.max(0.8, Math.min(5.0, baseR * taper));
            this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, radius, yMin, yMax);
        }
        return new StepResult(x, y, z, yaw);
    }

    private void carveArenaAndSpur(ChunkGenerator.ChunkData data, Random rr, CaveTask t, ArrayDeque<CaveTask> queue, int created, int maxTasks, int yMin, int yMax, int chunkBaseX, int chunkBaseZ) {
        double cx = t.x;
        double cz = t.z;
        int cy = t.y;
        double radH = 6.0 + rr.nextDouble() * 6.0;
        double radV = 1.5 + rr.nextDouble() * 1.0;
        this.carveEllipsoid(data, chunkBaseX, chunkBaseZ, cx, cy, cz, radH, radV, yMin, yMax);
        if (created < maxTasks) {
            if (rr.nextDouble() < 0.5) {
                int steps = 80 + rr.nextInt(80);
                queue.add(new CaveTask(CaveType.OMINOUS_PASSAGE, cx, cy, cz, 0.0, -1.0, steps));
            } else {
                double phi = rr.nextDouble() * Math.PI * 2.0;
                double sx = cx + Math.cos(phi) * (radH - 0.5);
                double sz = cz + Math.sin(phi) * (radH - 0.5);
                int sy = cy;
                CaveType next = rr.nextBoolean() ? CaveType.WORM : CaveType.CHAOS_TUNNEL;
                int steps = next == CaveType.CHAOS_TUNNEL ? 220 + rr.nextInt(100) : 160 + rr.nextInt(80);
                queue.add(new CaveTask(next, sx, sy, sz, phi, 0.0, steps));
            }
        }
    }

    private void carveOminousPassage(ChunkGenerator.ChunkData data, Random rr, CaveTask t, ArrayDeque<CaveTask> queue, int created, int maxTasks, int yMin, int yMax, int chunkBaseX, int chunkBaseZ) {
        double x = t.x;
        double z = t.z;
        int y = t.y;
        int steps = t.steps;
        double radiusBase = 2.8 + rr.nextDouble() * 1.8;
        this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, radiusBase + 1.0, yMin, yMax);
        int carved = 0;
        for (int s = 0; s < steps && y > yMin + 1 && carved < 25; y -= 1 + (rr.nextDouble() < 0.15 ? 1 : 0), ++carved, ++s) {
            double r = radiusBase + (s % 7 == 0 ? 0.4 : 0.0);
            this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, r, yMin, yMax);
        }
        this.carveSphere(data, chunkBaseX, chunkBaseZ, x, y, z, radiusBase + 1.2, yMin, yMax);
        if (y <= yMin + 8) {
            this.dentTowardsAir(data, chunkBaseX, chunkBaseZ, x, y, z, yMin, yMax);
        }
        if (created < maxTasks) {
            double yaw = rr.nextDouble() * Math.PI * 2.0;
            queue.add(new CaveTask(CaveType.WORM, x, Math.max(yMin + 2, y), z, yaw, 0.0, 160));
        }
    }

    private void carveSphere(ChunkGenerator.ChunkData data, int chunkBaseX, int chunkBaseZ, double cx, int cy, double cz, double r, int yMin, int yMax) {
        int ix = (int)Math.floor(cx);
        int iz = (int)Math.floor(cz);
        int minX = ix - (int)Math.ceil(r);
        int maxX = ix + (int)Math.ceil(r);
        int minZ = iz - (int)Math.ceil(r);
        int maxZ = iz + (int)Math.ceil(r);
        int minY = Math.max(cy - (int)Math.ceil(r), yMin);
        int maxY = Math.min(cy + (int)Math.ceil(r), yMax);
        for (int wx = Math.max(minX, chunkBaseX); wx <= Math.min(maxX, chunkBaseX + 15); ++wx) {
            int lx = wx - chunkBaseX;
            for (int wz = Math.max(minZ, chunkBaseZ); wz <= Math.min(maxZ, chunkBaseZ + 15); ++wz) {
                int lz = wz - chunkBaseZ;
                for (int wy = minY; wy <= maxY; ++wy) {
                    double dz;
                    double dy;
                    double dx;
                    if (wy <= -192 || !((dx = (double)wx - cx) * dx + (dy = (double)(wy - cy)) * dy + (dz = (double)wz - cz) * dz <= r * r)) continue;
                    if (wy > 120) {
                        boolean waterAbove = false;
                        int upper = Math.min(yMax, 154);
                        for (int ya = wy + 1; ya <= upper; ++ya) {
                            try {
                                if (data.getType(lx, ya, lz) != Material.WATER) continue;
                                waterAbove = true;
                                break;
                            }
                            catch (Throwable throwable) {
                                // empty catch block
                            }
                        }
                        if (waterAbove || this.isNearWaterColumn(data, lx, wy, lz, 4, yMin, yMax)) continue;
                    }
                    data.setBlock(lx, wy, lz, Material.AIR);
                    this.hardenHighCaveWalls(data, lx, wy, lz);
                }
            }
        }
    }

    private void carveEllipsoid(ChunkGenerator.ChunkData data, int chunkBaseX, int chunkBaseZ, double cx, int cy, double cz, double radH, double radV, int yMin, int yMax) {
        int ix = (int)Math.floor(cx);
        int iz = (int)Math.floor(cz);
        int minX = ix - (int)Math.ceil(radH);
        int maxX = ix + (int)Math.ceil(radH);
        int minZ = iz - (int)Math.ceil(radH);
        int maxZ = iz + (int)Math.ceil(radH);
        int minY = Math.max(cy - (int)Math.ceil(radV), yMin);
        int maxY = Math.min(cy + (int)Math.ceil(radV), yMax);
        double invH2 = 1.0 / (radH * radH);
        for (int wx = Math.max(minX, chunkBaseX); wx <= Math.min(maxX, chunkBaseX + 15); ++wx) {
            int lx = wx - chunkBaseX;
            for (int wz = Math.max(minZ, chunkBaseZ); wz <= Math.min(maxZ, chunkBaseZ + 15); ++wz) {
                int lz = wz - chunkBaseZ;
                for (int wy = minY; wy <= maxY; ++wy) {
                    double dy;
                    double dz;
                    double dx;
                    double rho;
                    if (wy <= -192 || !((rho = ((dx = (double)wx - cx) * dx + (dz = (double)wz - cz) * dz) * invH2 + (dy = (double)(wy - cy) / radV) * dy) <= 1.0)) continue;
                    if (wy > 120) {
                        boolean waterAbove = false;
                        int upper = Math.min(yMax, 154);
                        for (int ya = wy + 1; ya <= upper; ++ya) {
                            try {
                                if (data.getType(lx, ya, lz) != Material.WATER) continue;
                                waterAbove = true;
                                break;
                            }
                            catch (Throwable throwable) {
                                // empty catch block
                            }
                        }
                        if (waterAbove || this.isNearWaterColumn(data, lx, wy, lz, 4, yMin, yMax)) continue;
                    }
                    data.setBlock(lx, wy, lz, Material.AIR);
                    this.hardenHighCaveWalls(data, lx, wy, lz);
                }
            }
        }
    }

    private void fixStoneAboveSand(ChunkGenerator.ChunkData data, int lx, int lz, int yMin, int yMax) {
        boolean seenSand = false;
        for (int y = Math.max(0, yMin); y <= Math.min(320, yMax); ++y) {
            try {
                Material m = data.getType(lx, y, lz);
                if (m == Material.SAND) {
                    seenSand = true;
                    continue;
                }
                if (!seenSand || m != Material.STONE && m != Material.DIRT) continue;
                data.setBlock(lx, y, lz, Material.SAND);
                continue;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void postProcessSandAndDirt(World world, ChunkGenerator.ChunkData data) {
        int lz;
        int lx;
        int r = 20;
        int yMin = Math.max(world.getMinHeight(), 140);
        int yMax = Math.min(world.getMaxHeight() - 1, 170);
        boolean[][] hasWater = new boolean[16][16];
        for (int lx2 = 0; lx2 < 16; ++lx2) {
            for (int lz2 = 0; lz2 < 16; ++lz2) {
                boolean found = false;
                for (int y = yMin; y <= yMax; ++y) {
                    try {
                        if (data.getType(lx2, y, lz2) != Material.WATER) continue;
                        found = true;
                        break;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                hasWater[lx2][lz2] = found;
            }
        }
        boolean[][] nearWater = new boolean[16][16];
        int r2 = 400;
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                boolean nw = false;
                block11: for (int dx = -20; dx <= 20 && !nw; ++dx) {
                    int x = lx + dx;
                    if (x < 0 || x > 15) continue;
                    int dx2 = dx * dx;
                    for (int dz = -20; dz <= 20; ++dz) {
                        int z = lz + dz;
                        if (z < 0 || z > 15 || dx2 + dz * dz > r2 || !hasWater[x][z]) continue;
                        nw = true;
                        continue block11;
                    }
                }
                nearWater[lx][lz] = nw;
            }
        }
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                if (!nearWater[lx][lz]) continue;
                for (int y = yMin; y <= yMax; ++y) {
                    try {
                        Material m = data.getType(lx, y, lz);
                        if (m != Material.DIRT && m != Material.GRASS_BLOCK) continue;
                        data.setBlock(lx, y, lz, Material.SAND);
                        continue;
                    }
                    catch (Throwable m) {
                        // empty catch block
                    }
                }
            }
        }
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                boolean seenSand = false;
                for (int y = yMin; y <= yMax; ++y) {
                    try {
                        Material m = data.getType(lx, y, lz);
                        if (m == Material.SAND) {
                            seenSand = true;
                            continue;
                        }
                        if (!seenSand || m != Material.DIRT && m != Material.GRASS_BLOCK && m != Material.STONE) continue;
                        data.setBlock(lx, y, lz, Material.SAND);
                        continue;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
        }
    }

    private boolean isNearWaterColumn(ChunkGenerator.ChunkData data, int lx, int ly, int lz, int radius, int yMin, int yMax) {
        int r2 = radius * radius;
        int yStart = Math.max(ly - 2, yMin);
        int yEnd = Math.min(156, yMax);
        for (int dx = -radius; dx <= radius; ++dx) {
            int x = lx + dx;
            if (x < 0 || x > 15) continue;
            for (int dz = -radius; dz <= radius; ++dz) {
                int z;
                if (dx * dx + dz * dz > r2 || (z = lz + dz) < 0 || z > 15) continue;
                for (int y = yStart; y <= yEnd; ++y) {
                    try {
                        if (data.getType(x, y, z) != Material.WATER) continue;
                        return true;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
        }
        return false;
    }

    private void hardenHighCaveWalls(ChunkGenerator.ChunkData data, int lx, int ly, int lz) {
        int[][] dirs;
        if (ly <= 147) {
            return;
        }
        for (int[] d : dirs = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
            int nx = lx + d[0];
            int ny = ly + d[1];
            int nz = lz + d[2];
            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny <= 147) continue;
            try {
                Material m = data.getType(nx, ny, nz);
                if (m != Material.DIRT && m != Material.GRASS_BLOCK) continue;
                data.setBlock(nx, ny, nz, Material.STONE);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void dentTowardsAir(ChunkGenerator.ChunkData data, int chunkBaseX, int chunkBaseZ, double ex, int ey, double ez, int yMin, int yMax) {
        int bestLX = -1;
        int bestLY = -1;
        int bestLZ = -1;
        double bestDist2 = Double.POSITIVE_INFINITY;
        int exi = (int)Math.floor(ex);
        int ezi = (int)Math.floor(ez);
        int lx0 = exi - chunkBaseX;
        int lz0 = ezi - chunkBaseZ;
        int R = 12;
        for (int dy = -R; dy <= R; ++dy) {
            int y = ey + dy;
            if (y < yMin || y > yMax || y <= -61) continue;
            for (int dx = -R; dx <= R; ++dx) {
                int lx = lx0 + dx;
                if (lx < 0 || lx > 15) continue;
                for (int dz = -R; dz <= R; ++dz) {
                    double d2;
                    int lz = lz0 + dz;
                    if (lz < 0 || lz > 15 || (d2 = (double)(dx * dx + dy * dy + dz * dz)) >= bestDist2) continue;
                    try {
                        if (data.getType(lx, y, lz) != Material.AIR) continue;
                        bestDist2 = d2;
                        bestLX = lx;
                        bestLY = y;
                        bestLZ = lz;
                        continue;
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
        }
        if (bestLX == -1) {
            return;
        }
        double tx = (double)(chunkBaseX + bestLX) + 0.5;
        double tz = (double)(chunkBaseZ + bestLZ) + 0.5;
        double ty = (double)bestLY + 0.5;
        int steps = (int)Math.ceil(Math.sqrt(bestDist2));
        steps = Math.max(2, Math.min(steps, 16));
        for (int s = 0; s <= steps; ++s) {
            double t = (double)s / (double)steps;
            double cx = ex + (tx - ex) * t;
            double cy = (double)ey + (ty - (double)ey) * t;
            double cz = ez + (tz - ez) * t;
            this.carveSphere(data, chunkBaseX, chunkBaseZ, cx, (int)Math.round(cy), cz, 1.0, yMin, yMax);
        }
    }

    private static final class Reserve {
        final int x;
        final int z;
        final int y;
        final int r;

        Reserve(int x, int z, int y, int r) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.r = r;
        }
    }

    private static enum Bias {
        TOP,
        BOTTOM,
        UNIFORM,
        TRIANGULAR;

    }

    private static final class CaveTask {
        final CaveType type;
        final double x;
        final double z;
        final int y;
        final double yaw;
        final double pitch;
        final int steps;

        CaveTask(CaveType type, double x, int y, double z, double yaw, double pitch, int steps) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.steps = steps;
        }
    }

    private static enum CaveType {
        WORM,
        CHAOS_TUNNEL,
        ARENA,
        STAIRCASE,
        OMINOUS_PASSAGE;

    }

    private static final class StepResult {
        final double x;
        final double z;
        final double yaw;
        final int y;

        StepResult(double x, int y, double z, double yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }
}

