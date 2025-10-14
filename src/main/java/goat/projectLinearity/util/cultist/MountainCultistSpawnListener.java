package goat.projectLinearity.util.cultist;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.world.ConsegrityRegions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MountainCultistSpawnListener implements Listener {

    private final ProjectLinearity plugin;
    private final Map<UUID, ConsegrityRegions.Region> lastRegion = new HashMap<>();

    public MountainCultistSpawnListener(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isTrackedWorld(player.getWorld())) {
            return;
        }
        ConsegrityRegions.Region region = ConsegrityRegions.regionAt(player.getWorld(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        lastRegion.put(player.getUniqueId(), region);
        if (region == ConsegrityRegions.Region.MOUNTAIN) {
            triggerRespawn(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!isTrackedWorld(world)) {
            return;
        }
        ConsegrityRegions.Region current = ConsegrityRegions.regionAt(world,
                event.getTo().getBlockX(),
                event.getTo().getBlockY(),
                event.getTo().getBlockZ());
        ConsegrityRegions.Region previous = lastRegion.put(player.getUniqueId(), current);
        if (current == ConsegrityRegions.Region.MOUNTAIN && previous != ConsegrityRegions.Region.MOUNTAIN) {
            triggerRespawn(player);
        }
    }

    private boolean isTrackedWorld(World world) {
        return world != null && "Consegrity".equals(world.getName());
    }

    private void triggerRespawn(Player player) {
        CultistPopulationManager manager = plugin.getCultistPopulationManager();
        MountainCultistBehaviour behaviour = plugin.getMountainCultistBehaviour();
        if (manager == null || behaviour == null) {
            return;
        }
        manager.setTargetPopulation(CultistPopulationManager.DEFAULT_POPULATION);
        behaviour.clearRoamStates();
        manager.respawnPopulation(player);
    }
}
