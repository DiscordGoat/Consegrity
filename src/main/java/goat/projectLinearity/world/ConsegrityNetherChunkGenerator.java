package goat.projectLinearity.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class ConsegrityNetherChunkGenerator extends ChunkGenerator {
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;

    private static final int FLOOR_Y = -64;
    private static final int CEILING_Y = 128;

    private static final int WALL_THICKNESS = 4;
    private static final int WALL_START = 250;
    private static final int WALL_OUTER = WALL_START + WALL_THICKNESS - 1;

    private static final int FLOOR_LAYER_COUNT = 4;
    private static final int CEILING_LAYER_COUNT = 4;

    private static final double[] FLOOR_COVERAGE = new double[]{1.0, 0.75, 0.5, 0.25};
    private static final double[] CEILING_COVERAGE = new double[]{1.0, 0.75, 0.5, 0.25};
    private static final double[] WALL_COVERAGE = new double[]{1.0, 0.75, 0.5, 0.25};

    private static final int LAVA_LEVEL = 35;
    private static final double QUADRANT_BLEND = 48.0;

    private static final long SALT_QUARTZ = 0x0A27A72DL;

    public enum Sector { WASTELAND, BASIN, CLIFF, OCEAN, WALL, OUTSIDE }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        ChunkData data = this.createChunkData(world);
        long seed = world.getSeed();

        int minY = Math.max(world.getMinHeight(), FLOOR_Y);
        int maxY = Math.min(world.getMaxHeight() - 1, CEILING_Y);
        int interiorMinY = FLOOR_Y + FLOOR_LAYER_COUNT;
        int interiorMaxY = CEILING_Y - CEILING_LAYER_COUNT - 1;

        for (int lx = 0; lx < 16; lx++) {
            int wx = (chunkX << 4) + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = (chunkZ << 4) + lz;

                QuadrantWeights weights = computeQuadrantWeights(wx, wz);
                Sector sector = resolveSector(weights, wx, wz);

                Biome biome = biomeFor(weights, sector);
                applyBiomeColumn(world, biomeGrid, lx, lz, biome);

                placeFloorBands(data, seed, lx, lz, wx, wz, minY);
                placeCeilingBands(data, seed, lx, lz, wx, wz, maxY);

                int wallDepth = wallDepthFor(wx, wz);
                if (wallDepth >= WALL_THICKNESS) {
                    fillOutside(data, lx, lz, minY, maxY);
                    continue;
                }
                if (wallDepth >= 0) {
                    fillWallColumn(data, seed, lx, lz, wx, wz, wallDepth, interiorMinY, interiorMaxY);
                    continue;
                }

                data.setRegion(lx, interiorMinY, lz, lx + 1, interiorMaxY + 1, lz + 1, Material.NETHERRACK);

                double floorHeight = computeFloorHeight(seed, weights, wx, wz, sector);
                double ceilingBottom = computeCeilingBottom(weights, floorHeight, sector);

                int surfaceY = carveColumn(data, seed, lx, lz, wx, wz, interiorMinY, interiorMaxY,
                        floorHeight, ceilingBottom, weights, sector);

                applySurfaceMaterials(data, seed, lx, lz, wx, wz, surfaceY, interiorMinY, sector);
                ensureLavaOcean(data, weights, sector, lx, lz, surfaceY, interiorMinY);
            }
        }

        return data;
    }

    public static Sector sectorFor(int x, int z) {
        int wallDepth = wallDepthFor(x, z);
        if (wallDepth >= WALL_THICKNESS) return Sector.OUTSIDE;
        if (wallDepth >= 0) return Sector.WALL;
        return resolveSector(computeQuadrantWeights(x, z), x, z);
    }

    private static QuadrantWeights computeQuadrantWeights(int wx, int wz) {
        double right = sigmoid(wx / QUADRANT_BLEND);
        double down = sigmoid(wz / QUADRANT_BLEND);
        double left = 1.0 - right;
        double top = 1.0 - down;

        double q1 = left * top;
        double q2 = right * top;
        double q3 = left * down;
        double q4 = right * down;

        double sum = q1 + q2 + q3 + q4;
        if (sum != 1.0) {
            q1 /= sum;
            q2 /= sum;
            q3 /= sum;
            q4 /= sum;
        }
        return new QuadrantWeights(q1, q2, q3, q4, right, down);
    }

    private static double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    private static Sector resolveSector(QuadrantWeights w, int wx, int wz) {
        double ocean = w.oceanWeight();
        if (ocean > 0.6) {
            return Sector.OCEAN;
        }
        double cliffScore = w.q1() * Math.max(w.right(), w.down()) + w.q3() * w.right();
        if (cliffScore > 0.35 && ocean > 0.2) {
            return Sector.CLIFF;
        }
        if (w.q1() >= w.q3()) {
            return Sector.WASTELAND;
        }
        return Sector.BASIN;
    }

    private Biome biomeFor(QuadrantWeights w, Sector sector) {
        return switch (sector) {
            case WASTELAND -> Biome.SOUL_SAND_VALLEY;
            case CLIFF -> Biome.BASALT_DELTAS;
            case BASIN -> Biome.NETHER_WASTES;
            case OCEAN -> Biome.NETHER_WASTES;
            case WALL, OUTSIDE -> Biome.NETHER_WASTES;
        };
    }

    private double computeFloorHeight(long seed, QuadrantWeights w, int wx, int wz, Sector sector) {
        double terrain = w.terrainWeight();
        double ocean = w.oceanWeight();

        double base = w.q1() * 90.0 + w.q3() * 44.0 + ocean * 24.0;
        double noise = valueNoise2(seed ^ 0x3F1A57BCL, wx / 160.0, wz / 160.0);
        base += (noise - 0.5) * (6.0 + terrain * 4.0);

        double cliffDrop = w.q1() * w.right() * 30.0 + w.q1() * w.down() * 12.0;
        base -= cliffDrop;

        if (sector == Sector.CLIFF) {
            base -= 6.0;
        }

        double slopeFactor = clamp01((ocean - 0.3) / 0.5);
        double minFloor = lerp(LAVA_LEVEL + 5.0, LAVA_LEVEL - 8.0, slopeFactor);

        base = Math.max(minFloor, base);
        if (ocean > 0.5) {
            base = Math.min(LAVA_LEVEL - 1.0 + (1.0 - slopeFactor) * 4.0, base);
        }
        base = Math.min(CEILING_Y - 12.0, base);
        return base;
    }

    private double computeCeilingBottom(QuadrantWeights w, double floorHeight, Sector sector) {
        double ocean = w.oceanWeight();
        double terrain = w.terrainWeight();

        double gap = 24.0;
        gap -= w.q1() * 5.0;
        gap += w.q3() * 3.5;
        gap += terrain * 4.0;

        double slopeFactor = clamp01((ocean - 0.3) / 0.5);
        gap = lerp(gap, 18.0, slopeFactor * 0.4);

        gap += ocean * 32.0;
        if (ocean > 0.6) gap += 12.0;
        if (sector == Sector.OCEAN) gap += 12.0;
        if (sector == Sector.CLIFF) gap += 6.0;

        gap = clamp(gap, 14.0, 95.0);

        double ceilingBottom = floorHeight + gap;
        double targetOceanCeiling = LAVA_LEVEL + 14.0;
        ceilingBottom = lerp(ceilingBottom, targetOceanCeiling, slopeFactor);

        ceilingBottom = Math.min(CEILING_Y - 5.0, ceilingBottom);
        return ceilingBottom;
    }

    private int carveColumn(ChunkData data, long seed, int lx, int lz, int wx, int wz,
                             int interiorMinY, int interiorMaxY,
                             double floorHeight, double ceilingBottom,
                             QuadrantWeights w, Sector sector) {

        int floorY = Math.max(interiorMinY, Math.min(interiorMaxY, (int) Math.round(floorHeight)));
        int ceilingY = Math.max(floorY + 1, Math.min(interiorMaxY, (int) Math.round(ceilingBottom)));

        double ocean = w.oceanWeight();
        double terrain = w.terrainWeight();

        for (int y = ceilingY; y >= floorY + 1; y--) {
            double noise = carveNoise(seed, wx, y, wz);
            double normalized = (double) (y - (floorY + 1)) / Math.max(1, (ceilingY - floorY - 1));

            double carveChance = 0.45 + ocean * 0.35 - terrain * 0.08;
            carveChance += (normalized - 0.5) * 0.2;
            if (normalized < 0.2) carveChance -= 0.25;
            if (normalized > 0.8) carveChance -= 0.2;

            carveChance += w.q1() * (normalized * 0.15);
            carveChance -= w.q1() * ((1.0 - normalized) * 0.25);
            carveChance -= w.q3() * (normalized * 0.1);
            carveChance += w.q3() * ocean * 0.25 * (1.0 - Math.abs(0.5 - normalized));

            if (sector == Sector.CLIFF) carveChance += 0.08;
            if (sector == Sector.BASIN) carveChance -= 0.04;

            carveChance = clamp01(carveChance);

            if (noise < carveChance) {
                if (y <= LAVA_LEVEL && ocean > 0.35) {
                    data.setBlock(lx, y, lz, Material.LAVA);
                } else {
                    data.setBlock(lx, y, lz, Material.AIR);
                }
            } else {
                maybePlaceQuartz(data, seed, lx, lz, wx, wz, y, w);
            }
        }

        return floorY;
    }

    private double carveNoise(long seed, int wx, int y, int wz) {
        double n1 = valueNoise2(seed ^ 0x13579BDFL, (wx + y * 0.7) / 48.0, (wz - y * 0.7) / 48.0);
        double n2 = valueNoise2(seed ^ 0x2468ACE1L, (wx - y * 1.2) / 96.0, (wz + y * 1.1) / 96.0);
        double n3 = valueNoise2(seed ^ 0x5A5A5A5AL, (wx + y * 0.33) / 32.0, (wz - y * 0.41) / 32.0);
        return n1 * 0.55 + n2 * 0.35 + n3 * 0.10;
    }

    private void maybePlaceQuartz(ChunkData data, long seed, int lx, int lz, int wx, int wz, int y, QuadrantWeights w) {
        Material current = data.getType(lx, y, lz);
        if (current != Material.NETHERRACK) return;

        double chance = 0.012 + w.oceanWeight() * 0.01 + w.q1() * 0.015 + w.q3() * 0.005;
        if (randomDouble(seed ^ SALT_QUARTZ, wx, y, wz) < chance) {
            data.setBlock(lx, y, lz, Material.NETHER_QUARTZ_ORE);
        }
    }

    private void applySurfaceMaterials(ChunkData data, long seed, int lx, int lz, int wx, int wz,
                                       int surfaceY, int interiorMinY, Sector sector) {
        if (surfaceY <= interiorMinY) return;
        if (sector == Sector.OCEAN) return;

        if (sector == Sector.WASTELAND) {
            double roll = randomDouble(seed ^ 0x3B55AA1L, wx, surfaceY, wz);
            data.setBlock(lx, surfaceY, lz, roll < 0.5 ? Material.SOUL_SOIL : Material.SOUL_SAND);
            if (roll < 0.12 && data.getType(lx, surfaceY + 1, lz) == Material.AIR) {
                data.setBlock(lx, surfaceY + 1, lz, Material.NETHER_SPROUTS);
            }
        } else if (sector == Sector.CLIFF) {
            data.setBlock(lx, surfaceY, lz, Material.BLACKSTONE);
            if (surfaceY - 1 >= interiorMinY) {
                data.setBlock(lx, surfaceY - 1, lz, Material.BLACKSTONE);
            }
        }
    }

    private void ensureLavaOcean(ChunkData data, QuadrantWeights weights, Sector sector, int lx, int lz, int surfaceY, int interiorMinY) {
        double ocean = weights.oceanWeight();
        if (sector != Sector.OCEAN && ocean < 0.35) return;

        int lavaSurface = LAVA_LEVEL;
        int lavaBottom = ocean > 0.55
                ? Math.max(interiorMinY, FLOOR_Y + FLOOR_LAYER_COUNT)
                : Math.max(Math.min(surfaceY + 1, lavaSurface), interiorMinY);
        lavaBottom = Math.min(lavaBottom, lavaSurface);

        boolean floodAll = ocean > 0.55;

        for (int y = lavaBottom; y <= lavaSurface; y++) {
            Material current = data.getType(lx, y, lz);
            if (floodAll) {
                if (current != Material.BEDROCK) {
                    data.setBlock(lx, y, lz, Material.LAVA);
                }
            } else if (current == Material.AIR || current == Material.CAVE_AIR || current == Material.WATER) {
                data.setBlock(lx, y, lz, Material.LAVA);
            } else if (current != Material.BEDROCK && y == lavaSurface && ocean > 0.5) {
                data.setBlock(lx, y, lz, Material.LAVA);
            }
        }

        if (surfaceY >= interiorMinY && surfaceY < lavaBottom && sector == Sector.OCEAN && ocean < 0.55) {
            data.setBlock(lx, surfaceY, lz, Material.NETHERRACK);
        }

        if (ocean > 0.45) {
            int airTop = lavaSurface + (ocean > 0.7 ? 6 : 3);
            for (int y = lavaSurface + 1; y <= airTop && y < CEILING_Y; y++) {
                Material current = data.getType(lx, y, lz);
                if (current != Material.BEDROCK && current != Material.LAVA) {
                    data.setBlock(lx, y, lz, Material.AIR);
                }
            }
        }
    }

    private void placeFloorBands(ChunkData data, long seed, int lx, int lz, int wx, int wz, int minY) {
        for (int i = 0; i < FLOOR_LAYER_COUNT; i++) {
            int y = FLOOR_Y + i;
            if (y < minY) continue;
            boolean bedrock = shouldPlace(seed, wx, y, wz, FLOOR_COVERAGE[i]);
            data.setBlock(lx, y, lz, bedrock ? Material.BEDROCK : Material.NETHERRACK);
        }
    }

    private void placeCeilingBands(ChunkData data, long seed, int lx, int lz, int wx, int wz, int maxY) {
        for (int i = 0; i < CEILING_LAYER_COUNT; i++) {
            int y = CEILING_Y - i;
            if (y > maxY) continue;
            boolean bedrock = shouldPlace(seed, wx, y, wz, CEILING_COVERAGE[i]);
            data.setBlock(lx, y, lz, bedrock ? Material.BEDROCK : Material.NETHERRACK);
        }
    }

    private void fillWallColumn(ChunkData data, long seed, int lx, int lz, int wx, int wz, int depth, int interiorMinY, int interiorMaxY) {
        double coverage = WALL_COVERAGE[Math.min(depth, WALL_COVERAGE.length - 1)];
        for (int y = interiorMinY; y <= interiorMaxY; y++) {
            if (shouldPlace(seed, wx, y, wz, coverage)) {
                data.setBlock(lx, y, lz, Material.BEDROCK);
            } else if (data.getType(lx, y, lz) == Material.AIR) {
                data.setBlock(lx, y, lz, Material.BLACKSTONE);
            }
        }
    }

    private void fillOutside(ChunkData data, int lx, int lz, int minY, int maxY) {
        for (int y = minY; y <= maxY; y++) {
            data.setBlock(lx, y, lz, Material.BEDROCK);
        }
    }

    private void applyBiomeColumn(World world, BiomeGrid grid, int lx, int lz, Biome biome) {
        if (grid == null || biome == null) return;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        try {
            for (int y = minY; y <= maxY; y += 4) {
                grid.setBiome(lx, y, lz, biome);
            }
        } catch (Throwable ignored) {
            try {
                grid.setBiome(lx, lz, biome);
            } catch (Throwable ignoredAgain) {
                // ignore legacy APIs without biomes per column
            }
        }
    }

    private boolean shouldPlace(long seed, int x, int y, int z, double coverage) {
        if (coverage >= 1.0) return true;
        if (coverage <= 0.0) return false;
        return randomDouble(seed, x, y, z) < coverage;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private double valueNoise2(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        double tx = fade(x - x0);
        double tz = fade(z - z0);
        double c00 = randomDouble(seed, x0, 0, z0);
        double c10 = randomDouble(seed, x1, 0, z0);
        double c01 = randomDouble(seed, x0, 0, z1);
        double c11 = randomDouble(seed, x1, 0, z1);
        double a = lerp(c00, c10, tx);
        double b = lerp(c01, c11, tx);
        return lerp(a, b, tz);
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double randomDouble(long seed, int x, int y, int z) {
        long h = hash(seed, x, y, z);
        return (double) (h & 0x1FFFFFFFFFFFFFL) / (double) (1L << 53);
    }

    private long hash(long seed, int x, int y, int z) {
        long h = seed ^ 0x632BE59BD9B4E019L;
        h ^= ((long) x * 0x9E3779B97F4A7C15L);
        h ^= ((long) y * 0xC2B2AE3D27D4EB4FL);
        h ^= ((long) z * 0x94D049BB133111EBL);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h;
    }

    public static int wallDepthFor(int x, int z) {
        int dx = x - CENTER_X;
        int dz = z - CENTER_Z;
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int maxAbs = Math.max(ax, az);
        if (maxAbs > WALL_OUTER) {
            return WALL_THICKNESS;
        }
        if (maxAbs >= WALL_START) {
            return Math.max(0, Math.min(WALL_THICKNESS - 1, maxAbs - WALL_START));
        }
        return -1;
    }

    private record QuadrantWeights(double q1, double q2, double q3, double q4, double right, double down) {
        double terrainWeight() { return q1 + q3; }
        double oceanWeight() { return q2 + q4; }
    }
}
