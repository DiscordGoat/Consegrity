package goat.projectLinearity.world;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.SchemManager;
import goat.projectLinearity.world.structure.StructureStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class KeystoneManager {

    private final ProjectLinearity plugin;
    private final SchemManager schemManager;
    private final Map<String, KeystoneDefinition> definitions = new HashMap<>();
    private final Map<UUID, Map<String, KeystoneInstance>> instances = new ConcurrentHashMap<>();
    private final File dataFile;
    private BukkitTask rewardTask;
    private boolean dirty;

    public KeystoneManager(ProjectLinearity plugin) {
        this.plugin = plugin;
        this.schemManager = new SchemManager(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "keystones.yml");
    }

    public void startup() {
        load();
        rewardTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                tickWorld(world);
            }
        }, 100L, 100L);
    }

    public void shutdown() {
        if (rewardTask != null) {
            rewardTask.cancel();
            rewardTask = null;
        }
        save();
    }

    public void registerDefinition(KeystoneDefinition definition) {
        definitions.put(definition.key, definition);
    }

    public Optional<KeystoneInstance> findInstance(World world, Location location, boolean includeProtection) {
        Map<String, KeystoneInstance> worldMap = instances.computeIfAbsent(world.getUID(), uuid -> new HashMap<>());
        ensureInstancesLoaded(world, worldMap);
        for (KeystoneInstance instance : worldMap.values()) {
            if (!instance.location.getWorld().equals(world)) continue;
            double protectionRadius = includeProtection ? instance.definition.protectionRadius : instance.definition.bounds;
            if (Math.abs(location.getBlockX() - instance.location.getBlockX()) <= protectionRadius
                    && Math.abs(location.getBlockY() - instance.location.getBlockY()) <= instance.definition.height
                    && Math.abs(location.getBlockZ() - instance.location.getBlockZ()) <= protectionRadius) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    public boolean tryConsumeContribution(Player player, KeystoneInstance instance, ItemStack stack) {
        KeystoneDefinition definition = instance.definition;
        if (!definition.requiredItem.test(stack)) {
            return false;
        }

        int remaining = definition.totalProgress - instance.progress;
        if (remaining <= 0) {
            return false;
        }

        if (instance.processingFrame) {
            return false;
        }

        if (stack.getAmount() <= 0) {
            return false;
        }

        stack.setAmount(stack.getAmount() - 1);
        player.updateInventory();
        instance.progress += 1;
        if (instance.progress > definition.totalProgress) {
            instance.progress = definition.totalProgress;
        }
        dirty = true;
        String progressText = definition.displayName + " repairs: " + instance.progress + "/" + definition.totalProgress;
        player.sendMessage(progressText);
        handleProgressUpdate(instance);
        saveIfDirty();
        return true;
    }

    public void registerStructurePlacement(World world, String name, int x, int y, int z, int bounds) {
        KeystoneDefinition definition = definitions.get(name.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return;
        }
        Map<String, KeystoneInstance> worldMap = instances.computeIfAbsent(world.getUID(), uuid -> new HashMap<>());
        ensureInstancesLoaded(world, worldMap);
        worldMap.computeIfAbsent(definition.key, key -> {
            KeystoneInstance instance = new KeystoneInstance(definition, new Location(world, x, y, z));
            dirty = true;
            saveIfDirty();
            return instance;
        });
    }

    private void tickWorld(World world) {
        Map<String, KeystoneInstance> worldMap = instances.computeIfAbsent(world.getUID(), uuid -> new HashMap<>());
        ensureInstancesLoaded(world, worldMap);
        for (KeystoneInstance instance : worldMap.values()) {
            if (instance.progress < instance.definition.totalProgress) {
                continue;
            }
            applyReward(world, instance);
        }
    }

    private void applyReward(World world, KeystoneInstance instance) {
        double radius = instance.definition.rewardRadius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(instance.location) <= radius * radius) {
                instance.definition.applyReward(player);
            }
        }
    }

    private void handleProgressUpdate(KeystoneInstance instance) {
        KeystoneDefinition definition = instance.definition;
        int frames = definition.frames.size();
        int stage;

        if (instance.progress >= definition.totalProgress) {
            stage = frames - 1; // Final frame is only for 100% completion
        } else {
            // Spread the other frames (0 to frames-2) across the progress
            int intermediateFrames = frames - 1;
            stage = Math.min(intermediateFrames - 1,
                    (int) Math.floor((instance.progress / (double) definition.totalProgress) * intermediateFrames));
        }

        if (stage > instance.currentFrameIndex) {
            for (int s = instance.currentFrameIndex + 1; s <= stage; s++) {
                instance.pendingFrames.add(s);
            }
            processNextFrame(instance);
        }

        if (instance.progress >= definition.totalProgress && !instance.completed) {
            instance.completed = true;
            // Announce completion to nearby players
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Set<Player> watchers = gatherNearbyPlayers(instance.location.getWorld(), instance.location, definition.notifyRadius);
                for (Player watcher : watchers) {
                    if (watcher.isOnline()) {
                        watcher.sendTitle(ChatColor.GOLD + definition.displayName, ChatColor.GREEN + "Restoration Complete!", 10, 70, 20);
                    }
                }
            }, 5 * 20L + 10L); // Delay slightly after the final frame paste
        }
    }

    private void processNextFrame(KeystoneInstance instance) {
        if (instance.processingFrame) {
            return;
        }
        Integer next = instance.pendingFrames.poll();
        if (next == null) {
            return;
        }
        instance.processingFrame = true;
        scheduleFrameReveal(instance, next);
    }

    private void scheduleFrameReveal(KeystoneInstance instance, int targetFrame) {
        World world = instance.location.getWorld();
        Set<Player> watchers = gatherNearbyPlayers(world, instance.location, instance.definition.notifyRadius);
        for (int i = 0; i < 5; i++) {
            final int secondsLeft = 5 - i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player watcher : watchers) {
                    if (!watcher.isOnline()) continue;
                    watcher.sendTitle("",
                            ChatColor.GOLD + instance.definition.displayName + " stabilizing (" + secondsLeft + "s)",
                            0, 12, 4);
                    watcher.playSound(instance.location, Sound.BLOCK_AMETHYST_CLUSTER_STEP, 0.6f, 1.2f);
                }
            }, i * 20L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pasteFrame(instance, targetFrame);
            for (Player watcher : watchers) {
                if (!watcher.isOnline()) continue;
                watcher.playSound(instance.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                watcher.resetTitle();
            }
            spawnCelebration(instance);
            instance.currentFrameIndex = targetFrame;
            instance.processingFrame = false;
            dirty = true;
            saveIfDirty();
            processNextFrame(instance);
        }, 5 * 20L);
    }

    private Set<Player> gatherNearbyPlayers(World world, Location origin, double radius) {
        Set<Player> set = new HashSet<>();
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(origin) <= radiusSq) {
                set.add(player);
            }
        }
        return set;
    }

    private void pasteFrame(KeystoneInstance instance, int targetFrame) {
        String frame = instance.definition.frames.get(targetFrame);
        schemManager.placeStructure(frame, instance.location.clone(), false);
        instance.frameInitialized = true;
    }

    private Location findOptimalFireworkLocation(Location keystoneLocation) {
        World world = keystoneLocation.getWorld();
        if (world == null) return keystoneLocation.clone().add(0.5, 1.0, 0.5);

        int startX = keystoneLocation.getBlockX();
        int startY = keystoneLocation.getBlockY();
        int startZ = keystoneLocation.getBlockZ();

        // Search within 20 blocks radius
        int maxDistance = 20;
        Location bestLocation = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    // Skip if too far (optimize search)
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > maxDistance * 1.5) continue;

                    int x = startX + dx;
                    int y = startY + dy;
                    int z = startZ + dz;

                    // Must be within world bounds
                    if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

                    Material blockType = world.getBlockAt(x, y, z).getType();
                    Material aboveType = world.getBlockAt(x, y + 1, z).getType();

                    // Check if this block is air or water
                    boolean isValidBlock = blockType == Material.AIR ||
                                         blockType == Material.WATER ||
                                         blockType == Material.CAVE_AIR;

                    // Check if the block above is air or ice variant (exposed)
                    boolean isExposed = aboveType == Material.AIR ||
                                      aboveType == Material.CAVE_AIR ||
                                      aboveType == Material.ICE ||
                                      aboveType == Material.PACKED_ICE ||
                                      aboveType == Material.BLUE_ICE ||
                                      aboveType == Material.FROSTED_ICE;

                    if (isValidBlock && isExposed) {
                        // Calculate score based on distance from keystone and height preference
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double heightBonus = y > startY ? (y - startY) * 0.5 : 0; // Prefer higher locations
                        double score = heightBonus - distance;

                        if (score > bestScore) {
                            bestScore = score;
                            bestLocation = new Location(world, x + 0.5, y + 1.0, z + 0.5);
                        }
                    }
                }
            }
        }

        return bestLocation != null ? bestLocation : keystoneLocation.clone().add(0.5, 1.0, 0.5);
    }

    private void spawnCelebration(KeystoneInstance instance) {
        World world = instance.location.getWorld();
        Location base = findOptimalFireworkLocation(instance.location);
        world.spawnParticle(Particle.END_ROD, base, 200, 3, 4, 3, 0.05);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, base, 120, 2, 2, 2, 0.05);
        for (int i = 0; i < 6; i++) {
            Location spawn = base.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3),
                    ThreadLocalRandom.current().nextDouble(0, 2),
                    ThreadLocalRandom.current().nextDouble(-3, 3));
            Firework firework = world.spawn(spawn, Firework.class, fw -> {
                var meta = fw.getFireworkMeta();
                meta.setPower(1);
                meta.addEffect(org.bukkit.FireworkEffect.builder()
                        .withColor(org.bukkit.Color.LIME)
                        .withFade(org.bukkit.Color.WHITE)
                        .flicker(true)
                        .trail(true)
                        .build());
                fw.setFireworkMeta(meta);
            });
        }
    }

    private void ensureInstancesLoaded(World world, Map<String, KeystoneInstance> worldMap) {
        UUID worldId = world.getUID();
        for (KeystoneInstance existing : worldMap.values()) {
            if (existing.location.getWorld() == null) {
                existing.location.setWorld(world);
            }
            ensureFrameApplied(existing);
        }
        for (KeystoneDefinition definition : definitions.values()) {
            if (worldMap.containsKey(definition.key)) {
                continue;
            }
            StructureStore store = StructureStore.get(plugin);
            Collection<StructureStore.StructEntry> entries = store.getStructuresForName(worldId.toString(), definition.baseStructureName);
            if (!entries.isEmpty()) {
                StructureStore.StructEntry entry = entries.iterator().next();
                Location location = new Location(world, entry.x, entry.y, entry.z);
                KeystoneInstance instance = new KeystoneInstance(definition, location);
                worldMap.put(definition.key, instance);
                ensureFrameApplied(instance);
                dirty = true;
                saveIfDirty();
            }
        }
    }

    private void ensureFrameApplied(KeystoneInstance instance) {
        if (instance.frameInitialized) {
            return;
        }
        if (instance.location.getWorld() == null) {
            return;
        }
        String frame = instance.definition.frames.get(Math.min(instance.currentFrameIndex, instance.definition.frames.size() - 1));
        schemManager.placeStructure(frame, instance.location.clone(), false);
        instance.frameInitialized = true;
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        save();
        dirty = false;
    }

    public void save() {
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }
        org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, Map<String, KeystoneInstance>> worldEntry : instances.entrySet()) {
            String worldKey = worldEntry.getKey().toString();
            org.bukkit.configuration.ConfigurationSection worldSection = yml.createSection(worldKey);
            for (KeystoneInstance instance : worldEntry.getValue().values()) {
                org.bukkit.configuration.ConfigurationSection section = worldSection.createSection(instance.definition.key);
                section.set("x", instance.location.getBlockX());
                section.set("y", instance.location.getBlockY());
                section.set("z", instance.location.getBlockZ());
                section.set("progress", instance.progress);
                section.set("frameIndex", instance.currentFrameIndex);
                section.set("completed", instance.completed);
            }
        }
        try {
            yml.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save keystone data", ex);
        }
    }

    private void load() {
        instances.clear();
        if (!dataFile.exists()) {
            return;
        }
        org.bukkit.configuration.file.YamlConfiguration yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        for (String worldKey : yml.getKeys(false)) {
            World world = Bukkit.getWorld(UUID.fromString(worldKey));
            org.bukkit.configuration.ConfigurationSection section = yml.getConfigurationSection(worldKey);
            if (section == null) continue;
            Map<String, KeystoneInstance> map = new HashMap<>();
            for (String key : section.getKeys(false)) {
                KeystoneDefinition definition = definitions.get(key);
                if (definition == null) continue;
                org.bukkit.configuration.ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) continue;
                int x = entry.getInt("x");
                int y = entry.getInt("y");
                int z = entry.getInt("z");
                KeystoneInstance instance = new KeystoneInstance(definition, world == null ? new Location(null, x, y, z) : new Location(world, x, y, z));
                instance.progress = entry.getInt("progress");
                instance.currentFrameIndex = entry.getInt("frameIndex");
                instance.completed = entry.getBoolean("completed", false);
                map.put(definition.key, instance);
            }
            instances.put(UUID.fromString(worldKey), map);
        }
    }

    public static final class KeystoneDefinition {
        public final String key;
        public final String displayName;
        public final String baseStructureName;
        public final List<String> frames;
        public final int bounds;
        public final int height;
        public final int totalProgress;
        public final RequiredItem requiredItem;
        public final double protectionRadius;
        public final double notifyRadius;
        public final double rewardRadius;
        public final RewardApplier reward;

        public KeystoneDefinition(String key, String displayName, String baseStructureName, List<String> frames, int bounds,
                                  int height, int totalProgress, RequiredItem requiredItem, double protectionRadius,
                                  double notifyRadius, double rewardRadius, RewardApplier reward) {
            this.key = key.toLowerCase(Locale.ROOT);
            this.displayName = displayName;
            this.baseStructureName = baseStructureName;
            this.frames = frames;
            this.bounds = bounds;
            this.height = height;
            this.totalProgress = totalProgress;
            this.requiredItem = requiredItem;
            this.protectionRadius = protectionRadius;
            this.notifyRadius = notifyRadius;
            this.rewardRadius = rewardRadius;
            this.reward = reward;
        }

        public void applyReward(Player player) {
            reward.apply(player);
        }
    }

    public interface RewardApplier {
        void apply(Player player);
    }

    public static final class RequiredItem {
        private final Material material;
        private final String itemName;
        private ItemStack customItemCache; // Lazily-resolved item

        public RequiredItem(Material material) {
            this.material = material;
            this.itemName = null;
        }

        public RequiredItem(String itemName) {
            this.material = null;
            this.itemName = itemName;
        }

        public boolean test(ItemStack stack) {
            if (stack == null) return false;

            // Case 1: Constructed with a Material
            if (material != null) {
                return stack.getType() == material;
            }

            // Case 2: Constructed with a String name
            if (itemName != null) {
                // Lazily resolve and cache the item from the registry
                if (customItemCache == null) {
                    customItemCache = goat.projectLinearity.util.ItemRegistry.getItemByName(itemName);
                }
                // If resolution fails, this test will always fail.
                if (customItemCache == null) {
                    return false;
                }
                // isSimilar checks type and meta, but not amount.
                return stack.isSimilar(customItemCache);
            }
            return false;
        }
    }

    public static final class KeystoneInstance {
        final KeystoneDefinition definition;
        final Location location;
        int progress = 0;
        int currentFrameIndex = 0;
        final Queue<Integer> pendingFrames = new ArrayDeque<>();
        boolean processingFrame = false;
        boolean completed = false;
        boolean frameInitialized = false;

        KeystoneInstance(KeystoneDefinition definition, Location location) {
            this.definition = definition;
            this.location = location;
        }
    }
}
