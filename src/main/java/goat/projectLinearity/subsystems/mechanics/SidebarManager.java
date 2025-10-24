package goat.projectLinearity.subsystems.mechanics;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.mining.MiningOxygenManager;
import goat.projectLinearity.subsystems.mechanics.spaces.Space;
import goat.projectLinearity.subsystems.mechanics.spaces.SpaceManager;
import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import goat.projectLinearity.subsystems.world.ConsegrityRegions.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds and maintains a player-specific sidebar showing temperature and oxygen stats,
 * while handling thermal side-effects such as hunger drain and freezing.
 */
public final class SidebarManager implements Listener {

    private static final DecimalFormat TEMP_FORMAT = new DecimalFormat("0.0");
    private static final Map<Region, Double> REGION_TEMPS = new EnumMap<>(Region.class);

    static {
        REGION_TEMPS.put(Region.CENTRAL, 72.0);
        REGION_TEMPS.put(Region.DESERT, 102.0);
        REGION_TEMPS.put(Region.SAVANNAH, 95.0);
        REGION_TEMPS.put(Region.SWAMP, 78.0);
        REGION_TEMPS.put(Region.JUNGLE, 90.0);
        REGION_TEMPS.put(Region.MESA, 134.0);
        REGION_TEMPS.put(Region.MOUNTAIN, 48.0);
        REGION_TEMPS.put(Region.ICE_SPIKES, -4.0);
        REGION_TEMPS.put(Region.CHERRY, 68.0);
        REGION_TEMPS.put(Region.OCEAN, 60.0);
        REGION_TEMPS.put(Region.NETHER, 150.0);
        REGION_TEMPS.put(Region.NETHER_WASTELAND, 150.0);
        REGION_TEMPS.put(Region.NETHER_BASIN, 140.0);
        REGION_TEMPS.put(Region.NETHER_CLIFF, 150.0);
        REGION_TEMPS.put(Region.NETHER_OCEAN, 150.0);
        REGION_TEMPS.put(Region.NETHER_BOUNDARY, 150.0);
    }

    private final ProjectLinearity plugin;
    private final SpaceManager spaceManager;
    private final MiningOxygenManager oxygenManager;
    private final Map<UUID, PlayerSidebarData> players = new HashMap<>();
    private BukkitTask updateTask;
    private BukkitTask thermalTask;

    public SidebarManager(ProjectLinearity plugin, SpaceManager spaceManager, MiningOxygenManager oxygenManager) {
        this.plugin = plugin;
        this.spaceManager = spaceManager;
        this.oxygenManager = oxygenManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTask();
    }

