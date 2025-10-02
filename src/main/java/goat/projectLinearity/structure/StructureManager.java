package goat.projectLinearity.structure;

import goat.projectLinearity.libs.SchemManager;
import goat.projectLinearity.world.ConsegrityRegions;
import goat.projectLinearity.world.sector.*;
import org.bukkit.Bukkit;
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

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schemManager = new SchemManager(plugin);
    }

    public JavaPlugin getPlugin() { return plugin; }

    public List<Registration> getRegistrations() { return registrations; }

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
        registerStruct(schem, bounds, count, spacing, sector, type, false);
    }

    public void registerStruct(String schem, int bounds, int count, int spacing, Sector sector, GenCheckType type, boolean triggerListener) {
        String base = normalizeName(schem);
        registrations.add(new Registration(base, bounds, count, spacing, sector, type, triggerListener));
        plugin.getLogger().info("Registered structure '" + base + "' (count=" + count + ", spacing=" + spacing + ", bounds=" + bounds + ", type=" + type + ", trigger=" + triggerListener + ")");
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
            for (int a = 0; a < 3; a++) {
                int lx = rng.nextInt(16);
                int lz = rng.nextInt(16);
                int wx = (chunkX << 4) + lx;
                int wz = (chunkZ << 4) + lz;
                if (!reg.allowsAt(world, wx, wz)) continue;

                Location paste = pickPlacement(world, wx, wz, reg, rng);
                if (paste == null) continue;

                if (!reg.isSpaced(worldKey, wx, wz)) continue;

                boolean ignoreAir = (reg.type == GenCheckType.UNDERWATER);
                try {
                    schemManager.placeStructure(reg.schemName, paste, ignoreAir);
                    reg.recordPlacement(worldKey, wx, wz);
                    // Persist structure instance for listener tracking
                    if (reg.triggerListener) {
                        StructureStore.get(plugin).addStructure(worldKey, reg.schemName, paste.getBlockX(), paste.getBlockY(), paste.getBlockZ(), reg.bounds, true);
                    }
                    placedAny = true;
                    break; // one per registration per chunk attempt
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed placing '" + reg.schemName + "' at " + paste + ": " + t.getMessage());
                }
            }
        }
        return placedAny;
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
        private final ConsegrityRegions.Region region;

        // worldName -> placed XY (encoded as long)
        private final Map<String, Set<Long>> placements = new ConcurrentHashMap<>();

        Registration(String schemName, int bounds, int count, int spacing, Sector sector, GenCheckType type, boolean triggerListener) {
            this.schemName = schemName;
            this.bounds = Math.max(1, bounds);
            this.count = Math.max(0, count);
            this.spacing = Math.max(1, spacing);
            this.sector = sector;
            this.type = type;
            this.triggerListener = triggerListener;
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

        void recordPlacement(String worldKey, int wx, int wz) {
            placements.computeIfAbsent(worldKey, k -> new HashSet<>()).add(encode(wx, wz));
        }

        int placedCount(String worldKey) {
            Set<Long> set = placements.get(worldKey);
            return set == null ? 0 : set.size();
        }

        private static long encode(int x, int z) { return (((long) x) << 32) ^ (z & 0xFFFFFFFFL); }
    }
}
