package goat.projectLinearity.world;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static goat.projectLinearity.world.ConsegrityRegions.Region;

public class RegionTitleListener implements Listener {
    private static final String WORLD_NAME = "Consegrity";
    private final Map<UUID, Region> lastRegion = new HashMap<>();
    private final ProjectLinearity plugin;

    public RegionTitleListener(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!WORLD_NAME.equals(p.getWorld().getName())) return;
        Region r = ConsegrityRegions.regionAt(p.getWorld(), p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        lastRegion.put(p.getUniqueId(), r);
        sendTitle(p, r);
        // Also show the relevant advancement tab for the region
        showRegionTab(p, r);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player p = event.getPlayer();
        World w = p.getWorld();
        if (!WORLD_NAME.equals(w.getName())) return;

        Region r = ConsegrityRegions.regionAt(w, event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());
        Region prev = lastRegion.get(p.getUniqueId());
        if (prev != r) {
            lastRegion.put(p.getUniqueId(), r);
            sendTitle(p, r);
            showRegionTab(p, r);
        }
    }

    private void sendTitle(Player p, Region r) {
        String title = switch (r) {
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
        };
        try {
            p.sendTitle(title, "", 5, 40, 5);
        } catch (Throwable ignored) {
            // In case API changes, fallback to chat
            p.sendMessage("You have entered: " + title);
        }
    }

    private void showRegionTab(Player p, Region r) {
        try {
            plugin.showRegionTab(p, r);
        } catch (Throwable ignored) {
        }
    }
}
