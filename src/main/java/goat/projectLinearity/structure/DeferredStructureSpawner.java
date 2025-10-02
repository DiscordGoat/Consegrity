package goat.projectLinearity.structure;

import goat.projectLinearity.world.ConsegrityRegions;
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
    private static final int BATCH_BUDGET = 8;

    private final StructureManager manager;
    private final Queue<ChunkKey> queue = new ArrayDeque<>();

    public DeferredStructureSpawner(StructureManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (!WORLD_NAME.equals(world.getName())) return;

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
        int budget = BATCH_BUDGET;
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

