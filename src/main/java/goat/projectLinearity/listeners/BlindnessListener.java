package goat.projectLinearity.listeners;

import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles mob targeting behavior when affected by blindness.
 */
public class BlindnessListener implements Listener {
    private final CustomPotionEffectManager potionEffectManager;
    
    public BlindnessListener(CustomPotionEffectManager potionEffectManager) {
        this.potionEffectManager = potionEffectManager;
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getTarget() instanceof Player)) {
            return;
        }
        
        // If the mob is affected by blindness
        if (mob.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            // Only allow targeting if the player is within 2 blocks
            if (mob.getLocation().distanceSquared(event.getTarget().getLocation()) > 4) { // 2 blocks squared = 4
                event.setCancelled(true);
            }
        }
    }
}
