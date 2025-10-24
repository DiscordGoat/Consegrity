package goat.projectLinearity.subsystems.world;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.SchemManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Spawns and despawns night-only structures.
 */
public final class NocturnalStructureManager implements Listener {

    private static final long MAINTENANCE_INTERVAL_TICKS = 20L * 20L; // every 20 seconds
    private static final int PLAYER_SAMPLE_RADIUS = 256;

    private final ProjectLinearity plugin;
    private final File dataFile;
    private final Map<String, NightRegistration> registrations = new java.util.LinkedHashMap<>();
    private final Map<NightRegistration, Set<StructureInstance>> structures = new java.util.LinkedHashMap<>();
    private final SchemManager schemManager;
    private BukkitTask maintenanceTask;
    private boolean dirty;

    public NocturnalStructureManager(ProjectLinearity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dataFile = new File(plugin.getDataFolder(), "nocturnal_structures.yml");
        this.schemManager = new SchemManager(plugin);
    }

    public void registerStruct(String schem, int bounds, int count, int spacing) {
        registerStruct(schem, bounds, bounds, 5, count, spacing);
    }

    public void registerStruct(String schem, int width, int depth, int height, int count, int spacing) {
        String key = schem.toLowerCase(java.util.Locale.ROOT);
        NightRegistration reg = new NightRegistration(schem, width, depth, height, count, spacing);
        registrations.put(key, reg);
        structures.computeIfAbsent(reg, r -> new HashSet<>());
    }

    public void startup() {
        load();
        maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runMaintenance, 200L, MAINTENANCE_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        removeAllPlaced();
        save();
        HandlerList.unregisterAll(this);
    }

    private void runMaintenance() {
        World world = plugin.getServer().getWorld("Consegrity");
        if (world == null) {
            return;
        }
        boolean night = isNight(world);
        List<Player> mountainPlayers = collectMountainPlayers(world);

        for (NightRegistration registration : registrations.values()) {
            maintainRegistration(world, registration, night, mountainPlayers);
        }

        saveIfDirty();
    }

    private void maintainRegistration(World world, NightRegistration registration, boolean night, List<Player> players) {
        Set<StructureInstance> instances = structures.get(registration);
        if (instances == null) {
            return;
        }

        if (night) {
            if (players.isEmpty()) {
                for (StructureInstance instance : instances) {
                    if (instance.placed) {
                        removeStructure(world, registration, instance);
                    }
                }
                return;
            }
            // Ensure previously saved instances are placed
            for (StructureInstance instance : new ArrayList<>(instances)) {
                if (!instance.placed) {
                    if (!placeStructure(world, registration, instance)) {
                        instances.remove(instance);
                        dirty = true;
                    }
                }
            }

            // Spawn additional instances if needed
            int deficit = registration.targetCount - instances.size();
            if (deficit > 0 && !players.isEmpty()) {
                for (int i = 0; i < deficit; i++) {
                    Optional<StructureInstance> candidate = findStructureLocation(world, registration, players);
                    if (candidate.isEmpty()) {
                        break;
                    }
                    StructureInstance inst = candidate.get();
                    if (placeStructure(world, registration, inst)) {
                        instances.add(inst);
                        dirty = true;
                    }
                }
            }
        } else {
            // Daytime: remove structures but keep locations for the next night
            for (StructureInstance instance : instances) {
                if (instance.placed) {
                    removeStructure(world, registration, instance);
                }
            }
        }
    }

    private Optional<StructureInstance> findStructureLocation(World world, NightRegistration registration, List<Player> players) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int maxAttempts = registration.targetCount * 200;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Player pivot = players.get(rng.nextInt(players.size()));
            Location pivotLoc = pivot.getLocation();
            int x = pivotLoc.getBlockX() + rng.nextInt(-PLAYER_SAMPLE_RADIUS, PLAYER_SAMPLE_RADIUS + 1);
            int z = pivotLoc.getBlockZ() + rng.nextInt(-PLAYER_SAMPLE_RADIUS, PLAYER_SAMPLE_RADIUS + 1);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            if (ConsegrityRegions.regionAt(world, x, z) != ConsegrityRegions.Region.MOUNTAIN) {
                continue;
            }
            int centerY = world.getHighestBlockYAt(x, z);
            if (centerY <= world.getMinHeight() + 2) {
                continue;
            }
            int baseX = x - registration.width / 2;
            int baseZ = z - registration.depth / 2;

            Optional<Integer> baseYOpt = validateFootprint(world, baseX, baseZ, registration);
            if (baseYOpt.isEmpty()) {
                continue;
            }
            int baseY = baseYOpt.get();

            if (!meetsSpacing(world, baseX, baseY, baseZ, registration)) {
                continue;
            }