    private void startTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayer(player);
            }
        }, 20L, 20L);
        thermalTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double rate = 1.0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerSidebarData data = players.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSidebarData(player.getUniqueId()));
                if (data.scoreboard == null) {
                    createSidebar(player, data);
                }
                applyHeatTick(player, data);
                applyColdTick(player, data, rate);
            }
        }, 1L, 1L);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (thermalTask != null) {
            thermalTask.cancel();
            thermalTask = null;
        }
        for (PlayerSidebarData data : players.values()) {
            Player player = Bukkit.getPlayer(data.playerId);
            if (player != null && player.isOnline()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        players.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        setup(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        setup(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerSidebarData data = players.remove(event.getPlayer().getUniqueId());
        if (data != null) {
            event.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void setup(Player player) {
        PlayerSidebarData data = players.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSidebarData(player.getUniqueId()));
        createSidebar(player, data);
        updateSidebarLines(player, data);
    }

    public void setTemperature(Player player, double value) {
        PlayerSidebarData data = players.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSidebarData(player.getUniqueId()));
        if (data.scoreboard == null) {
            createSidebar(player, data);
        }
        data.temperature = value;
        data.targetTemperature = value;
        data.manualOverrideUntil = System.currentTimeMillis() + 2000L;
        updateSidebarLines(player, data);
    }

    public void initialise(Player player) {
        setup(player);
    }

    private void updatePlayer(Player player) {
        if (!player.isOnline()) return;
        PlayerSidebarData data = players.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSidebarData(player.getUniqueId()));
        if (data.scoreboard == null) {
            createSidebar(player, data);
            updateSidebarLines(player, data);
        }
        double rate = 1.0;
        evaluateTemperatureTarget(player, data);
        adjustTemperature(data, rate);
        updateSidebarLines(player, data);
    }

    private void createSidebar(Player player, PlayerSidebarData data) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("linearity", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "Project Linearity");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team tempLabel = board.registerNewTeam("tempLabel");
        String tempLabelEntry = ChatColor.DARK_RED.toString();
        tempLabel.addEntry(tempLabelEntry);
        objective.getScore(tempLabelEntry).setScore(4);
        tempLabel.setPrefix(ChatColor.GOLD + "Temperature");

        Team tempValue = board.registerNewTeam("tempValue");
        String tempValueEntry = ChatColor.RED.toString();
        tempValue.addEntry(tempValueEntry);
        objective.getScore(tempValueEntry).setScore(3);

        Team oxygenLabel = board.registerNewTeam("oxygenLabel");
        String oxygenLabelEntry = ChatColor.DARK_AQUA.toString();
        oxygenLabel.addEntry(oxygenLabelEntry);
        objective.getScore(oxygenLabelEntry).setScore(2);
        oxygenLabel.setPrefix(ChatColor.GOLD + "Oxygen");

        Team oxygenValue = board.registerNewTeam("oxygenValue");
        String oxygenValueEntry = ChatColor.DARK_BLUE.toString();
        oxygenValue.addEntry(oxygenValueEntry);
        objective.getScore(oxygenValueEntry).setScore(1);

        data.scoreboard = board;
        data.objective = objective;
        data.tempValueTeam = tempValue;
        data.oxygenValueTeam = oxygenValue;
        player.setScoreboard(board);
    }

    private void evaluateTemperatureTarget(Player player, PlayerSidebarData data) {
        if (System.currentTimeMillis() < data.manualOverrideUntil) {
            return;
        }
        Location loc = player.getLocation();
        double external = computeExternalTemperature(player, loc);
        Space space = spaceManager.findSpaceAt(player.getWorld(), loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ()).orElse(null);
        if (space != null) {
            if (!Objects.equals(data.currentSpaceId, space.getId())) {
                plugin.getLogger().info("[SpaceDebug] Player " + player.getName() + " entered space " + space.getId() + " temperature=" + TEMP_FORMAT.format(space.getTemperature()));
            }
            data.currentSpaceId = space.getId();
            data.targetTemperature = space.getTemperature();
        } else {
            if (data.currentSpaceId != null) {
                plugin.getLogger().info("[SpaceDebug] Player " + player.getName() + " left space " + data.currentSpaceId + ".");
            }
            data.currentSpaceId = null;
            data.targetTemperature = external;
        }
    }

    private double computeExternalTemperature(Player player, Location loc) {
        Region region = ConsegrityRegions.regionAt(player.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        double base = REGION_TEMPS.getOrDefault(region, 72.0);
        Biome biome = loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (biome == Biome.FROZEN_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN) {
            base = -64.0;
        } else if (biome == Biome.ICE_SPIKES) {
            base = -4.0;
        }
        return base;
    }

    private void adjustTemperature(PlayerSidebarData data, double rate) {
        double delta = data.targetTemperature - data.temperature;
        double abs = Math.abs(delta);
        if (abs < 0.05) {
            data.temperature = data.targetTemperature;
            return;
        }
        double step = delta > 0
                ? Math.min(abs, 0.15 * rate)
                : Math.min(abs, 0.1 * rate);
        data.temperature += Math.copySign(step, delta);
    }

    private void applyHeatTick(Player player, PlayerSidebarData data) {
        double temperature = data.temperature;
        if (temperature > 130.0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 1, true, false, false));
        } else if (temperature > 100.0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0, true, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.HUNGER);
        }
    }

    private void applyColdTick(Player player, PlayerSidebarData data, double rate) {
        double temperature = data.temperature;
        Location loc = player.getLocation();
        boolean moving = true;
        if (data.lastTickLocation != null && data.lastTickLocation.getWorld().equals(loc.getWorld())) {
            moving = data.lastTickLocation.distanceSquared(loc) > 0.0001;
        }
        data.lastTickLocation = loc.clone();

        Material blockType = loc.getBlock().getType();
        boolean inPowderSnow = blockType == Material.POWDER_SNOW;

        int freeze = player.getFreezeTicks();
        int maxFreeze = player.getMaxFreezeTicks();

        if (temperature <= -40.0) {
            freeze = maxFreeze;
        } else {
            if (!inPowderSnow && freeze > 0) {
                int thaw = (int) Math.max(1, Math.round(2 * rate));
                freeze = Math.max(0, freeze - thaw);
            }
            if (temperature < 0.0 && !moving) {
                int baseIncrease = 10;
                int increase = (int) Math.max(1, Math.round(baseIncrease * rate));
                freeze = Math.min(maxFreeze, freeze + increase);
            }
        }

        freeze = Math.min(maxFreeze, Math.max(0, freeze));
        player.setFreezeTicks(freeze);
    }

    private void updateSidebarLines(Player player, PlayerSidebarData data) {
        if (data.tempValueTeam == null || data.oxygenValueTeam == null) return;
        String tempColor = temperatureColor(data.temperature);
        data.tempValueTeam.setPrefix(tempColor + TEMP_FORMAT.format(data.temperature) + ChatColor.RESET + " °F");

        int oxygen = oxygenManager.getOxygen(player);
        String oxygenColor = oxygenColor(oxygen);
        data.oxygenValueTeam.setPrefix(oxygenColor + oxygen + ChatColor.RESET + "/600");
    }

    private String temperatureColor(double temperature) {
        if (temperature > 120) return ChatColor.DARK_RED.toString();
        if (temperature > 100) return ChatColor.RED.toString();
        if (temperature < -10) return ChatColor.BLUE.toString();
        if (temperature < 32) return ChatColor.AQUA.toString();
        return ChatColor.GREEN.toString();
    }

    private String oxygenColor(int oxygen) {
        if (oxygen <= 50) return ChatColor.DARK_RED.toString();
        if (oxygen <= 150) return ChatColor.GOLD.toString();
        return ChatColor.GREEN.toString();
    }

    private static final class PlayerSidebarData {
        final UUID playerId;
        Scoreboard scoreboard;
        Objective objective;
        Team tempValueTeam;
        Team oxygenValueTeam;
        double temperature = 72.0;
        double targetTemperature = 72.0;
        Location lastTickLocation;
        UUID currentSpaceId;
        long manualOverrideUntil = 0L;

        PlayerSidebarData(UUID playerId) {
            this.playerId = playerId;
        }
    }
}
