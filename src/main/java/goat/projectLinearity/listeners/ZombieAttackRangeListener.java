package goat.projectLinearity.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ZombieAttackRangeListener implements Listener {
    
    private final JavaPlugin plugin;
    private static final double MAX_ATTACK_DISTANCE = 1.0; // 2 blocks
    private static final double MAX_ATTACK_DISTANCE_SQ = MAX_ATTACK_DISTANCE * MAX_ATTACK_DISTANCE;

    public ZombieAttackRangeListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damagee = event.getEntity();

        // Only process if the damager is a zombie and the target is a player or another living entity
        if (!(damager instanceof Zombie) || !(damagee instanceof LivingEntity)) {
            return;
        }

        // Check the distance between the zombie and its target
        double distanceSq = damager.getLocation().distanceSquared(damagee.getLocation());
        
        // If the distance is greater than 2 blocks, cancel the event
        if (distanceSq > MAX_ATTACK_DISTANCE_SQ) {
            event.setCancelled(true);
        }
    }
}
