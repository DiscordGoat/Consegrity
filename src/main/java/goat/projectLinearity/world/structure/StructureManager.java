package goat.projectLinearity.world.structure;

import goat.projectLinearity.libs.SchemManager;
import goat.projectLinearity.world.ConsegrityRegions;
import goat.projectLinearity.world.sector.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages schematic-based structures with world-aware generation checks.
 * Registrations define desired count, spacing, sector (wedge), and placement type.
 * Actual placement occurs via DeferredStructureSpawner after chunk generation.
 */
public final class StructureManager {
    private final JavaPlugin plugin;
    private final SchemManager schemManager;

    private final List<Registration> registrations = new ArrayList<>();
    // Debugging counters (opt-in)
    private volatile boolean debugEnabled = false;
    private final Map<String, DebugCounters> debug = new ConcurrentHashMap<>();

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemManager = new SchemManager(plugin);
    }

    public JavaPlugin getPlugin() { return plugin; }

    public List<Registration> getRegistrations() { return registrations; }

    public void setDebug(boolean on) { this.debugEnabled = on; }
    public boolean isDebug() { return debugEnabled; }
    public void clearDebug() { debug.clear(); }
    private DebugCounters dbg(String regName) { return debug.computeIfAbsent(regName, k -> new DebugCounters()); }
    public String debugSummary(String worldKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Consegrity] Structure debug summary\n");
        for (Registration r : registrations) {
            DebugCounters c = debug.get(r.schemName);
            int have = 0;
            try { have = r.placedCount(worldKey); } catch (Throwable ignored) {}
            if (c == null) {
                sb.append("- ").append(r.schemName).append(" (have ").append(have).append("/ ").append(r.count).append(") no attempts recorded\n");
                continue;
            }
            sb.append("- ").append(r.schemName).append(" have ").append(have).append("/").append(r.count)
              .append(", attempts=").append(c.attempts).append(", successes=").append(c.successes)
              .append(", spacingFail=").append(c.get("SPACING"))
              .append(", regionFail=").append(c.get("REGION_FILTER"))
              .append(", minDistFail=").append(c.get("MIN_DISTANCE"))
              .append(", pickFail=").append(c.get("PICK_FAILED"))
              .append("\n");
            for (Map.Entry<String, Long> e : c.reasons.entrySet()) {
                String k = e.getKey();
                if (k.equals("SPACING") || k.equals("REGION_FILTER") || k.equals("MIN_DISTANCE") || k.equals("PICK_FAILED")) continue;
                sb.append("    ").append(k).append("=").append(e.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Enforce configured counts for all registered structures by preplanning and placing immediately.
     * If spacing prevents reaching the target, gradually relax spacing until the target is satisfied.
     */
    public void enforceCountsInstant(World world) {
        if (world == null) return;
        String worldKey = world.getUID().toString();
        Random rng = new Random(world.getSeed() ^ 0xCAFEBABECAFEL);

        StructureStore store = StructureStore.get(plugin);
        for (Registration reg : registrations) {
            int have;
            if (reg.triggerListener) {
                try { have = store.getPlacedCountForName(worldKey, reg.schemName); }
                catch (Throwable t) { have = reg.placedCount(worldKey); }
            } else {
                have = reg.placedCount(worldKey);
            }
            int need = Math.max(0, reg.count - have);
            if (need <= 0) continue;

            int initialSpacing = Math.max(reg.bounds + 2, reg.spacing);
            int minSpacing = Math.max(reg.bounds + 2, 8);
            // progressively relax spacing factors
            double[] factors = new double[]{1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3};

            for (double f : factors) {
                if (need <= 0) break;
                int useSpacing = Math.max(minSpacing, (int) Math.round(initialSpacing * f));

                // attempt many candidates spread across a large radius around origin
                int attempts = Math.min(4000, Math.max(need * 300, 1000));
                int radius = 1100; // covers all ringed regions defined by the generator

                for (int a = 0; a < attempts && need > 0; a++) {
                    int wx = rng.nextInt(radius * 2 + 1) - radius;
                    int wz = rng.nextInt(radius * 2 + 1) - radius;
                    if (!reg.allowsAt(world, wx, wz)) continue;
                    if (tryPlaceAtCandidate(world, reg, wx, wz, useSpacing, rng)) {
                        need--;
                    }
                }
            }

            if (need > 0) {
                plugin.getLogger().warning("Could not fully satisfy count for '" + reg.schemName + "'. Missing: " + need + ". Consider reducing spacing or expanding region.");
            }
        }
    }


    private boolean tryPlaceAtCandidate(World world, Registration reg, int wx, int wz, int spacingOverride, Random rng) {
        String worldKey = world.getUID().toString();
        if (debugEnabled) dbg(reg.schemName).attempts++;
        if (!reg.isSpaced(worldKey, wx, wz, spacingOverride)) {
            if (debugEnabled) dbg(reg.schemName).inc("SPACING");
            return false;
        }

        // Enforce minimum radial distance from origin if configured
        if (reg.minimumDistance > 0) {
            long r2 = (long) wx * (long) wx + (long) wz * (long) wz;
            long min2 = (long) reg.minimumDistance * (long) reg.minimumDistance;
            if (r2 < min2) {
                if (debugEnabled) dbg(reg.schemName).inc("MIN_DISTANCE");
                return false;
            }
        }

        Location paste = pickPlacement(world, wx, wz, reg, rng);
        if (paste == null) {
            if (debugEnabled) {
                String reason = detailPickFail(world, reg, wx, wz, rng);
                dbg(reg.schemName).inc(reason == null ? "PICK_FAILED" : reason);
            }
            return false;
        }

        // Ensure target chunk is loaded before paste
        try {
            int cx = paste.getBlockX() >> 4;
            int cz = paste.getBlockZ() >> 4;
            world.getChunkAt(cx, cz).load(true);
        } catch (Throwable ignore) {}

        boolean ignoreAir = (reg.type == GenCheckType.UNDERWATER);
        try {
            schemManager.placeStructure(reg.schemName, paste, ignoreAir);
            // Surface-only: convert top layer of stone/dirt under the structure bounds to grass
            if (reg.type == GenCheckType.SURFACE) {
                try {
                    grassifyTopLayerUnderBounds(world, paste.getBlockX(), paste.getBlockZ(), paste.getBlockY() - 1, reg.bounds);
                } catch (Throwable ignored) {}
            }
            reg.recordPlacement(worldKey, wx, wz);
            if (reg.triggerListener) {
                StructureStore.get(plugin).addStructure(worldKey, reg.schemName, paste.getBlockX(), paste.getBlockY(), paste.getBlockZ(), reg.bounds, true);
            }
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed placing '" + reg.schemName + "' at " + paste + ": " + t.getMessage());
            if (debugEnabled) dbg(reg.schemName).inc("PLACE_ERROR:" + t.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Register a schematic to spawn with constraints.
     *
     * @param schem    The schematic filename or base name ("foo" or "foo.schem").
     * @param bounds   Structure width/depth in blocks (e.g., 10 or 20).
     * @param count    Target number of structures to place globally per world.
     * @param spacing  Minimum planar distance between two placements of this struct.
     * @param sector   Sector wedge allowed for placement (e.g., new JungleSector()).
     * @param type     Placement type (surface, underwater, etc.).
     */
    public void registerStruct(String schem, int bounds, int count, int spacing, Sector sector, GenCheckType type) {
        registerStruct(schem, bounds, count, spacing, sector, type, false, 0);
    }


    // Overload: include listener trigger and minimum distance from origin (blocks)
    public void registerStruct(String schem, int bounds, int count, int spacing, Sector sector, GenCheckType type, boolean triggerListener, int minimumDistance) {
        String base = normalizeName(schem);
        registrations.add(new Registration(base, bounds, count, spacing, sector, type, triggerListener, Math.max(0, minimumDistance)));
        plugin.getLogger().info("Registered structure '" + base + "' (count=" + count + ", spacing=" + spacing + ", bounds=" + bounds + ", type=" + type + ", trigger=" + triggerListener + ", minDist=" + minimumDistance + ")");
    }

    // Convenience overload for minimumDistance without triggerListener
    public void registerStruct(String schem, int bounds, int count, int spacing, Sector sector, GenCheckType type, int minimumDistance) {
        registerStruct(schem, bounds, count, spacing, sector, type, false, minimumDistance);
    }

    private static String normalizeName(String name) {
        if (name == null) return "";
        if (name.endsWith(".schem")) return name.substring(0, name.length() - 6);
        if (name.endsWith(".schematic")) return name.substring(0, name.length() - 10);
        return name;
    }

    // --- Placement engine (invoked by DeferredStructureSpawner) ---

    boolean tryPlaceOne(World world, int chunkX, int chunkZ, Random rng) {
        boolean placedAny = false;
        String worldKey = world.getUID().toString();
        for (Registration reg : registrations) {
            if (reg.placedCount(worldKey) >= reg.count) continue;
            // try a few attempts in this chunk
            for (int a = 0; a < 5; a++) {
                int lx = rng.nextInt(16);
                int lz = rng.nextInt(16);
                int wx = (chunkX << 4) + lx;
                int wz = (chunkZ << 4) + lz;
                if (debugEnabled) dbg(reg.schemName).attempts++;
                if (!reg.allowsAt(world, wx, wz)) {
                    if (debugEnabled) dbg(reg.schemName).inc(detailRegionFail(world, reg, wx, wz));
                    continue;
                }

                if (reg.minimumDistance > 0) {
                    long r2 = (long) wx * (long) wx + (long) wz * (long) wz;
                    long min2 = (long) reg.minimumDistance * (long) reg.minimumDistance;
                    if (r2 < min2) {
                        if (debugEnabled) dbg(reg.schemName).inc("MIN_DISTANCE");
                        continue;
                    }
                }

                Location paste = pickPlacement(world, wx, wz, reg, rng);
                if (paste == null) {
                    if (debugEnabled) {
                        String reason = detailPickFail(world, reg, wx, wz, rng);
                        dbg(reg.schemName).inc(reason == null ? "PICK_FAILED" : reason);
                    }
                    continue;
                }

                if (!reg.isSpaced(worldKey, wx, wz)) {
                    if (debugEnabled) dbg(reg.schemName).inc("SPACING");
                    continue;
                }

                boolean ignoreAir = (reg.type == GenCheckType.UNDERWATER);
                try {
                    schemManager.placeStructure(reg.schemName, paste, ignoreAir);
                    if (debugEnabled) dbg(reg.schemName).successes++;
                    if (reg.type == GenCheckType.SURFACE) {
                        try {
                            grassifyTopLayerUnderBounds(world, paste.getBlockX(), paste.getBlockZ(), paste.getBlockY() - 1, reg.bounds);
                        } catch (Throwable ignored) {}
                    }
                    reg.recordPlacement(worldKey, wx, wz);
                    // Persist structure instance for listener tracking
                    if (reg.triggerListener) {
                        StructureStore.get(plugin).addStructure(worldKey, reg.schemName, paste.getBlockX(), paste.getBlockY(), paste.getBlockZ(), reg.bounds, true);
                    }
                    placedAny = true;
                    break; // one per registration per chunk attempt
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed placing '" + reg.schemName + "' at " + paste + ": " + t.getMessage());
                    if (debugEnabled) dbg(reg.schemName).inc("PLACE_ERROR:" + t.getClass().getSimpleName());
                }
            }
        }
        return placedAny;
    }

    private String detailRegionFail(World world, Registration reg, int wx, int wz) {
        try {
            ConsegrityRegions.Region here = (reg.type == GenCheckType.HELL) ? ConsegrityRegions.regionAt(world, wx, -80, wz) : ConsegrityRegions.regionAt(world, wx, wz);
            if (reg.region != null && here != reg.region) return "REGION_MISMATCH:" + here;
            if (reg.type == GenCheckType.SURFACE && reg.region == null && here == ConsegrityRegions.Region.MOUNTAIN) return "SURFACE_AVOID_MOUNTAIN";
        } catch (Throwable ignored) {}
        return "REGION_FILTER";
    }

    private String detailPickFail(World world, Registration reg, int wx, int wz, Random rng) {
        try {
            switch (reg.type) {
                case SURFACE:
                    return debugReasonSurface(world, wx, wz, reg.bounds, false);
                case UNDERWATER:
                    return debugReasonUnderwater(world, wx, wz, reg.bounds);
                case UNDERGROUND:
                    return debugReasonUnderground(world, wx, wz, reg.bounds, rng);
                case SKYISLAND:
                    return debugReasonSkyIsland(world, wx, wz, reg.bounds, rng);
                case HELL:
                    return debugReasonNether(world, wx, wz, reg.bounds, rng);
                default:
                    return "PICK_FAILED";
            }
        } catch (Throwable ignored) {
            return "PICK_FAILED";
        }
    }

    private String debugReasonSurface(World world, int wx, int wz, int bounds, boolean allowWater) {
        int half = Math.max(1, bounds / 2);
        int[] xs = new int[] { wx - half, wx + half, wx - half, wx + half, wx };
        int[] zs = new int[] { wz - half, wz - half, wz + half, wz + half, wz };
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            int y = world.getHighestBlockYAt(x, z);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (!allowWater && isWater(ground)) return "WATER";
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (maxY - minY > 2) return "SLOPE";
        int yCenter = world.getHighestBlockYAt(wx, wz);
        Material above = world.getBlockAt(wx, yCenter + 1, wz).getType();
        if (above != Material.AIR) return "OCCUPIED";
        return null;
    }

    private String debugReasonUnderwater(World world, int wx, int wz, int bounds) {
        int half = Math.max(1, bounds / 2);
        int[] xs = new int[] { wx - half, wx + half, wx - half, wx + half, wx };
        int[] zs = new int[] { wz - half, wz - half, wz + half, wz + half, wz };
        int minFloor = Integer.MAX_VALUE, maxFloor = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            int sea = Math.max(60, ConsegrityRegions.SEA_LEVEL);
            int floorY = -1;
            for (int y = sea; y >= world.getMinHeight(); y--) {
                Material m = world.getBlockAt(x, y, z).getType();
                if (!isWater(m) && m != Material.AIR) { floorY = y; break; }
            }
            if (floorY < world.getMinHeight()) return "NO_SEAFLOOR";
            minFloor = Math.min(minFloor, floorY);
            maxFloor = Math.max(maxFloor, floorY);
        }
        if (maxFloor - minFloor > 3) return "SEAFLOOR_UNEVEN";
        int centerFloor = -1;
        int sea = Math.max(60, ConsegrityRegions.SEA_LEVEL);
        for (int y = sea; y >= world.getMinHeight(); y--) {
            Material m = world.getBlockAt(wx, y, wz).getType();
            if (!isWater(m) && m != Material.AIR) { centerFloor = y; break; }
        }
        if (centerFloor < world.getMinHeight()) return "CENTER_NO_FLOOR";
        return null;
    }

    private String debugReasonUnderground(World world, int wx, int wz, int bounds, Random rng) {
        int top = world.getMaxHeight() - 1;
        int bottom = Math.max(world.getMinHeight(), -40);
        int startY = Math.max(bottom + 8, Math.min(top - 16, ConsegrityRegions.SEA_LEVEL - 12 - rng.nextInt(30)));
        for (int y = startY; y >= bottom; y--) {
            Material m = world.getBlockAt(wx, y, wz).getType();
            Material up = world.getBlockAt(wx, y + 1, wz).getType();
            Material down = world.getBlockAt(wx, y - 1, wz).getType();
            if (m == Material.AIR && up == Material.AIR && isStoneLike(down)) {
                if (hasCaveConnectivity(world, wx, y, wz)) {
                    return null; // would succeed
                }
            }
        }
        return "NO_CAVE_POCKET";
    }

    private String debugReasonSkyIsland(World world, int wx, int wz, int bounds, Random rng) {
        if (ConsegrityRegions.regionAt(world, wx, wz) == ConsegrityRegions.Region.MOUNTAIN) return "SKY_MOUNTAIN_BLOCK";
        int highest = world.getHighestBlockYAt(wx, wz);
        if (highest > 200) return "SKY_TERRAIN_TOO_HIGH";
        return null; // would build island and succeed
    }

    private String debugReasonNether(World world, int wx, int wz, int bounds, Random rng) {
        int top = -10;
        int bottom = Math.max(world.getMinHeight(), -120);
        for (int y = top; y >= bottom; y--) {
            if (ConsegrityRegions.regionAt(world, wx, y, wz) != ConsegrityRegions.Region.NETHER) continue;
            Material here = world.getBlockAt(wx, y, wz).getType();
            Material above = world.getBlockAt(wx, y + 1, wz).getType();
            if ((here.isSolid()) && (above == Material.AIR)) return null;
        }
        return "NO_NETHER_SURFACE";
    }

    public int getWorldTargetCount(String worldKey) {
        int sum = 0;
        for (Registration r : registrations) if (r.triggerListener) sum += r.count;
        return sum;
    }

    private Location pickPlacement(World world, int wx, int wz, Registration reg, Random rng) {
        switch (reg.type) {
            case SURFACE:
                return findSurfaceSpot(world, wx, wz, reg.bounds, false);
            case UNDERWATER:
                return findUnderwaterSpot(world, wx, wz, reg.bounds);
            case UNDERGROUND:
                return findUndergroundSpot(world, wx, wz, reg.bounds, rng);
            case SKYISLAND:
                return findSkyIslandSpotAndBuild(world, wx, wz, reg.bounds, rng);
            case HELL:
                return findNetherSpot(world, wx, wz, reg.bounds, rng);
            default:
                return null;
        }
    }

    private Location findSurfaceSpot(World world, int wx, int wz, int bounds, boolean allowWater) {
        int half = Math.max(1, bounds / 2);
        int[] xs = new int[] { wx - half, wx + half, wx - half, wx + half, wx };
        int[] zs = new int[] { wz - half, wz - half, wz + half, wz + half, wz };
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            int y = world.getHighestBlockYAt(x, z);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (!allowWater && isWater(ground)) return null;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        // keep slope gentle to avoid carving into hills/mountains
        if (maxY - minY > 2) return null;
        int yCenter = world.getHighestBlockYAt(wx, wz);
        Material above = world.getBlockAt(wx, yCenter + 1, wz).getType();
        if (above != Material.AIR) return null;
        return new Location(world, wx + 0.5, yCenter + 1, wz + 0.5);
    }

    private Location findUnderwaterSpot(World world, int wx, int wz, int bounds) {
        int half = Math.max(1, bounds / 2);
        int[] xs = new int[] { wx - half, wx + half, wx - half, wx + half, wx };
        int[] zs = new int[] { wz - half, wz - half, wz + half, wz + half, wz };
        int minFloor = Integer.MAX_VALUE, maxFloor = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            // Walk down from sea level to find the first solid seafloor
            int sea = Math.max(60, ConsegrityRegions.SEA_LEVEL);
            int floorY = -1;
            for (int y = sea; y >= world.getMinHeight(); y--) {
                Material m = world.getBlockAt(x, y, z).getType();
                if (!isWater(m) && m != Material.AIR) { floorY = y; break; }
            }
            if (floorY < world.getMinHeight()) return null;
            minFloor = Math.min(minFloor, floorY);
            maxFloor = Math.max(maxFloor, floorY);
        }
        if (maxFloor - minFloor > 3) return null; // prefer flat seabeds
        int centerFloor = -1;
        int sea = Math.max(60, ConsegrityRegions.SEA_LEVEL);
        for (int y = sea; y >= world.getMinHeight(); y--) {
            Material m = world.getBlockAt(wx, y, wz).getType();
            if (!isWater(m) && m != Material.AIR) { centerFloor = y; break; }
        }
        if (centerFloor < world.getMinHeight()) return null;
        // paste with ignoreAir=true so water is not cleared
        return new Location(world, wx + 0.5, centerFloor + 1, wz + 0.5);
    }

    private Location findUndergroundSpot(World world, int wx, int wz, int bounds, Random rng) {
        int top = world.getMaxHeight() - 1;
        int bottom = Math.max(world.getMinHeight(), -40);
        // Start somewhere below surface and above bottom
        int startY = Math.max(bottom + 8, Math.min(top - 16, ConsegrityRegions.SEA_LEVEL - 12 - rng.nextInt(30)));
        for (int y = startY; y >= bottom; y--) {
            Material m = world.getBlockAt(wx, y, wz).getType();
            Material up = world.getBlockAt(wx, y + 1, wz).getType();
            Material down = world.getBlockAt(wx, y - 1, wz).getType();
            // Require a small cavern pocket (2-block height of air) adjacent to stone
            if (m == Material.AIR && up == Material.AIR && isStoneLike(down)) {
                if (hasCaveConnectivity(world, wx, y, wz)) {
                    return new Location(world, wx + 0.5, y, wz + 0.5);
                }
            }
        }
        return null;
    }

    private boolean hasCaveConnectivity(World world, int x, int y, int z) {
        // Simple breadth around (x,y,z) to ensure open neighbors
        int open = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    Material t = world.getBlockAt(x + dx, y + dy, z + dz).getType();
                    if (t == Material.AIR || t == Material.CAVE_AIR) open++;
                    if (open >= 18) return true;
                }
            }
        }
        return false;
    }

    private Location findSkyIslandSpotAndBuild(World world, int wx, int wz, int bounds, Random rng) {
        // Avoid mountains and high terrain
        if (ConsegrityRegions.regionAt(world, wx, wz) == ConsegrityRegions.Region.MOUNTAIN) return null;
        int highest = world.getHighestBlockYAt(wx, wz);
        if (highest > 200) return null;

        int y = Math.min(world.getMaxHeight() - 20, 220 + rng.nextInt(40));
        // Build a small floating island (stone base with grass top)
        int r = Math.max(4, bounds / 2);
        int thickness = 4;
        for (int dy = 0; dy < thickness; dy++) {
            int rr = Math.max(1, r - dy);
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr) continue;
                    int xx = wx + dx;
                    int yy = y - dy;
                    int zz = wz + dz;
                    Material mat = (dy == 0) ? Material.GRASS_BLOCK : Material.STONE;
                    world.getBlockAt(xx, yy, zz).setType(mat);
                }
            }
        }
        return new Location(world, wx + 0.5, y + 1, wz + 0.5);
    }

    private Location findNetherSpot(World world, int wx, int wz, int bounds, Random rng) {
        // Require the nether (underworld) layer by Y rule
        int top = -10;
        int bottom = Math.max(world.getMinHeight(), -120);
        for (int y = top; y >= bottom; y--) {
            if (ConsegrityRegions.regionAt(world, wx, y, wz) != ConsegrityRegions.Region.NETHER) continue;
            Material here = world.getBlockAt(wx, y, wz).getType();
            Material above = world.getBlockAt(wx, y + 1, wz).getType();
            if ((here.isSolid()) && (above == Material.AIR)) {
                return new Location(world, wx + 0.5, y + 1, wz + 0.5);
            }
        }
        return null;
    }

    private boolean isWater(Material m) {
        return m == Material.WATER || m == Material.KELP || m == Material.KELP_PLANT || m == Material.SEAGRASS;
    }

    private boolean isStoneLike(Material m) {
        if (m == null) return false;
        String n = m.name();
        return m == Material.STONE || m == Material.DEEPSLATE || n.contains("STONE") || n.contains("SLATE");
    }

    // --- Registration inner type ---
    public static final class Registration {
        public final String schemName;
        public final int bounds;
        public final int count;
        public final int spacing;
        public final Sector sector;
        public final GenCheckType type;
        public final boolean triggerListener;
        public final int minimumDistance;
        private final ConsegrityRegions.Region region;

        // worldName -> placed XY (encoded as long)
        private final Map<String, Set<Long>> placements = new ConcurrentHashMap<>();

        Registration(String schemName, int bounds, int count, int spacing, Sector sector, GenCheckType type, boolean triggerListener, int minimumDistance) {
            this.schemName = schemName;
            this.bounds = Math.max(1, bounds);
            this.count = Math.max(0, count);
            this.spacing = Math.max(1, spacing);
            this.sector = sector;
            this.type = type;
            this.triggerListener = triggerListener;
            this.minimumDistance = Math.max(0, minimumDistance);
            this.region = mapRegion(sector, type);
        }

        private ConsegrityRegions.Region mapRegion(Sector sector, GenCheckType type) {
            if (type == GenCheckType.HELL) return ConsegrityRegions.Region.NETHER;
            if (sector instanceof DesertBiome) return ConsegrityRegions.Region.DESERT;
            if (sector instanceof SavannaSector) return ConsegrityRegions.Region.SAVANNAH;
            if (sector instanceof SwampSector) return ConsegrityRegions.Region.SWAMP;
            if (sector instanceof JungleSector) return ConsegrityRegions.Region.JUNGLE;
            if (sector instanceof MesaSector) return ConsegrityRegions.Region.MESA;
            if (sector instanceof MountainSector) return ConsegrityRegions.Region.MOUNTAIN;
            if (sector instanceof IceSpikesSector) return ConsegrityRegions.Region.ICE_SPIKES;
            if (sector instanceof CherrySector) return ConsegrityRegions.Region.CHERRY;
            if (sector instanceof CentralSector) return ConsegrityRegions.Region.CENTRAL;
            return null; // fallback: allow any region
        }

        boolean allowsAt(World w, int wx, int wz) {
            ConsegrityRegions.Region here;
            if (type == GenCheckType.HELL) {
                here = ConsegrityRegions.regionAt(w, wx, -80, wz);
            } else {
                here = ConsegrityRegions.regionAt(w, wx, wz);
            }
            if (region != null && here != region) return false;
            // Additional soft filters
            if (type == GenCheckType.SURFACE) {
                // Avoid mountain region for surface unless specifically targeted
                if (region == null && here == ConsegrityRegions.Region.MOUNTAIN) return false;
            }
            return true;
        }

        boolean isSpaced(String worldKey, int wx, int wz) {
            Set<Long> set = placements.getOrDefault(worldKey, Collections.emptySet());
            long key = encode(wx, wz);
            int s2 = spacing * spacing;
            for (long other : set) {
                int ox = (int) (other >> 32);
                int oz = (int) (other & 0xFFFFFFFFL);
                int dx = ox - wx;
                int dz = oz - wz;
                if (dx * dx + dz * dz < s2) return false;
            }
            return true;
        }

        boolean isSpaced(String worldKey, int wx, int wz, int spacingOverride) {
            Set<Long> set = placements.getOrDefault(worldKey, Collections.emptySet());
            int s = Math.max(1, spacingOverride);
            int s2 = s * s;
            for (long other : set) {
                int ox = (int) (other >> 32);
                int oz = (int) (other & 0xFFFFFFFFL);
                int dx = ox - wx;
                int dz = oz - wz;
                if (dx * dx + dz * dz < s2) return false;
            }
            return true;
        }

        void recordPlacement(String worldKey, int wx, int wz) {
            placements.computeIfAbsent(worldKey, k -> new HashSet<>()).add(encode(wx, wz));
        }

        int placedCount(String worldKey) {
            Set<Long> set = placements.get(worldKey);
            return set == null ? 0 : set.size();
        }

        private static long encode(int x, int z) { return (((long) x) << 32) ^ (z & 0xFFFFFFFFL); }
    }

    // --- Utilities ---
    private void grassifyTopLayerUnderBounds(World world, int centerX, int centerZ, int startY, int bounds) {
        if (world == null || bounds <= 0) return;
        int half = bounds / 2;
        int minX = centerX - half;
        int minZ = centerZ - half;
        int maxX = minX + Math.max(1, bounds) - 1;
        int maxZ = minZ + Math.max(1, bounds) - 1;
        int bottomY = Math.max(world.getMinHeight(), startY - 24); // small search band

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = startY; y >= bottomY; y--) {
                    Material m = world.getBlockAt(x, y, z).getType();
                    if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR || m == Material.WATER || m == Material.LAVA) continue;
                    if (m == Material.DIRT || m == Material.STONE) {
                        try {
                            world.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK, false);
                        } catch (Throwable t) {
                            world.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK);
                        }
                    }
                    break; // only top-most solid
                }
            }
        }
    }

    private static final class DebugCounters {
        long attempts = 0;
        long successes = 0;
        java.util.Map<String, Long> reasons = new java.util.HashMap<>();
        void inc(String k) { if (k == null) k = "UNKNOWN"; reasons.put(k, reasons.getOrDefault(k, 0L) + 1L); }
        long get(String k) { return reasons.getOrDefault(k, 0L); }
    }
    
}

