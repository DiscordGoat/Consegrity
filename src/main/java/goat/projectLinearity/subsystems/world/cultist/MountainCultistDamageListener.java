package goat.projectLinearity.subsystems.world.cultist;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

public final class MountainCultistDamageListener implements Listener {

    private final CultistPopulationManager populationManager;

    public MountainCultistDamageListener(CultistPopulationManager populationManager) {
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!populationManager.isCultistEntity(event.getEntity())) {
            return;
        }
        event.setDamage(0.0D);
    }
}
