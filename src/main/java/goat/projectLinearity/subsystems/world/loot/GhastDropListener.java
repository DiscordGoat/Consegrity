package goat.projectLinearity.subsystems.world.loot;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adds the custom Golden Tear drop to ghasts.
 */
public final class GhastDropListener implements Listener {

    private static final double DROP_CHANCE = 0.10;

    @EventHandler(ignoreCancelled = true)
    public void onGhastDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.GHAST) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= DROP_CHANCE) {
            return;
        }

        ItemStack goldenTear = ItemRegistry.getGoldenTear();
        if (goldenTear == null) {
            return;
        }

        List<ItemStack> drops = event.getDrops();
        if (drops != null) {
            drops.add(goldenTear);
        } else {
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), goldenTear);
        }
    }
}
