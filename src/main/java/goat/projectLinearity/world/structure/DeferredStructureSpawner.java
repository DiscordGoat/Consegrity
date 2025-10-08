package goat.projectLinearity.world.structure;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * Post-generation structure spawner. Mirrors the cadence of entity spawning,
 * executing small batches on a repeating task and reacting to newly generated chunks.
 */
public final class DeferredStructureSpawner implements Listener, Runnable {
    private static final String WORLD_NAME = "Consegrity";
    private static final int MAX_BUDGET = 64;

    private final ProjectLinearity plugin;
    private final StructureManager manager;
    private final Queue<ChunkKey> queue = new ArrayDeque<>();

    public DeferredStructureSpawner(ProjectLinearity plugin, StructureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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
        queue.add(new ChunkKey(world.getName(), cx, cz));
    }

    @Override
    public void run() {
        // TPS gate: only place structures when server is healthy
        try {
            if (plugin != null && plugin.isRegenInProgress()) {
                return; // do not place during regen
            }
            if (plugin != null && plugin.getTpsMonitor() != null && !plugin.getTpsMonitor().isHealthy()) {
                return;
            }
        } catch (Throwable ignored) {}
        int budget = 16;
        try {
            double tps = (plugin != null && plugin.getTpsMonitor() != null) ? plugin.getTpsMonitor().getTps() : 20.0;
            if (tps >= 19.5) budget = 64;
            else if (tps >= 18.5) budget = 48;
            else if (tps >= 17.0) budget = 32;
            else if (tps >= 15.0) budget = 24;
            else budget = 16;
            if (budget > MAX_BUDGET) budget = MAX_BUDGET;
        } catch (Throwable ignored) {}
        while (budget-- > 0 && !queue.isEmpty()) {
            ChunkKey key = queue.poll();
            if (key == null) break;
            World world = Bukkit.getWorld(key.worldName);
            if (world == null) continue;

            long salt = (((long) key.cx) << 21) ^ (((long) key.cz) << 7) ^ 0x9E3779B97F4A7C15L;
            Random rng = new Random(world.getSeed() ^ salt);
            try {
                manager.tryPlaceOne(world, key.cx, key.cz, rng);
            } catch (Throwable t) {
                // keep loop robust; log once in a while
            }
        }
    }

    private static final class ChunkKey {
        final String worldName; final int cx, cz;
        ChunkKey(String worldName, int cx, int cz) { this.worldName = worldName; this.cx = cx; this.cz = cz; }
    }
}
