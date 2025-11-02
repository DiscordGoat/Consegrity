package goat.projectLinearity.subsystems.world.loot;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Registry that describes chest counts and available inventory templates for each structure.
 */
public final class LootRegistry {

    private static final int DEFAULT_TEMPLATE_SIZE = 27;

    private final JavaPlugin plugin;
    private final Map<String, StructureLootDefinition> definitions = new HashMap<>();
    private final Map<String, byte[]> schemCache = new ConcurrentHashMap<>();
    private final Map<String, Clipboard> clipboardCache = new ConcurrentHashMap<>();

    public LootRegistry(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        registerDefaults();
    }

    public StructureLootDefinition get(String structureKey) {
        if (structureKey == null) return null;
        return definitions.get(normalize(structureKey));
    }

    public Set<String> getKnownKeys() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    public java.util.Collection<StructureLootDefinition> getDefinitions() {
        return Collections.unmodifiableSet(new java.util.HashSet<>(definitions.values()));
    }

    private void registerDefaults() {
        registerDefinition("DesertTemple", Material.SAND, "deserttemple");
        registerDefinition("JungleTemple", Material.MOSSY_COBBLESTONE, "jungletemple");
        registerDefinition("WitchHut", Material.REDSTONE, "witchhut");
        registerDefinition("WitchFestival", Material.PURPLE_WOOL, "witchfestival");
        registerDefinition("Monastery", Material.BOOKSHELF, "monastery");
        registerDefinition("HotSpring", Material.PRISMARINE_CRYSTALS, "hotspring");
        registerDefinition("Monument", Material.PRISMARINE, "monument", "abandonedmonument");
        registerDefinition("JadeStatue", Material.EMERALD, "jadestatue1", "jadestatue2", "jadestatue3", "jadestatuefinal");
        registerDefinition("Beacon", Material.IRON_BLOCK, "beacon0", "beacon1", "beacon2", "beacon3", "beacon4");
        registerDefinition("Conduit", Material.HEART_OF_THE_SEA, "conduit1", "conduit2", "conduit3", "conduit4");
        registerDefinition("Sarcophagus", Material.GOLD_INGOT, "sarcophagus0", "sarcophagus1", "sarcophagus2", "sarcophagus3", "sarcophagus4", "sarcophagus5", "sarcophagus6", "sarcophagus7", "sarcophagus8");
        registerDefinition("Pillager", Material.CROSSBOW, "pillager");
        registerDefinition("Prospect", Material.RAW_COPPER, "prospect");
        registerDefinition("HayWagon", Material.HAY_BLOCK, "haywagon");
    }

    private void registerDefinition(String directoryName, Material placeholder, String primaryKey, String... aliases) {
        StructureLootDefinition definition = new StructureLootDefinition(directoryName, placeholder, primaryKey, aliases);
        for (String key : definition.getStructureKeys()) {
            definitions.put(key, definition);
        }
        definition.reload();
        plugin.getLogger().info(() -> "[LootRegistry] Registered loot definition for " + directoryName +
                " (keys=" + definition.getStructureKeys() + ", chests=" + definition.getChestCount() +
                ", templates=" + definition.getInventoryCount() + ")");
    }

    public void reload(String structureKey) {
        StructureLootDefinition def = get(structureKey);
        if (def != null) {
            def.reload();
        }
    }

