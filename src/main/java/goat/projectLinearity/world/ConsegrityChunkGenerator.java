package goat.projectLinearity.world;

import goat.projectLinearity.libs.ArcticChunkGenerator;
import goat.projectLinearity.world.ConsegrityRegions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.PinkPetals;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.ChunkGenerator;

public class ConsegrityChunkGenerator
extends ArcticChunkGenerator {
    private static final int SEA_LEVEL = 153;
    private static final int AUDIT_Y_MIN = -192;
    private static final int AUDIT_Y_MAX = 320;
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    // Y level separating overworld and the custom underworld (Nether-like) layer
    private static final int UNDERWORLD_ROOF_Y = -70;
    private static final double TRANSITION = 10.0;
    private static final double CENTRAL_RADIUS = 100.0;
    private static final double CENTRAL_FEATHER = 90.0;
    private static final double CENTRAL_JITTER = 20.0;
    private static final double R1_INNER = 120.0;
    private static final double R1_OUTER = 520.0;
    private static final double R1_FEATHER = 20.0;
    private static final double R2_INNER = 500.0;
    private static final double R2_OUTER = 800.0;
    private static final double R2_FEATHER = 50.0;

    // --- Massive Mountain (MM) spec ---
    private static final int MM_PEAK_Y_CAP = 700;   // hard cap (also clamped by world.getMaxHeight())
    private static final int MM_GROUND_Y  = 160;    // flat valley floor target
    private static final double MM_EFFECT_R = 420.0;   // how far the “influence” of the big mountain extends
    private static final double MM_CORE_R    = 26.0;   // flat top (plateau) radius
    private static final double MM_CROWN_IN  = 36.0;   // crown ring inner radius
    private static final double MM_CROWN_OUT = 72.0;   // crown ring outer radius
    private static final double MM_RANGE_CELL = 180.0; // ring of secondary ranges around the massif
    private static final double MM_VALLEY_R0 = 160.0;  // inner edge of flat valleys
    private static final double MM_VALLEY_R1 = 300.0;  // outer edge of flat valleys
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
        double rotR1 = baseRot = ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, 101L, 0L, 0L, 7466709L));
        int r2Arcs = R2_SPLITS.length - 1;
        double rotR2 = ConsegrityChunkGenerator.wrap01(baseRot + 0.5 / (double)Math.max(1, r2Arcs) + (ConsegrityChunkGenerator.rand01(ConsegrityChunkGenerator.hash(seed, 202L, 0L, 0L, 7532245L)) - 0.5) * (0.2 / (double)Math.max(1, r2Arcs)));
        int[][] topYGrid = new int[16][16];
        int[][] floorYGrid = new int[16][16];
        double[][] centralMaskGrid = new double[16][16];
        boolean[][] needsWater = new boolean[16][16];
        boolean[][] frozenOcean = new boolean[16][16];
        boolean[][] oceanGrid = new boolean[16][16];
        ConsegrityRegions.Region[][] regionGrid = new ConsegrityRegions.Region[16][16];
        for (lx = 0; lx < 16; ++lx) {
            for (lz = 0; lz < 16; ++lz) {
                int idx;
                int topY;
                double target;
                int wx = (chunkX << 4) + lx;
                int wz = (chunkZ << 4) + lz;
                RingEdges C = this.centralEdges(seed, wx, wz, CENTRAL_RADIUS, CENTRAL_FEATHER);
                RingEdges R1 = this.ringEdges(seed, wx, wz, R1_INNER, R1_OUTER, R1_FEATHER);
                RingEdges R2 = this.ringEdges(seed, wx, wz, R2_INNER, R2_OUTER, R2_FEATHER);
                double cm = 1.0 - ConsegrityChunkGenerator.clamp01((C.r - C.inner) / CENTRAL_FEATHER);
                double r1 = ConsegrityChunkGenerator.bandMask(R1.r, R1.inner, R1.outer, R1_FEATHER);
                double r2 = ConsegrityChunkGenerator.bandMask(R2.r, R2.inner, R2.outer, R2_FEATHER);
                double r1m = r1 * (1.0 - cm);
                double r2m = r2 * (1.0 - cm);
                double union = Math.max(cm, Math.max(r1, r2));
                boolean inOcean = union <= 0.03 && !(C.r <= CENTRAL_RADIUS * 3.0);
                oceanGrid[lx][lz] = inOcean;
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
                int mountainTarget = this.ring2MountainSurfaceY(seed, wx, wz, world.getMaxHeight());
                int DESERT_BASE = 163;
                int SAVANNA_BASE = 165;
                int SWAMP_BASE = 161;
                int MESA_BASE = 175;
                int JUNGLE_BASE = 169;
                int ICE1_BASE = 167;
                int CHERRY1_BASE = 173;
                int MTN_BASE = 183;
                // Swap: put mountains into ring1 (index 2)
                double r1DeltaA = switch (b1.a) {
                    case 0 -> desertTarget - 163;
                    case 1 -> savannahTarget - 165;
                    case 2 -> mountainTarget - 175; // approximate to ring1 base
                    case 3 -> cherryTarget - 173;
                    case 4 -> swampTarget - 161;
                    default -> 0.0;
                };
                double r1DeltaB = switch (b1.b) {
                    case 0 -> desertTarget - 163;
                    case 1 -> savannahTarget - 165;
                    case 2 -> mountainTarget - 175;
                    case 3 -> cherryTarget - 173;
                    case 4 -> swampTarget - 161;
                    default -> r1DeltaA;
                };
                double r1MainH = 165.0 + r1DeltaA;
                double tArc = b1.a == b1.b ? 0.0 : ConsegrityChunkGenerator.smooth01(1.0 - b1.t);
                double r1CrossH = 165.0 + ConsegrityChunkGenerator.lerp(r1DeltaA, r1DeltaB, tArc);
                double tBridge1 = b1.a == b1.b ? 0.0 : ConsegrityChunkGenerator.smooth01(1.0 - Math.abs(b1.t * 2.0 - 1.0));
                double ring1H = ConsegrityChunkGenerator.lerp(r1MainH, r1CrossH, tBridge1);
                // Swap: put ice spikes into ring2 (default index 2)
                double r2DeltaA = switch (b2.a) {
                    case 0 -> mesaTarget - 175;
                    case 1 -> jungleTarget - 169;
                    default -> (double)(iceTarget - 167);
                };
                double r2DeltaB = switch (b2.b) {
                    case 0 -> mesaTarget - 175;
                    case 1 -> jungleTarget - 169;
                    default -> (double)(iceTarget - 167);
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
                    int fillBase = Math.max(minY, UNDERWORLD_ROOF_Y + 1);
                    if (topY - 2 > fillBase) {
                        data.setRegion(lx, fillBase, lz, lx + 1, topY - 2, lz + 1, Material.STONE);
                    }
                }
                if (inOcean) {
                    boolean isFrozen;
                    this.paintSeafloorCap(data, lx, lz, topY, 153);
                    int depth = 153 - topY;
                    double uO = this.angle01Warped(seed, wx, wz);
                    // Frozen ocean now follows ring2 ice spikes sector (index 2)
                    int sectorIdx2 = ConsegrityChunkGenerator.arcIndex(ConsegrityChunkGenerator.wrap01(uO + rotR2), R2_SPLITS);
                    frozenOcean[lx][lz] = isFrozen = sectorIdx2 == 2;
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
                        } else if (left == 2 || right == 2) {
                            // Mountains now in ring1 index 2
                            this.paintMountainCap(data, lx, lz, topY);
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
                            biome.setBiome(lx, lz, Biome.STONY_PEAKS);
                            regionGrid[lx][lz] = ConsegrityRegions.Region.MOUNTAIN;
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
                        this.paintMountainCap(data, lx, lz, topY);
                        biome.setBiome(lx, lz, Biome.STONY_PEAKS);
                        regionGrid[lx][lz] = ConsegrityRegions.Region.MOUNTAIN;
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
                    biome.setBiome(lx, lz, Biome.ICE_SPIKES);
                    regionGrid[lx][lz] = ConsegrityRegions.Region.ICE_SPIKES;
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
                this.paintLandCap(data, lx, lz, topY);
                biome.setBiome(lx, lz, Biome.ICE_SPIKES);
                regionGrid[lx][lz] = ConsegrityRegions.Region.ICE_SPIKES;
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
        // Underworld Nether-like layer beneath the overworld
        this.placeUnderNether(world, data, seed, chunkX, chunkZ, regionGrid, oceanGrid);
        this.placeCentralTrees(world, data, seed, chunkX, chunkZ, topYGrid, centralMaskGrid);
        this.placeMountainRivers(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeMountainTaigaEdge(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeIceSpikesFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeCherryFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeDesertFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeSwampFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeSavannaFeatures(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid);
        this.placeCentralGrass(world, data, seed, chunkX, chunkZ, topYGrid, centralMaskGrid);
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

    // Angular mid of ring1 mountain wedge (index 2), then place the massif
private V2 massiveMountainCenter(long seed) {
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        int r1Arcs = R1_SPLITS.length - 1;
        double rotR1 = wrap01(baseRot);

        double a0 = R1_SPLITS[2], a1 = R1_SPLITS[3];
        double uMid = wrap01((a0 + a1) * 0.5 + rotR1);
        double ang = uMid * Math.PI * 2.0 - Math.PI;

        double rMid = (R1_INNER + R1_OUTER) * 0.5;

        double jx = (rand01(hash(seed, 9001L, 0L, 0L, 0xCAFEBABEL)) - 0.5) * 40.0;
        double jz = (rand01(hash(seed, 9002L, 0L, 0L, 0xCAFED00DL)) - 0.5) * 40.0;

        double x = CENTER_X + Math.cos(ang) * rMid + jx;
        double z = CENTER_Z + Math.sin(ang) * rMid + jz;
        return new V2(x, z);
    }

    // Are we inside the ring1 "mountain" sector at (wx, wz)?
    private boolean inMountainSector(long seed, int wx, int wz) {
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        double rotR1 = wrap01(baseRot);
        double u = wrap01(angle01Warped(seed, wx, wz) + rotR1);
        int idx = arcIndex(u, R1_SPLITS);
        return idx == 2; // ring1 index 2 == mountains (your swap)
    }

    // 0..1 falloff from massif center with angular bias towards the sector centerline
    private double massiveMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;

        // Radial falloff from massif center
        V2 c = massiveMountainCenter(seed);
        double dx = wx - c.x, dz = wz - c.z;
        double d = Math.hypot(dx, dz);
        double radial = smooth01(clamp01(1.0 - d / MM_EFFECT_R));

        // Angular bias towards the middle of the mountain wedge
        double baseRot = rand01(hash(seed, 101L, 0L, 0L, 7466709L));
        double rotR1 = wrap01(baseRot);
        double u = wrap01(angle01Warped(seed, wx, wz) + rotR1);
        double a0 = R1_SPLITS[2], a1 = R1_SPLITS[3];
        double uMid = wrap01((a0 + a1) * 0.5 + rotR1);
        double diff = Math.abs(u - uMid);
        if (diff > 0.5) diff = 1.0 - diff; // shortest wrap-around distance
        double halfWidth = Math.max(1e-6, (a1 - a0) * 0.5);
        double angNorm = clamp01(1.0 - diff / halfWidth); // 1 at centerline, 0 at edges
        double angular = smooth01(Math.pow(angNorm, 0.9)); // slightly broader shoulder

        return radial * angular;
    }

    // use existing V2 class defined earlier in this file

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
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 3303206417L);
        boolean[][] treeBases = new boolean[16][16];

        int treeAttempts = 9;
        for (int i = 0; i < treeAttempts; ++i) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY) continue;
            int groundY = topYGrid[lx][lz];
            if (groundY < 160 || groundY > 205) continue;
            if (this.slopeGrid(topYGrid, lx, lz) > 4) continue;
            Material ground = this.safeType(data, lx, groundY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT && ground != Material.STONE) continue;

            int height = 5 + rng.nextInt(3);
            this.placeCherryTree(data, lx, groundY + 1, lz, height, rng);
            treeBases[lx][lz] = true;
        }

        int petalAttempts = 80;
        for (int i = 0; i < petalAttempts; ++i) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.CHERRY) continue;
            int groundY = topYGrid[lx][lz];
            if (groundY < 158 || groundY > 210) continue;
            if (treeBases[lx][lz]) continue;
            if (this.slopeGrid(topYGrid, lx, lz) > 4) continue;

            Material ground = this.safeType(data, lx, groundY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            Material above = this.safeType(data, lx, groundY + 1, lz);
            if (above != Material.AIR) continue;

            this.placePinkPetals(data, lx, groundY + 1, lz, rng);

            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dz == 0) continue;
                    if (rng.nextDouble() > 0.55) continue;
                    int xx = lx + dx;
                    int zz = lz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    if (treeBases[xx][zz]) continue;
                    int ny = topYGrid[xx][zz];
                    if (Math.abs(ny - groundY) > 1) continue;
                    Material nGround = this.safeType(data, xx, ny, zz);
                    Material nAbove = this.safeType(data, xx, ny + 1, zz);
                    if ((nGround == Material.GRASS_BLOCK || nGround == Material.DIRT) && nAbove == Material.AIR) {
                        this.placePinkPetals(data, xx, ny + 1, zz, rng);
                    }
                }
            }
        }
    }

    private void placeCherryTree(ChunkGenerator.ChunkData data, int lx, int y, int lz, int height, SplittableRandom rng) {
        int trunkHeight = height + rng.nextInt(2);
        int currentX = lx;
        int currentZ = lz;
        for (int i = 0; i < trunkHeight; ++i) {
            int yy = y + i;
            data.setBlock(currentX, yy, currentZ, Material.CHERRY_LOG);
            if (i >= trunkHeight - 3 && rng.nextDouble() < 0.35) {
                int dir = rng.nextInt(4);
                int offX = dir == 0 ? 1 : dir == 1 ? -1 : 0;
                int offZ = dir == 2 ? 1 : dir == 3 ? -1 : 0;
                int nextX = currentX + offX;
                int nextZ = currentZ + offZ;
                if (nextX >= 0 && nextX <= 15 && nextZ >= 0 && nextZ <= 15) {
                    currentX = nextX;
                    currentZ = nextZ;
                }
            }
        }

        int topY = y + trunkHeight - 1;
        int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<int[]> canopyAnchors = new ArrayList<>();
        canopyAnchors.add(new int[]{currentX, topY, currentZ});

        for (int[] dir : dirs) {
            if (rng.nextDouble() > 0.6) continue;
            int length = 1 + rng.nextInt(2);
            int branchY = topY - rng.nextInt(2);
            int bx = currentX;
            int bz = currentZ;
            int placed = 0;
            for (int step = 0; step < length; ++step) {
                bx += dir[0];
                bz += dir[1];
                if (bx < 0 || bx > 15 || bz < 0 || bz > 15) break;
                Material existing = this.safeType(data, bx, branchY, bz);
                if (existing != Material.AIR && existing != Material.CHERRY_LEAVES) break;
                Orientable branch = (Orientable)Material.CHERRY_LOG.createBlockData();
                branch.setAxis(dir[0] != 0 ? Axis.X : Axis.Z);
                data.setBlock(bx, branchY, bz, branch);
                placed++;
            }
            if (placed > 0) {
                canopyAnchors.add(new int[]{bx, branchY, bz});
            }
        }

        for (int[] anchor : canopyAnchors) {
            this.placeCherryCanopy(data, anchor[0], anchor[1], anchor[2], rng);
        }
    }

    private void placeCherryCanopy(ChunkGenerator.ChunkData data, int cx, int cy, int cz, SplittableRandom rng) {
        int baseRadius = 3 + rng.nextInt(2);
        for (int dy = -2; dy <= 3; ++dy) {
            double layerRadius = baseRadius - 0.6 * Math.abs(dy - 1);
            if (dy <= 0) {
                layerRadius += 0.5;
            }
            int r = (int)Math.ceil(Math.max(1.5, layerRadius));
            int yy = cy + dy;
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    int xx = cx + dx;
                    int zz = cz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > layerRadius + rng.nextDouble() * 0.5) continue;
                    Material existing = this.safeType(data, xx, yy, zz);
                    if (existing != Material.AIR) continue;
                    if (rng.nextDouble() < 0.85) {
                        data.setBlock(xx, yy, zz, Material.CHERRY_LEAVES);
                        if (dy <= 0 && rng.nextDouble() < 0.28) {
                            int hangY = yy - 1;
                            if (hangY >= 0 && this.safeType(data, xx, hangY, zz) == Material.AIR) {
                                data.setBlock(xx, hangY, zz, Material.CHERRY_LEAVES);
                            }
                        }
                    }
                }
            }
        }
        if (this.safeType(data, cx, cy + 4, cz) == Material.AIR) {
            data.setBlock(cx, cy + 4, cz, Material.CHERRY_LEAVES);
        }
    }

    private void placePinkPetals(ChunkGenerator.ChunkData data, int lx, int y, int lz, SplittableRandom rng) {
        BlockData petals = Material.PINK_PETALS.createBlockData();
        if (petals instanceof PinkPetals pink) {
            pink.setFlowerAmount(2 + rng.nextInt(3));
            petals = pink;
        }
        data.setBlock(lx, y, lz, petals);
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
        // Vertical core with slight bulges at base and mid
        for (int dy = 0; dy < height; dy++) {
            int yy = y + dy;
            if (yy >= y + height) break;
            Material m = (rng.nextDouble() < 0.15) ? Material.BLUE_ICE : Material.PACKED_ICE;
            data.setBlock(lx, yy, lz, m);

            int r = (dy < 2) ? (radius + 1)
                    : (dy < height / 3 ? radius : (dy > height * 2 / 3 ? 0 : (radius - 1)));
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
        // Point tip
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

    private void placeSavannaFeatures(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 151587097L);
        // Acacia-style trees: a few scattered attempts per chunk
        for (int i = 0; i < 4; i++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (lx < 1 || lx > 14 || lz < 1 || lz > 14) continue;
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SAVANNAH) continue;
            int topY = topYGrid[lx][lz];
            if (topY < 155) continue; // slightly above sea level
            if (this.slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = this.safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.35) continue; // gate

            int height = 4 + rng.nextInt(3); // 4..6
            int dir = rng.nextInt(4);
            int ox = dir == 0 ? 1 : (dir == 1 ? -1 : 0);
            int oz = dir == 2 ? 1 : (dir == 3 ? -1 : 0);

            int x = lx;
            int z = lz;
            for (int dy = 0; dy < height; dy++) {
                int yy = topY + 1 + dy;
                if (dy >= height - 2) { x += ox; z += oz; }
                if (x < 0 || x > 15 || z < 0 || z > 15) break;
                data.setBlock(x, yy, z, Material.ACACIA_LOG);
            }
            int canopyY = topY + height;
            int cx = x, cz = z;
            int r = 2 + rng.nextInt(2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                    int xx = cx + dx, zz = cz + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) continue;
                    if (this.safeType(data, xx, canopyY, zz) == Material.AIR) {
                        data.setBlock(xx, canopyY, zz, Material.ACACIA_LEAVES);
                    }
                    if (rng.nextDouble() < 0.35 && this.safeType(data, xx, canopyY + 1, zz) == Material.AIR) {
                        data.setBlock(xx, canopyY + 1, zz, Material.ACACIA_LEAVES);
                    }
                }
            }
        }

        // Heavy grass coverage across savanna
        Material grassPlant = pickGrassPlant();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SAVANNAH) continue;
                int topY = topYGrid[lx][lz];
                Material ground = this.safeType(data, lx, topY, lz);
                Material above = this.safeType(data, lx, topY + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    if (rng.nextDouble() < 0.4) data.setBlock(lx, topY + 1, lz, grassPlant);
                }
            }
        }
    }

    private void placeCentralGrass(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, double[][] centralMaskGrid) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 206158430L);
        Material grassPlant = pickGrassPlant();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (centralMaskGrid[lx][lz] < 0.5) continue;
                int topY = topYGrid[lx][lz];
                if (topY <= 157) continue;
                Material ground = this.safeType(data, lx, topY, lz);
                Material above = this.safeType(data, lx, topY + 1, lz);
                if ((ground == Material.GRASS_BLOCK || ground == Material.DIRT) && above == Material.AIR) {
                    double roll = rng.nextDouble();
                    if (roll < 0.03) {
                        Material flower = rng.nextDouble() < 0.55 ? Material.POPPY : Material.DANDELION;
                        data.setBlock(lx, topY + 1, lz, flower);
                    } else if (roll < 0.11) {
                        data.setBlock(lx, topY + 1, lz, grassPlant);
                    }
                }
            }
        }
    }

    private Material pickGrassPlant() {
        try { return Material.valueOf("SHORT_GRASS"); } catch (Throwable ignore) {
            try { return Material.valueOf("TALL_GRASS"); } catch (Throwable ignore2) {
                try { return Material.valueOf("GRASS"); } catch (Throwable ignore3) {
                    return Material.FERN;
                }
            }
        }
    }

    private void placeSwampFeatures(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 271828183L);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // 40% water coverage via smooth noise mask; 60% land paths
        Material lily;
        try { lily = Material.valueOf("LILY_PAD"); } catch (Throwable t) { lily = Material.valueOf("WATER_LILY"); }
        final double waterFrac = 0.40;
        final double scale = 34.0; // control size of pools/paths
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
                int topY = topYGrid[lx][lz];
                int wx = baseX + lx;
                int wz = baseZ + lz;
                double n = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A4D00A1L, (double)wx / scale, (double)wz / scale);
                boolean water = (n < waterFrac);
                if (water) {
                    // carve water at surface and occasionally add lily pads (10%)
                    data.setBlock(lx, topY, lz, Material.WATER);
                    if (rng.nextDouble() < 0.10 && this.safeType(data, lx, topY + 1, lz) == Material.AIR) {
                        try { data.setBlock(lx, topY + 1, lz, lily); } catch (Throwable ignore) {}
                    }
                } else {
                    // Ensure path stays clear (replace odd sand/mud tops with grass)
                    Material g = this.safeType(data, lx, topY, lz);
                    if (g != Material.GRASS_BLOCK && g != Material.DIRT && g != Material.MUD) {
                        data.setBlock(lx, topY, lz, Material.GRASS_BLOCK);
                    }
                }
            }
        }

        // Rare oak trees placed only on land paths
        for (int i = 0; i < 10; i++) {
            int lx = rng.nextInt(16);
            int lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
            int topY = topYGrid[lx][lz];
            int wx = baseX + lx;
            int wz = baseZ + lz;
            double n = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x5A4D00A1L, (double)wx / scale, (double)wz / scale);
            if (n < waterFrac) continue; // avoid water cells
            if (this.slopeGrid(topYGrid, lx, lz) > 3) continue;
            Material ground = this.safeType(data, lx, topY, lz);
            if (ground != Material.GRASS_BLOCK && ground != Material.DIRT) continue;
            if (rng.nextDouble() > 0.18) continue;
            this.growSimpleTree(data, lx, topY + 1, lz, Material.OAK_LOG, Material.OAK_LEAVES, new Random(rng.nextLong()));
            if (rng.nextBoolean()) addVinesAround(data, lx, topY + 1, lz, rng);
        }
    }

    private void addVinesAround(ChunkGenerator.ChunkData data, int x, int y, int z, SplittableRandom rng) {
        // Try each cardinal side with small downward lengths
        try {
            for (int side = 0; side < 4; side++) {
                if (rng.nextDouble() > 0.6) continue; // not all sides
                int dx = (side == 0) ? 1 : (side == 1 ? -1 : 0);
                int dz = (side == 2) ? 1 : (side == 3 ? -1 : 0);
                BlockFace face = (side == 0) ? BlockFace.WEST : (side == 1 ? BlockFace.EAST : (side == 2 ? BlockFace.NORTH : BlockFace.SOUTH));
                int len = 1 + rng.nextInt(3);
                int yy = y + 2;
                for (int k = 0; k < len; k++) {
                    int xx = x + dx;
                    int zz = z + dz;
                    if (xx < 0 || xx > 15 || zz < 0 || zz > 15) break;
                    if (this.safeType(data, xx, yy - k, zz) != Material.AIR) continue;
                    BlockData bd = Bukkit.createBlockData(Material.VINE);
                    try { ((MultipleFacing)bd).setFace(face, true); } catch (Throwable ignore) {}
                    try { data.setBlock(xx, yy - k, zz, bd); } catch (Throwable t) { data.setBlock(xx, yy - k, zz, Material.VINE); }
                }
            }
        } catch (Throwable ignore) {
            // Best-effort; ignore if Vine blockdata not supported
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
        // Flatter, low-lying swamp terrain just above sea level
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x51A7A11BL, (double)wx / 260.0, (double)wz / 260.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0x51A7A11CL, (double)wx / 90.0,  (double)wz / 90.0);
        double h  = (h1 * 0.75 + h2 * 0.25) * 2.0 - 1.0; // [-1,1]
        int base = SEA_LEVEL + 1; // ~154
        int amp  = 2;             // very small relief
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
        double h1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADA11D5L, (double)wx / 300.0, (double)wz / 300.0);
        double h2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADA11D6L, (double)wx / 96.0, (double)wz / 96.0);
        double h3 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xBADA11D7L, (double)wx / 36.0, (double)wz / 36.0);
        double h = (h1 * 0.5 + h2 * 0.3 + h3 * 0.2) * 2.0 - 1.0;
        int base = 175;
        int amp = 28;
        return base + (int)Math.round(h * (double)amp);
    }

    private int ring2MountainSurfaceY(long seed, int wx, int wz, int worldMax) {
        // --- baseline "regular" ranges you already liked (kept, slightly tidied) ---
        double base = 155.0;
        double low = ConsegrityChunkGenerator.fbm2(seed ^ 0xA1B2C3D4L, (double)wx * 0.003, (double)wz * 0.003);
        double mountainBand = (low - 0.5) * 2.0;
        double t0 = 0.0, t1 = 0.38;
        double mask = ConsegrityChunkGenerator.clamp01((mountainBand - t0) / (t1 - t0));

        // clustered peaks field (unchanged from your version)
        double field = 0.0;
        field += this.peakSum(seed, wx, wz, 176.0, 70.0, 64.0, 2.4, 2576L);
        field += this.peakSum(seed, wx, wz, 112.0, 46.0, 44.0, 2.1, 2832L);
        field += this.peakSum(seed, wx, wz, 72.0,  30.0, 28.0, 1.9, 3088L);
        field += this.peakSum(seed, wx, wz, 48.0,  20.0, 16.0, 1.75,3344L);

        double ridge = ConsegrityChunkGenerator.fbm2(seed ^ 0xB3C4D5E6L, (double)wx * 0.02, (double)wz * 0.02);
        double plains = (ConsegrityChunkGenerator.fbm2(seed ^ 0x600DD00EL, (double)wx * 0.02, (double)wz * 0.02) - 0.5) * 2.0 * 4.8;
        double between = (ConsegrityChunkGenerator.fbm2(seed ^ 0x77EEDD11L, (double)wx * 0.01, (double)wz * 0.01) - 0.5) * 2.0;
        double betweenAmp = 18.0 * (1.0 - mask * 0.85);
        double micro = (ConsegrityChunkGenerator.fbm2(seed ^ 0x33AA55CCL, (double)wx * 0.06, (double)wz * 0.06) - 0.5) * 2.0 * 2.6;

        double baseH = base + plains + between * betweenAmp + micro + (ridge - 0.5) * 2.0 * 20.0 * mask + field;
        baseH += ConsegrityChunkGenerator.fbm2(seed ^ 0xE01234L, (double)wx * 0.004, (double)wz * 0.004) * 70.0 * mask;

        // --- Massive Mountain overlay (single peak with crown, valleys, and ring ranges) ---
        // Nerf: cap mountain to ~Y=300 so the massif doesn't dominate sky height
        int yCap = Math.min(300, Math.max(256, worldMax - 1));
        double mmW = this.massiveMask(seed, wx, wz); // 0..1 only inside the mountain wedge
        if (mmW > 0.0) {
            V2 c = this.massiveMountainCenter(seed);
            double dx = (double)wx - c.x, dz = (double)wz - c.z;
            double d  = Math.hypot(dx, dz);

            // core rise (smooth and wide, takes time to climb)
            double rise = Math.pow(ConsegrityChunkGenerator.clamp01(1.0 - d / (MM_EFFECT_R)), 1.45);
            double H = MM_GROUND_Y + rise * (yCap - MM_GROUND_Y);   // theoretical cone to ~700

            // flat top
            if (d <= MM_CORE_R) {
                H = Math.max(H, yCap - 6); // give it that weirdly-flat summit
            }

            // crown spikes ring
            if (d > MM_CROWN_IN && d < MM_CROWN_OUT) {
                double t = 1.0 - Math.abs((d - (MM_CROWN_IN + MM_CROWN_OUT) * 0.5) / ((MM_CROWN_OUT - MM_CROWN_IN) * 0.5));
                double spike = (ConsegrityChunkGenerator.valueNoise2(seed ^ 0x51ABCDL, (double)wx / 6.0, (double)wz / 6.0) - 0.5) * 2.0;
                H += Math.max(0.0, t) * (10.0 + spike * 14.0);
            }

            // stepped terraces feel
            double step = 5.5;
            H = Math.floor(H / step) * step + (H - Math.floor(H / step) * step) * 0.35;

            // valley ring flattening with rivers later
            if (d > MM_VALLEY_R0 && d < MM_VALLEY_R1) {
                double vt = 1.0 - Math.abs((d - (MM_VALLEY_R0 + MM_VALLEY_R1) * 0.5) / ((MM_VALLEY_R1 - MM_VALLEY_R0) * 0.5));
                H = ConsegrityChunkGenerator.lerp(H, MM_GROUND_Y, ConsegrityChunkGenerator.smooth01(ConsegrityChunkGenerator.clamp01(vt)) * 0.85);
            }

            // ring of secondary ranges beyond the valley — varied but lower than the massif
            double ring = ConsegrityChunkGenerator.clamp01(1.0 - Math.abs(d - (MM_VALLEY_R1 + MM_EFFECT_R) * 0.5) / (MM_EFFECT_R * 0.5));
            if (ring > 0.0) {
                double rr = (ConsegrityChunkGenerator.fbm2(seed ^ 0xA55A5AL, (double)wx / MM_RANGE_CELL, (double)wz / MM_RANGE_CELL) - 0.5) * 2.0;
                H += ring * rr * 60.0;
            }

            baseH = Math.max(baseH, H); // the massif dominates within its wedge
        }

        // safety and floor
        if (baseH < 150.0) baseH = 150.0;
        if (baseH > yCap)  baseH = yCap;
        return (int)Math.floor(baseH);
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
        int start = Math.max(-60, topY - 12);
        for (int y = start; y <= topY; y++) {
            int band = Math.floorMod(y, 7);
            Material m = switch (band) {
                case 0 -> Material.TERRACOTTA;
                case 1 -> Material.RED_TERRACOTTA;
                case 2 -> Material.ORANGE_TERRACOTTA;
                case 3 -> Material.BROWN_TERRACOTTA;
                case 4 -> Material.YELLOW_TERRACOTTA;
                case 5 -> Material.WHITE_TERRACOTTA;
                default -> Material.LIGHT_GRAY_TERRACOTTA;
            };
            if (y == topY) m = Material.RED_SAND;
            data.setBlock(lx, y, lz, m);
        }
    }

    private void paintMesaLandBlendCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY, double tLand) {
        int i;
        double t = ConsegrityChunkGenerator.clamp01(tLand);
        int terr = (int)Math.round(ConsegrityChunkGenerator.lerp(10.0, 0.0, t));
        int dirt = (int)Math.round(ConsegrityChunkGenerator.lerp(0.0, 3.0, t));
        boolean grassTop = t > 0.15;
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
                        if (dx == 0 && dz == 0) continue;
                        int nx = lx + dx;
                        int nz = lz + dz;
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || (h = topYGrid[nx][nz]) >= y) continue;
                        ++countLower;
                    }
                }
                boolean bl = isHill = countLower >= 4;
                if (!isHill || rng.nextDouble() > 0.25) continue;
                Material wood = rng.nextDouble() < 0.9 ? Material.OAK_LOG : Material.BIRCH_LOG;
                Material leaves = wood == Material.OAK_LOG ? Material.OAK_LEAVES : Material.BIRCH_LEAVES;
                this.growSimpleTree(data, lx, h + 1, lz, wood, leaves, rng);
            }
        }
    }

    // valley mask ~1 on the flat ring
    private double valleyMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;
        V2 c = massiveMountainCenter(seed);
        double d = Math.hypot(wx - c.x, wz - c.z);
        double mid = (MM_VALLEY_R0 + MM_VALLEY_R1) * 0.5;
        double half = (MM_VALLEY_R1 - MM_VALLEY_R0) * 0.5;
        double t = 1.0 - Math.abs((d - mid) / half);
        return clamp01(smooth01(Math.max(0.0, t)));
    }

    // Spruce near the wedge boundary (far from the massif center)
    private double taigaEdgeMask(long seed, int wx, int wz) {
        if (!inMountainSector(seed, wx, wz)) return 0.0;
        V2 c = massiveMountainCenter(seed);
        double d = Math.hypot(wx - c.x, wz - c.z);
        double t = clamp01((d - (MM_EFFECT_R * 0.70)) / (MM_EFFECT_R * 0.25)); // 0 at 70%, 1 near edge
        return smooth01(t);
    }

    private void placeMountainRivers(World world, ChunkGenerator.ChunkData data, long seed,
                                     int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
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

    private void placeMountainTaigaEdge(World world, ChunkGenerator.ChunkData data, long seed,
                                        int chunkX, int chunkZ, int[][] topYGrid, ConsegrityRegions.Region[][] regionGrid) {
        SplittableRandom rng = ConsegrityChunkGenerator.rngFor(seed, chunkX, chunkZ, 0x55AA33L);
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
            int wxAttempt = (chunkX << 4) + ax;
            int wzAttempt = (chunkZ << 4) + az;
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
                    W_EMERALD_R = 0;
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
                    W_EMERALD_R = 0;
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
                    W_EMERALD_R = 0; // moved to targeted logic
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
            // --- Targeted emeralds: only on the massive mountain and weighted by height ---
            if (region == ConsegrityRegions.Region.MOUNTAIN) {
                double mmW = this.massiveMask(world.getSeed(), wxAttempt, wzAttempt);
                if (mmW > 0.12) { // keep it tight around the massif
                    int tries = (int)Math.round(1 + mmW * 6); // up to 7 micro-clusters per loop
                    for (int k = 0; k < tries; k++) {
                        int worldTop = Math.min(MM_PEAK_Y_CAP, world.getMaxHeight() - 1);
                        int yMin = Math.max(MM_GROUND_Y + 10, world.getMinHeight());
                        int yMax = worldTop;
                        this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ,
                                Material.EMERALD_ORE,
                                6, // light cluster; frequency comes from 'tries'
                                yMin, yMax,
                                Bias.TOP, 0, false);
                    }
                }
            }
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.OBSIDIAN, 1, Math.max(-64, world.getMinHeight()), -40, Bias.BOTTOM, 0, true);
        }
    }

    private void oreAttemptsLocal(World world, ChunkGenerator.ChunkData data, SplittableRandom rng, int chunkX, int chunkZ, Material ore, int weight, int minY, int maxY, Bias bias, int peakY, boolean isObsidian) {
        int yMax;
        int yMin = Math.max(Math.max(Math.max(-192, minY), world.getMinHeight()), UNDERWORLD_ROOF_Y + 1);
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
                    double d;
                    int x = cx + dx;
                    int z = cz + dz;
                    if (x < 0 || x > 15 || z < 0 || z > 15 || !frozenOcean[x][z] || (d = Math.hypot(dx, dz)) > (double)r + 0.8) continue;
                    double t = Math.max(0.0, 1.0 - d / ((double)r + 0.8));
                    int peak = sea + (int)Math.round((double)h * t * (0.85 + rng.nextDouble() * 0.2));
                    int base = Math.max(floor + 1, sea - (1 + rng.nextInt(3)));
                    for (int y = base; y <= peak; ++y) {
                        Material m = rng.nextDouble() < 0.2 ? Material.BLUE_ICE : Material.PACKED_ICE;
                        data.setBlock(x, y, z, m);
                    }
                }
            }
        }
    }

    // Create a mirrored bedrock roof at y ~ -70, low-amplitude netherrack dunes below it,
    // a lava ocean around -180, classic-nether-like land islands, and vertical bedrock walls
    // along the overworld coastline boundary. Also adds glowstone near the ceiling and
    // occasional fire and lava drips.
    private void placeUnderNether(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ,
                                  ConsegrityRegions.Region[][] regionGrid, boolean[][] oceanGrid) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int roofY = UNDERWORLD_ROOF_Y;
        int lavaY = -180;
        if (roofY <= minY + 8 || lavaY <= minY + 4) return; // out of bounds safety

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 0xC0FFEE7L);

        // 1) Mirrored bedrock roof centered at -70 with random thickness like bottom bedrock
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int t = 1 + rng.nextInt(5); // thickness 1..5 mirrored
                for (int i = 0; i < t; i++) {
                    int y1 = roofY - i;
                    int y2 = roofY + i;
                    if (y1 >= minY && y1 <= maxY) data.setBlock(lx, y1, lz, Material.BEDROCK);
                    if (y2 >= minY && y2 <= maxY) data.setBlock(lx, y2, lz, Material.BEDROCK);
                }
            }
        }

        // 2) Place occasional holes in the roof under jungle
        for (int tries = 0; tries < 6; tries++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.JUNGLE) continue;
            if (rng.nextDouble() > 0.33) continue;
            int r = 1 + rng.nextInt(2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                int x = lx + dx, z = lz + dz;
                if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                for (int yy = roofY - 2; yy <= roofY + 2; yy++) {
                    if (yy >= minY && yy <= maxY) data.setBlock(x, yy, z, Material.AIR);
                }
            }
        }

        // 3) Pre-fill underworld (between roof and lava sea) with netherrack
        int fillTop = Math.max(minY, roofY - 1);
        int fillBottom = Math.max(minY, lavaY + 1);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = fillBottom; y <= fillTop; y++) {
                    Material m = data.getType(lx, y, lz);
                    if (m == Material.AIR || m == Material.WATER || m == Material.LAVA) {
                        data.setBlock(lx, y, lz, Material.NETHERRACK);
                    }
                }
            }
        }

        // 4) Carve vaulted caverns with multi-scale pseudo-3D noise and slight ceiling droop
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = baseX + lx, wz = baseZ + lz;
                double warp = fbm2(seed ^ 0x51D2A1L, wx / 96.0, wz / 96.0) * 24.0;
                for (int y = fillBottom; y <= fillTop; y++) {
                    // fake 3D: inject Y into the 2D FBM inputs to create vertical coherence
                    double d1 = fbm2(seed ^ 0xC0FEF1L, (wx + y * 0.8) / 64.0, (wz + y * 0.8) / 64.0);
                    double d2 = fbm2(seed ^ 0xC0FEF2L, (wx - y * 1.2) / 24.0, (wz + y * 1.2) / 24.0) * 0.6;
                    double d3 = fbm2(seed ^ 0xC0FEF3L, (wx + warp + y * 0.6) / 96.0, (wz + warp - y * 0.6) / 96.0) * 0.2;
                    double d = d1 + d2 + d3; // roughly -1..+1
                    // Droop ceilings: bias towards more air near the roof
                    double roofBias = Math.max(0.0, (roofY - y) / 24.0);
                    double threshold = 0.25 - roofBias * 0.10;
                    if (d > threshold) {
                        // carve air; lava will be filled later
                        data.setBlock(lx, y, lz, Material.AIR);
                    }
                }
            }
        }

        // 4b) Remove tiny floaters
        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                for (int y = Math.min(fillTop - 2, roofY - 2); y >= fillBottom + 2; y--) {
                    if (data.getType(lx, y, lz) != Material.NETHERRACK) continue;
                    int airSides = 0;
                    if (data.getType(lx + 1, y, lz) == Material.AIR) airSides++;
                    if (data.getType(lx - 1, y, lz) == Material.AIR) airSides++;
                    if (data.getType(lx, y, lz + 1) == Material.AIR) airSides++;
                    if (data.getType(lx, y, lz - 1) == Material.AIR) airSides++;
                    if (data.getType(lx, y + 1, lz) == Material.AIR) airSides++;
                    if (data.getType(lx, y - 1, lz) == Material.AIR) airSides++;
                    if (airSides >= 5) data.setBlock(lx, y, lz, Material.AIR);
                }
            }
        }

        // 5) Fill lava up to lavaY
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = minY; y <= Math.min(lavaY, maxY); y++) {
                    Material m = data.getType(lx, y, lz);
                    if (m == Material.AIR) data.setBlock(lx, y, lz, Material.LAVA);
                }
            }
        }

        // 5b) Soul sand / gravel beaches around lava edges
        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                int y = lavaY + 1;
                if (y < minY || y > maxY) continue;
                if (data.getType(lx, y - 1, lz) == Material.LAVA && data.getType(lx, y, lz) == Material.NETHERRACK) {
                    double n = fbm2(seed ^ 0x55AAL, (baseX + lx) / 18.0, (baseZ + lz) / 18.0);
                    if (n > 0.55) {
                        // 70% gravel, 30% soul sand
                        Material beach = (rand01(hash(seed, lx, y, lz, 9123L)) < 0.7) ? Material.GRAVEL : Material.SOUL_SAND;
                        for (int by = y; by <= y + 2 && by <= maxY; by++) {
                            if (data.getType(lx, by, lz) == Material.NETHERRACK) data.setBlock(lx, by, lz, beach);
                        }
                    }
                }
            }
        }

        // 6) Vertical bedrock walls at coastline boundary, with 25/50/75/100% gradient into ocean
        // Compute distance into ocean from nearest land within 4 blocks (Chebyshev)
        int[][] oceanDist = new int[16][16];
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) oceanDist[lx][lz] = 9999;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (!oceanGrid[lx][lz]) continue;
                int best = 9999;
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        int nx = lx + dx, nz = lz + dz;
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                        if (!oceanGrid[nx][nz]) {
                            best = Math.min(best, Math.max(Math.abs(dx), Math.abs(dz)));
                        }
                    }
                }
                oceanDist[lx][lz] = best;
            }
        }
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int dOcean = oceanDist[lx][lz];
                if (dOcean < 1 || dOcean > 4) continue; // only within 1..4 into the ocean
                double p = switch (dOcean) { case 1 -> 0.25; case 2 -> 0.5; case 3 -> 0.75; default -> 1.0; };
                SplittableRandom colRng = rngFor(seed, baseX + lx, baseZ + lz, 0xBEEF12L);
                for (int y = Math.max(minY, lavaY); y <= Math.min(roofY + 8, maxY); y++) {
                    if (colRng.nextDouble() < p) data.setBlock(lx, y, lz, Material.BEDROCK);
                }
            }
        }

        // 7) Glowstone: 1-2 clusters per chunk, random-walk from ceiling anchors
        int glowStarts = 1 + (rng.nextInt(3) == 0 ? 1 : 0);
        for (int s = 0; s < glowStarts; s++) {
            int tries = 0;
            int lx, lz, gy;
            do {
                lx = rng.nextInt(16); lz = rng.nextInt(16);
                gy = Math.max(minY, roofY - (2 + rng.nextInt(4)));
                tries++;
                if (tries > 12) break;
            } while (!(gy >= minY && gy <= maxY && data.getType(lx, gy + 1, lz) != Material.AIR && data.getType(lx, gy, lz) == Material.AIR));
            if (tries > 12) continue;
            int steps = 10 + rng.nextInt(16);
            int x = lx, z = lz, y = gy;
            for (int step = 0; step < steps; step++) {
                if (x < 1 || x > 14 || z < 1 || z > 14 || y < minY || y > maxY) break;
                if (data.getType(x, y, z) == Material.AIR && data.getType(x, y + 1, z) != Material.AIR) {
                    data.setBlock(x, y, z, Material.GLOWSTONE);
                    // occasional stalactite nub
                    if (rng.nextInt(5) == 0 && y - 1 >= minY && data.getType(x, y - 1, z) == Material.AIR) {
                        data.setBlock(x, y - 1, z, Material.GLOWSTONE);
                    }
                }
                x += rng.nextInt(3) - 1;
                z += rng.nextInt(3) - 1;
                if (rng.nextInt(4) == 0 && y > lavaY + 8) y -= 1; // slight downward drift
            }
        }

        // 8) Pillars: vertical netherrack columns connecting near floor to roof
        for (int i = 0; i < 3; i++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            double k = fbm2(seed ^ 0xD1E1L, (baseX + lx) / 256.0, (baseZ + lz) / 256.0);
            if (k < 0.78) continue;
            int rad = 2 + rng.nextInt(5);
            int top = roofY - 2;
            int bottom = lavaY + 2;
            for (int x = Math.max(0, lx - rad); x <= Math.min(15, lx + rad); x++) {
                for (int z = Math.max(0, lz - rad); z <= Math.min(15, lz + rad); z++) {
                    double d = Math.hypot(x - lx, z - lz);
                    if (d > rad) continue;
                    for (int y = bottom; y <= top; y++) {
                        if (data.getType(x, y, z) == Material.AIR) data.setBlock(x, y, z, Material.NETHERRACK);
                    }
                }
            }
        }

        // 9) Sparse fire near lava edges only
        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                for (int y = lavaY + 1; y <= lavaY + 4; y++) {
                    if (y < minY || y > maxY) continue;
                    if (data.getType(lx, y, lz) != Material.AIR) continue;
                    if (data.getType(lx, y - 1, lz) != Material.NETHERRACK) continue;
                    boolean nearLava = false;
                    for (int dx = -2; dx <= 2 && !nearLava; dx++) {
                        for (int dz = -2; dz <= 2 && !nearLava; dz++) {
                            if (data.getType(lx + dx, lavaY, lz + dz) == Material.LAVA) nearLava = true;
                        }
                    }
                    if (nearLava && rng.nextInt(32) == 0) data.setBlock(lx, y, lz, Material.FIRE);
                }
            }
        }

        // 10) Quartz ore veins inside netherrack
        int quartzTries = 10 + rng.nextInt(7);
        for (int t = 0; t < quartzTries; t++) {
            int sx = rng.nextInt(16), sz = rng.nextInt(16);
            int sy = lavaY + 10 + rng.nextInt(Math.max(1, (roofY - (lavaY + 20))));
            int size = 8 + rng.nextInt(8);
            int placed = 0;
            java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
            q.add(new int[]{sx, sy, sz});
            while (!q.isEmpty() && placed < size) {
                int[] p = q.removeFirst();
                int x = p[0], y = p[1], z = p[2];
                if (x < 1 || x > 14 || z < 1 || z > 14 || y <= lavaY + 1 || y >= roofY - 2) continue;
                if (data.getType(x, y, z) == Material.NETHERRACK) {
                    data.setBlock(x, y, z, Material.NETHER_QUARTZ_ORE);
                    placed++;
                    if (rng.nextBoolean()) q.add(new int[]{x + (rng.nextBoolean()?1:-1), y + (rng.nextInt(3)-1), z});
                    if (rng.nextBoolean()) q.add(new int[]{x, y + (rng.nextInt(3)-1), z + (rng.nextBoolean()?1:-1)});
                }
            }
        }

        // 11) Occasional lavafalls from high roof to lava sea
        if (rng.nextInt(3) == 0) { // ~1 per 2-3 chunks
            int lx = 1 + rng.nextInt(14), lz = 1 + rng.nextInt(14);
            int y = roofY - 1 - rng.nextInt(3);
            for (int yy = y; yy >= lavaY; yy--) {
                if (data.getType(lx, yy, lz) != Material.BEDROCK) data.setBlock(lx, yy, lz, Material.LAVA);
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
        if (t < (b = Math.max(1.0E-6, blendFrac))) {
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
        int cellMinX = cx * 64;
        int cellMaxX = cellMinX + 64;
        int cellMinZ = cz * 64;
        int cellMaxZ = cellMinZ + 64;
        int chMinX = chunkX << 4;
        int chMaxX = chMinX + 16;
        int chMinZ = chunkZ << 4;
        int chMaxZ = chMinZ + 16;
        double dx = (cellMaxX < chMinX) ? (double) (chMinX - cellMaxX)
                : (cellMinX > chMaxX ? (double) (cellMinX - chMaxX) : 0.0);
        double dz = (cellMaxZ < chMinZ) ? (double) (chMinZ - cellMaxZ)
                : (cellMinZ > chMaxZ ? (double) (cellMinZ - chMaxZ) : 0.0);
        return Math.hypot(dx, dz) <= 222.6;
    }

    private void carveCavesWorldAnchored(World world, ChunkGenerator.ChunkData data, long seed, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int yMin = Math.max(world.getMinHeight(), -192);
        int yMax = Math.min(world.getMaxHeight() - 1, 193);
        // Stop carving once we reach the underworld roof to avoid caves below -70
        int yStop = Math.max(world.getMinHeight(), UNDERWORLD_ROOF_Y);
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
