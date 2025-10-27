package goat.projectLinearity.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles visibility mechanics for the invisibility potion effect.
 */
public class InvisibilityListener implements Listener {
    private final JavaPlugin plugin;
    private static final String RECENTLY_ATTACKED_METADATA = "recently_attacked";
    
    public InvisibilityListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();
        
        // If the player is invisible and attacks a mob, make them temporarily visible
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY) && 
            target instanceof org.bukkit.entity.Mob) {
            
            // Mark player as having recently attacked
            player.setMetadata(RECENTLY_ATTACKED_METADATA, 
                new FixedMetadataValue(plugin, System.currentTimeMillis()));
            
            // Make the mob target the player
            ((org.bukkit.entity.Mob) target).setTarget(player);
            
            // Schedule removal of the metadata after 5 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isValid()) {
                    player.removeMetadata(RECENTLY_ATTACKED_METADATA, plugin);
                }
            }, 100L); // 5 seconds (20 ticks/second * 5)
        }
    }
}
