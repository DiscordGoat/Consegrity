/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.WorldCreator
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.generator.ChunkGenerator
 */
package goat.projectLinearity.commands;

import goat.projectLinearity.world.ConsegrityChunkGenerator;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

public class RegenerateCommand
implements CommandExecutor {
    public static final String WORLD_NAME = "Consegrity";

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }
        player.sendMessage("Regenerating world 'Consegrity'...");
        World existing = Bukkit.getWorld((String)WORLD_NAME);
        if (existing != null) {
            existing.getPlayers().forEach(p -> p.teleport(((World)Bukkit.getWorlds().get(0)).getSpawnLocation()));
            boolean ok = Bukkit.unloadWorld((World)existing, (boolean)false);
            if (!ok) {
                player.sendMessage("Failed to unload existing world. Try again.");
                return true;
            }
            try {
                this.deleteWorldFolder(existing.getWorldFolder().toPath());
            }
            catch (IOException e) {
                player.sendMessage("Failed to delete world folder: " + e.getMessage());
                return true;
            }
        }
        WorldCreator wc = new WorldCreator(WORLD_NAME);
        wc.generateStructures(false);
        wc.generator((ChunkGenerator)new ConsegrityChunkGenerator());
        World newWorld = Bukkit.createWorld((WorldCreator)wc);
        if (newWorld == null) {
            player.sendMessage("World creation failed.");
            return true;
        }
        try {
            newWorld.setKeepSpawnInMemory(false);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        int sx = 0;
        int sz = 0;
        int sy = newWorld.getHighestBlockYAt(sx, sz) + 1;
        newWorld.setSpawnLocation(sx, sy, sz);
        player.teleport(newWorld.getSpawnLocation());
        try {
            player.sendTitle("Central", "", 5, 40, 5);

        }
        catch (Throwable ignored) {
            player.sendMessage("Central");
        }
        player.sendMessage("World regenerated and you have been teleported.");
        return true;
    }

    private void deleteWorldFolder(Path worldPath) throws IOException {
        if (worldPath == null) {
            return;
        }
        if (!Files.exists(worldPath, new LinkOption[0])) {
            return;
        }
        Files.walkFileTree(worldPath, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){

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

