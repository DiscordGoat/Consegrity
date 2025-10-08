package goat.projectLinearity.util;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks the custom Mining Oxygen stat and manages depletion/regeneration based on depth and sealed spaces.
 */
public final class MiningOxygenManager implements Listener {

    private static final int MAX_OXYGEN = 600;
    private static final int FLOOD_FILL_BUDGET = 3000;
    private static final int OXYGEN_TICK_INTERVAL_TICKS = 20;   // 1 second
    private static final int ENVIRONMENT_CHECK_TICKS = 100;     // 5 seconds
    private static final int REGEN_INTERVAL_SECONDS = 6;
    private static final int BREATH_INTERVAL_SECONDS = 6;

    private final ProjectLinearity plugin;
    private final SpaceManager spaceManager;
    private final NamespacedKey oxygenKey;

    private final Map<UUID, PlayerOxygenData> oxygenData = new HashMap<>();
    private BukkitTask oxygenTask;
    private BukkitTask environmentTask;

    private static final EnumSet<Material> PASSABLE_MATERIALS = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.WATER,
            Material.BUBBLE_COLUMN,
            Material.LAVA
    );
    private static final BlockFace[] FLOOD_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public MiningOxygenManager(ProjectLinearity plugin, SpaceManager spaceManager) {
        this.plugin = plugin;
        this.spaceManager = spaceManager;
        this.oxygenKey = new NamespacedKey(plugin, "mining_oxygen");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTasks();
    }

    private void handleDeepBreathing(Player player, PlayerOxygenData data, double rate) {
        data.regenCounter = 0;
        data.depleteAccumulator += rate;
        while (data.depleteAccumulator >= 1.0) {
            data.depleteAccumulator -= 1.0;
            if (data.oxygen > 0) {
                data.oxygen = Math.max(0, data.oxygen - 1);
                storeOxygen(player, data.oxygen);
            }
        }

        if (data.oxygen == 0 && !data.notifiedEmpty) {
            player.sendMessage(ChatColor.RED + "You have run out of oxygen!");
            data.notifiedEmpty = true;
        } else if (data.oxygen > 0) {
            data.notifiedEmpty = false;
        }

        applyDeepEffects(player, data);

        if (data.oxygen > 0 && data.oxygen <= 50) {
            data.breathTimer += rate;
            if (data.breathTimer >= BREATH_INTERVAL_SECONDS) {
                data.breathTimer -= BREATH_INTERVAL_SECONDS;
                playBreathingCue(player);
            }
        } else {
            data.breathTimer = 0.0;
        }
    }

    private void handleShallowRecovery(Player player, PlayerOxygenData data, double rate) {
        data.depleteAccumulator = 0.0;
        data.breathTimer = 0.0;
        data.regenCounter += rate;
        double interval = REGEN_INTERVAL_SECONDS;
        while (data.regenCounter >= interval) {
            data.regenCounter -= interval;
            if (data.oxygen < MAX_OXYGEN) {
                data.oxygen = Math.min(MAX_OXYGEN, data.oxygen + 1);
                storeOxygen(player, data.oxygen);
            }
        }
        data.notifiedEmpty = data.oxygen == 0;
        clearDeepEffects(player, data);
    }

    private void applyDeepEffects(Player player, PlayerOxygenData data) {
        int oxygen = data.oxygen;
        if (oxygen <= 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2, true, false, false));
            player.removePotionEffect(PotionEffectType.DARKNESS);
        } else {
            int amplifier = oxygen <= 150 ? 0 : -1;
            if (amplifier >= 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, amplifier, true, false, false));
            } else {
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            }
        }
        if (oxygen > 50) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    private void clearDeepEffects(Player player, PlayerOxygenData data) {
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        data.breathTimer = 0.0;
    }

    private void playBreathingCue(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.8f, 0.8f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 70, 0, true, false, false));
    }

    private void startTasks() {
        environmentTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshEnvironments, ENVIRONMENT_CHECK_TICKS, ENVIRONMENT_CHECK_TICKS);
        oxygenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickOxygenAll, OXYGEN_TICK_INTERVAL_TICKS, OXYGEN_TICK_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (oxygenTask != null) {
            oxygenTask.cancel();
            oxygenTask = null;
        }
        if (environmentTask != null) {
            environmentTask.cancel();
            environmentTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerOxygenData data = oxygenData.get(player.getUniqueId());
            if (data != null) {
                storeOxygen(player, data.oxygen);
            }
        }
        oxygenData.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerOxygenData data = getData(player);
        if (!data.initialised) {
            data.oxygen = 0;
            storeOxygen(player, data.oxygen);
            data.initialised = true;
        }
        evaluateEnvironment(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerOxygenData data = getData(player);
        data.oxygen = 0;
        data.regenCounter = 0;
        storeOxygen(player, data.oxygen);
        data.mode = OxygenMode.SHALLOW;
        data.notifiedEmpty = false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerOxygenData data = oxygenData.remove(player.getUniqueId());
        if (data != null) {
            storeOxygen(player, data.oxygen);
        }
    }

    @EventHandler
    public void onSpaceEnter(SpaceEnterEvent event) {
        scheduleEnvironmentCheck(event.getPlayer());
    }

    @EventHandler
    public void onSpaceLeave(SpaceLeaveEvent event) {
        scheduleEnvironmentCheck(event.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        scheduleEnvironmentCheck(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        scheduleEnvironmentCheck(event.getPlayer());
    }

    private void scheduleEnvironmentCheck(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> evaluateEnvironment(player));
    }

    private void refreshEnvironments() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            evaluateEnvironment(player);
        }
    }

    private void tickOxygenAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickOxygen(player);
        }
    }

    private void tickOxygen(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        PlayerOxygenData data = getData(player);
        double rate = plugin.getStatRate();
        switch (data.mode) {
            case DEEP -> handleDeepBreathing(player, data, rate);
            case PAUSED -> {
                data.regenCounter = 0;
                data.depleteAccumulator = 0.0;
                data.breathTimer = 0.0;
                clearDeepEffects(player, data);
            }
            case SHALLOW -> handleShallowRecovery(player, data, rate);
        }
    }

    private void evaluateEnvironment(Player player) {
        if (!player.isOnline()) return;
        PlayerOxygenData data = getData(player);
        OxygenMode previous = data.mode;
        OxygenMode now = determineMode(player);
        if (now != previous) {
            data.mode = now;
            data.regenCounter = 0;
            data.depleteAccumulator = 0.0;
            data.breathTimer = 0.0;
            if (now != OxygenMode.DEEP) {
                clearDeepEffects(player, data);
                data.notifiedEmpty = false;
            }
            sendModeMessage(player, now);
        } else {
            data.mode = now;
            if (now != OxygenMode.DEEP) {
                clearDeepEffects(player, data);
            }
        }
    }

    private OxygenMode determineMode(Player player) {
        Location loc = player.getLocation();
        Space space = null;
        if (spaceManager != null) {
            space = spaceManager.findSpaceAt(player.getWorld(), loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ()).orElse(null);
        }
        if (space != null) {
            return loc.getBlockY() >= 100 ? OxygenMode.SHALLOW : OxygenMode.PAUSED;
        }

        Block feet = loc.getBlock();
        Block above = feet.getRelative(BlockFace.UP);
        Block below = feet.getRelative(BlockFace.DOWN);
        boolean stoneEnvelope = above.getType() == Material.STONE && below.getType() == Material.STONE;
        if (stoneEnvelope) {
            boolean deep = isDeepCave(player, above);
            return deep ? OxygenMode.DEEP : OxygenMode.PAUSED;
        }
        return OxygenMode.SHALLOW;
    }

    private boolean isDeepCave(Player player, Block seedBlock) {
        Block seed = seedBlock;
        if (!isPassable(seed)) {
            seed = player.getLocation().getBlock();
            if (!isPassable(seed)) {
                return true;
            }
        }

        Queue<Block> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(seed);
        visited.add(pack(seed));

        int processed = 0;
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            if (isSkyExposed(current)) {
                return false;
            }
            processed++;
            if (processed >= FLOOD_FILL_BUDGET) {
                return true;
            }
            for (BlockFace face : FLOOD_FACES) {
                Block next = current.getRelative(face);
                if (!isWithinWorld(next)) continue;
                if (!isPassable(next)) continue;
                long key = pack(next);
                if (visited.add(key)) {
                    queue.add(next);
                }
            }
        }
        return true;
    }

    private boolean isWithinWorld(Block block) {
        int y = block.getY();
        return y >= block.getWorld().getMinHeight() && y < block.getWorld().getMaxHeight();
    }

    private boolean isPassable(Block block) {
        Material type = block.getType();
        return PASSABLE_MATERIALS.contains(type) || !type.isSolid();
    }

    private boolean isSkyExposed(Block block) {
        try {
            int highest = block.getWorld().getHighestBlockYAt(block.getX(), block.getZ());
            if (highest <= block.getY()) {
                return true;
            }
        } catch (Throwable ignored) { }
        try {
            return block.getLightFromSky() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void sendModeMessage(Player player, OxygenMode mode) {
        String message = switch (mode) {
            case SHALLOW -> ChatColor.AQUA + "Oxygen recovering.";
            case PAUSED -> ChatColor.YELLOW + "Oxygen stable.";
            case DEEP -> ChatColor.RED + "Oxygen depleting!";
        };
        player.sendMessage(message);
    }

    private PlayerOxygenData getData(Player player) {
        return oxygenData.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerOxygenData data = new PlayerOxygenData();
            data.oxygen = loadOxygen(player);
            data.mode = OxygenMode.SHALLOW;
            data.regenCounter = 0;
            return data;
        });
    }

    public int getOxygen(Player player) {
        return getData(player).oxygen;
    }

    public void setOxygen(Player player, int value, boolean announce) {
        PlayerOxygenData data = getData(player);
        data.oxygen = Math.max(0, Math.min(MAX_OXYGEN, value));
        storeOxygen(player, data.oxygen);
        data.notifiedEmpty = data.oxygen == 0;
        if (announce) {
            player.sendMessage(ChatColor.GREEN + "Oxygen set to " + data.oxygen + ".");
        }
    }

    public OxygenMode getMode(Player player) {
        return getData(player).mode;
    }

    private int loadOxygen(Player player) {
        PersistentDataContainer container = player.getPersistentDataContainer();
        Integer stored = container.get(oxygenKey, PersistentDataType.INTEGER);
        if (stored == null) {
            container.set(oxygenKey, PersistentDataType.INTEGER, 0);
            return 0;
        }
        return Math.max(0, Math.min(MAX_OXYGEN, stored));
    }

    private void storeOxygen(Player player, int value) {
        player.getPersistentDataContainer().set(oxygenKey, PersistentDataType.INTEGER, Math.max(0, Math.min(MAX_OXYGEN, value)));
    }

    private long pack(Block block) {
        return (((long) block.getX() & 0x3FFFFFFL) << 38)
                | (((long) block.getZ() & 0x3FFFFFFL) << 12)
                | ((long) (block.getY() & 0xFFFL));
    }

    private enum OxygenMode {
        SHALLOW,
        PAUSED,
        DEEP
    }

    private static final class PlayerOxygenData {
        int oxygen = 0;
        int regenCounter = 0;
        OxygenMode mode = OxygenMode.SHALLOW;
        boolean notifiedEmpty = false;
        double depleteAccumulator = 0.0;
        double breathTimer = 0.0;
        boolean initialised = false;
    }
}
