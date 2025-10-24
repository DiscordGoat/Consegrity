package goat.projectLinearity.subsystems.world.structure;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * Post-generation structure spawner. Mirrors the cadence of entity spawning,
 * executing small batches on a repeating task and reacting to newly generated chunks.
 * Persists its queue so unfinished work survives plugin reloads.
 */
public final class DeferredStructureSpawner implements Listener, Runnable {
    private static final String WORLD_NAME = "Consegrity";
    private static final int MAX_BUDGET = 64;
    private static final String SAVE_FILE = "structure-queue.dat";

    private final ProjectLinearity plugin;
    private final StructureManager manager;
    private final Queue<ChunkKey> queue = new ArrayDeque<>();

    public DeferredStructureSpawner(ProjectLinearity plugin, StructureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        loadPersistedQueue();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!WORLD_NAME.equals(world.getName())) return;
        if (plugin != null && plugin.isRegenInProgress()) return; // skip queuing during regen

        boolean isNew;
        try {
            isNew = event.isNewChunk();
        } catch (Throwable t) {
            isNew = (world.getFullTime() & 3) == 0; // coarse fallback
        }
        if (!isNew) return;

        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        // Add timestamp for delayed processing
        long enqueueTime = System.currentTimeMillis();
        queue.add(new ChunkKey(world.getName(), cx, cz, enqueueTime));
    }

    @Override
    public void run() {
        // Skip placement while regeneration is in progress.
        try {
            if (plugin != null && plugin.isRegenInProgress()) {
                return;
            }
        } catch (Throwable ignored) {}
        int budget = MAX_BUDGET;
        long now = System.currentTimeMillis();
        long delayMs = 120000; // 60 seconds delay
        while (budget-- > 0 && !queue.isEmpty()) {
            ChunkKey key = queue.poll();
            if (key == null) break;
            if (now - key.enqueueTime < delayMs) {
                // Put it back since it's not ready yet
                queue.add(key);
                break; // Stop processing for now
            }
            World world = Bukkit.getWorld(key.worldName);
            if (world == null) continue;

            long salt = (((long) key.cx) << 21) ^ (((long) key.cz) << 7) ^ 0x9E3779B97F4A7C15L;
            Random rng = new Random(world.getSeed() ^ salt);
            try {
                manager.tryPlaceOne(world, key.cx, key.cz, rng);
            } catch (Throwable t) {
                // keep loop robust
            }
        }
    }

    public void shutdown() {
        saveQueueState();
        queue.clear();
    }

    private void loadPersistedQueue() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                return;
            }
        }
        File file = new File(folder, SAVE_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 4) continue;
                try {
                    String worldName = parts[0];
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    long enqueueTime = Long.parseLong(parts[3]);
                    queue.add(new ChunkKey(worldName, cx, cz, enqueueTime));
                } catch (NumberFormatException ignored) {
                    // skip malformed entry
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to restore deferred structure queue: " + e.getMessage());
        }

        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed clearing deferred structure queue snapshot: " + e.getMessage());
        }
    }

    private void saveQueueState() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File file = new File(folder, SAVE_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (ChunkKey key : queue) {
                writer
                        .append(key.worldName)
                        .append(',')
                        .append(Integer.toString(key.cx))
                        .append(',')
                        .append(Integer.toString(key.cz))
                        .append(',')
                        .append(Long.toString(key.enqueueTime))
                        .append('\n');
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to persist deferred structure queue: " + e.getMessage());
        }
    }

    private static final class ChunkKey {
        final String worldName; final int cx, cz;
        final long enqueueTime;
        ChunkKey(String worldName, int cx, int cz, long enqueueTime) { 
            this.worldName = worldName; 
            this.cx = cx; 
            this.cz = cz; 
            this.enqueueTime = enqueueTime; 
        }
    }
}
