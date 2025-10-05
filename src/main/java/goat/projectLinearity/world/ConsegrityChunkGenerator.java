package goat.projectLinearity.world;

import goat.projectLinearity.libs.ArcticChunkGenerator;
import java.util.Random;
import java.util.SplittableRandom;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import goat.projectLinearity.world.sector.CentralSector;
import goat.projectLinearity.world.sector.DesertBiome;
import goat.projectLinearity.world.sector.SavannaSector;
import goat.projectLinearity.world.sector.SwampSector;
import goat.projectLinearity.world.sector.JungleSector;
import goat.projectLinearity.world.sector.IceSpikesSector;
import goat.projectLinearity.world.sector.CherrySector;
import goat.projectLinearity.world.sector.MesaSector;
import goat.projectLinearity.world.sector.MountainSector;

public class ConsegrityChunkGenerator
extends ArcticChunkGenerator {
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    // Y level separating overworld and the custom underworld (Nether-like) layer
    private static final int UNDERWORLD_ROOF_Y = -70;
    private static final double CENTRAL_RADIUS = 100.0;
    private static final double CENTRAL_FEATHER = 90.0;
    private static final double R1_INNER = 120.0;
    private static final double R1_OUTER = 520.0;
    private static final double R1_FEATHER = 20.0;
    private static final double R2_INNER = 500.0;
    private static final double R2_OUTER = 800.0;
    private static final double R2_FEATHER = 50.0;
    // --- RNG salts (valid hex, stable) ---
    private static final long SALT_GLOWSTONE   = 0x9A0B57A1L;
    private static final long SALT_LAVA_DRIPS  = 0x0D1F5A1AL;
    private static final long SALT_QUARTZ      = 0x0A27A72DL;
    private static final long SALT_PASSAGES    = 0x0A55A9E1L;
    private static final long SALT_BASIN_PREF  = 0x0C3AA0A1L;

    private static final int NETHER_CELL          = 160;  // coarse seed grid (~10x overworld scale)
    private static final int NETHER_WORM_STEPS    = 90;   // long meanders
    private static final int NETHER_WORM_STEP     = 4;    // step in blocks between ellipsoids
    private static final int NETHER_R_MIN         = 10;   // 10x bigger than typical overworld caverns
    private static final int NETHER_R_MAX         = 26;
    private static final int NETHER_ROOM_MIN_R    = 22;   // big chambers
    private static final int NETHER_ROOM_MAX_R    = 40;
    private static final double NETHER_WORM_PROB  = 0.65; // spawn chance per cell
    private static final double NETHER_ROOM_PROB  = 0.22;
    private static final double[] R1_SPLITS = new double[]{0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
    private static final double[] R2_SPLITS = new double[]{0.0, 0.3333333333333, 0.6666666666666, 1.0};
    private static final int[] OFFS = new int[]{0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1};

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

        // Sector instances
        CentralSector centralSec = new CentralSector();
        DesertBiome desertSec = new DesertBiome();
        SavannaSector savannaSec = new SavannaSector();
        SwampSector swampSec = new SwampSector();
        JungleSector jungleSec = new JungleSector();
        IceSpikesSector iceSec = new IceSpikesSector();
        CherrySector cherrySec = new CherrySector();
        MesaSector mesaSec = new MesaSector();
        MountainSector mountainSec = new MountainSector();
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
                int centralTarget = centralSec.computeSurfaceY(world, seed, wx, wz);
                int desertTarget = desertSec.computeSurfaceY(world, seed, wx, wz);
                int savannahTarget = savannaSec.computeSurfaceY(world, seed, wx, wz);
                int swampTarget = swampSec.computeSurfaceY(world, seed, wx, wz);
                int jungleTarget = jungleSec.computeSurfaceY(world, seed, wx, wz);
                int iceTarget = iceSec.computeSurfaceY(world, seed, wx, wz);
                int cherryTarget = cherrySec.computeSurfaceY(world, seed, wx, wz);
                int mesaTarget = mesaSec.computeSurfaceY(world, seed, wx, wz);
                int mountainTarget = mountainSec.computeSurfaceY(world, seed, wx, wz);
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
        this.generateColossalCaveNether(world, data, seed, chunkX, chunkZ);
        centralSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, centralMaskGrid);
        mountainSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        iceSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        cherrySec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        desertSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        swampSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        jungleSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        mesaSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        savannaSec.decorate(world, data, seed, chunkX, chunkZ, topYGrid, regionGrid, null);
        this.placeBedrockBand(world, data, chunkX, chunkZ);
        return data;
    }

    // Using DeferredStructureSpawner/RegenerateCommand for structure placement; no custom populators here.

    // ===== Colossal Cave Nether (Stage 1) =====

    /**
     * Square-bounded Nether underworld for "Colossal Cave".
     * - Square walls of BEDROCK confine the Nether to |x|,|z| <= R1_OUTER (Central + Ring1), regardless of ring masks.
     * - BEDROCK roof band around UNDERWORLD_ROOF_Y.
     * - Huge vaulted caverns above a lava ocean (lavaY), carved from NETHERRACK with multi-scale pseudo-3D noise.
     * - Ceiling glowstone clusters, occasional lava drips, and NETHER_QUARTZ_ORE veins.
     * - Exactly 5 global ominous passages that drill through the roof and down to near the lava, only over open lava.
     *
     * No structures/mobs here; terrain only.
     */
    private void generateColossalCaveNether(World world,
                                            ChunkGenerator.ChunkData data,
                                            long seed,
                                            int chunkX,
                                            int chunkZ) {
        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight() - 1;

        // Clamp roof/lava to world limits, preserving your constants
        final int unclampedRoof = UNDERWORLD_ROOF_Y;     // requested roof pivot
        final int roofY = Math.max(minY + 8, Math.min(unclampedRoof, maxY - 16));

        // Place lava sea far below; clamp if world bottom is higher
        int targetLavaY = -180;                          // desired lava level
        if (targetLavaY < minY + 3) targetLavaY = minY + 3;
        if (targetLavaY > roofY - 40) targetLavaY = roofY - 40;
        final int lavaY = targetLavaY;

        // Square boundary: Central + Ring1 extent, made square
        final int HALF = (int)Math.ceil(R1_OUTER);       // 520 by your constants
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;

        // Quick column masks: inside vs outside the square
        final boolean[][] inside = new boolean[16][16];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = baseX + lx - CENTER_X;
                int wz = baseZ + lz - CENTER_Z;
                inside[lx][lz] = (Math.abs(wx) <= HALF) && (Math.abs(wz) <= HALF);
            }
        }

        // 0) Outside the square -> solid vertical BEDROCK wall from bottom to just below roof
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (!inside[lx][lz]) {
                    data.setRegion(lx, minY, lz, lx + 1, Math.min(roofY + 6, maxY + 1), lz + 1, Material.BEDROCK);
                }
            }
        }

        // 1) Roof band (BEDROCK) around roofY for all columns (inside only)
        //    Small randomized thickness for texture, but deterministic per column.
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (!inside[lx][lz]) continue;
                int t = 1 + (int)(rand01(hash(seed, baseX + lx, roofY, baseZ + lz, 0xB0A7L)) * 4.0); // 1..4
                int y0 = Math.max(minY, roofY - t + 1);
                int y1 = Math.min(maxY, roofY);
                if (y1 >= y0) data.setRegion(lx, y0, lz, lx + 1, y1 + 1, lz + 1, Material.BEDROCK);
            }
        }

        // 2) Prefill interior volume (lavaY+1 .. roofY-1) with NETHERRACK for inside columns
        final int fillBottom = lavaY + 1;
        final int fillTop = Math.max(fillBottom, roofY - 1);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (!inside[lx][lz]) continue;
                if (fillTop >= fillBottom) {
                    data.setRegion(lx, fillBottom, lz, lx + 1, fillTop + 1, lz + 1, Material.NETHERRACK);
                }
            }
        }

        carveNetherCavesLikeV2(world, data, seed, chunkX, chunkZ, inside, lavaY, roofY, fillBottom, fillTop);


        // 4) Lava ocean: fill any AIR below/at lavaY with LAVA
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (!inside[lx][lz]) continue;
                for (int y = minY; y <= Math.min(lavaY, maxY); y++) {
                    if (data.getType(lx, y, lz) == Material.AIR) {
                        data.setBlock(lx, y, lz, Material.LAVA);
                    }
                }
            }
        }

        // 5) Clean up tiny floaters (remove isolated 1-block netherrack nuggets)
        for (int lx = 1; lx < 15; lx++) {
            for (int lz = 1; lz < 15; lz++) {
                if (!inside[lx][lz]) continue;
                for (int y = Math.min(fillTop - 1, roofY - 2); y >= fillBottom + 1; y--) {
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

        // 6) Ceiling glowstone clusters (1–2 per chunk), anchored under solid roof/ceiling
        {
            java.util.SplittableRandom rng = rngFor(seed, chunkX, chunkZ, SALT_GLOWSTONE);
            int starts = 1 + (rng.nextInt(3) == 0 ? 1 : 0);
            for (int s = 0; s < starts; s++) {
                for (int tries = 0; tries < 10; tries++) {
                    int lx = rng.nextInt(16), lz = rng.nextInt(16);
                    if (!inside[lx][lz]) continue;
                    int y = Math.max(minY, roofY - (2 + rng.nextInt(4)));
                    // Need: solid above, air at the anchor
                    if (y + 1 <= maxY && data.getType(lx, y + 1, lz) != Material.AIR && data.getType(lx, y, lz) == Material.AIR) {                        int size = 10 + rng.nextInt(12);
                        int placed = 0;
                        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                        q.add(new int[]{lx, y, lz});
                        while (!q.isEmpty() && placed < size) {
                            int[] p = q.removeFirst();
                            int x = p[0], yy = p[1], z = p[2];
                            if (x < 1 || x > 14 || z < 1 || z > 14 || yy < fillBottom || yy > roofY - 1) continue;
                            if (data.getType(x, yy, z) == Material.AIR && data.getType(x, yy + 1, z).isSolid()) {
                                data.setBlock(x, yy, z, Material.GLOWSTONE);
                                placed++;
                                // small tendrils downward
                                if (placed < size && yy - 1 >= fillBottom && data.getType(x, yy - 1, z) == Material.AIR) {
                                    data.setBlock(x, yy - 1, z, Material.GLOWSTONE);
                                    placed++;
                                }
                                // random walk
                                if (rng.nextBoolean()) q.add(new int[]{x + (rng.nextBoolean() ? 1 : -1), yy + (rng.nextInt(3) - 1), z});
                                if (rng.nextBoolean()) q.add(new int[]{x, yy + (rng.nextInt(3) - 1), z + (rng.nextBoolean() ? 1 : -1)});
                            }
                        }
                        break;
                    }
                }
            }
        }

        // 7) Sparse lava drips (thin falls) from high ceiling
        {
            java.util.SplittableRandom rng = rngFor(seed, chunkX, chunkZ, SALT_LAVA_DRIPS);
            if (rng.nextInt(3) == 0) {
                int lx = 1 + rng.nextInt(14), lz = 1 + rng.nextInt(14);
                if (inside[lx][lz]) {
                    int startY = roofY - 1 - rng.nextInt(3);
                    for (int y = startY; y >= lavaY; y--) {
                        if (data.getType(lx, y, lz) == Material.BEDROCK) break;
                        data.setBlock(lx, y, lz, Material.LAVA);
                    }
                }
            }
        }

        // 8) Quartz ore veins in NETHERRACK
        {
            java.util.SplittableRandom rng = rngFor(seed, chunkX, chunkZ, SALT_QUARTZ);
            int tries = 8 + rng.nextInt(5);
            for (int t = 0; t < tries; t++) {
                int lx = rng.nextInt(16), lz = rng.nextInt(16);
                if (!inside[lx][lz]) continue;
                int y = lavaY + 8 + rng.nextInt(Math.max(1, (roofY - lavaY - 24)));
                if (y < fillBottom || y > fillTop) continue;
                if (data.getType(lx, y, lz) != Material.NETHERRACK) continue;

                int size = 6 + rng.nextInt(8);
                java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                q.add(new int[]{lx, y, lz});
                int placed = 0;
                while (!q.isEmpty() && placed < size) {
                    int[] p = q.removeFirst();
                    int x = p[0], yy = p[1], z = p[2];
                    if (x < 1 || x > 14 || z < 1 || z > 14 || yy <= lavaY + 1 || yy >= roofY - 2) continue;
                    if (data.getType(x, yy, z) == Material.NETHERRACK) {
                        data.setBlock(x, yy, z, Material.NETHER_QUARTZ_ORE);
                        placed++;
                        if (rng.nextBoolean()) q.add(new int[]{x + (rng.nextBoolean() ? 1 : -1), yy + (rng.nextInt(3) - 1), z});
                        if (rng.nextBoolean()) q.add(new int[]{x, yy + (rng.nextInt(3) - 1), z + (rng.nextBoolean() ? 1 : -1)});
                    }
                }
            }
        }

        // 9) Five ominous global passages: carve vertical shafts through roof + cavern down toward lava.
        //    They only exist where the column is over open lava (predicted by the same density/threshold at lavaY).
        {
            P2i[] centers = computeGlobalPassageCenters(seed, HALF);
            for (P2i c : centers) {
                // Does this chunk contain the passage center?
                if (c.x >= baseX && c.x <= baseX + 15 && c.z >= baseZ && c.z <= baseZ + 15) {
                    int lx = c.x - baseX, lz = c.z - baseZ;
                    if (!inside[lx][lz]) continue;

                    // Validate "over lava ocean": check predicted air at lavaY
                    if (!isLavaOpenAt(seed, c.x, c.z, lavaY, roofY)) continue;

                    // Carve a 3x3 ominous bore (cross-shaped) from above the roof down to just above lava
                    int top = Math.min(roofY + 4, maxY - 1);
                    int bottom = Math.max(lavaY + 2, minY + 2);
                    for (int y = top; y >= bottom; y--) {
                        // Cross shape (plus) gives a menacing vertical drill
                        carveAirIfInside(data, lx, y, lz);
                        carveAirIfInside(data, lx + 1, y, lz);
                        carveAirIfInside(data, lx - 1, y, lz);
                        carveAirIfInside(data, lx, y, lz + 1);
                        carveAirIfInside(data, lx, y, lz - 1);
                        // Also remove any bedrock encountered to truly "drill through"
                        data.setBlock(lx, y, lz, Material.AIR);
                    }
                }
            }
        }

        // 10) Bedrock wall thickening exactly on the square border inside the square (1-block face)
        //     This makes the boundary read clearly when you approach it from the inside.
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = baseX + lx - CENTER_X;
                int wz = baseZ + lz - CENTER_Z;
                boolean onBorder = inside[lx][lz] && (Math.abs(wx) == HALF || Math.abs(wz) == HALF);
                if (!onBorder) continue;
                data.setRegion(lx, Math.max(minY, lavaY), lz,
                        lx + 1, Math.min(roofY + 6, maxY + 1), lz + 1,
                        Material.BEDROCK);
            }
        }
    }

