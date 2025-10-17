package goat.projectLinearity.util;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the tablist display for showing active curses to players.
 */
public final class TablistManager {

    private final ProjectLinearity plugin;
    private final CurseManager curseManager;
    private final BukkitTask updateTask;
    private final Map<UUID, String> lastTablistContent = new HashMap<>();

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

        if (activeCurses.isEmpty()) {
            // Clear tablist if no curses
            clearPlayerTablist(player);
            return;
        }

        StringBuilder curseDisplay = new StringBuilder();
        curseDisplay.append("\n");
        curseDisplay.append(ChatColor.DARK_RED).append("§l⚠ CURSES ⚠").append("\n");

        for (Map.Entry<CurseRegistry.Curse, Long> entry : activeCurses.entrySet()) {
            CurseRegistry.Curse curse = entry.getKey();
            long remainingSeconds = entry.getValue() / 1000;

            curseDisplay.append(ChatColor.RED)
                       .append(curse.displayName())
                       .append(ChatColor.GRAY)
                       .append(": ")
                       .append(ChatColor.YELLOW)
                       .append(remainingSeconds)
                       .append("s")
                       .append("\n");
        }

        String newContent = curseDisplay.toString();

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
    }
}
