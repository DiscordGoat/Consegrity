package goat.projectLinearity.subsystems.world.structure;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persistent registry of placed structures per world.
 * Stores center location, bounds, and discovery state for trigger-enabled structures.
 */
public final class StructureStore {
    private static StructureStore INSTANCE;

    public static StructureStore get(JavaPlugin plugin) {
        if (INSTANCE == null) INSTANCE = new StructureStore(plugin);
        return INSTANCE;
    }

    private final JavaPlugin plugin;
    private final File folder;

    // worldKey (UUID string) -> id -> entry
    private final Map<String, Map<String, StructEntry>> cache = new HashMap<>();

    private StructureStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "structures");
        if (!folder.exists()) folder.mkdirs();
    }

    public synchronized void addStructure(String worldKey, String name, int x, int y, int z, int bounds, boolean trigger) {
        StructEntry e = new StructEntry(makeId(name, x, z), name, x, y, z, bounds, trigger, false);
        Map<String, StructEntry> map = loadWorld(worldKey);
        if (!map.containsKey(e.id)) {
            map.put(e.id, e);
            saveWorld(worldKey, map);
        }
    }

    public synchronized Optional<StructEntry> findUndiscoveredNear(String worldKey, int x, int z) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        for (StructEntry e : map.values()) {
            if (!e.trigger || e.discovered) continue;
            int half = Math.max(1, e.bounds / 2);
            if (Math.abs(x - e.x) <= half && Math.abs(z - e.z) <= half) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    // Per-player aware discovery search
    public synchronized Optional<StructEntry> findUndiscoveredNearForPlayer(String worldKey, java.util.UUID playerId, int x, int z) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        for (StructEntry e : map.values()) {
            if (!e.trigger) continue;
            if (e.discoveredBy != null && e.discoveredBy.contains(playerId.toString())) continue;
            int half = Math.max(1, e.bounds / 2);
            if (Math.abs(x - e.x) <= half && Math.abs(z - e.z) <= half) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public synchronized void markDiscovered(String worldKey, String id) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        StructEntry e = map.get(id);
        if (e != null && !e.discovered) {
            e.discovered = true;
            saveWorld(worldKey, map);
        }
    }

    public synchronized void markDiscoveredForPlayer(String worldKey, String id, java.util.UUID playerId) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        StructEntry e = map.get(id);
        if (e != null) {
            if (e.discoveredBy == null) e.discoveredBy = new HashSet<>();
            if (e.discoveredBy.add(playerId.toString())) {
                // Keep legacy discovered true if anyone discovered it
                e.discovered = true;
                saveWorld(worldKey, map);
            }
        }
    }

    public synchronized int getDiscoveredCount(String worldKey) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        int c = 0;
        for (StructEntry e : map.values()) if (e.trigger && e.discovered) c++;
        return c;
    }

    public synchronized int getPlacedCount(String worldKey) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        int c = 0;
        for (StructEntry e : map.values()) if (e.trigger) c++;
        return c;
    }

    public synchronized int getPlacedCountForName(String worldKey, String name) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        int c = 0;
        for (StructEntry e : map.values()) if (e.trigger && e.name.equalsIgnoreCase(name)) c++;
        return c;
    }

    public synchronized java.util.Collection<StructEntry> getAllStructures(String worldKey) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        return java.util.Collections.unmodifiableCollection(map.values());
    }

    public synchronized java.util.Collection<StructEntry> getStructuresForName(String worldKey, String name) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        if (map.isEmpty()) return java.util.Collections.emptyList();
        java.util.List<StructEntry> list = new java.util.ArrayList<>();
        for (StructEntry e : map.values()) {
            if (e.name.equalsIgnoreCase(name)) {
                list.add(e);
            }
        }
        return java.util.Collections.unmodifiableList(list);
    }

    public synchronized int getPlayerDiscoveredCountForName(String worldKey, java.util.UUID playerId, String name) {
        Map<String, StructEntry> map = loadWorld(worldKey);
        int c = 0;
        String pid = playerId.toString();
        for (StructEntry e : map.values()) {
            if (!e.trigger) continue;
            if (!e.name.equalsIgnoreCase(name)) continue;
            if (e.discoveredBy != null && e.discoveredBy.contains(pid)) c++;
        }
        return c;
    }

    public synchronized boolean hasWorldData(String worldKey) {
        File f = fileFor(worldKey);
        if (f.exists() && f.isFile()) return true;
        Map<String, StructEntry> map = cache.get(worldKey);
        return map != null && !map.isEmpty();
    }

    private String makeId(String name, int x, int z) {
        return name + "@" + x + "," + z;
    }

    private File fileFor(String worldKey) {
        return new File(folder, worldKey + ".yml");
    }

    private Map<String, StructEntry> loadWorld(String worldKey) {
        Map<String, StructEntry> map = cache.get(worldKey);
        if (map != null) return map;
        map = new LinkedHashMap<>();
        File f = fileFor(worldKey);
        if (f.exists()) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                ConfigurationSection root = yml.getConfigurationSection("structures");
                if (root != null) {
                    for (String key : root.getKeys(false)) {
                        ConfigurationSection s = root.getConfigurationSection(key);
                        if (s == null) continue;
                        String name = s.getString("name", "");
                        int x = s.getInt("x");
                        int y = s.getInt("y");
                        int z = s.getInt("z");
                        int bounds = s.getInt("bounds", 16);
                        boolean trigger = s.getBoolean("trigger", false);
                        boolean discovered = s.getBoolean("discovered", false);
                        java.util.List<String> discoveredBy = s.getStringList("discoveredBy");
                        StructEntry e = new StructEntry(key, name, x, y, z, bounds, trigger, discovered);
                        if (discoveredBy != null && !discoveredBy.isEmpty()) e.discoveredBy = new HashSet<>(discoveredBy);
                        map.put(key, e);
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to load structures for worldKey '" + worldKey + "': " + t.getMessage());
            }
        }
        cache.put(worldKey, map);
        return map;
    }

    private void saveWorld(String worldKey, Map<String, StructEntry> map) {
        File f = fileFor(worldKey);
        YamlConfiguration yml = new YamlConfiguration();
        ConfigurationSection root = yml.createSection("structures");
        for (StructEntry e : map.values()) {
            ConfigurationSection s = root.createSection(e.id);
            s.set("name", e.name);
            s.set("x", e.x);
            s.set("y", e.y);
            s.set("z", e.z);
            s.set("bounds", e.bounds);
            s.set("trigger", e.trigger);
            s.set("discovered", e.discovered);
            if (e.discoveredBy != null && !e.discoveredBy.isEmpty()) {
                s.set("discoveredBy", new ArrayList<>(e.discoveredBy));
            }
        }
        try { yml.save(f); }
        catch (IOException e) { plugin.getLogger().warning("Failed to save structures for '" + worldKey + "': " + e.getMessage()); }
    }

    public static final class StructEntry {
        public final String id;
        public final String name;
        public final int x, y, z;
        public final int bounds;
        public final boolean trigger;
        public boolean discovered;
        public Set<String> discoveredBy;

        public StructEntry(String id, String name, int x, int y, int z, int bounds, boolean trigger, boolean discovered) {
            this.id = id; this.name = name; this.x = x; this.y = y; this.z = z; this.bounds = bounds; this.trigger = trigger; this.discovered = discovered;
        }
    }
}
