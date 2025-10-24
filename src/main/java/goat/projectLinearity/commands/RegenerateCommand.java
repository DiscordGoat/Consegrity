package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.world.ConsegrityChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RegenerateCommand implements CommandExecutor {
    public static final String WORLD_NAME = "Consegrity";
    private final ProjectLinearity plugin;

    // Legacy pregen settings removed; sector-level enforcement is used instead

    public RegenerateCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        boolean debug = args != null && args.length > 0 && "debug".equalsIgnoreCase(args[0]);
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        player.sendMessage("Regenerating world '" + WORLD_NAME + "'...");
        try { plugin.setRegenInProgress(true); } catch (Throwable ignored) {}

        // 1) Unload and delete existing world if present
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            // Move any players out
            // find or create a safe temporary world to move players during regen
            World fallback = Bukkit.getWorlds().stream().filter(w -> !w.getName().equals(WORLD_NAME)).findFirst().orElse(null);
            if (fallback == null) {
                try {
                    fallback = Bukkit.createWorld(new WorldCreator("consegrity_safe_temp"));
                } catch (Throwable ignored) {}
            }
            World finalFallback = fallback != null ? fallback : existing;
            existing.getPlayers().forEach(p -> p.teleport(finalFallback.getSpawnLocation()));
            boolean ok = Bukkit.unloadWorld(existing, false);
            if (!ok) {
                player.sendMessage("Failed to unload existing world. Try again.");
                return true;
            }
            try {
                deleteWorldFolder(existing.getWorldFolder().toPath());
            } catch (IOException e) {
                player.sendMessage("Failed to delete world folder: " + e.getMessage());
                return true;
            }
        }

        // 2) Create fresh world using ConsegrityChunkGenerator
        WorldCreator wc = new WorldCreator(WORLD_NAME);
        wc.generateStructures(false);
        wc.generator(new ConsegrityChunkGenerator());
        World newWorld = Bukkit.createWorld(wc);
        if (newWorld == null) {
            sender.sendMessage("World creation failed.");
            try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
            return true;
        }
        try { newWorld.setKeepSpawnInMemory(false); } catch (Throwable ignored) {}

        // 3) Set spawn and teleport immediately
        int sx = 0, sz = 0;
        int sy = newWorld.getHighestBlockYAt(sx, sz) + 1;
        newWorld.setSpawnLocation(sx, sy, sz);
        player.teleport(newWorld.getSpawnLocation());
        try { player.sendTitle("Central", "", 5, 40, 5); } catch (Throwable ignored) { player.sendMessage("Central"); }

        if (debug) {
            try {
                var sm = plugin.getStructureManager();
                if (sm != null) {
                    sm.clearDebug();
                    sm.setDebug(true);
                    plugin.getLogger().info("[Consegrity] Debug audit: beginning enforcement sweep to collect placement reasons...");
                    sm.enforceCountsInstant(newWorld);
                    String report = sm.debugSummary(newWorld.getUID().toString());
                    for (String line : report.split("\n")) plugin.getLogger().info(line);
                    sm.setDebug(false);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Debug audit encountered an issue: " + t.getMessage());
            }
        }

        try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
        return true;
    }

    private void deleteWorldFolder(Path worldPath) throws IOException {
        if (worldPath == null) return;
        if (!Files.exists(worldPath)) return;
        Files.walkFileTree(worldPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // Legacy pregen helper methods removed
}
