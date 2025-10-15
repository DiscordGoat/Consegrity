package goat.projectLinearity.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.Set;

public class TreeFellingListener implements Listener {

    private static final Set<Material> LOG_MATERIALS = new HashSet<>();
    private static final Set<Material> LEAF_MATERIALS = new HashSet<>();

    static {
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (name.endsWith("_LOG")) {
                LOG_MATERIALS.add(mat);
            }
            if (name.endsWith("_LEAVES")) {
                LEAF_MATERIALS.add(mat);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Block brokenBlock = event.getBlock();
        if (!LOG_MATERIALS.contains(brokenBlock.getType())) {
            return;
        }

        // Check if this is the last log connected to the broken one
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (LOG_MATERIALS.contains(brokenBlock.getRelative(x, y, z).getType())) {
                        return; // Found another log, so not the last one
                    }
                }
            }
        }

        // It's the last log, break nearby leaves
        Location loc = brokenBlock.getLocation();
        int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (loc.clone().add(x, y, z).distanceSquared(loc) > radius * radius) continue;
                    Block block = loc.clone().add(x, y, z).getBlock();
                    if (LEAF_MATERIALS.contains(block.getType())) {
                        block.breakNaturally();
                    }
                }
            }
        }
    }
}
