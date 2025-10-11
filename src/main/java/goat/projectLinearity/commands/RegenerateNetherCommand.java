package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.world.ConsegrityNetherChunkGenerator;
import goat.projectLinearity.world.structure.StructureManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RegenerateNetherCommand implements CommandExecutor {
    public static final String WORLD_NAME = "ConsegrityNether";
    private final ProjectLinearity plugin;

    public RegenerateNetherCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        player.sendMessage("Regenerating Nether world '" + WORLD_NAME + "'...");
        try { plugin.setRegenInProgress(true); } catch (Throwable ignored) {}

        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            World fallback = Bukkit.getWorld(RegenerateCommand.WORLD_NAME);
            if (fallback == null) {
                fallback = Bukkit.getWorlds().stream().filter(w -> !WORLD_NAME.equals(w.getName())).findFirst().orElse(existing);
            }
            World finalFallback = fallback != null ? fallback : existing;
            existing.getPlayers().forEach(p -> p.teleport(finalFallback.getSpawnLocation()));
            if (!Bukkit.unloadWorld(existing, false)) {
                player.sendMessage("Failed to unload existing Nether world. Try again.");
                try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
                return true;
            }
            try {
                deleteWorldFolder(existing.getWorldFolder().toPath());
            } catch (IOException e) {
                player.sendMessage("Failed to delete Nether world folder: " + e.getMessage());
                try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
                return true;
            }
        }

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NETHER);
        creator.generateStructures(false);
        creator.generator(new ConsegrityNetherChunkGenerator());
        World nether = Bukkit.createWorld(creator);
        if (nether == null) {
            player.sendMessage("Nether world creation failed.");
            try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
            return true;
        }

        try { nether.setKeepSpawnInMemory(false); } catch (Throwable ignored) {}
        try { nether.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true); } catch (Throwable ignored) {}

        StructureManager structureManager = plugin.getStructureManager();
        if (structureManager != null) {
            try {
                structureManager.enforceCountsInstant(nether);
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to seed Nether structures: " + t.getMessage());
            }
        }

        Location spawn = locateSpawn(nether, 0, 0);
        nether.setSpawnLocation(spawn);
        player.teleport(spawn);
        try {
            player.sendTitle("Nether Central", "", 5, 40, 5);
        } catch (Throwable ignored) {
            player.sendMessage("Welcome to the Nether Central sector.");
        }

        try { plugin.setRegenInProgress(false); } catch (Throwable ignored) {}
        return true;
    }

    private Location locateSpawn(World world, int x, int z) {
        int ceilingLimit = CEILING_Y_SAFE();
        int minY = world.getMinHeight();
        for (int y = ceilingLimit; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (type == Material.AIR || type == Material.LAVA || type == Material.BEDROCK) continue;
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return new Location(world, x + 0.5, minY + 2.0, z + 0.5);
    }

    private int CEILING_Y_SAFE() {
        return CEILING_Y - CEILING_LAYER_COUNT - 1;
    }

    private static final int CEILING_Y = 128;
    private static final int CEILING_LAYER_COUNT = 4;

    private void deleteWorldFolder(Path worldPath) throws IOException {
        if (worldPath == null) return;
        if (!Files.exists(worldPath)) return;
        Files.walkFileTree(worldPath, new SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
}
