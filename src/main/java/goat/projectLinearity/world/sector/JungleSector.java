package goat.projectLinearity.world.sector;

import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;
import java.util.SplittableRandom;

public class JungleSector extends SectorBase {
    @Override
    public int computeSurfaceY(World world, long seed, int wx, int wz) {
        // Base jungle relief
        double h1 = valueNoise2(seed ^ 0x1B6C1E11L, (double) wx / 220.0, (double) wz / 220.0);
        double h2 = valueNoise2(seed ^ 0x1B6C1E12L, (double) wx / 70.0, (double) wz / 70.0);
        double baseNoise = (h1 * 0.55 + h2 * 0.45) * 2.0 - 1.0;
        double base = 169.0 + baseNoise * 16.0;

        // Mountain overlay for jungle: clustered peaks + ridges (lighter than Massive Mountain)
        double field = 0.0;
        field += peakSum(seed, wx, wz, 140.0, 56.0, 36.0, 2.2, 0xA11L);
        field += peakSum(seed, wx, wz, 96.0, 36.0, 24.0, 2.0, 0xA12L);
        field += peakSum(seed, wx, wz, 64.0, 22.0, 14.0, 1.8, 0xA13L);

        double ridge = fbm2(seed ^ 0xCAFED00DL, (double) wx * 0.018, (double) wz * 0.018);
        double plains = (fbm2(seed ^ 0x600DD00EL, (double) wx * 0.02, (double) wz * 0.02) - 0.5) * 2.0 * 4.2;
        double micro = (fbm2(seed ^ 0x33AA55CCL, (double) wx * 0.06, (double) wz * 0.06) - 0.5) * 2.0 * 2.2;
        // Halve mountain overlay intensity per request
        double mountains = 0.5 * (plains + micro + (ridge - 0.5) * 2.0 * 16.0 + field);

        int yCap = Math.min(360, Math.max(256, world.getMaxHeight() - 1));
        double H = base + mountains;
        if (H < 150.0) H = 150.0;
        if (H > yCap) H = yCap;
        return (int) Math.round(H);
    }

    @Override
    public void decorate(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                         int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid, double[][] centralMaskGrid) {
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 0x701A11L);

