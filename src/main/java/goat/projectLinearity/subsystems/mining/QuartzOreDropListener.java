package goat.projectLinearity.subsystems.mining;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles custom gemstone drops from Nether quartz ore.
 */
public final class QuartzOreDropListener implements Listener {

    private static final double REFINED_DROP_CHANCE = 0.10;
    private static final double STARCUT_DROP_CHANCE = 0.01;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuartzOreBreak(BlockBreakEvent event) {
        if (!event.isDropItems()) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.NETHER_QUARTZ_ORE) {
            return;
        }
        if (block.getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() < REFINED_DROP_CHANCE) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), ItemRegistry.getRefinedQuartz());
        }
        if (random.nextDouble() < STARCUT_DROP_CHANCE) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), ItemRegistry.getStarcutQuartz());
        }
    }
}
