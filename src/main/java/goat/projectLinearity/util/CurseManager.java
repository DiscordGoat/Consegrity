package goat.projectLinearity.util;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages active curses on players with duration tracking.
 */
public final class CurseManager {

    private final ProjectLinearity plugin;
    private final Map<UUID, Map<CurseRegistry.Curse, Long>> activeCurses = new ConcurrentHashMap<>();
    private final BukkitTask cleanupTask;
    private final File dataFile;
    private BukkitTask pendingSaveTask;
    private TablistManager tablistManager;

    public CurseManager(ProjectLinearity plugin) {
        this.plugin = plugin;

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for curse persistence.");
        }
        this.dataFile = new File(dataFolder, "curses.yml");

        loadFromDisk();

        // Start cleanup task to remove expired curses every 20 ticks (1 second)
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCurses();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Sets the tablist manager for curse display updates.
     */
    public void setTablistManager(TablistManager tablistManager) {
        this.tablistManager = tablistManager;
        if (tablistManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tablistManager.updatePlayerTablist(player);
            }
        }
    }

    /**
     * Applies a curse to a player for a specific duration.
     */
    public void applyCurse(Player player, CurseRegistry.Curse curse, long durationTicks) {
        UUID playerId = player.getUniqueId();
        activeCurses.computeIfAbsent(playerId, k -> new HashMap<>());

        // Set or update the curse expiration time
        long expirationTime = System.currentTimeMillis() + (durationTicks * 50); // Convert ticks to milliseconds
        activeCurses.get(playerId).put(curse, expirationTime);

        player.sendMessage(curse.displayName() + " §7has been applied for §e" + (durationTicks / 20) + " seconds§7!");

        // Update tablist display
        if (tablistManager != null) {
            tablistManager.updatePlayerTablist(player);
        }

        scheduleSave();
    }

    /**
     * Removes a specific curse from a player.
     */
    public void removeCurse(Player player, CurseRegistry.Curse curse) {
        UUID playerId = player.getUniqueId();
        Map<CurseRegistry.Curse, Long> playerCurses = activeCurses.get(playerId);
        if (playerCurses != null) {
            playerCurses.remove(curse);
            if (playerCurses.isEmpty()) {
                activeCurses.remove(playerId);
            }

            // Update tablist display
            if (tablistManager != null) {
                tablistManager.updatePlayerTablist(player);
            }

            scheduleSave();
        }
    }

    /**
     * Removes all curses from a player.
     */
    public void removeAllCurses(Player player) {
        UUID playerId = player.getUniqueId();
        activeCurses.remove(playerId);

        // Update tablist display
        if (tablistManager != null) {
            tablistManager.updatePlayerTablist(player);
        }

        scheduleSave();
    }

    /**
     * Gets all active curses for a player with their remaining durations.
     */
    public Map<CurseRegistry.Curse, Long> getActiveCurses(Player player) {
        UUID playerId = player.getUniqueId();
        Map<CurseRegistry.Curse, Long> playerCurses = activeCurses.get(playerId);
        if (playerCurses == null) {
            return Collections.emptyMap();
        }

        // Return a copy with updated remaining times
        Map<CurseRegistry.Curse, Long> result = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<CurseRegistry.Curse, Long> entry : playerCurses.entrySet()) {
            long remaining = entry.getValue() - currentTime;
            if (remaining > 0) {
                result.put(entry.getKey(), remaining);
            }
        }

        return result;
    }

    /**
     * Gets all active curses for a player formatted for tablist display.
     */
    public List<String> getFormattedCurseDisplay(Player player) {
        Map<CurseRegistry.Curse, Long> curses = getActiveCurses(player);
        List<String> display = new ArrayList<>();

        for (Map.Entry<CurseRegistry.Curse, Long> entry : curses.entrySet()) {
            long remainingSeconds = entry.getValue() / 1000;
            display.add(entry.getKey().displayName() + "§7: §e" + remainingSeconds + "s");
        }

        return display;
    }

    /**
     * Checks if a player has a specific curse active.
     */
    public boolean hasCurse(Player player, CurseRegistry.Curse curse) {
        return hasCurse(player.getUniqueId(), curse);
    }

    public boolean hasCurse(Player player, String curseId) {
        CurseRegistry.Curse curse = CurseRegistry.getCurse(curseId);
        if (curse == null) {
            return false;
        }
        return hasCurse(player.getUniqueId(), curse);
    }

    public boolean hasCurse(UUID playerId, CurseRegistry.Curse curse) {
        if (curse == null) {
            return false;
        }
        Map<CurseRegistry.Curse, Long> playerCurses = activeCurses.get(playerId);
        if (playerCurses == null) {
            return false;
        }

        Long expirationTime = playerCurses.get(curse);
        if (expirationTime == null) {
            return false;
        }

        return expirationTime > System.currentTimeMillis();
    }

    /**
     * Gets the remaining duration of a specific curse for a player in seconds.
     */
    public long getCurseDuration(Player player, CurseRegistry.Curse curse) {
        UUID playerId = player.getUniqueId();
        Map<CurseRegistry.Curse, Long> playerCurses = activeCurses.get(playerId);
        if (playerCurses == null) {
            return 0;
        }

        Long expirationTime = playerCurses.get(curse);
        if (expirationTime == null) {
            return 0;
        }

        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    private void cleanupExpiredCurses() {
        long currentTime = System.currentTimeMillis();
        boolean dirty = false;

        for (UUID playerId : new ArrayList<>(activeCurses.keySet())) {
            Map<CurseRegistry.Curse, Long> playerCurses = activeCurses.get(playerId);
            if (playerCurses == null) {
                continue;
            }

            boolean playerChanged = false;
            Iterator<Map.Entry<CurseRegistry.Curse, Long>> iterator = playerCurses.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<CurseRegistry.Curse, Long> entry = iterator.next();
                if (entry.getValue() <= currentTime) {
                    iterator.remove();
                    playerChanged = true;
                }
            }

            if (playerCurses.isEmpty()) {
                activeCurses.remove(playerId);
                playerChanged = true;
            }

            if (playerChanged) {
                dirty = true;
            }

            if (tablistManager != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    tablistManager.updatePlayerTablist(player);
                }
            }
        }

        if (dirty) {
            scheduleSave();
        }
    }

    private void loadFromDisk() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        long now = System.currentTimeMillis();

        for (String playerKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ignoring invalid UUID '" + playerKey + "' in curses.yml.");
                continue;
            }

            ConfigurationSection cursesSection = playersSection.getConfigurationSection(playerKey);
            if (cursesSection == null) {
                continue;
            }

            Map<CurseRegistry.Curse, Long> curses = new HashMap<>();
            for (String curseId : cursesSection.getKeys(false)) {
                long expiration = cursesSection.getLong(curseId, -1L);
                if (expiration <= now) {
                    continue;
                }
                CurseRegistry.Curse curse = CurseRegistry.getCurse(curseId);
                if (curse == null) {
                    plugin.getLogger().warning("Unknown curse '" + curseId + "' for player " + playerId + " in curses.yml.");
                    continue;
                }
                curses.put(curse, expiration);
            }

            if (!curses.isEmpty()) {
                activeCurses.put(playerId, curses);
            }
        }
    }

    private void performSynchronousSave() {
        YamlConfiguration configuration = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<CurseRegistry.Curse, Long>> entry : activeCurses.entrySet()) {
            UUID playerId = entry.getKey();
            Map<CurseRegistry.Curse, Long> curses = entry.getValue();
            if (curses.isEmpty()) {
                continue;
            }

            for (Map.Entry<CurseRegistry.Curse, Long> curseEntry : curses.entrySet()) {
                long expiration = curseEntry.getValue();
                if (expiration <= now) {
                    continue;
                }
                configuration.set("players." + playerId + "." + curseEntry.getKey().id(), expiration);
            }
        }

        try {
            configuration.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save active curses to disk.", e);
        }
    }

    private void scheduleSave() {
        if (!plugin.isEnabled()) {
            performSynchronousSave();
            return;
        }
        synchronized (this) {
            if (pendingSaveTask != null) {
                return;
            }
            pendingSaveTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        performSynchronousSave();
                    } finally {
                        synchronized (CurseManager.this) {
                            pendingSaveTask = null;
                        }
                    }
                }
            }.runTaskLaterAsynchronously(plugin, 40L);
        }
    }

    private void flushPendingSave() {
        BukkitTask taskToCancel;
        synchronized (this) {
            taskToCancel = pendingSaveTask;
            pendingSaveTask = null;
        }
        if (taskToCancel != null) {
            taskToCancel.cancel();
        }
        performSynchronousSave();
    }

    /**
     * Shuts down the curse manager and cancels the cleanup task.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        flushPendingSave();
        activeCurses.clear();
    }
}
