package goat.projectLinearity.listeners;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles hunger potion effects for mobs, specifically zombie speed boost.
 */
public class HungerListener implements Listener {
    
    private final ProjectLinearity plugin;
    
    public HungerListener(ProjectLinearity plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        // Only care about hunger effects
        if (event.getModifiedType() != PotionEffectType.HUNGER) {
            return;
        }
        
        // Only handle zombies
        if (!(event.getEntity() instanceof org.bukkit.entity.Zombie)) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // When hunger effect is removed or cleared
        if ((event.getAction() == EntityPotionEffectEvent.Action.REMOVED || 
             event.getAction() == EntityPotionEffectEvent.Action.CLEARED) &&
            entity.hasMetadata("originalMovementSpeed")) {
            
            // Restore original movement speed
            org.bukkit.attribute.AttributeInstance movementSpeed = 
                entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
            
            if (movementSpeed != null) {
                double originalSpeed = entity.getMetadata("originalMovementSpeed").get(0).asDouble();
                movementSpeed.setBaseValue(originalSpeed);
                entity.removeMetadata("originalMovementSpeed", plugin);
            }
        }
    }
    
    /**
     * Cleans up any modified zombies when they die or the plugin disables.
     */
    public void cleanup() {
        // This would be called on plugin disable to clean up any modified zombies
        // Implementation would depend on how you want to handle this
    }
}
