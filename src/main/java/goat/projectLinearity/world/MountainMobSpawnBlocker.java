package goat.projectLinearity.world;

import goat.projectLinearity.util.cultist.CultistPopulationManager;
import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Prevents vanilla monster spawns in the mountain region so only custom cultists appear.
 */
public final class MountainMobSpawnBlocker implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> BLOCKED_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.CHUNK_GEN,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION,
            CreatureSpawnEvent.SpawnReason.DEFAULT
    );

    private final CultistPopulationManager cultistPopulationManager;

    public MountainMobSpawnBlocker(CultistPopulationManager cultistPopulationManager) {
        this.cultistPopulationManager = Objects.requireNonNull(cultistPopulationManager, "cultistPopulationManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (!BLOCKED_REASONS.contains(reason)) {
            return;
        }
        if (cultistPopulationManager.isCultistEntity(entity)) {
            return;
        }

        Location location = event.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        ConsegrityRegions.Region region = ConsegrityRegions.regionAt(world, location.getBlockX(), location.getBlockZ());
        if (region != ConsegrityRegions.Region.MOUNTAIN) {
            return;
        }

        event.setCancelled(true);
    }
}
