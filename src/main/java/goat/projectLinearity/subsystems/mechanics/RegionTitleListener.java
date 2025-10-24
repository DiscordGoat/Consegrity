package goat.projectLinearity.subsystems.mechanics;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static goat.projectLinearity.subsystems.world.ConsegrityRegions.Region;

public class RegionTitleListener implements Listener {
    private static final String CONSEGRITY_WORLD = "Consegrity";

    private final Map<UUID, Region> lastRegion = new HashMap<>();
    private final ProjectLinearity plugin;

    public RegionTitleListener(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isTrackedWorld(player.getWorld())) return;

        Region region = ConsegrityRegions.regionAt(player.getWorld(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        lastRegion.put(player.getUniqueId(), region);
        sendTitle(player, region);
        showRegionTab(player, region);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!isTrackedWorld(world)) return;

        Region region = ConsegrityRegions.regionAt(world,
                event.getTo().getBlockX(),
                event.getTo().getBlockY(),
                event.getTo().getBlockZ());
        Region previous = lastRegion.get(player.getUniqueId());
        if (previous != region) {
            lastRegion.put(player.getUniqueId(), region);
            sendTitle(player, region);
            showRegionTab(player, region);
        }
    }

    private boolean isTrackedWorld(World world) {
        if (world == null) return false;
        String name = world.getName();
        return CONSEGRITY_WORLD.equals(name);
    }

    private void sendTitle(Player player, Region region) {
        String title = switch (region) {
            case CENTRAL -> "Central";
            case DESERT -> "Desert";
            case SAVANNAH -> "Savannah";
            case SWAMP -> "Swamp";
            case JUNGLE -> "Jungle";
            case MESA -> "Mesa";
            case MOUNTAIN -> "Mountain";
            case ICE_SPIKES -> "Ice Spikes";
            case CHERRY -> "Cherry";
            case OCEAN -> "Ocean";
            case NETHER -> "Nether";
            case NETHER_WASTELAND -> "The Wasteland";
            case NETHER_CLIFF -> "The Cliff";
            case NETHER_BASIN -> "Nether Basin";
            case NETHER_OCEAN -> "Nether Ocean";
            case NETHER_BOUNDARY -> "Nether Boundary";
        };
        try {
            player.sendTitle(title, "", 5, 40, 5);
        } catch (Throwable ignored) {
            player.sendMessage("You have entered: " + title);
        }
    }

    private void showRegionTab(Player player, Region region) {
        try {
            plugin.showRegionTab(player, region);
        } catch (Throwable ignored) {
        }
    }
}