    private Clipboard loadClipboard(String structureName) {
        String normalized = normalize(structureName);
        return clipboardCache.computeIfAbsent(normalized, key -> {
            byte[] bytes = schemCache.computeIfAbsent(key, this::readSchematicBytes);
            if (bytes == null || bytes.length == 0) {
                plugin.getLogger().warning("[LootRegistry] Missing schematic for " + structureName);
                return null;
            }
            ClipboardFormat format = ClipboardFormats.findByAlias("schem");
            if (format == null) {
                plugin.getLogger().severe("[LootRegistry] Unable to resolve schematic format for " + structureName);
                return null;
            }
            try (ClipboardReader reader = format.getReader(new ByteArrayInputStream(bytes))) {
                return reader.read();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "[LootRegistry] Failed to parse schematic for " + structureName, ex);
                return null;
            }
        });
    }

    private byte[] readSchematicBytes(String structureName) {
        if (structureName == null) return null;
        String resourcePath = "schematics/" + structureName + ".schem";
        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) return null;
            return readAll(is);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "[LootRegistry] Failed reading schematic bytes for " + structureName, ex);
            return null;
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        int r;
        while ((r = is.read(buffer)) != -1) {
            bos.write(buffer, 0, r);
        }
        return bos.toByteArray();
    }

    private static String normalize(String key) {
        return key == null ? null : key.toLowerCase(Locale.ROOT);
    }

    public final class StructureLootDefinition {
        private final String directoryName;
        private final Material placeholderMaterial;
        private final String primaryKey;
        private final Set<String> keys = new HashSet<>();
        private final File directory;
        private final List<LootInventory> inventories = new ArrayList<>();
        private final Map<String, Integer> chestCounts = new HashMap<>();

        private StructureLootDefinition(String directoryName, Material placeholderMaterial, String primaryKey, String... aliases) {
            this.directoryName = Objects.requireNonNull(directoryName, "directoryName");
            this.placeholderMaterial = Objects.requireNonNull(placeholderMaterial, "placeholderMaterial");
            this.primaryKey = Objects.requireNonNull(primaryKey, "primaryKey");
            keys.add(normalize(primaryKey));
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias != null && !alias.isBlank()) {
                        keys.add(normalize(alias));
                    }
                }
            }
            this.directory = new File(plugin.getDataFolder(), "structureloot/" + directoryName);
            refreshChestCounts();
        }

        public Set<String> getStructureKeys() {
            return Collections.unmodifiableSet(keys);
        }

        public int getChestCount() {
            return chestCounts.getOrDefault(normalize(primaryKey), 0);
        }

        public int getChestCountForKey(String structureKey) {
            if (structureKey != null) {
                Integer v = chestCounts.get(normalize(structureKey));
                if (v != null) {
                    return v;
                }
            }
            if (!chestCounts.isEmpty()) {
                return chestCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            }
            return 0;
        }

        public int getInventoryCount() {
            return inventories.size();
        }

        public List<LootInventory> getInventories() {
            return Collections.unmodifiableList(inventories);
        }

        public Material getPlaceholderMaterial() {
            return placeholderMaterial;
        }

        public String getDirectoryName() {
            return directoryName;
        }

        public String getPrimaryKey() {
            return primaryKey;
        }

        public List<ItemStack[]> pickTemplates(int count, Random random) {
            if (count <= 0) return Collections.emptyList();
            List<ItemStack[]> results = new ArrayList<>(count);
            List<LootInventory> pool = new ArrayList<>();
            if (inventories.isEmpty()) {
                pool.add(new LootInventory("__generated__", generatePlaceholderInventory(), true));
            } else {
                pool.addAll(inventories);
            }
            if (pool.size() > 1) {
                Collections.shuffle(pool, random);
            }
            int index = 0;
            for (int i = 0; i < count; i++) {
                LootInventory chosen;
                if (index < pool.size()) {
                    chosen = pool.get(index++);
                } else {
                    chosen = pool.get(random.nextInt(pool.size()));
                }
                results.add(cloneContents(chosen.contents()));
            }
            return results;
        }

        public void reload() {
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().warning("[LootRegistry] Unable to create loot directory " + directory.getAbsolutePath());
            }
            inventories.clear();
            refreshChestCounts();
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        ItemStack[] contents = InventorySerializer.load(file);
                        inventories.add(new LootInventory(file.getName(), contents, false));
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING, "[LootRegistry] Failed loading loot inventory " + file.getName(), ex);
                    }
                }
            }
            if (inventories.isEmpty()) {
                inventories.add(new LootInventory("__generated__", generatePlaceholderInventory(), true));
            }
        }

        private void refreshChestCounts() {
            chestCounts.clear();
            int primaryCount = detectChestCount(primaryKey);
            if (primaryCount > 0) {
                chestCounts.put(normalize(primaryKey), primaryCount);
            }
            for (String key : keys) {
                if (key.equals(normalize(primaryKey))) continue;
                int count = detectChestCount(key);
                if (count > 0) {
                    chestCounts.put(key, count);
                }
            }
        }

        private ItemStack[] generatePlaceholderInventory() {
            int size = DEFAULT_TEMPLATE_SIZE;
            return new ItemStack[size];
        }

        private int detectChestCount(String structureName) {
            Clipboard clipboard = loadClipboard(structureName);
            if (clipboard == null) return 0;
            try {
                int total = 0;
                for (BlockVector3 pos : clipboard.getRegion()) {
                    BaseBlock block = clipboard.getFullBlock(pos);
                    if (block == null) continue;
                    BlockType type = block.getBlockType();
                    if (type == null) continue;
                    String id = type.getId();
                    if ("minecraft:chest".equals(id) || "minecraft:trapped_chest".equals(id)) {
                        total++;
                    }
                }
                return total;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "[LootRegistry] Failed counting chests for " + structureName, t);
                return 0;
            }
        }
    }

    public static final class LootInventory {
        private final String id;
        private final ItemStack[] contents;
        private final boolean generated;

        private LootInventory(String id, ItemStack[] contents, boolean generated) {
            this.id = id;
            this.contents = contents;
            this.generated = generated;
        }

        public String id() {
            return id;
        }

        public ItemStack[] contents() {
            return contents;
        }

        public boolean generated() {
            return generated;
        }
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            clone[i] = item == null ? null : item.clone();
        }
        return clone;
    }
}