// --- Small helpers for the Nether ---

    // Predict whether the cavern logic would be "open" (air) at lavaY for this column.
// We reuse the same density + threshold model used in carving to keep behavior consistent.
    private boolean isLavaOpenAt(long seed, int wx, int wz, int lavaY, int roofY) {
        double warp = fbm2(seed ^ 0x51D2A1L, wx / 96.0, wz / 96.0) * 24.0;
        int y = lavaY;

        double d1 = fbm2(seed ^ 0xC0FEF1L, (wx + y * 0.80) / 64.0, (wz + y * 0.80) / 64.0);
        double d2 = fbm2(seed ^ 0xC0FEF2L, (wx - y * 1.20) / 24.0, (wz + y * 1.20) / 24.0) * 0.6;
        double d3 = fbm2(seed ^ 0xC0FEF3L, (wx + warp + y * 0.60) / 96.0, (wz + warp - y * 0.60) / 96.0) * 0.2;

        double density = (d1 + d2 + d3) * 2.0 - 1.0;
        double roofBias = Math.max(0.0, (roofY - y) / 28.0);
        double threshold = 0.22 - roofBias * 0.10;
        return density > threshold; // would be carved to air -> lava ocean occupies it
    }

    // Exactly five global passage centers, deterministically from seed.
// We sample within the square (HALF margin), keep them fairly separated, and require "over lava".
    private P2i[] computeGlobalPassageCenters(long seed, int HALF) {
        java.util.ArrayList<P2i> picks = new java.util.ArrayList<>(5);
        java.util.SplittableRandom rng = new java.util.SplittableRandom(hash(seed, 777, 0, 0, SALT_PASSAGES));
        final int margin = 96;                   // keep away from walls
        final int spread2 = 160 * 160;          // min separation^2

        // We don't know lavaY/roofY here, but we will re-check validity at carve time.
        // Use a loose position pre-filter (noise band) to bias toward ocean-y places.
        int tries = 0;
        while (picks.size() < 5 && tries < 2000) {
            tries++;
            int wx = (rng.nextInt(HALF - margin) + margin) * (rng.nextBoolean() ? 1 : -1);
            int wz = (rng.nextInt(HALF - margin) + margin) * (rng.nextBoolean() ? 1 : -1);

            // Bias toward lower-frequency basins
            double basin = fbm2(seed ^ SALT_BASIN_PREF, wx / 480.0, wz / 480.0);
            if (basin < 0.45) continue;

            boolean ok = true;
            for (P2i p : picks) {
                int dx = wx - p.x, dz = wz - p.z;
                if (dx * dx + dz * dz < spread2) { ok = false; break; }
            }
            if (!ok) continue;
            picks.add(new P2i(wx + CENTER_X, wz + CENTER_Z));
        }

        // Fallback fill if something went wrong
        while (picks.size() < 5) {
            picks.add(new P2i(CENTER_X, CENTER_Z));
        }
        return picks.toArray(new P2i[0]);
    }

    private void carveAirIfInside(ChunkGenerator.ChunkData data, int lx, int y, int lz) {
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
            data.setBlock(lx, y, lz, Material.AIR);
        }
    }

    // Simple int pair (world-space)
    private static final class P2i {
        final int x, z;
        P2i(int x, int z) { this.x = x; this.z = z; }
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
            // Only place sand on the base; mountain tops remain terracotta
            if (y == topY) {
                int MESA_MTN_THRESHOLD = 178; // treat higher columns as mountains
                if (topY <= MESA_MTN_THRESHOLD) {
                    m = Material.RED_SAND;
                }
            }
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
        // Only generate sand on base; on higher columns keep terracotta top
        if (grassTop) {
            data.setBlock(lx, topY, lz, Material.GRASS_BLOCK);
        } else {
            int MESA_MTN_THRESHOLD = 178;
            if (topY > MESA_MTN_THRESHOLD) {
                int band = Math.floorMod(topY, 7);
                Material m = switch (band) {
                    case 0 -> Material.TERRACOTTA;
                    case 1 -> Material.RED_TERRACOTTA;
                    case 2 -> Material.ORANGE_TERRACOTTA;
                    case 3 -> Material.BROWN_TERRACOTTA;
                    case 4 -> Material.YELLOW_TERRACOTTA;
                    case 5 -> Material.WHITE_TERRACOTTA;
                    default -> Material.LIGHT_GRAY_TERRACOTTA;
                };
                data.setBlock(lx, topY, lz, m);
            } else {
                data.setBlock(lx, topY, lz, Material.RED_SAND);
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
                    W_GOLD_R = 28; // doubled gold spawn rate in Mesa
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
                    W_EMERALD_R = 8; // changed to 8
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
            this.oreAttemptsLocal(world, data, rng, chunkX, chunkZ, Material.EMERALD_ORE, W_EMERALD_R, 170, 420, Bias.TOP, 320, deepDesert);
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
            if (isObsidian) {
                // Preserve existing obsidian behavior: sparse, independent 5% checks per position
                double p = 0.05;
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
            } else {
                // Ores: 2x2x2 cluster with 4 guaranteed blocks, plus up to 4 more at 20% each
                double p = 0.2;

                // Shuffle the 8 indices so placement varies within the 2x2x2 cube
                int[] order = new int[8];
                for (i = 0; i < 8; ++i) order[i] = i;
                for (int j = 7; j > 0; --j) {
                    int k = local.nextInt(j + 1);
                    int t = order[j];
                    order[j] = order[k];
                    order[k] = t;
                }

                // Base of 4 blocks, plus up to 4 extras with chance p each (independent)
                int extras = 0;
                for (int e = 0; e < 4; ++e) {
                    if (local.nextDouble() < p) ++extras;
                }
                int total = 4 + extras; // 4..8

                for (i = 0; i < total; ++i) {
                    int idx = order[i];
                    int lx = ax + OFFS[idx * 3];
                    int ly = ay + OFFS[idx * 3 + 1];
                    int lz = az + OFFS[idx * 3 + 2];
                    if (ly < yMin || ly > yMax || data.getType(lx, ly, lz) != Material.STONE) continue; // replace stone only
                    data.setBlock(lx, ly, lz, ore);
                }
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

    private void paintLandCap(ChunkGenerator.ChunkData data, int lx, int lz, int topY) {
        int y2 = topY;
        int y1 = Math.max(-60, topY - 3);
        if (y2 > y1) {
            data.setRegion(lx, y1, lz, lx + 1, y2, lz + 1, Material.DIRT);
        }
        data.setBlock(lx, topY, lz, Material.GRASS_BLOCK);
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
        SplittableRandom rng = rngFor(seed, chunkX, chunkZ, 30323687L);
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

        // 2) Place occasional holes in the roof under swamp
        for (int tries = 0; tries < 6; tries++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            if (regionGrid[lx][lz] != ConsegrityRegions.Region.SWAMP) continue;
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
                    // Make it easier to open near the lava so caves can reach the ocean
                    if (y <= lavaY + 6) threshold -= 0.06; else if (y <= lavaY + 10) threshold -= 0.03;

                    // Random size variation per-column: half-size or double-size tunnels
                    double v = valueNoise2(seed ^ 0x55AA7711L, wx / 160.0, wz / 160.0);
                    double scale = (v < 0.33) ? 0.5 : ((v > 0.66) ? 2.0 : 1.0);

                    if (d * scale > threshold) {
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
            int steps = (10 + rng.nextInt(16)) * 3; // triple cluster size
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

        // 9) Sparse fire near lava edges only (once every ~4 chunks)
        if (rng.nextInt(4) == 0) {
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

        // 10b) Ominous passages: up to 5 per world; drill where near-overworld cave is within 50 blocks
        // (approximate locally using current chunk data). Ensure min spacing of 50 blocks and at least
        // one per chunk that contains a center.
        {
            // Use chunk-local RNG salt to pick tentative centers; keep small and deterministic.
            java.util.ArrayList<int[]> placed = new java.util.ArrayList<>();
            int attempts = 6;
            for (int a = 0; a < attempts && placed.size() < 5; a++) {
                int lx = 2 + rng.nextInt(13);
                int lz = 2 + rng.nextInt(13);
                int wx = baseX + lx, wz = baseZ + lz;

                // Min spacing check (>= 50 blocks)
                boolean ok = true;
                for (int[] p : placed) {
                    int dx = wx - p[0], dz = wz - p[1];
                    if (dx * dx + dz * dz < 50 * 50) { ok = false; break; }
                }
                if (!ok) continue;

                // Overworld cave bottom near the roof (within 50 blocks)
                int overBottom = -1;
                for (int y = Math.min(roofY + 49, maxY - 1); y >= Math.min(roofY + 2, maxY - 1); y--) {
                    Material m = data.getType(lx, y, lz);
                    Material below = (y - 1 >= minY) ? data.getType(lx, y - 1, lz) : Material.BEDROCK;
                    if (m == Material.AIR && below != Material.AIR) { overBottom = y; break; }
                }
                if (overBottom == -1) continue;

                // Nether cave roof right below the roof
                int underTop = -1;
                for (int y = Math.min(roofY - 1, maxY - 1); y >= Math.max(lavaY + 2, minY + 2); y--) {
                    Material m = data.getType(lx, y, lz);
                    Material above = (y + 1 <= maxY) ? data.getType(lx, y + 1, lz) : Material.BEDROCK;
                    if (m == Material.AIR && above != Material.AIR) { underTop = y; break; }
                }
                if (underTop == -1) continue;

                if (overBottom - underTop >= 50) continue;

                // Carve ominous vertical bore (cross shape) through roof down close to lava
                int top = Math.min(roofY + 4, maxY - 1);
                int bottom = Math.max(lavaY + 2, minY + 2);
                for (int y = top; y >= bottom; y--) {
                    if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) data.setBlock(lx, y, lz, Material.AIR);
                    if (lx + 1 < 16) data.setBlock(lx + 1, y, lz, Material.AIR);
                    if (lx - 1 >= 0) data.setBlock(lx - 1, y, lz, Material.AIR);
                    if (lz + 1 < 16) data.setBlock(lx, y, lz + 1, Material.AIR);
                    if (lz - 1 >= 0) data.setBlock(lx, y, lz - 1, Material.AIR);
                }
                placed.add(new int[]{wx, wz});
            }
            // Enforce at least one if none placed
            if (placed.isEmpty()) {
                int lx = 8, lz = 8;
                int top = Math.min(roofY + 4, maxY - 1);
                int bottom = Math.max(lavaY + 2, minY + 2);
                for (int y = top; y >= bottom; y--) {
                    data.setBlock(lx, y, lz, Material.AIR);
                    if (lx + 1 < 16) data.setBlock(lx + 1, y, lz, Material.AIR);
                    if (lx - 1 >= 0) data.setBlock(lx - 1, y, lz, Material.AIR);
                    if (lz + 1 < 16) data.setBlock(lx, y, lz + 1, Material.AIR);
                    if (lz - 1 >= 0) data.setBlock(lx, y, lz - 1, Material.AIR);
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

        // 12) Mushrooms on netherrack at similar rate to surface flowers
        for (int t = 0; t < 60; t++) {
            int lx = rng.nextInt(16), lz = rng.nextInt(16);
            int y = lavaY + 2 + rng.nextInt(Math.max(1, (roofY - (lavaY + 4))));
            if (y < minY + 1 || y > maxY - 1) continue;
            Material ground = data.getType(lx, y, lz);
            if (ground != Material.NETHERRACK) continue;
            if (data.getType(lx, y + 1, lz) != Material.AIR) continue;
            if (rng.nextDouble() < 0.08) {
                data.setBlock(lx, y + 1, lz, rng.nextBoolean() ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM);
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

    private double angle01Warped(long seed, int wx, int wz) {
        double ang = Math.atan2(wz - 0, wx - 0);
        double n1 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xA11C1EEDL, (double)wx / 180.0, (double)wz / 180.0);
        double n2 = ConsegrityChunkGenerator.valueNoise2(seed ^ 0xB22C2EEDL, (double)wx / 64.0, (double)wz / 64.0);
        double jitter = (n1 * 0.65 + n2 * 0.35 - 0.5) * 2.0 * 0.35;
        return ((ang += jitter * 0.3490658503988659) + Math.PI) / (Math.PI * 2);
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

    private static double bandMask(double r, double a, double b, double feather) {
        double aa = ConsegrityChunkGenerator.clamp01((r - (a - feather * 0.5)) / feather);
        double bb = 1.0 - ConsegrityChunkGenerator.clamp01((r - (b - feather * 0.5)) / feather);
        return ConsegrityChunkGenerator.clamp01(Math.min(aa, bb));
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
    private void carveNetherCavesLikeV2(
            World world,
            ChunkGenerator.ChunkData data,
            long seed,
            int chunkX, int chunkZ,
            boolean[][] inside,         // 16x16 mask for the square nether
            int lavaY, int roofY,
            int fillBottom, int fillTop) {

        final int baseX = (chunkX << 4);
        final int baseZ = (chunkZ << 4);
        final int minY  = Math.max(world.getMinHeight(), lavaY + 1);
        final int maxY  = Math.min(world.getMaxHeight() - 1, roofY - 1);

        // How far a single ellipsoid might spill out of its center
        final int RMAX = Math.max(NETHER_R_MAX, NETHER_ROOM_MAX_R);

        // Iterate over coarse cells that could affect this chunk (with margin for RMAX + steps)
        int gx0 = floorDiv(baseX - RMAX - NETHER_WORM_STEPS * NETHER_WORM_STEP, NETHER_CELL);
        int gz0 = floorDiv(baseZ - RMAX - NETHER_WORM_STEPS * NETHER_WORM_STEP, NETHER_CELL);
        int gx1 = floorDiv(baseX + 15 + RMAX + NETHER_WORM_STEPS * NETHER_WORM_STEP, NETHER_CELL);
        int gz1 = floorDiv(baseZ + 15 + RMAX + NETHER_WORM_STEPS * NETHER_WORM_STEP, NETHER_CELL);

        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                long cellSalt = hash(seed, gx, 0L, gz, 0x4E77C4A7L);
                SplittableRandom rng = new SplittableRandom(cellSalt);

                // 1) Big rooms
                if (rng.nextDouble() < NETHER_ROOM_PROB) {
                    int cx = gx * NETHER_CELL + rng.nextInt(NETHER_CELL);
                    int cz = gz * NETHER_CELL + rng.nextInt(NETHER_CELL);
                    int cy = lerpInt(lavaY + 28, roofY - 10, rng.nextDouble()); // float above lava, under roof
                    int r  = rng.nextInt(NETHER_ROOM_MIN_R, NETHER_ROOM_MAX_R + 1);
                    carveEllipsoidSpillToThisChunk(data, baseX, baseZ, inside, cx, cy, cz, r, (int)(r * 0.75), r, minY, maxY);
                }

                // 2) Long meandering worms (like V2 but scaled up)
                if (rng.nextDouble() < NETHER_WORM_PROB) {
                    // Start in this cell
                    double x = gx * (double)NETHER_CELL + rng.nextInt(NETHER_CELL);
                    double z = gz * (double)NETHER_CELL + rng.nextInt(NETHER_CELL);
                    double y = lerp(lavaY + 34, roofY - 16, rng.nextDouble());

                    // Initial heading; pitch near-horizontal so we get cavelike tunnels
                    double yaw   = rng.nextDouble() * Math.PI * 2.0;
                    double pitch = (rng.nextDouble() - 0.5) * 0.35;

                    // Noise to slowly bend the path
                    long nSeed1 = cellSalt ^ 0x7F4A12B3L;
                    long nSeed2 = cellSalt ^ 0xB3C18E5DL;

                    for (int step = 0; step < NETHER_WORM_STEPS; step++) {
                        int r  = rng.nextInt(NETHER_R_MIN, NETHER_R_MAX + 1);
                        int ry = Math.max(8, (int)(r * 0.65));  // a bit squashed vertically
                        carveEllipsoidSpillToThisChunk(
                                data, baseX, baseZ, inside,
                                (int)Math.round(x), (int)Math.round(y), (int)Math.round(z),
                                r, ry, r, minY, maxY);

                        // Advance with gentle bends
                        double bendYaw   = (valueNoise2(nSeed1, x / 180.0, z / 180.0) - 0.5) * 0.25;
                        double bendPitch = (valueNoise2(nSeed2, z / 220.0, x / 220.0) - 0.5) * 0.18;
                        yaw   += bendYaw;
                        pitch = clamp(pitch + bendPitch, -0.55, 0.55);

                        x += Math.cos(yaw) * Math.cos(pitch) * NETHER_WORM_STEP;
                        z += Math.sin(yaw) * Math.cos(pitch) * NETHER_WORM_STEP;
                        y += Math.sin(pitch) * NETHER_WORM_STEP;

                        // Keep in vertical band, reflect softly
                        if (y < lavaY + 20) { y = lavaY + 20 + (lavaY + 20 - y) * 0.5; pitch = Math.abs(pitch); }
                        if (y > roofY - 12) { y = roofY - 12 - (y - (roofY - 12)) * 0.5; pitch = -Math.abs(pitch); }
                    }
                }
            }
        }
    }

    private void carveEllipsoidSpillToThisChunk(
            ChunkGenerator.ChunkData data,
            int baseX, int baseZ, boolean[][] inside,
            int cx, int cy, int cz,
            int rx, int ry, int rz,
            int minY, int maxY) {

        // Tight AABB intersect with this chunk
        int minX = Math.max(baseX,            cx - rx);
        int maxX = Math.min(baseX + 15,       cx + rx);
        int minZ = Math.max(baseZ,            cz - rz);
        int maxZ = Math.min(baseZ + 15,       cz + rz);
        if (minX > maxX || minZ > maxZ) return;

        int rx2 = rx * rx, ry2 = ry * ry, rz2 = rz * rz;

        for (int wx = minX; wx <= maxX; wx++) {
            int lx = wx - baseX;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int lz = wz - baseZ;
                if (!inside[lx][lz]) continue; // preserve square boundary

                int dx2 = (wx - cx); dx2 *= dx2;
                int dz2 = (wz - cz); dz2 *= dz2;

                // Project horizontal distance first; if already outside, skip entire column
                // using minimal y so we avoid per-y checks when not needed
                if ((double)dx2 / rx2 + (double)dz2 / rz2 > 1.0) continue;

                int y0 = Math.max(minY, cy - ry);
                int y1 = Math.min(maxY, cy + ry);
                for (int y = y0; y <= y1; y++) {
                    int dy2 = (y - cy); dy2 *= dy2;
                    // inside ellipsoid?
                    if ((double)dx2 / rx2 + (double)dy2 / ry2 + (double)dz2 / rz2 <= 1.0) {
                        if (data.getType(lx, y, lz) != Material.BEDROCK) {
                            data.setBlock(lx, y, lz, Material.AIR);
                        }
                    }
                }
            }
        }
    }

    // helpers
    private static int floorDiv(int a, int b) {
        int q = a / b;
        if ((a ^ b) < 0 && q * b != a) q--;
        return q;
    }
    private static int lerpInt(int a, int b, double t) { return a + (int)Math.round((b - a) * t); }
    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
