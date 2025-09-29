package goat.projectLinearity.commands;

import goat.projectLinearity.world.ConsegrityChunkGenerator;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        player.sendMessage("Regenerating world '" + WORLD_NAME + "'...");

        // 1) Unload and delete existing world if present
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            // Move any players out
            existing.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
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
            player.sendMessage("World creation failed.");
            return true;
        }
        try { newWorld.setKeepSpawnInMemory(false); } catch (Throwable ignored) {}

        // 3) Set spawn to Central region center and teleport
        int sx = 0, sz = 0;
        int sy = newWorld.getHighestBlockYAt(sx, sz) + 1;
        newWorld.setSpawnLocation(sx, sy, sz);
        player.teleport(newWorld.getSpawnLocation());
        try {
            player.sendTitle("Central", "", 5, 40, 5);
        } catch (Throwable ignored) {
            player.sendMessage("Central");
        }
        player.sendMessage("World regenerated and you have been teleported.");
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
}