            return Optional.of(new StructureInstance(registration.key, world.getName(), baseX, baseY, baseZ));
        }
        return Optional.empty();
    }

    private Optional<Integer> validateFootprint(World world, int baseX, int baseZ, NightRegistration registration) {
        int expectedY = -1;
        for (int dx = 0; dx < registration.width; dx++) {
            for (int dz = 0; dz < registration.depth; dz++) {
                int wx = baseX + dx;
                int wz = baseZ + dz;
                int topY = world.getHighestBlockYAt(wx, wz);
                if (expectedY == -1) {
                    expectedY = topY;
                } else if (Math.abs(topY - expectedY) > 0) {
                    return Optional.empty();
                }
                Material ground = world.getBlockAt(wx, topY, wz).getType();
                if (ground == Material.WATER || ground == Material.LAVA || ground == Material.AIR) {
                    return Optional.empty();
                }
                for (int dy = 1; dy <= registration.height + 1; dy++) {
                    Block head = world.getBlockAt(wx, topY + dy, wz);
                    if (!head.isEmpty()) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(expectedY);
    }

    private boolean meetsSpacing(World world, int baseX, int baseY, int baseZ, NightRegistration registration) {
        double centerX = baseX + registration.width / 2.0;
        double centerZ = baseZ + registration.depth / 2.0;
        Set<StructureInstance> instances = structures.get(registration);
        if (instances == null) {
            return true;
        }
        double spacingSq = registration.minSpacing * registration.minSpacing;
        for (StructureInstance instance : instances) {
            if (!instance.worldName.equals(world.getName())) {
                continue;
            }
            double otherX = instance.baseX + registration.width / 2.0;
            double otherZ = instance.baseZ + registration.depth / 2.0;
            double distSq = (centerX - otherX) * (centerX - otherX) + (centerZ - otherZ) * (centerZ - otherZ);
            if (distSq < spacingSq) {
                return false;
            }
        }
        return true;
    }

    private boolean placeStructure(World world, NightRegistration registration, StructureInstance instance) {
        if (!world.isChunkLoaded(instance.baseX >> 4, instance.baseZ >> 4)) {
            return false;
        }
        Location pasteLocation = new Location(world, instance.baseX, instance.baseY + 1, instance.baseZ);
        try {
            schemManager.placeStructure(registration.schemName, pasteLocation, false);
            instance.placed = true;
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to paste nocturnal structure '" + registration.schemName + "' at " + pasteLocation + ": " + t.getMessage());
            return false;
        }
    }

    private void removeStructure(World world, NightRegistration registration, StructureInstance instance) {
        if (!world.isChunkLoaded(instance.baseX >> 4, instance.baseZ >> 4)) {
            instance.placed = false;
            return;
        }
        Location pasteLocation = new Location(world, instance.baseX, instance.baseY + 1, instance.baseZ);
        try {
            schemManager.placeStructure("null", pasteLocation, false);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear nocturnal structure '" + registration.schemName + "' at " + pasteLocation + ": " + t.getMessage());
        }
        instance.placed = false;
    }

    private void removeAllPlaced() {
        World world = plugin.getServer().getWorld("Consegrity");
        if (world == null) {
            return;
        }
        for (NightRegistration registration : registrations.values()) {
            Set<StructureInstance> set = structures.get(registration);
            if (set == null) continue;
            for (StructureInstance instance : set) {
                if (instance.placed) {
                    removeStructure(world, registration, instance);
                }
            }
        }
    }

    private List<Player> collectMountainPlayers(World world) {
        List<Player> players = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (!player.isValid() || player.isDead()) {
                continue;
            }
            if (ConsegrityRegions.regionAt(world, player.getLocation().getBlockX(), player.getLocation().getBlockZ())
                    == ConsegrityRegions.Region.MOUNTAIN) {
                players.add(player);
            }
        }
        return players;
    }

    private boolean isNight(World world) {
        long time = world.getTime() % 24000;
        return time >= 12300 && time <= 23850;
    }

    private void load() {
        structures.values().forEach(Set::clear);
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("structures");
        if (root == null) {
            return;
        }
        for (String typeKey : root.getKeys(false)) {
            NightRegistration registration = registrations.get(typeKey.toLowerCase(java.util.Locale.ROOT));
            if (registration == null) {
                continue;
            }
            ConfigurationSection list = root.getConfigurationSection(typeKey);
            if (list == null) {
                continue;
            }
            for (String id : list.getKeys(false)) {
                ConfigurationSection entry = list.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                String worldName = entry.getString("world", "Consegrity");
                int baseX = entry.getInt("x");
                int baseY = entry.getInt("y");
                int baseZ = entry.getInt("z");
                structures.computeIfAbsent(registration, r -> new HashSet<>())
                        .add(new StructureInstance(registration.key, worldName, baseX, baseY, baseZ));
            }
        }
        dirty = false;
    }

    private void saveIfDirty() {
        if (!dirty) {
            return;
        }
        save();
        dirty = false;
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("structures");
        for (Map.Entry<NightRegistration, Set<StructureInstance>> entry : structures.entrySet()) {
            NightRegistration registration = entry.getKey();
            Set<StructureInstance> set = entry.getValue();
            ConfigurationSection typeSection = root.createSection(registration.key);
            int index = 0;
            for (StructureInstance instance : set) {
                ConfigurationSection child = typeSection.createSection(Integer.toString(index++));
                child.set("world", instance.worldName);
                child.set("x", instance.baseX);
                child.set("y", instance.baseY);
                child.set("z", instance.baseZ);
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save nocturnal structure data.", ex);
        }
    }

    private static final class NightRegistration {
        final String key;
        final String schemName;
        final int width;
        final int depth;
        final int height;
        final int targetCount;
        final int minSpacing;

        NightRegistration(String schemName, int width, int depth, int height, int targetCount, int minSpacing) {
            this.key = schemName.toLowerCase(java.util.Locale.ROOT);
            this.schemName = schemName;
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.targetCount = targetCount;
            this.minSpacing = minSpacing;
        }
    }

    private static final class StructureInstance {
        final String structureKey;
        final String worldName;
        final int baseX;
        final int baseY;
        final int baseZ;
        boolean placed;

        StructureInstance(String structureKey, String worldName, int baseX, int baseY, int baseZ) {
            this.structureKey = structureKey;
            this.worldName = worldName;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
        }
    }
}
