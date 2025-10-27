package goat.projectLinearity.subsystems.mechanics;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager;
import goat.projectLinearity.subsystems.world.desert.CurseManager;
import goat.projectLinearity.subsystems.world.desert.CurseRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/**
 * Manages the tablist display for showing active curses to players.
 */
public final class TablistManager {

    private final ProjectLinearity plugin;
    private final CurseManager curseManager;
    private CustomPotionEffectManager potionEffectManager;
    private final BukkitTask updateTask;
    private final Map<UUID, String> lastTablistContent = new HashMap<>();
    private final Set<UUID> fireDebug = new HashSet<>();
    private final Set<UUID> oxygenDebug = new HashSet<>();

    public TablistManager(ProjectLinearity plugin, CurseManager curseManager) {
        this.plugin = plugin;
        this.curseManager = curseManager;

        // Update tablist every 20 ticks (1 second) to show curse durations
        this.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllTablists();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void setPotionEffectManager(CustomPotionEffectManager manager) {
        this.potionEffectManager = manager;
    }

    /**
     * Updates the tablist for all online players to show their active curses.
     */
    private void updateAllTablists() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTablist(player);
        }
    }

    /**
     * Updates a specific player's tablist to show their active curses.
     */
    public void updatePlayerTablist(Player player) {
        Map<CurseRegistry.Curse, Long> activeCurses = curseManager.getActiveCurses(player);

        List<String> potionLines = potionEffectManager != null ? potionEffectManager.getTabLines(player) : Collections.emptyList();
        boolean hasCurses = !activeCurses.isEmpty();
        boolean hasPotions = potionLines != null && !potionLines.isEmpty();
        boolean hasDebug = fireDebug.contains(player.getUniqueId()) || oxygenDebug.contains(player.getUniqueId());

        if (!hasCurses && !hasPotions && !hasDebug) {
            clearPlayerTablist(player);
            return;
        }

        StringBuilder footer = new StringBuilder();
        footer.append("\n");

        if (hasCurses) {
            footer.append(ChatColor.DARK_RED).append("§l⚠ CURSES ⚠").append("\n");
            for (Map.Entry<CurseRegistry.Curse, Long> entry : activeCurses.entrySet()) {
                CurseRegistry.Curse curse = entry.getKey();
                long remainingSeconds = entry.getValue() / 1000;

                footer.append(ChatColor.RED)
                      .append(curse.displayName())
                      .append(ChatColor.GRAY)
                      .append(": ")
                      .append(ChatColor.YELLOW)
                      .append(remainingSeconds)
                      .append("s")
                      .append("\n");
            }
        }

        if (hasPotions) {
            if (hasCurses) {
                footer.append("\n");
            }
            footer.append(ChatColor.DARK_AQUA).append("§lPOTIONS").append("\n");
            for (String line : potionLines) {
                footer.append(line).append("\n");
            }
        }

        if (hasDebug) {
            if (footer.length() > 1) {
                footer.append("\n");
            }
            footer.append(ChatColor.GRAY).append("§lDEBUG").append("\n");
            if (fireDebug.contains(player.getUniqueId())) {
                footer.append(ChatColor.RED)
                        .append("Fire Ticks: ")
                        .append(ChatColor.YELLOW)
                        .append(player.getFireTicks())
                        .append("\n");
            }
            if (oxygenDebug.contains(player.getUniqueId())) {
                int remainingAir = player.getRemainingAir();
                int maxAir = player.getMaximumAir();
                footer.append(ChatColor.AQUA)
                        .append("Vanilla Air: ")
                        .append(ChatColor.YELLOW)
                        .append(remainingAir)
                        .append(ChatColor.GRAY)
                        .append("/")
                        .append(ChatColor.YELLOW)
                        .append(maxAir)
                        .append("\n");
                if (plugin.getMiningOxygenManager() != null) {
                    int miningOxygen = plugin.getMiningOxygenManager().getOxygen(player);
                    footer.append(ChatColor.AQUA)
                            .append("Mining Oxygen: ")
                            .append(ChatColor.YELLOW)
                            .append(miningOxygen)
                            .append(ChatColor.GRAY)
                            .append("/")
                            .append(ChatColor.YELLOW)
                            .append(600)
                            .append("\n");
                }
            }
        }

        String newContent = footer.toString();

        // Only update if content changed to avoid unnecessary packets
        String lastContent = lastTablistContent.get(player.getUniqueId());
        if (lastContent == null || !lastContent.equals(newContent)) {
            setPlayerTablistHeaderFooter(player, newContent);
            lastTablistContent.put(player.getUniqueId(), newContent);
        }
    }

    /**
     * Sets the tablist header and footer for a player.
     */
    private void setPlayerTablistHeaderFooter(Player player, String footer) {
        // Create or get the player's scoreboard
        Scoreboard scoreboard = player.getScoreboard();

        // Set header (empty for now)
        String header = ChatColor.GOLD + "§lConsegrity";

        // Set footer with curse information
        player.setPlayerListHeaderFooter(header, footer);
    }

    /**
     * Clears the curse display from a player's tablist.
     */
    private void clearPlayerTablist(Player player) {
        String emptyFooter = "";
        setPlayerTablistHeaderFooter(player, emptyFooter);
        lastTablistContent.remove(player.getUniqueId());
    }

    /**
     * Manually refresh a player's tablist display.
     */
    public void refreshPlayer(Player player) {
        updatePlayerTablist(player);
    }

    /**
     * Shuts down the tablist manager and cancels the update task.
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        lastTablistContent.clear();
        fireDebug.clear();
        oxygenDebug.clear();
    }

    public boolean toggleFireDebug(Player player) {
        UUID id = player.getUniqueId();
        if (fireDebug.contains(id)) {
            fireDebug.remove(id);
            return false;
        }
        fireDebug.add(id);
        return true;
    }

    public boolean toggleOxygenDebug(Player player) {
        UUID id = player.getUniqueId();
        if (oxygenDebug.contains(id)) {
            oxygenDebug.remove(id);
            return false;
        }
        oxygenDebug.add(id);
        return true;
    }
}
