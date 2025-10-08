/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 */
package goat.projectLinearity.world;

import goat.projectLinearity.world.ConsegrityRegions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class RegionTitleListener
implements Listener {
    private static final String WORLD_NAME = "Consegrity";
    private final Map<UUID, ConsegrityRegions.Region> lastRegion = new HashMap<UUID, ConsegrityRegions.Region>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!WORLD_NAME.equals(p.getWorld().getName())) {
            return;
        }
        ConsegrityRegions.Region r = ConsegrityRegions.regionAt(p.getWorld(), p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        this.lastRegion.put(p.getUniqueId(), r);
        this.sendTitle(p, r);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player p = event.getPlayer();

        World w = p.getWorld();
        if (!WORLD_NAME.equals(w.getName())) {
            return;
        }
        ConsegrityRegions.Region r = ConsegrityRegions.regionAt(w, event.getTo().getBlockX(), event.getTo().getBlockZ());
        ConsegrityRegions.Region prev = this.lastRegion.get(p.getUniqueId());
        if (prev != r) {
            this.lastRegion.put(p.getUniqueId(), r);
            this.sendTitle(p, r);
        }
    }

    private void sendTitle(Player p, ConsegrityRegions.Region r) {
        String title = switch (r) {
            default -> throw new MatchException(null, null);
            case ConsegrityRegions.Region.CENTRAL -> "Central";
            case ConsegrityRegions.Region.DESERT -> "Desert";
            case ConsegrityRegions.Region.SAVANNAH -> "Savannah";
            case ConsegrityRegions.Region.SWAMP -> "Swamp";
            case ConsegrityRegions.Region.JUNGLE -> "Jungle";
            case ConsegrityRegions.Region.MESA -> "Mesa";
            case ConsegrityRegions.Region.MOUNTAIN -> "Mountain";
            case ConsegrityRegions.Region.ICE_SPIKES -> "Ice Spikes";
            case ConsegrityRegions.Region.CHERRY -> "Cherry";
            case ConsegrityRegions.Region.OCEAN -> "Ocean";
        };
        try {
            p.sendTitle(title, "", 5, 40, 5);
        }
        catch (Throwable ignored) {
            p.sendMessage("You have entered: " + title);
        }
    }
}

