package goat.projectLinearity.subsystems.world.samurai;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class CherrySamuraiDeathListener implements Listener {

    private final SamuraiPopulationManager populationManager;

    public CherrySamuraiDeathListener(SamuraiPopulationManager populationManager) {
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!populationManager.isSamuraiEntity(event.getEntity())) {
            return;
        }

        LivingEntity entity = event.getEntity();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // 80% chance to drop 1-3 rosegold chunks
        if (rng.nextDouble() < 0.8) {
            int chunkCount = 1 + rng.nextInt(3); // 1-3 chunks
            for (int i = 0; i < chunkCount; i++) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), ItemRegistry.getRosegoldChunk());
            }
        }

        // 20% chance to drop a rosegold ingot (separate roll)
        if (rng.nextDouble() < 0.2) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), ItemRegistry.getRosegoldIngot());
        }
    }
}
