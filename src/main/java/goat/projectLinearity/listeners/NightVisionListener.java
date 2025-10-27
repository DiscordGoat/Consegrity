package goat.projectLinearity.listeners;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles night vision potion effects for mobs.
 */
public class NightVisionListener implements Listener {
    
    private final ProjectLinearity plugin;
    
    public NightVisionListener(ProjectLinearity plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        // Only care about night vision effects
        if (event.getModifiedType() != PotionEffectType.NIGHT_VISION) {
            return;
        }
        
        // Only handle mobs
        if (!(event.getEntity() instanceof org.bukkit.entity.Mob mob)) {
            return;
        }
        
        // When night vision is applied
        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED || 
            event.getAction() == EntityPotionEffectEvent.Action.CHANGED) {
            
            // Store the original follow range if not already stored
            if (!mob.hasMetadata("originalFollowRange")) {
                double originalRange = mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE).getBaseValue();
                mob.setMetadata("originalFollowRange", 
                    new org.bukkit.metadata.FixedMetadataValue(plugin, originalRange));
                
                // Increase follow range by 50%
                double newRange = originalRange * 1.5;
                mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(newRange);
            }
        }
        // When night vision is removed
        else if (event.getAction() == EntityPotionEffectEvent.Action.REMOVED || 
                 event.getAction() == EntityPotionEffectEvent.Action.CLEARED) {
            
            // Restore original follow range if we have it stored
            if (mob.hasMetadata("originalFollowRange")) {
                double originalRange = mob.getMetadata("originalFollowRange").get(0).asDouble();
                mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(originalRange);
                mob.removeMetadata("originalFollowRange", plugin);
            }
        }
    }
    
    /**
     * Cleans up any modified mobs when they die or the plugin disables.
     */
    public void cleanup() {
        // This would be called on plugin disable to clean up any modified mobs
        // Implementation would depend on how you want to handle this
    }
}