        // 1) Sparse jungle trees; rare mega jungle trees (reduced rates)
        for (int attempt = 0; attempt < 6; attempt++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.JUNGLE) continue;
            if (slopeGrid(topYGrid, lx, lz) > 4) continue;
            int y = topYGrid[lx][lz];
            Material g = safeType(data, lx, y, lz);
            if (g != Material.GRASS_BLOCK && g != Material.DIRT && g != Material.STONE) continue;
            double roll = rng.nextDouble();
            if (roll < 0.03) {
                placeJungleTreeMega(data, lx, y + 1, lz, 12 + rng.nextInt(7), rng);
                if (rng.nextDouble() < 0.4) attachCocoaToTrunk(data, lx, y + 1, lz, rng);
            } else if (roll < 0.20) {
                placeJungleTreeSmall(data, lx, y + 1, lz, 5 + rng.nextInt(4), rng);
                if (rng.nextDouble() < 0.35) attachCocoaToTrunk(data, lx, y + 1, lz, rng);
            }
        }

        // 2) Grass only (remove vines)
        Material grassPlant = pickGrassPlant();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.JUNGLE) continue;
                int y = topYGrid[lx][lz];
                Material ground = safeType(data, lx, y, lz);
                Material above = safeType(data, lx, y + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    if (rng.nextDouble() < 0.55) data.setBlock(lx, y + 1, lz, grassPlant);
                }
            }
        }

        // 3) Rare melon patches
        if (rng.nextDouble() < 0.22) {
            int cx = 2 + rng.nextInt(12), cz = 2 + rng.nextInt(12);
            if (regionGrid[cx][cz] == ConsegrityRegions.Region.JUNGLE) {
                int y = topYGrid[cx][cz];
                int r = 3 + rng.nextInt(3);
                Material melon = safeMaterial("MELON", "MELON_BLOCK");
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz > r * r) continue;
                        int x = cx + dx, z = cz + dz;
                        if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                        int yy = topYGrid[x][z];
                        if (safeType(data, x, yy + 1, z) != Material.AIR) continue;
                        Material gg = safeType(data, x, yy, z);
                        if (gg != Material.GRASS_BLOCK && gg != Material.DIRT) continue;
                        if (rng.nextDouble() < 0.42) data.setBlock(x, yy + 1, z, melon);
                    }
                }
            }
        }

        // 4) Bamboo patch with 2 pandas (no special clearing)
        if (rng.nextDouble() < 0.28) {
            int cx = 2 + rng.nextInt(12), cz = 2 + rng.nextInt(12);
            if (regionGrid[cx][cz] == ConsegrityRegions.Region.JUNGLE) {
                int y = topYGrid[cx][cz];
                int r = 5 + rng.nextInt(3);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz > r * r) continue;
                        int x = cx + dx, z = cz + dz;
                        if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                        int yy = topYGrid[x][z];
                        if (safeType(data, x, yy + 1, z) != Material.AIR) continue;
                        if (rng.nextDouble() < 0.45) data.setBlock(x, yy + 1, z, Material.BAMBOO);
                    }
                }
                // Entity spawns in Jungle are managed by DeferredSpawnManager
            }
        }

        // 5) Entity spawns removed from Jungle sector (handled elsewhere)

        // 6) Clearing logic removed per request
    }

    // --- helpers ---
    private void placeJungleTreeSmall(ChunkGenerator.ChunkData data, int x, int y, int z, int height, SplittableRandom rng) {
        for (int i = 0; i < height; i++) data.setBlock(x, y + i, z, Material.JUNGLE_LOG);
        int top = y + height - 1;
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                int xx = x + dx, zz = z + dz;
                if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                data.setBlock(xx, top, zz, Material.JUNGLE_LEAVES);
                if (rng.nextDouble() < 0.5) data.setBlock(xx, top + 1, zz, Material.JUNGLE_LEAVES);
            }
        }
        // denser top canopy
        addTopCanopy(data, x, top, z, 2, rng);
    }

    private void placeJungleTreeMega(ChunkGenerator.ChunkData data, int x, int y, int z, int height, SplittableRandom rng) {
        // ensure space for 2x2 trunk
        if (x >= 15 || z >= 15) return;
        for (int i = 0; i < height; i++) {
            data.setBlock(x, y + i, z, Material.JUNGLE_LOG);
            data.setBlock(x + 1, y + i, z, Material.JUNGLE_LOG);
            data.setBlock(x, y + i, z + 1, Material.JUNGLE_LOG);
            data.setBlock(x + 1, y + i, z + 1, Material.JUNGLE_LOG);
        }
        int top = y + height - 2;
        int r = 4;
        org.bukkit.block.data.BlockData jungleLeaves = persistentLeaves(Material.JUNGLE_LEAVES, 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 2) continue;
                int xx = x + dx, zz = z + dz;
                if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                if (rng.nextDouble() < 0.8) data.setBlock(xx, top, zz, jungleLeaves);
                if (rng.nextDouble() < 0.35) data.setBlock(xx, top + 1, zz, jungleLeaves);
            }
        }
        // denser layered canopy at the top
        addTopCanopy(data, x + 1, top + 1, z + 1, 3, rng);
    }

    private void attachCocoaToTrunk(ChunkGenerator.ChunkData data, int x, int y, int z, SplittableRandom rng) {
        int pods = 1 + rng.nextInt(3);
        BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (int i = 0; i < pods; i++) {
            BlockFace face = faces[rng.nextInt(faces.length)];
            int tx = x + (face == BlockFace.EAST ? 1 : face == BlockFace.WEST ? -1 : 0);
            int tz = z + (face == BlockFace.SOUTH ? 1 : face == BlockFace.NORTH ? -1 : 0);
            int ty = y + 1 + rng.nextInt(3);
            if (tx < 0 || tx > 15 || tz < 0 || tz > 15) continue;
            if (safeType(data, tx, ty, tz) != Material.AIR) continue;
            // ensure trunk is behind
            int bx = tx - (face == BlockFace.EAST ? 1 : face == BlockFace.WEST ? -1 : 0);
            int bz = tz - (face == BlockFace.SOUTH ? 1 : face == BlockFace.NORTH ? -1 : 0);
            if (bx < 0 || bx > 15 || bz < 0 || bz > 15) continue;
            if (safeType(data, bx, ty, bz) != Material.JUNGLE_LOG) continue;
            try {
                BlockData bd = Bukkit.createBlockData(Material.COCOA);
                if (bd instanceof Directional) ((Directional) bd).setFacing(face);
                if (bd instanceof Ageable) ((Ageable) bd).setAge(rng.nextInt(3));
                data.setBlock(tx, ty, tz, bd);
            } catch (Throwable ignore) { /* ignore if cocoa not present */ }
        }
    }

    // Passive mob spawns are handled by DeferredSpawnManager

    private void addTopCanopy(ChunkGenerator.ChunkData data, int cx, int cy, int cz, int layers, SplittableRandom rng) {
        int r = 3;
        org.bukkit.block.data.BlockData jungleLeaves = persistentLeaves(Material.JUNGLE_LEAVES, 1);
        for (int ly = 0; ly < layers; ly++) {
            int rr = Math.max(1, r - ly);
            int y = cy + ly;
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > rr + 1) continue;
                    int x = cx + dx, z = cz + dz;
                    if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                    if (safeType(data, x, y, z) == Material.AIR && rng.nextDouble() < 0.9)
                        data.setBlock(x, y, z, jungleLeaves);
                }
            }
        }
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

    private Material safeMaterial(String preferred, String fallback) {
        try { return Material.valueOf(preferred); } catch (Throwable ignore) {
            try { return Material.valueOf(fallback); } catch (Throwable ignore2) { return Material.MELON; }
        }
    }
}
