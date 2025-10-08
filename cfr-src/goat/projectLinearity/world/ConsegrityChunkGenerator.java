/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Biome
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.type.Snow
 *  org.bukkit.generator.ChunkGenerator$BiomeGrid
 *  org.bukkit.generator.ChunkGenerator$ChunkData
 */
package goat.projectLinearity.world;

import goat.projectLinearity.world.ConsegrityRegions;
import java.util.Random;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.generator.ChunkGenerator;

public class ConsegrityChunkGenerator
extends ArcticChunkGenerator {
    private static final int SEA_LEVEL = 153;
    private static final int AUDIT_Y_MIN = -192;
    private static final int AUDIT_Y_MAX = 320;
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    private static final double TRANSITION = 10.0;
    private static final double CENTRAL_RADIUS = 50.0;
    private static final double CENTRAL_FEATHER = 45.0;
    private static final double CENTRAL_JITTER = 20.0;
    private static final double R1_INNER = 60.0;
    private static final double R1_OUTER = 260.0;
    private static final double R1_FEATHER = 10.0;
    private static final double R2_INNER = 250.0;
    private static final double R2_OUTER = 400.0;
    private static final double R2_FEATHER = 25.0;
    private static final double FEATHER_WIDTH = 30.0;
    private static final double[] R1_SPLITS = new double[]{0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
    private static final double[] R2_SPLITS = new double[]{0.0, 0.3333333333333, 0.6666666666666, 1.0};
    private static final double SECTOR_BLEND_FRAC = -1.28;
    private static final int R1_BASE = 165;
    private static final int R2_BASE = 175;
    private static final double UNION_EPS = 0.03;
    private static final double COAST_BACK = 80.0;
    private static final int SHELF_DEPTH = 13;
    private static final int CAVE_CELL = 64;
    private static final double CAVE_STEP = 1.1;
    private static final int CAVE_STEPS_MIN = 120;
    private static final int CAVE_STEPS_RANGE = 80;
    private static final double CAVE_RMAX = 2.6;
    private static final int CAVE_STEPS_MAX = 200;
    private static final double CAVE_MAX_REACH = 222.60000000000002;
    private static final int CAVE_REACH = (int)Math.ceil(3.4781250000000004) + 1;
    private static final long CAVE_SALT = 3237997278L;
    private static final int[] OFFS = new int[]{0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1};
    private static final double BORDER_WARP_BIG = 18.0;
    private static final double BORDER_WARP_SMALL = 7.0;
    private static final double BORDER_WARP_F1 = 340.0;
    private static final double BORDER_WARP_F2 = 120.0;

    @Override
    public ChunkGenerator.ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biome) {
        int lz;
        int lx;
        double baseRot;
        ChunkGenerator.ChunkData data = this.createChunkData(world);
        long seed = world.getSeed();
        int minY = Math.max(world.getMinHeight(), -192);
        int maxY = Math.min(world.getMaxHeight() - 1, 320);
        double rotR1 = baseRot = ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, 101L, 0L, 0L, 7466709L));
        int r2Arcs = R2_SPLITS.length - 1;
        double rotR2 = ConsegrityChunkGenerator.wrap01(baseRot + 0.5 / (double)Math.max(1, r2Arcs) + (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, 202L, 0L, 0L, 7532245L)) - 0.5) * (0.2 / (double)Math.max(1, r2Arcs)));
        int[][] topYGrid = new int[16][16];
        int[][] floorYGrid = new int[16][16];
        double[][] centralMaskGrid = new double[16][16];
        boolean[][] needsWater = new boolean[16][16];
        boolean[][] frozenOcean = new boolean[16][16];
        ConsegrityRegions.Region[][] regionGrid = new ConsegrityRegions.Region[16][16];
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                int idx;
                int topY;
                double target;
                int wx = (chunkX << 4) + lx;
                int wz = (chunkZ << 4) + lz;
                RingEdges C = this.centralEdges(seed, wx, wz, 50.0, 45.0);
                RingEdges R1 = this.ringEdges(seed, wx, wz, 60.0, 260.0, 10.0);
                RingEdges R2 = this.ringEdges(seed, wx, wz, 250.0, 400.0, 25.0);
                double cm = 1.0 - ConsegrityChunkGenerator.clamp01((C.r - C.inner) / 45.0);
                double r1 = ConsegrityChunkGenerator.bandMask(R1.r, R1.inner, R1.outer, 10.0);
                double r2 = ConsegrityChunkGenerator.bandMask(R2.r, R2.inner, R2.outer, 25.0);
                double r1m = r1 * (1.0 - cm);
                double r2m = r2 * (1.0 - cm);
                double union = Math.max(cm, Math.max(r1, r2));
                boolean inOcean = union <= 0.03 && !(C.r <= 150.0);
                double uWarp = this.angle01Warped(seed, wx, wz);
                Blend b1 = ConsegrityChunkGenerator.arcBlendFromSplits(ConsegrityChunkGenerator.wrap01(uWarp + rotR1), R1_SPLITS, 160.0);
                Blend b2 = ConsegrityChunkGenerator.arcBlendFromSplits(ConsegrityChunkGenerator.wrap01(uWarp + rotR2), R2_SPLITS, 325.0);
                int idxR1 = ConsegrityChunkGenerator.arcIndex(ConsegrityChunkGenerator.wrap01(uWarp + rotR1), R1_SPLITS);
                int idxR2 = ConsegrityChunkGenerator.arcIndex(ConsegrityChunkGenerator.wrap01(uWarp + rotR2), R2_SPLITS);
                int oceanFloor = this.oceanFloorY(seed, wx, wz);
                int centralTarget = this.centralSurfaceY(seed, wx, wz);
                int desertTarget = this.ring1DesertSurfaceY(seed, wx, wz);
                int savannahTarget = this.ring1SavannahSurfaceY(seed, wx, wz);
                int swampTarget = this.ring1SwampSurfaceY(seed, wx, wz);
                int jungleTarget = this.ring2JungleSurfaceY(seed, wx, wz);
                int iceTarget = this.ring1IceSpikesSurfaceY(seed, wx, wz);
                int cherryTarget = this.ring1CherrySurfaceY(seed, wx, wz);
                int mesaTarget = this.ring2MesaSurfaceY(seed, wx, wz);
                int mountainTarget = this.ring2MountainSurfaceY(seed, wx, wz);
                int DESERT_BASE = 163;
                int SAVANNA_BASE = 165;
                int SWAMP_BASE = 161;
                int MESA_BASE = 175;
                int JUNGLE_BASE = 169;
                int ICE1_BASE = 167;
                int CHERRY1_BASE = 173;
                int MTN_BASE = 183;
                double r1DeltaA = switch (b1.a) {
                    case 0 -> desertTarget - 163;
                    case 1 -> savannahTarget - 165;
                    case 2 -> iceTarget - 167;
                    case 3 -> cherryTarget - 173;
                    case 4 -> swampTarget - 161;
                    default -> 0.0;
                };
                double r1DeltaB = switch (b1.b) {
                    case 0 -> desertTarget - 163;
                    case 1 -> savannahTarget - 165;
                    case 2 -> iceTarget - 167;
                    case 3 -> cherryTarget - 173;
                    case 4 -> swampTarget - 161;
                    default -> r1DeltaA;
                };
                double r1MainH = 165.0 + r1DeltaA;
                double tArc = b1.a == b1.b ? 0.0 : ConsegrityChunkGenerator.smooth01(1.0 - b1.t);
                double r1CrossH = 165.0 + ConsegrityChunkGenerator.lerp(r1DeltaA, r1DeltaB, tArc);
                double tBridge1 = b1.a == b1.b ? 0.0 : ConsegrityChunkGenerator.smooth01(1.0 - Math.abs(b1.t * 2.0 - 1.0));
                double ring1H = ConsegrityChunkGenerator.lerp(r1MainH, r1CrossH, tBridge1);
                double r2DeltaA = switch (b2.a) {
                    case 0 -> mesaTarget - 175;
                    case 1 -> jungleTarget - 169;
                    default -> (double)(mountainTarget - 183) * 1.15;
                };
                double r2DeltaB = switch (b2.b) {
                    case 0 -> mesaTarget - 175;
                    case 1 -> jungleTarget - 169;
                    default -> (double)(mountainTarget - 183) * 1.15;
                };
                double r2MainH = 175.0 + r2DeltaA;
                double tArc2 = b2.a == b2.b ? 0.0 : ConsegrityChunkGenerator.smooth01(b2.t);
                double r2CrossH = 175.0 + ConsegrityChunkGenerator.lerp(r2DeltaA, r2DeltaB, tArc2);
                double tBridge2 = b2.a == b2.b ? 0.0 : ConsegrityChunkGenerator.smooth01(1.0 - Math.abs(b2.t * 2.0 - 1.0));
                double ring2H = ConsegrityChunkGenerator.lerp(r2MainH, r2CrossH, tBridge2);
                double wC = cm;
                double w1 = r1m;
                double w2 = r2m;
                double wSum = wC + w1 + w2;
                if (union > 0.03 || C.r <= 150.0) {
                    double landH = (wC * (double)centralTarget + w1 * ring1H + w2 * ring2H) / wSum;
                    double tInR1 = 1.0 - ConsegrityChunkGenerator.clamp01((R1.r - (R1.outer - 5.0)) / Math.max(1.0, 10.0));
                    double tInR2 = 1.0 - ConsegrityChunkGenerator.clamp01((R2.inner + 12.5 - R2.r) / Math.max(1.0, 25.0));
                    double tBridge = ConsegrityChunkGenerator.smooth01(ConsegrityChunkGenerator.clamp01(Math.min(tInR1, tInR2)));
                    double crossT = ConsegrityChunkGenerator.clamp01((R2.r - R2.inner) / Math.max(1.0, R2.outer - R2.inner));
                    double crossH = ConsegrityChunkGenerator.lerp(ring1H, ring2H, ConsegrityChunkGenerator.smooth01(crossT));
                    landH = ConsegrityChunkGenerator.lerp(landH, crossH, tBridge);
                    double sIn = ConsegrityChunkGenerator.clamp01((R2.r - (R2.outer - 80.0)) / Math.max(1.0, 80.0));
                    if ((landH = ConsegrityChunkGenerator.lerp(landH, 152.0, sIn)) < 152.0) {
                        landH = 152.0;
                    }
                    target = landH;
                } else if (R1.r < R1.innerGate) {
                    double t = ConsegrityChunkGenerator.clamp01((R1.r - C.outerGate) / Math.max(1.0, R1.innerGate - C.outerGate));
                    t = t * t * (t * -2.0 + 3.0);
                    target = ConsegrityChunkGenerator.lerp(centralTarget, ring1H, t);
                } else {
                    double oceanRaised;
                    double sOut = ConsegrityChunkGenerator.clamp01((R2.outer + 80.0 - R2.r) / Math.max(1.0, 80.0));
                    int shelfY = 140;
                    target = oceanRaised = ConsegrityChunkGenerator.lerp(oceanFloor, Math.max(oceanFloor, shelfY), sOut);
                }
                topYGrid[lx][lz] = topY = (int)Math.round(target);
                centralMaskGrid[lx][lz] = cm;
                if (topY - 3 > minY) {
                    data.setRegion(lx, minY, lz, lx + 1, topY - 2, lz + 1, Material.STONE);
                }
                if (inOcean) {
                    boolean isFrozen;
                    this.paintSeafloorCap(data, lx, lz, topY, 153);
                    int depth = 153 - topY;
                    double uO = this.angle01Warped(seed, wx, wz);
                    int sectorIdx = ConsegrityChunkGenerator.arcIndex(ConsegrityChunkGenerator.wrap01(uO + rotR1), R1_SPLITS);
                    frozenOcean[lx][lz] = isFrozen = sectorIdx == 2;
                    if (isFrozen) {
                        biome.setBiome(lx, lz, depth >= 30 ? Biome.DEEP_FROZEN_OCEAN : Biome.FROZEN_OCEAN);
                    } else {
                        biome.setBiome(lx, lz, depth >= 30 ? Biome.DEEP_OCEAN : Biome.OCEAN);
                    }
                    needsWater[lx][lz] = true;
                    floorYGrid[lx][lz] = topY;
                    regionGrid[lx][lz] = ConsegrityRegions.Region.OCEAN;
                    continue;
                }
                if (cm > Math.max(r1, r2)) {
                    this.paintLandCap(data, lx, lz, topY);
                    biome.setBiome(lx, lz, Biome.PLAINS);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.CENTRAL;
                    continue;
                }
                if (r1m >= r2m) {
                    if (b1.a != b1.b) {
                        double t = ConsegrityChunkGenerator.smooth01(1.0 - b1.t);
                        int left = b1.a;
                        int right = b1.b;
                        if (left == 0 && right != 0 || right == 0 && left != 0) {
                            double tLand = right != 0 ? t : 1.0 - t;
                            this.paintDesertLandBlendCap(data, lx, lz, topY, tLand);
                        } else {
                            this.paintLandCap(data, lx, lz, topY);
                        }
                        int n = idx = t >= 0.5 ? left : right;
                        if (idx == 0) {
                            biome.setBiome(lx, lz, Biome.DESERT);
                            regionGrid[lx][lz] = ConsegrityRegions.Region.DESERT;
                            continue;
                        }
                        if (idx == 1) {
                            biome.setBiome(lx, lz, Biome.SAVANNA);
                            regionGrid[lx][lz] = ConsegrityRegions.Region.SAVANNAH;
                            continue;
                        }
                        if (idx == 2) {
                            biome.setBiome(lx, lz, Biome.ICE_SPIKES);
                            regionGrid[lx][lz] = ConsegrityRegions.Region.ICE_SPIKES;
                            continue;
                        }
                        if (idx == 3) {
                            biome.setBiome(lx, lz, Biome.CHERRY_GROVE);
                            regionGrid[lx][lz] = ConsegrityRegions.Region.CHERRY;
                            continue;
                        }
                        biome.setBiome(lx, lz, Biome.SWAMP);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.SWAMP;
                        continue;
                    }
                    int idx2 = idxR1;
                    if (idx2 == 0) {
                        this.paintDesertCap(data, lx, lz, topY);
                        biome.setBiome(lx, lz, Biome.DESERT);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.DESERT;
                        continue;
                    }
                    if (idx2 == 1) {
                        this.paintLandCap(data, lx, lz, topY);
                        biome.setBiome(lx, lz, Biome.SAVANNA);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.SAVANNAH;
                        continue;
                    }
                    if (idx2 == 2) {
                        this.paintLandCap(data, lx, lz, topY);
                        biome.setBiome(lx, lz, Biome.ICE_SPIKES);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.ICE_SPIKES;
                        continue;
                    }
                    if (idx2 == 3) {
                        this.paintLandCap(data, lx, lz, topY);
                        biome.setBiome(lx, lz, Biome.CHERRY_GROVE);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.CHERRY;
                        continue;
                    }
                    this.paintLandCap(data, lx, lz, topY);
                    biome.setBiome(lx, lz, Biome.SWAMP);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.SWAMP;
                    continue;
                }
                if (b2.a != b2.b) {
                    double t = ConsegrityChunkGenerator.smooth01(1.0 - b2.t);
                    int left = b2.a;
                    int right = b2.b;
                    if (left == 0 && right != 0 || right == 0 && left != 0) {
                        double tLand = right != 0 ? t : 1.0 - t;
                        this.paintMesaLandBlendCap(data, lx, lz, topY, tLand);
                    } else if (left == 2 || right == 2) {
                        this.paintMountainCap(data, lx, lz, topY);
                    } else {
                        this.paintLandCap(data, lx, lz, topY);
                    }
                    int n = idx = t >= 0.5 ? left : right;
                    if (idx == 0) {
                        biome.setBiome(lx, lz, Biome.ERODED_BADLANDS);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.MESA;
                        continue;
                    }
                    if (idx == 1) {
                        biome.setBiome(lx, lz, Biome.JUNGLE);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.JUNGLE;
                        continue;
                    }
                    biome.setBiome(lx, lz, Biome.STONY_PEAKS);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.MOUNTAIN;
                    continue;
                }
                int idx3 = idxR2;
                if (idx3 == 0) {
                    this.paintMesaStrata(data, lx, lz, topY);
                    biome.setBiome(lx, lz, Biome.ERODED_BADLANDS);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.MESA;
                    continue;
                }
                if (idx3 == 1) {
                    this.paintLandCap(data, lx, lz, topY);
                    biome.setBiome(lx, lz, Biome.JUNGLE);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.JUNGLE;
                    continue;
                }
                this.paintMountainCap(data, lx, lz, topY);
                biome.setBiome(lx, lz, Biome.STONY_PEAKS);
                regionGrid[lx][lz] = ConsegrityRegions.Region.MOUNTAIN;
            }
        }
        this.placeOresByRegion(world, data, seed, chunkX, chunkZ, regionGrid);
        this.carveCavesV2(world, data, seed, chunkX, chunkZ);
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                if (!needsWater[lx][lz]) continue;
                int topSolid = this.findTopSolidY(data, world, lx, lz, minY, 153);
                if (153 >= topSolid + 1) {
                    data.setRegion(lx, topSolid + 1, lz, lx + 1, 154, lz + 1, Material.WATER);
                }
                if (frozenOcean[lx][lz]) continue;
                this.addSeaVegetation(world, data, seed, chunkX, chunkZ, lx, lz, floorYGrid[lx][lz]);
            }
        }
        this.placeFrozenOceanIcebergs(world, data, seed, chunkX, chunkZ, floorYGrid, frozenOcean);
        this.placeCentralTrees(world, data, seed, chunkX, chunkZ, topYGrid, centralMaskGrid);
        this.placeIceSpikesFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeCherryFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeDesertFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeBedrockBand(world, data, chunkX, chunkZ);
        return data;
    }

    private int ring1IceSpikesSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x11CE5EEDL, (double)wx / 240.0, (double)wz / 240.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x22CE5EEDL, (double)wx / 84.0, (double)wz / 84.0);
        double h = (h1 * 0.65 + h2 * 0.35) * 2.0 - 1.0;
        int base = 171;
        int amp = 14;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring1CherrySurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x7C3EE111L, (double)wx / 260.0, (double)wz / 260.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x7C3EE112L, (double)wx / 88.0, (double)wz / 88.0);
        double h3 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x7C3EE113L, (double)wx / 34.0, (double)wz / 34.0);
        double h = (h1 * 0.52 + h2 * 0.32 + h3 * 0.16) * 2.0 - 1.0;
        int base = 177;
        int amp = 26;
        return base + (int)Math.round(h * (double)amp);
    }

    private void placeIceSpikesFeatures(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        int topY;
        int lz;
        int lx;
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 484811169L);
        for (int lx2 = 0; lx2 < 16; ++lx2) {
            for (int lz2 = 0; lz2 < 16; ++lz2) {
                int topY2;
                if (regionGrid[lx2][lz2] != ConsegrityRegions.Region.ICE_SPIKES || (topY2 = topYGrid[lx2][lz2]) < world.getMinHeight() + 1) continue;
                int wx = baseX + lx2;
                int wz = baseZ + lz2;
                int ySnow = topY2 + 1;
                if (ySnow > world.getMaxHeight() - 1) continue;
                try {
                    Snow snow = (Snow)Bukkit.createBlockData((Material)Material.SNOW);
                    int layers = 1 + rng.nextInt(3);
                    snow.setLayers(layers);
                    if (data.getType(lx2, ySnow, lz2) != Material.AIR) continue;
                    data.setBlock(lx2, ySnow, lz2, (BlockData)snow);
                    continue;
                }
                catch (Throwable ignore) {
                    if (data.getType(lx2, ySnow, lz2) != Material.AIR) continue;
                    data.setBlock(lx2, ySnow, lz2, Material.SNOW);
                }
            }
        }
        int spikes = rng.nextDouble() < 0.65 ? 1 + rng.nextInt(3) : 0;
        for (int i = 0; i < spikes; ++i) {
            lx = 2 + rng.nextInt(12);
            if (regionGrid[lx][lz = 2 + rng.nextInt(12)] != ConsegrityRegions.Region.ICE_SPIKES || (topY = topYGrid[lx][lz]) < 151 || this.slopeGrid(topYGrid, lx, lz) > 3) continue;
            int h = 6 + rng.nextInt(11);
            int r = rng.nextDouble() < 0.2 ? 2 : 1;
            this.makeIceSpike(data, lx, topY + 1, lz, h, r, rng);
        }
        for (int t = 0; t < 60; ++t) {
            Material ground;
            lx = rng.nextInt(16);
            lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14 || regionGrid[lx][lz] != ConsegrityRegions.Region.ICE_SPIKES) continue;
            topY = topYGrid[lx][lz];
            int wx = baseX + lx;
            int wz = baseZ + lz;
            double dx = (double)wx - 0.0;
            double dz = (double)wz - 0.0;
            double r = Math.sqrt(dx * dx + dz * dz);
            double inner = 60.0;
            double outer = 260.0;
            double closeness = ConsegrityChunkGenerator.clamp01(1.0 - (r - inner) / Math.max(1.0, outer - inner));
            double chance = 0.02 + 0.1 * closeness;
            if (rng.nextDouble() > chance || this.slopeGrid(topYGrid, lx, lz) > 3 || (ground = data.getType(lx, topY, lz)) != Material.GRASS_BLOCK && ground != Material.DIRT && ground != Material.SNOW_BLOCK && ground != Material.STONE) continue;
            int height = 5 + rng.nextInt(6);
            this.placeSpruceSimple(data, lx, topY + 1, lz, height);
        }
    }

    private void placeCherryFeatures(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 3303206417L);
        int attempts = 7;
        for (int i = 0; i < attempts; ++i) {
            Material top;
            int y;
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14 || regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY || (y = topYGrid[lx][lz]) < 160 || y > 200 || this.slopeGrid(topYGrid, lx, lz) > 4 || (top = data.getType(lx, y, lz)) != Material.GRASS_BLOCK && top != Material.DIRT && top != Material.STONE) continue;
            int h = 4 + rng.nextInt(4);
            this.placeCherryTree(data, lx, y + 1, lz, h, rng);
            if (!(rng.nextDouble() < 0.35)) continue;
            this.scatterPetals(data, lx, y + 1, lz, rng);
        }
    }

    private void placeCherryTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height, SplittableRandom rng) {
        for (int i = 0; i < height; ++i) {
            data.setBlock(lx, y + i, lz, Material.CHERRY_LOG);
        }
        int top = y + height - 1;
        int rMax = 2 + rng.nextInt(2);
        for (int dy = -1; dy <= 2; ++dy) {
            int ry = rMax - Math.max(0, dy);
            for (int dx = -ry; dx <= ry; ++dx) {
                for (int dz = -ry; dz <= ry; ++dz) {
                    Material cur;
                    if (Math.abs(dx) + Math.abs(dz) > ry + 1 || rng.nextDouble() < 0.15) continue;
                    int xx = lx + dx;
                    int yy = top + dy;
                    int zz = lz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    try {
                        cur = data.getType(xx, yy, zz);
                    }
                    catch (Throwable t) {
                        cur = Material.AIR;
                    }
                    if (cur != Material.AIR) continue;
                    data.setBlock(xx, yy, zz, Material.CHERRY_LEAVES);
                }
            }
        }
    }

    private void scatterPetals(ChunkGenerator.ChunkData data, int lx, int y, int lz, SplittableRandom rng) {
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                int yy;
                Material ground;
                if (Math.abs(dx) + Math.abs(dz) > 3 || rng.nextDouble() > 0.35) continue;
                int xx = lx + dx;
                int zz = lz + dz;
                if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || (ground = data.getType(xx, yy = y - 1, zz)) != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
                try {
                    data.setBlock(xx, yy + 1, zz, Material.PINK_PETALS);
                    continue;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
    }

    private int slopeGrid(int[][] topY, int lx, int lz) {
        int y0 = topY[lx][lz];
        int max = 0;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (dx == 0 && dz == 0) continue;
                int x = lx + dx;
                int z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                int y1 = topY[x][z];
                max = Math.max(max, Math.abs(y1 - y0));
            }
        }
        return max;
    }

    private void placeDesertFeatures(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 3546184538L);
        double pCactus = 0.005;
        double pDead = 0.005;
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                Material above;
                boolean sandy;
                Material ground;
                int topY;
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.DESERT || (topY = topYGrid[lx][lz]) <= world.getMinHeight() + 1) continue;
                try {
                    ground = data.getType(lx, topY, lz);
                }
                catch (Throwable t) {
                    ground = Material.SAND;
                }
                boolean bl = sandy = ground == Material.SAND || ground == Material.RED_SAND || ground == Material.SANDSTONE;
                if (!sandy) continue;
                double r = rng.nextDouble();
                if (r < 0.005) {
                    int dy;
                    if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
                    int maxH = Math.min(world.getMaxHeight() - 2, topY + 4);
                    int height = 1 + rng.nextInt(3);
                    if (topY + height >= maxH) {
                        height = Math.max(1, maxH - topY - 1);
                    }
                    boolean clear = true;
                    for (dy = 1; dy <= height; ++dy) {
                        Material above2;
                        try {
                            above2 = data.getType(lx, topY + dy, lz);
                        }
                        catch (Throwable t) {
                            above2 = Material.AIR;
                        }
                        if (above2 != Material.AIR) {
                            clear = false;
                            break;
                        }
                        Material e1 = this.safeType(data, lx + 1, topY + dy, lz);
                        Material e2 = this.safeType(data, lx - 1, topY + dy, lz);
                        Material e3 = this.safeType(data, lx, topY + dy, lz + 1);
                        Material e4 = this.safeType(data, lx, topY + dy, lz - 1);
                        if (e1 == Material.AIR && e2 == Material.AIR && e3 == Material.AIR && e4 == Material.AIR) continue;
                        clear = false;
                        break;
                    }
                    if (!clear) continue;
                    for (dy = 1; dy <= height; ++dy) {
                        data.setBlock(lx, topY + dy, lz, Material.CACTUS);
                    }
                    continue;
                }
                if (!(r < 0.01)) continue;
                try {
                    above = data.getType(lx, topY + 1, lz);
                }
                catch (Throwable t) {
                    above = Material.AIR;
                }
                if (above != Material.AIR) continue;
                try {
                    data.setBlock(lx, topY + 1, lz, Material.DEAD_BUSH);
                    continue;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
    }

    private Material safeType(ChunkGenerator.ChunkData data, int lx, int y, int lz) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
            return Material.AIR;
        }
        try {
            return data.getType(lx, y, lz);
        }
        catch (Throwable t) {
            return Material.AIR;
        }
    }

    private void makeIceSpike(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height, int radius, SplittableRandom rng) {
        int yy;
        for (int dy = 0; dy < height && (yy = y + dy) < y + height; ++dy) {
            int r;
            Material m = rng.nextDouble() < 0.15 ? Material.BLUE_ICE : Material.PACKED_ICE;
            data.setBlock(lx, yy, lz, m);
            int n = dy < 2 ? radius + 1 : (dy < height / 3 ? radius : (r = dy > height * 2 / 3 ? 0 : radius - 1));
            if (r < 0) {
                r = 0;
            }
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    if (dx == 0 && dz == 0 || Math.abs(dx) + Math.abs(dz) > r) continue;
                    int xx = lx + dx;
                    int zz = lz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || !(rng.nextDouble() < 0.75)) continue;
                    data.setBlock(xx, yy, zz, Material.PACKED_ICE);
                }
            }
        }
        data.setBlock(lx, y + height, lz, Material.BLUE_ICE);
    }

    private void placeSpruceSimple(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height) {
        String gn;
        Material ground = data.getType(lx, Math.max(0, y - 1), lz);
        String string = gn = ground != null ? ground.name() : "";
        if (gn.endsWith("_LEAVES") || gn.endsWith("_LOG") || gn.contains("LEAVES") || gn.contains("LOG")) {
            return;
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
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15 || data.getType(xx, yy, zz) != Material.AIR) continue;
                    data.setBlock(xx, yy, zz, Material.SPRUCE_LEAVES);
                }
            }
        }
        if (top + 1 < y + height + 8) {
            data.setBlock(lx, top + 1, lz, Material.SPRUCE_LEAVES);
        }
    }

    private int findTopSolidY(ChunkGenerator.ChunkData data, World world, int lx, int lz, int yMin, int seaLevel) {
        int yStart;
        for (int y = yStart = Math.min(seaLevel, Math.min(world.getMaxHeight() - 1, 320)); y >= yMin; --y) {
            Material m = data.getType(lx, y, lz);
            if (m == Material.AIR || m == Material.WATER || m == Material.KELP || m == Material.KELP_PLANT || m == Material.SEAGRASS) continue;
            return y;
        }
        return yMin - 1;
    }

    private double centralMask(long seed, int wx, int wz) {
        double dx = wx - 0;
        double dz = wz - 0;
        double r = Math.sqrt(dx * dx + dz * dz);
        double jitter = (ConsegrityChunkGenerator.valueNoise2(seed ^ 0xC3A7A11BL, (double)wx / 90.0, (double)wz / 90.0) - 0.5) * 2.0 * 20.0;
        double edge = r - (50.0 + jitter);
        double m = 1.0 - ConsegrityChunkGenerator.clamp01((edge + 22.5) / 45.0);
        return ConsegrityChunkGenerator.clamp01(m);
    }

    private double ringMask(long seed, int wx, int wz, double innerR, double outerR, double feather) {
        double dx = wx - 0;
        double dz = wz - 0;
        double r = Math.sqrt(dx * dx + dz * dz);
        double jitter = (ConsegrityChunkGenerator.valueNoise2(seed ^ 0xA1B6A1B6L, (double)wx / 120.0, (double)wz / 120.0) - 0.5) * 2.0 * 18.0;
        double inner = innerR + jitter;
        double outer = outerR + jitter;
        double a = ConsegrityChunkGenerator.clamp01((r - (inner - feather * 0.5)) / feather);
        double b = 1.0 - ConsegrityChunkGenerator.clamp01((r - (outer - feather * 0.5)) / feather);
        return ConsegrityChunkGenerator.clamp01(Math.min(a, b));
    }

    private int centralSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xC3A1A15AL, (double)wx / 180.0, (double)wz / 180.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xC3A1A15BL, (double)wx / 64.0, (double)wz / 64.0);
        double h = (h1 * 0.7 + h2 * 0.3) * 2.0 - 1.0;
        int base = 159;
        int amp = 9;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring1DesertSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xD3512E57L, (double)wx / 240.0, (double)wz / 240.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xD3512E58L, (double)wx / 80.0, (double)wz / 80.0);
        double h3 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xD3512E59L, (double)wx / 34.0, (double)wz / 34.0);
        double h = (h1 * 0.52 + h2 * 0.32 + h3 * 0.16) * 2.0 - 1.0;
        int base = 163;
        int amp = 18;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring1SavannahSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A7A7A11L, (double)wx / 260.0, (double)wz / 260.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A7A7A12L, (double)wx / 90.0, (double)wz / 90.0);
        double h = (h1 * 0.65 + h2 * 0.35) * 2.0 - 1.0;
        int base = 165;
        int amp = 12;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring1SwampSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x51A7A11BL, (double)wx / 280.0, (double)wz / 280.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x51A7A11CL, (double)wx / 95.0, (double)wz / 95.0);
        double h = (h1 * 0.7 + h2 * 0.3) * 2.0 - 1.0;
        int base = 161;
        int amp = 8;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring2JungleSurfaceY(long seed, int wx, int wz) {
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x1B6C1E11L, (double)wx / 220.0, (double)wz / 220.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x1B6C1E12L, (double)wx / 70.0, (double)wz / 70.0);
        double h = (h1 * 0.55 + h2 * 0.45) * 2.0 - 1.0;
        int base = 169;
        int amp = 16;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring2MesaSurfaceY(long seed, int wx, int wz) {
        // Smoother, flatter mesas with gentle plateaus (savanna-like amplitude)
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADA11D5L, (double)wx / 320.0, (double)wz / 320.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADA11D6L, (double)wx / 120.0, (double)wz / 120.0);
        double raw = (h1 * 0.7 + h2 * 0.3) * 2.0 - 1.0;
        // Plateau shaping: compress the upper range to encourage flat tops
        double u = (raw + 1.0) * 0.5; // [0,1]
        double tStart = 0.65; // start flattening the upper 35%
        if (u > tStart) {
            double t = (u - tStart) / (1.0 - tStart);
            // Strong compression near the top; map 35% of the range into ~6%
            u = tStart + t * 0.06;
        }
        double h = u * 2.0 - 1.0;
        int base = 175;
        int amp = 8; // savanna-level low amplitude
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring2MountainSurfaceY(long seed, int wx, int wz) {
        int worldMaxY = 320;
        double base = 155.0;
        double low = ConsegrityChunkGenerator.fbm2(seed ^ 0xA1B2C3D4L, (double)wx * 0.003, (double)wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = 0.0;
        double t1 = 0.38;
        double mask = ConsegrityChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
        double cellSize = 144.0;
        int nx = (int)Math.round((double)wx / 144.0);
        int nz = (int)Math.round((double)wz / 144.0);
        double jx = (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, nx, 51L, nz, 10601445L)) - 0.5) * 50.4;
        double jz = (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, nx, 87L, nz, 11719926L)) - 0.5) * 50.4;
        double cx = (double)nx * 144.0 + jx;
        double cz = (double)nz * 144.0 + jz;
        double ddx = (double)wx - cx;
        double ddz = (double)wz - cz;
        double d = Math.sqrt(ddx * ddx + ddz * ddz);
        double R = 72.0;
        double w = ConsegrityChunkGenerator.clamp01(1.0 - d / R);
        double evenBoost = (w - 0.5) * 0.24;
        mask = ConsegrityChunkGenerator.clamp01(mask + evenBoost);
        double field = 0.0;
        field += this.peakSum(seed, wx, wz, 176.0, 70.0, 64.0, 2.4, 2576L);
        field += this.peakSum(seed, wx, wz, 112.0, 46.0, 44.0, 2.1, 2832L);
        field += this.peakSum(seed, wx, wz, 72.0, 30.0, 28.0, 1.9, 3088L);
        field += this.peakSum(seed, wx, wz, 48.0, 20.0, 16.0, 1.75, 3344L);
        double edge = ConsegrityChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));
        double edgeTaper = edge * edge * (3.0 - 2.0 * edge);
        field *= mask * edgeTaper;
        double ridge = ConsegrityChunkGenerator.fbm2(seed ^ 0xB3C4D5E6L, (double)wx * 0.02, (double)wz * 0.02);
        double ridgeAdd = (ridge - 0.5) * 2.0 * 20.0 * (mask * edgeTaper);
        double plains = (ConsegrityChunkGenerator.fbm2(seed ^ 0x600DD00EL, (double)wx * 0.02, (double)wz * 0.02) - 0.5) * 2.0 * 4.8;
        double between = (ConsegrityChunkGenerator.fbm2(seed ^ 0x77EEDD11L, (double)wx * 0.01, (double)wz * 0.01) - 0.5) * 2.0;
        double betweenAmp = 18.0 * (1.0 - mask * 0.85);
        double micro = (ConsegrityChunkGenerator.fbm2(seed ^ 0x33AA55CCL, (double)wx * 0.06, (double)wz * 0.06) - 0.5) * 2.0 * 2.6;
        double h = base + plains + between * betweenAmp + micro + ridgeAdd + field;
        double erosion = ConsegrityChunkGenerator.fbm2(seed ^ 0xE01234L, (double)wx * 0.004, (double)wz * 0.004);
        h += erosion * erosion * 70.0 * (mask * edgeTaper);
        double cellVar = 0.85 + ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, nx, 99L, nz, -267531522L)) * 0.7;
        if ((h = base + (h - base) * (1.35 * cellVar)) > (double)worldMaxY) {
            h = worldMaxY;
        }
        // Plateau shaping for mountain tops: flatten peaks into mesas rather than sharp points
        double plateauStart = 200.0;
        if (h > plateauStart) {
            double excess = h - plateauStart;
            double compressed = Math.min(24.0, excess * 0.2);
            h = plateauStart + compressed;
        }
        if (h < 150.0) {
            h = 150.0;
        }
        return (int)Math.floor(h);
    }

    private double peakSum(long seed, int wx, int wz, double cell, double baseRadius, double baseAmp, double power, long salt) {
        int ix = (int)Math.floor((double)wx / cell);
        int iz = (int)Math.floor((double)wz / cell);
        double sum = 0.0;
        for (int cx = ix - 1; cx <= ix + 1; ++cx) {
            for (int cz = iz - 1; cz <= iz + 1; ++cz) {
                double jx = (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, cx, 51L, cz, salt ^ 1L)) - 0.5) * (cell * 0.8);
                double jz = (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, cx, 87L, cz, salt ^ 2L)) - 0.5) * (cell * 0.8);
                double px = (double)cx * cell + jx;
                double pz = (double)cz * cell + jz;
                double dx = (double)wx - px;
                double dz = (double)wz - pz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double rScale = 0.7 + ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, cx, 21L, cz, salt ^ 3L)) * 1.0;
                double radius = baseRadius * rScale;
                if (radius < 6.0) {
                    radius = 6.0;
                }
                if (dist >= radius) continue;
                double aScale = 0.6 + ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, cx, 11L, cz, salt ^ 4L)) * 1.2;
                double amp = baseAmp * aScale;
                double t = 1.0 - dist / radius;
                double w = Math.pow(t, power);
                sum += amp * w;
            }
        }
        return sum;
    }

    private int oceanFloorY(long seed, int wx, int wz) {
        double n1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xA1B2C3D4L, (double)wx / 260.0, (double)wz / 260.0);
        double n2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5EEDBEEFL, (double)wx / 110.0, (double)wz / 110.0);
        double n3 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x13579BDFL, (double)wx / 40.0, (double)wz / 40.0);
        double h = n1 * 0.55 + n2 * 0.3 + n3 * 0.15;
        h = h * 2.0 - 1.0;
        int base = 116;
        int amplitude = 26;
        int floor = base + (int)Math.round(h * (double)amplitude);
        double trench = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xDEADC0DEL, (double)wx / 600.0, (double)wz / 600.0);
        if (trench > 0.75) {
            floor -= 8 + (int)Math.round((trench - 0.75) * 16.0);
        }
        if (floor < 80) {
            floor = 80;
        }
        if (floor > 147) {
            floor = 147;
        }
        return floor;
    }

    private void paintDesertCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        int y3;
        int y4;
        int y2 = topY - 1;
        int y1 = Math.max(-60, topY - 5);
        if (y2 > y1) {
            data.setRegion(lx, y1, lz, lx + 1, y2, lz + 1, Material.SANDSTONE);
        }
        if ((y4 = topY + 1) > (y3 = Math.max(-60, topY - 1))) {
            data.setRegion(lx, y3, lz, lx + 1, y4, lz + 1, Material.SAND);
        }
    }

    private void paintDesertLandBlendCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY, double tLand) {
        int i;
        double t = ConsegrityChunkGenerator.clamp01(tLand);
        int sandTop = (int)Math.round(ConsegrityChunkGenerator.lerp(2.0, 0.0, t));
        int sandstone = (int)Math.round(ConsegrityChunkGenerator.lerp(4.0, 0.0, t));
        int dirt = (int)Math.round(ConsegrityChunkGenerator.lerp(0.0, 3.0, t));
        boolean grassTop = t > 0.15;
        int total = sandTop + sandstone + dirt + 1;
        int y = topY - total + 1;
        y = Math.max(-60, y);
        for (i = 0; i < sandstone; ++i) {
            data.setBlock(lx, y++, lz, Material.SANDSTONE);
        }
        for (i = 0; i < dirt; ++i) {
            data.setBlock(lx, y++, lz, Material.DIRT);
        }
        for (i = 0; i < sandTop; ++i) {
            data.setBlock(lx, y++, lz, Material.SAND);
        }
        data.setBlock(lx, topY, lz, grassTop ? Material.GRASS_BLOCK : Material.SAND);
    }

    private void paintMesaStrata(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        int start;
        for (int y = start = Math.max(-60, topY - 12); y <= topY; ++y) {
            Material m;
            int band = Math.floorMod(y, 7);
            switch (band) {
                case 0: {
                    Material material = Material.TERRACOTTA;
                    break;
                }
                case 1: {
                    Material material = Material.RED_TERRACOTTA;
                    break;
                }
                case 2: {
                    Material material = Material.ORANGE_TERRACOTTA;
                    break;
                }
                case 3: {
                    Material material = Material.BROWN_TERRACOTTA;
                    break;
                }
                case 4: {
                    Material material = Material.YELLOW_TERRACOTTA;
                    break;
                }
                case 5: {
                    Material material = Material.WHITE_TERRACOTTA;
                    break;
                }
                default: {
                    Material material = m = Material.LIGHT_GRAY_TERRACOTTA;
                }
            }
            // Avoid red sand caps on flats near mountain bases: use terracotta at lower tops
            if (y == topY) {
                m = topY <= 180 ? Material.ORANGE_TERRACOTTA : Material.RED_SAND;
            }
            data.setBlock(lx, y, lz, m);
        }
    }

    private void paintMesaLandBlendCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY, double tLand) {
        int i;
        double t = ConsegrityChunkGenerator.clamp01(tLand);
        int terr = (int)Math.round(ConsegrityChunkGenerator.lerp(10.0, 0.0, t));
        int dirt = (int)Math.round(ConsegrityChunkGenerator.lerp(0.0, 3.0, t));
        // Always favor grass on the blend to avoid patchy sand at mountain bases
        boolean grassTop = true;
        int total = terr + dirt + 1;
        int y = Math.max(-60, topY - total + 1);
        for (i = 0; i < terr; ++i) {
            int band = Math.floorMod(y + i, 7);
            Material m = switch (band) {
                case 0 -> Material.TERRACOTTA;
                case 1 -> Material.RED_TERRACOTTA;
                case 2 -> Material.ORANGE_TERRACOTTA;
                case 3 -> Material.BROWN_TERRACOTTA;
                case 4 -> Material.YELLOW_TERRACOTTA;
                case 5 -> Material.WHITE_TERRACOTTA;
                default -> Material.LIGHT_GRAY_TERRACOTTA;
            };
            data.setBlock(lx, y + i, lz, m);
        }
        y += terr;
        for (i = 0; i < dirt; ++i) {
            data.setBlock(lx, y++, lz, Material.DIRT);
        }
        data.setBlock(lx, topY, lz, grassTop ? Material.GRASS_BLOCK : Material.RED_SAND);
    }

    private void placeCentralTrees(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, double[][] centralMaskGrid) {
        Random rng = new Random(ConsegrityChunkGenerator.hash(seed, chunkX, 123L, chunkZ, 466661L));
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                boolean isHill;
                int y;
                if (centralMaskGrid[lx][lz] < 0.6 || (y = topYGrid[lx][lz]) <= 159) continue;
                int h = y;
                int countLower = 0;
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        int nh;
                        if (dx == 0 && dz == 0) continue;
                        int nx = lx + dx;
                        int nz = lz + dz;
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || (nh = topYGrid[nx][nz]) >= h) continue;
                        ++countLower;
                    }
                }
                boolean bl = isHill = countLower >= 4;
                if (!isHill || rng.nextDouble() > 0.08) continue;
                Material wood = rng.nextDouble() < 0.9 ? Material.OAK_LOG : Material.BIRCH_LOG;
                Material leaves = wood == Material.OAK_LOG ? Material.OAK_LEAVES : Material.BIRCH_LEAVES;
                this.growSimpleTree(data, lx, h + 1, lz, wood, leaves, rng);
            }
        }
    }

    private void growSimpleTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, Material log, Material leaves, Random rng) {
        int height = 4 + rng.nextInt(3);
        for (int i = 0; i < height; ++i) {
            data.setBlock(lx, y + i, lz, log);
        }
        int cy = y + height - 1;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                int r2;
                int x = lx + dx;
                int z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15 || (r2 = dx * dx + dz * dz) > 6) continue;
                data.setBlock(x, cy, z, leaves);
                if (r2 > 3) continue;
                data.setBlock(x, cy + 1, z, leaves);
            }
        }
    }

    @Override
    protected void placeOres(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        ConsegrityRegions.Region[][] tmp = new ConsegrityRegions.Region[16][16];
        for (int ax = 0; ax < 16; ++ax) {
            for (int az = 0; az < 16; ++az) {
                int wx = (chunkX << 4) + ax;
                int wz = (chunkZ << 4) + az;
                tmp[ax][az] = ConsegrityRegions.regionAt(world, wx, wz);
            }
        }
        this.placeOresByRegion(world, data, seed, chunkX, chunkZ, tmp);
    }

    private void placeOresByRegion(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 3729846285L);
        for (int i = 0; i < 72; ++i) {
            int W_LAPIS_R;
            int W_RED_R;
            int W_GOLD_R;
            int W_IRON_R;
            int W_COAL_R;
            int ax = rng.nextInt(15);
            int az = rng.nextInt(15);
            ConsegrityRegions.Region region = regionGrid[ax][az];
            int W_COPPER_R = 0;
            int W_EMERALD_R = 0;
            int W_DIAMOND_R = 1;
            boolean deepDesert = false;
            switch (region) {
                case SWAMP: {
                    W_COPPER_R = 12;
                    W_COAL_R = 40;
                    W_IRON_R = 8;
                    W_GOLD_R = 8;
                    W_RED_R = 8;
                    W_LAPIS_R = 8;
                    W_DIAMOND_R = 2;
                    W_EMERALD_R = 4;
                    break;
                }
                case ICE_SPIKES: {
                    W_COAL_R = 8;
                    W_IRON_R = 8;
                    W_GOLD_R = 8;
                    W_RED_R = 8;
                    W_LAPIS_R = 12;
                    W_DIAMOND_R = 2;
                    break;
                }
                case CHERRY: {
                    W_COAL_R = 40;
                    W_IRON_R = 20;
                    W_GOLD_R = 10;
                    W_RED_R = 8;
                    W_LAPIS_R = 6;
                    W_DIAMOND_R = 2;
                    break;
                }
                case DESERT: {
                    W_COPPER_R = 0;
                    W_COAL_R = 40;
                    W_IRON_R = 8;
                    W_GOLD_R = 8;
                    W_RED_R = 8;
                    W_LAPIS_R = 8;
                    W_DIAMOND_R = 2;
                    W_EMERALD_R = 4;
                    break;
                }
                case MESA: {
                    W_COPPER_R = 0;
                    W_COAL_R = 16;
                    W_IRON_R = 12;
                    W_GOLD_R = 14;
                    W_RED_R = 8;
                    W_LAPIS_R = 6;
                    W_DIAMOND_R = 2;
                    break;
                }
                case MOUNTAIN: {
                    W_COPPER_R = 0;
                    W_COAL_R = 16;
                    W_IRON_R = 16;
                    W_GOLD_R = 8;
                    W_RED_R = 8;
                    W_LAPIS_R = 8;
                    W_DIAMOND_R = 4;
                    W_EMERALD_R = 12;
                    break;
                }
                default: {
                    W_COAL_R = 40;
                    W_IRON_R = 20;
                    W_GOLD_R = 10;
                    W_RED_R = 8;
                    W_LAPIS_R = 6;
                    W_COPPER_R = 0;
                    W_EMERALD_R = 0;
                    W_DIAMOND_R = 2;
                }
            }
            if (W_COPPER_R > 0) {
                this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.COPPER_ORE, W_COPPER_R, Math.max(-32, world.getMinHeight()), 150, Bias.UNIFORM, 0, false);
            }
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.COAL_ORE, W_COAL_R, Math.max(-64, world.getMinHeight()), 150, Bias.TOP, 0, false);
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.IRON_ORE, W_IRON_R, Math.max(-64, world.getMinHeight()), 150, Bias.UNIFORM, 0, false);
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.GOLD_ORE, W_GOLD_R, Math.max(-64, world.getMinHeight()), 150, Bias.TRIANGULAR, -50, false);
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.LAPIS_ORE, W_LAPIS_R, Math.max(-64, world.getMinHeight()), 150, Bias.BOTTOM, 0, false);
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.REDSTONE_ORE, W_RED_R, Math.max(-64, world.getMinHeight()), 150, Bias.BOTTOM, 0, false);
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.DIAMOND_ORE, W_DIAMOND_R, Math.max(-64, world.getMinHeight()), -10, Bias.BOTTOM, 0, false);
            if (W_EMERALD_R > 0) {
                this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.EMERALD_ORE, W_EMERALD_R, Math.max(-64, world.getMinHeight()), 150, Bias.UNIFORM, 0, false);
            }
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.OBSIDIAN, 1, Math.max(-64, world.getMinHeight()), -40, Bias.BOTTOM, 0, true);
        }
    }

    private void oreAttemptsLocal(World world, ChunkGenerator.ChunkData data, SplittableRandom rng, int chunkX, int chunkZ, Material ore, int weight, int minY, int maxY, Bias bias, int peakY, boolean isObsidian) {
        int yMax;
        int yMin = Math.max(Math.max(-192, minY), world.getMinHeight());
        if (yMin > (yMax = Math.min(Math.min(320, maxY), world.getMaxHeight() - 1))) {
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
            int ay = this.biasedYLocal(rng, yMin, yMax - 1, bias, peakY);
            SplittableRandom local = ConsegrityChunkGenerator.rngFor(world.getSeed(), (chunkX << 4) + ax, (chunkZ << 4) + az, ore.ordinal() * 40503 + c);
            double p = isObsidian ? 0.05 : 0.2;
            int mask = 0;
            for (i = 0; i < 8; ++i) {
                if (!(local.nextDouble() < p)) continue;
                mask |= 1 << i;
            }
            if (Integer.bitCount(mask) < 3) {
                mask |= 1 << local.nextInt(8);
            }
            for (i = 0; i < 8; ++i) {
                if ((mask & 1 << i) == 0) continue;
                int lx = ax + OFFS[i * 3];
                int ly = ay + OFFS[i * 3 + 1];
                int lz = az + OFFS[i * 3 + 2];
                if (ly < yMin || ly > yMax || data.getType(lx, ly, lz) != Material.STONE) continue;
                data.setBlock(lx, ly, lz, ore);
            }
        }
    }

    private int biasedYLocal(SplittableRandom rng, int yMin, int yMax, Bias bias, int peakY) {
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
                    w = this.triangularWeightLocal(y, yMin, yMax, yMax);
                    break;
                }
                case 1: {
                    w = this.triangularWeightLocal(y, yMin, yMax, yMin);
                    break;
                }
                default: {
                    w = this.triangularWeightLocal(y, yMin, yMax, peakY);
                }
            }
            if (!(rng.nextDouble() < w)) continue;
            return y;
        }
        return yMin + rng.nextInt(yMax - yMin + 1);
    }

    private double triangularWeightLocal(int y, int yMin, int yMax, int peakY) {
        if (yMax == yMin) {
            return 1.0;
        }
        double range = yMax - yMin;
        double d = 1.0 - Math.min(1.0, (double)Math.abs(y - peakY) / (range * 0.5));
        return Math.max(0.05, d);
    }

    private void carveSphere(ChunkGenerator.ChunkData data, int chunkBaseX, int chunkBaseZ, double cx, int cy, double cz, double r, int yMin, int yMax) {
        int lx0 = (int)Math.floor(cx) - chunkBaseX;
        int lz0 = (int)Math.floor(cz) - chunkBaseZ;
        int ir = (int)Math.ceil(r);
        for (int dx = -ir; dx <= ir; ++dx) {
            int lx = lx0 + dx;
            if (lx < 0 || lx > 15) continue;
            for (int dz = -ir; dz <= ir; ++dz) {
                int lz = lz0 + dz;
                if (lz < 0 || lz > 15) continue;
                for (int dy = -ir; dy <= ir; ++dy) {
                    double d;
                    int y = cy + dy;
                    if (y < yMin || y > yMax || (d = (double)(dx * dx + dy * dy + dz * dz)) > r * r) continue;
                    data.setBlock(lx, y, lz, Material.AIR);
                }
            }
        }
    }

    private void paintLandCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        int y2 = topY;
        int y1 = Math.max(-60, topY - 3);
        if (y2 > y1) {
            data.setRegion(lx, y1, lz, lx + 1, y2, lz + 1, Material.DIRT);
        }
        data.setBlock(lx, topY, lz, Material.GRASS_BLOCK);
    }

    private void paintBeachCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        int y2 = topY + 1;
        int y1 = Math.max(-60, topY - 4);
        if (y2 > y1) {
            data.setRegion(lx, y1, lz, lx + 1, y2, lz + 1, Material.SAND);
        }
    }

    private void paintSeafloorCap(ChunkGenerator.ChunkData data, int lx, int lz, int floorY, int seaLevel) {
        int depth = seaLevel - floorY;
        for (int y = floorY - 3; y <= floorY - 1; ++y) {
            if (y < -60) continue;
            Material m = (ConsegrityChunkGenerator.hash(depth, lx, y, lz, 17L) & 3L) == 0L ? Material.CLAY : Material.GRAVEL;
            data.setBlock(lx, y, lz, m);
        }
        double pSand = depth < 12 ? 0.85 : (depth < 28 ? 0.55 : 0.25);
        Material top = ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(depth, lx, floorY, lz, 34L)) < pSand ? Material.SAND : Material.GRAVEL;
        data.setBlock(lx, floorY, lz, top);
    }

    private void paintMountainCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        if (topY <= 180) {
            this.paintLandCap(data, lx, lz, topY);
            return;
        }
        int yEndExclusive = topY + 1;
        int yStart = Math.max(181, topY - 3);
        if (yEndExclusive > yStart) {
            data.setRegion(lx, yStart, lz, lx + 1, yEndExclusive, lz + 1, Material.STONE);
        }
        if (topY >= 200) {
            data.setBlock(lx, topY, lz, Material.SNOW_BLOCK);
        } else {
            data.setBlock(lx, topY, lz, Material.STONE);
        }
    }

    private void addSeaVegetation(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int lx, int lz, int floorY) {
        int wx = (chunkX << 4) + lx;
        int wz = (chunkZ << 4) + lz;
        int depth = 153 - floorY;
        if (depth < 3) {
            return;
        }
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, wx ^ floorY, wz, 51L);
        if (depth <= 20) {
            if (rng.nextDouble() < 0.18) {
                data.setBlock(lx, floorY + 1, lz, Material.SEAGRASS);
            }
        } else if (rng.nextDouble() < 0.08) {
            data.setBlock(lx, floorY + 1, lz, Material.SEAGRASS);
        }
        if (depth >= 6 && rng.nextDouble() < Math.min(0.22, 0.02 + (double)depth * 0.005)) {
            int y;
            int maxHeight = Math.min(depth - 1, 12);
            int h = 2 + rng.nextInt(Math.max(1, maxHeight));
            for (int i = 1; i <= h && (y = floorY + i) < 153; ++i) {
                data.setBlock(lx, y, lz, i == h ? Material.KELP : Material.KELP_PLANT);
            }
        }
    }

    private void placeBedrockBand(World world, ChunkGenerator.ChunkData data, int chunkX, int chunkZ) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(world.getSeed(), chunkX, chunkZ, 68L);
        int base = world.getMinHeight();
        for (int lx = 0; lx < 16; ++lx) {
            for (int lz = 0; lz < 16; ++lz) {
                int thickness = 1 + rng.nextInt(5);
                for (int y = base; y < base + thickness; ++y) {
                    data.setBlock(lx, y, lz, Material.BEDROCK);
                }
            }
        }
    }

    private void placeFrozenOceanIcebergs(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] floorYGrid, boolean[][] frozenOcean) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 30323687L);
        boolean anyFrozen = false;
        block0: for (int x = 0; x < 16 && !anyFrozen; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (!frozenOcean[x][z]) continue;
                anyFrozen = true;
                continue block0;
            }
        }
        if (!anyFrozen) {
            return;
        }
        int centers = 3 + rng.nextInt(4);
        for (int i = 0; i < centers; ++i) {
            int cz;
            int cx = 2 + rng.nextInt(12);
            if (!frozenOcean[cx][cz = 2 + rng.nextInt(12)]) continue;
            int floor = floorYGrid[cx][cz];
            int sea = 153;
            int r = 3 + rng.nextInt(4);
            int h = 6 + rng.nextInt(8);
            for (int dx = -r - 1; dx <= r + 1; ++dx) {
                for (int dz = -r - 1; dz <= r + 1; ++dz) {
                    int base;
                    double d;
                    int x = cx + dx;
                    int z = cz + dz;
                    if (x < 0 || x > 15 || z < 0 || z > 15 || !frozenOcean[x][z] || (d = Math.hypot(dx, dz)) > (double)r + 0.8) continue;
                    double t = Math.max(0.0, 1.0 - d / ((double)r + 0.8));
                    int peak = sea + (int)Math.round((double)h * t * (0.85 + rng.nextDouble() * 0.2));
                    for (int y = base = Math.max(floor + 1, sea - (1 + rng.nextInt(3))); y <= peak; ++y) {
                        Material m = rng.nextDouble() < 0.2 ? Material.BLUE_ICE : Material.PACKED_ICE;
                        data.setBlock(x, y, z, m);
                    }
                }
            }
        }
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

    private static double rand01(long h) {
        long v = h >>> 11 & 0x1FFFFFFFFFFFFFL;
        return (double)v / 9.007199254740992E15;
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

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static SplittableRandom rngFor(long seed, long x, long z, long salt) {
        long mixed = ConsegrityChunkGenerator.hash(seed, x, 0L, z, salt);
        return new SplittableRandom(mixed);
    }

    private static double valueAt(long seed, int xi, int yi, int zi) {
        long h = ConsegrityChunkGenerator.hash(seed, xi, yi, zi, 305419896L);
        long v = h >>> 11 & 0x1FFFFFFFFFFFFFL;
        return (double)v / 9.007199254740992E15;
    }

    private static double valueNoise2(long seed, double x, double y) {
        int x0 = (int)Math.floor(x);
        int y0 = (int)Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        double tx = ConsegrityChunkGenerator.fade(x - (double)x0);
        double ty = ConsegrityChunkGenerator.fade(y - (double)y0);
        double c00 = ConsegrityChunkGenerator.valueAt(seed, x0, y0, 0);
        double c10 = ConsegrityChunkGenerator.valueAt(seed, x1, y0, 0);
        double c01 = ConsegrityChunkGenerator.valueAt(seed, x0, y1, 0);
        double c11 = ConsegrityChunkGenerator.valueAt(seed, x1, y1, 0);
        double x0v = ConsegrityChunkGenerator.lerp(c00, c10, tx);
        double x1v = ConsegrityChunkGenerator.lerp(c01, c11, tx);
        return ConsegrityChunkGenerator.lerp(x0v, x1v, ty);
    }

    private static double fbm2(long seed, double x, double z) {
        double sum = 0.0;
        double amp = 1.0;
        double freq = 1.0;
        for (int o = 0; o < 5; ++o) {
            sum += (ConsegrityChunkGenerator.valueNoise2(seed, x * freq, z * freq) - 0.5) * 2.0 * amp;
            amp *= 0.55;
            freq *= 1.9;
        }
        return ConsegrityChunkGenerator.clamp01(sum * 0.5 + 0.5);
    }

    private static double wrap01(double u) {
        return u - Math.floor(u);
    }

    private static double smooth01(double x) {
        return x * x * (3.0 - 2.0 * x);
    }

    private static double angle01(int wx, int wz) {
        double ang = Math.atan2(wz - 0, wx - 0);
        return (ang + Math.PI) / (Math.PI * 2);
    }

    private double angle01Warped(long seed, int wx, int wz) {
        double ang = Math.atan2(wz - 0, wx - 0);
        double n1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xA11C1EEDL, (double)wx / 180.0, (double)wz / 180.0);
        double n2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xB22C2EEDL, (double)wx / 64.0, (double)wz / 64.0);
        double jitter = (n1 * 0.65 + n2 * 0.35 - 0.5) * 2.0 * 0.35;
        return ((ang += jitter * 0.3490658503988659) + Math.PI) / (Math.PI * 2);
    }

    private static int mod(int a, int m) {
        int r = a % m;
        return r < 0 ? r + m : r;
    }

    private static double[] sectorWeightsEqual(double u, int sectors, double blendFrac) {
        double b;
        double[] w = new double[sectors];
        double v = u * (double)sectors;
        int k = (int)Math.floor(v);
        double t = v - (double)k;
        if (t < (b = Math.max(1.0E-4, blendFrac))) {
            double s = ConsegrityChunkGenerator.smooth01(t / b);
            w[ConsegrityChunkGenerator.mod((int)(k - 1), (int)sectors)] = 1.0 - s;
            w[k] = s;
        } else if (t > 1.0 - b) {
            double s = ConsegrityChunkGenerator.smooth01((t - (1.0 - b)) / b);
            w[k] = 1.0 - s;
            w[ConsegrityChunkGenerator.mod((int)(k + 1), (int)sectors)] = s;
        } else {
            w[k] = 1.0;
        }
        return w;
    }

    private static int argmax3(double a, double b, double c) {
        return a >= b && a >= c ? 0 : (b >= c ? 1 : 2);
    }

    private static Blend arcBlendFromSplits(double u, double[] splits, double ringRadius) {
        int n = splits.length - 1;
        if (n <= 0) {
            return new Blend(0, 0, 0.0);
        }
        double uu = ConsegrityChunkGenerator.wrap01(u);
        int i = n - 1;
        for (int k = 0; k < n; ++k) {
            if (!(uu >= splits[k]) || !(uu < splits[k + 1])) continue;
            i = k;
            break;
        }
        double a0 = splits[i];
        double a1 = splits[i + 1];
        double b = 15.0 / (Math.PI * 2 * ringRadius);
        if (uu > a0 - b && uu < a0 + b) {
            int prev = (i - 1 + n) % n;
            double t = ConsegrityChunkGenerator.smooth01((uu - (a0 - b)) / (2.0 * b));
            return new Blend(i, prev, t);
        }
        return new Blend(i, i, 0.0);
    }

    private static int arcIndex(double u, double[] splits) {
        int n = splits.length - 1;
        if (n <= 0) {
            return 0;
        }
        double uu = ConsegrityChunkGenerator.wrap01(u);
        for (int i = 0; i < n; ++i) {
            if (!(uu >= splits[i]) || !(uu < splits[i + 1])) continue;
            return i;
        }
        return n - 1;
    }

    private static Blend angularTwoWay(double u, int sectors, double blendFrac) {
        double b;
        double v = u * (double)sectors;
        int k = (int)Math.floor(v);
        double t = v - (double)k;
        if (t < (b = Math.max(1.0E-6, Math.min(0.49, blendFrac)))) {
            double s = ConsegrityChunkGenerator.smooth01(t / b);
            return new Blend(ConsegrityChunkGenerator.mod(k - 1, sectors), k, s);
        }
        if (t > 1.0 - b) {
            double s = ConsegrityChunkGenerator.smooth01((t - (1.0 - b)) / b);
            return new Blend(k, ConsegrityChunkGenerator.mod(k + 1, sectors), s);
        }
        return new Blend(k, k, 0.0);
    }

    private static double ringCoreEnable(RingEdges R, double feather) {
        double start = R.inner + 0.5 * feather;
        double end = R.outer - 0.5 * feather;
        double t = ConsegrityChunkGenerator.clamp01((R.r - start) / Math.max(1.0, end - start));
        return ConsegrityChunkGenerator.smooth01(t);
    }

    private static void freezeNearEdges(double[] w, double enable) {
        int i;
        int k = 0;
        for (i = 1; i < w.length; ++i) {
            if (!(w[i] > w[k])) continue;
            k = i;
        }
        for (i = 0; i < w.length; ++i) {
            double hard = i == k ? 1.0 : 0.0;
            w[i] = hard + (w[i] - hard) * enable;
        }
        double s = 0.0;
        for (double v : w) {
            s += v;
        }
        if (s <= 1.0E-9) {
            return;
        }
        int i2 = 0;
        while (i2 < w.length) {
            int n = i2++;
            w[n] = w[n] / s;
        }
    }

    private static double angularStepPreU(double u, int wx, int wz, double splitFrac, double featherFrac, long seed, double jitterFrac) {
        double j1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x97E2D14BL, (double)wx / 260.0, (double)wz / 260.0);
        double j2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x97E2D14CL, (double)wx / 70.0, (double)wz / 70.0);
        double jitter = (0.7 * j1 + 0.3 * j2 - 0.5) * 2.0 * jitterFrac;
        double uJ = ConsegrityChunkGenerator.wrap01(u + jitter);
        double fnoise = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A1F0A3DL, (double)wx / 180.0, (double)wz / 180.0);
        double f = Math.max(1.0E-4, featherFrac * (0.85 + 0.3 * fnoise));
        double d = ConsegrityChunkGenerator.wrap01(uJ - splitFrac);
        double t = (d - 0.5 + f) / (2.0 * f);
        return ConsegrityChunkGenerator.smooth01(ConsegrityChunkGenerator.clamp01(t));
    }

    private static double angularStep(int wx, int wz, double splitFrac, double featherFrac, long seed, double jitterFrac) {
        double u = ConsegrityChunkGenerator.angle01(wx, wz);
        double j1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x97E2D14BL, (double)wx / 260.0, (double)wz / 260.0);
        double j2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x97E2D14CL, (double)wx / 70.0, (double)wz / 70.0);
        double jitter = (0.7 * j1 + 0.3 * j2 - 0.5) * 2.0 * jitterFrac;
        double uJ = ConsegrityChunkGenerator.wrap01(u + jitter);
        double fnoise = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A1F0A3DL, (double)wx / 180.0, (double)wz / 180.0);
        double f = Math.max(1.0E-4, featherFrac * (0.85 + 0.3 * fnoise));
        double d = ConsegrityChunkGenerator.wrap01(uJ - splitFrac);
        double t = (d - 0.5 + f) / (2.0 * f);
        return ConsegrityChunkGenerator.smooth01(ConsegrityChunkGenerator.clamp01(t));
    }

    private V2 warpXZ(long seed, int wx, int wz) {
        double n1x = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x6CE5C11AL, (double)wx / 340.0, (double)wz / 340.0);
        double n1z = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x6CE5C11BL, (double)wx / 340.0, (double)wz / 340.0);
        double n2x = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADC0FFEL, (double)wx / 120.0, (double)wz / 120.0);
        double n2z = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xFACEFEEDL, (double)wx / 120.0, (double)wz / 120.0);
        double dx = (n1x - 0.5) * 2.0 * 18.0 + (n2x - 0.5) * 2.0 * 7.0;
        double dz = (n1z - 0.5) * 2.0 * 18.0 + (n2z - 0.5) * 2.0 * 7.0;
        return new V2((double)wx + dx, (double)wz + dz);
    }

    private RingEdges ringEdges(long seed, int wx, int wz, double innerR, double outerR, double feather) {
        V2 p = this.warpXZ(seed ^ 0xA1B6A1B6L, wx, wz);
        double dx = p.x - 0.0;
        double dz = p.z - 0.0;
        double r = Math.sqrt(dx * dx + dz * dz);
        double jitter = (ConsegrityChunkGenerator.valueNoise2(seed ^ 0xB7E15163L, (double)wx / 220.0, (double)wz / 220.0) - 0.5) * 2.0 * 12.0;
        double inner = innerR + jitter;
        double outer = outerR + jitter;
        return new RingEdges(r, inner, outer, inner - 0.5 * feather - 2.0, outer + 0.5 * feather + 2.0);
    }

    private RingEdges centralEdges(long seed, int wx, int wz, double radius, double feather) {
        V2 p = this.warpXZ(seed ^ 0xC3A7A11BL, wx, wz);
        double dx = p.x - 0.0;
        double dz = p.z - 0.0;
        double r = Math.sqrt(dx * dx + dz * dz);
        double jitter = (ConsegrityChunkGenerator.valueNoise2(seed ^ 0xC3A7A11CL, (double)wx / 180.0, (double)wz / 180.0) - 0.5) * 2.0 * 12.0;
        double c = radius + jitter;
        return new RingEdges(r, c, c, c - 0.5 * feather - 2.0, c + 0.5 * feather + 2.0);
    }

    private double warpX(long seed, int wx, int wz, double amp) {
        double n1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x71EE5EEDL, (double)wx / 160.0, (double)wz / 160.0);
        double n2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x13579BDFL, (double)wx / 56.0, (double)wz / 56.0);
        return (n1 - 0.5) * 2.0 * amp + (n2 - 0.5) * 2.0 * (amp * 0.2);
    }

    private double warpZ(long seed, int wx, int wz, double amp) {
        double n1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x8BADF00DL, (double)wx / 150.0, (double)wz / 150.0);
        double n2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x2468ACE0L, (double)wx / 52.0, (double)wz / 52.0);
        return (n1 - 0.5) * 2.0 * amp + (n2 - 0.5) * 2.0 * (amp * 0.2);
    }

    private static double bandMask(double r, double a, double b, double feather) {
        double aa = ConsegrityChunkGenerator.clamp01((r - (a - feather * 0.5)) / feather);
        double bb = 1.0 - ConsegrityChunkGenerator.clamp01((r - (b - feather * 0.5)) / feather);
        return ConsegrityChunkGenerator.clamp01(Math.min(aa, bb));
    }

    private static int floorDiv(int a, int b) {
        int q = a / b;
        int r = a % b;
        return r < 0 ? q - 1 : q;
    }

    private boolean cellMayTouchChunk(int cx, int cz, int chunkX, int chunkZ) {
        double dx;
        int cellMinX = cx * 64;
        int cellMaxX = cellMinX + 64;
        int cellMinZ = cz * 64;
        int cellMaxZ = cellMinZ + 64;
        int chMinX = chunkX << 4;
        int chMaxX = chMinX + 16;
        int chMinZ = chunkZ << 4;
        int chMaxZ = chMinZ + 16;
        double d = cellMaxX < chMinX ? (double)(chMinX - cellMaxX) : (dx = cellMinX > chMaxX ? (double)(cellMinX - chMaxX) : 0.0);
        double dz = cellMaxZ < chMinZ ? (double)(chMinZ - cellMaxZ) : (cellMinZ > chMaxZ ? (double)(cellMinZ - chMaxZ) : 0.0);
        return Math.hypot(dx, dz) <= 222.60000000000002;
    }

    private void carveCavesWorldAnchored(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int yMin = Math.max(world.getMinHeight(), -192);
        int yMax = Math.min(world.getMaxHeight() - 1, 193);
        int yStop = Math.max(world.getMinHeight(), -180);
        int cellX0 = ConsegrityChunkGenerator.floorDiv(baseX, 64);
        int cellZ0 = ConsegrityChunkGenerator.floorDiv(baseZ, 64);
        for (int cx = cellX0 - CAVE_REACH; cx <= cellX0 + CAVE_REACH; ++cx) {
            for (int cz = cellZ0 - CAVE_REACH; cz <= cellZ0 + CAVE_REACH; ++cz) {
                if (!this.cellMayTouchChunk(cx, cz, chunkX, chunkZ)) continue;
                SplittableRandom rr = ConsegrityChunkGenerator.rngFor(seed, cx, cz, 3237997278L);
                int spawns = rr.nextInt(3) == 0 ? 0 : 1 + (rr.nextInt(4) == 0 ? 1 : 0);
                block2: for (int i = 0; i < spawns; ++i) {
                    double x = (double)cx * 64.0 + (double)rr.nextInt(64) + rr.nextDouble();
                    double z = (double)cz * 64.0 + (double)rr.nextInt(64) + rr.nextDouble();
                    int y = Math.min(yMax, 153 + rr.nextInt(-8, 12));
                    int steps = 120 + rr.nextInt(80);
                    double yaw = rr.nextDouble() * Math.PI * 2.0;
                    double pitch = -0.2 + (rr.nextDouble() - 0.5) * 0.2;
                    double radius = 1.6 + rr.nextDouble() * 1.0;
                    for (int s = 0; s < steps; ++s) {
                        yaw += (rr.nextDouble() - 0.5) * 0.4;
                        if ((pitch += (rr.nextDouble() - 0.5) * 0.2 - 0.035) < -0.9) {
                            pitch = -0.9;
                        }
                        if (pitch > 0.9) {
                            pitch = 0.9;
                        }
                        x += Math.cos(yaw) * 1.1;
                        z += Math.sin(yaw) * 1.1;
                        if ((y += (int)Math.round(Math.sin(pitch))) <= yStop) continue block2;
                        this.carveSphere(data, baseX, baseZ, x, y, z, radius, yStop, yMax);
                    }
                }
            }
        }
    }

    private static final class RingEdges {
        final double r;
        final double inner;
        final double outer;
        final double innerGate;
        final double outerGate;

        RingEdges(double r, double inner, double outer, double innerGate, double outerGate) {
            this.r = r;
            this.inner = inner;
            this.outer = outer;
            this.innerGate = innerGate;
            this.outerGate = outerGate;
        }
    }

    private static final class Blend {
        final int a;
        final int b;
        final double t;

        Blend(int a, int b, double t) {
            this.a = a;
            this.b = b;
            this.t = t;
        }
    }

    private static enum Bias {
        TOP,
        BOTTOM,
        UNIFORM,
        TRIANGULAR;

    }

    private static final class V2 {
        final double x;
        final double z;

        V2(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
}
