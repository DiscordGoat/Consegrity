package goat.projectLinearity.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Queues entity spawns while chunks are being generated and flushes them once
 * the chunk has fully loaded on the main server thread. This avoids calling
 * Bukkit entity APIs from inside the chunk generator which can run on a worker
 * thread on modern server implementations.
 */
public final class DeferredSpawnManager implements Listener {
    private static DeferredSpawnManager instance;

    private final Plugin plugin;
    private final Map<String, Map<Long, List<SpawnTask>>> pending = new ConcurrentHashMap<>();

    private DeferredSpawnManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void initialize(Plugin plugin) {
        if (instance != null) return;
        instance = new DeferredSpawnManager(plugin);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
    }

    public static void queueEntitySpawn(World world, int chunkX, int chunkZ, double x, double y, double z, EntityType type) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(type, "type");
        if (instance == null) {
            // Should not happen, but guard to avoid NPEs if called very early.
            initialize(JavaPlugin.getProvidingPlugin(DeferredSpawnManager.class));
        }
        instance.enqueue(world, chunkX, chunkZ, x, y, z, type);
    }

    private void enqueue(World world, int chunkX, int chunkZ, double x, double y, double z, EntityType type) {
        long chunkKey = chunkKey(chunkX, chunkZ);
        String worldName = world.getName();
        pending
            .computeIfAbsent(worldName, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey, key -> Collections.synchronizedList(new ArrayList<>()))
            .add(new SpawnTask(x, y, z, type));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        Map<Long, List<SpawnTask>> byChunk = pending.get(world.getName());
        if (byChunk == null || byChunk.isEmpty()) return;
        long key = chunkKey(event.getChunk().getX(), event.getChunk().getZ());
        List<SpawnTask> tasks = byChunk.remove(key);
        if (tasks == null || tasks.isEmpty()) {
            if (byChunk.isEmpty()) pending.remove(world.getName());
            return;
        }
        if (byChunk.isEmpty()) pending.remove(world.getName());
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SpawnTask task : tasks) {
                try {
                    Location location = new Location(world, task.x(), task.y(), task.z());
                    world.spawnEntity(location, task.type());
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private record SpawnTask(double x, double y, double z, EntityType type) {
    }
}
