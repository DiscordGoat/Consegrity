package goat.projectLinearity.util.samurai;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Objects;

public final class CherrySamuraiDamageListener implements Listener {

    private static final PotionEffect HEALTH_BOOST = new PotionEffect(
            PotionEffectType.HEALTH_BOOST,
            Integer.MAX_VALUE,
            19,
            true,
            false,
            false
    );

    private final JavaPlugin plugin;
    private final SamuraiPopulationManager populationManager;
    private final CherrySamuraiBehaviour behaviour;

    public CherrySamuraiDamageListener(JavaPlugin plugin, SamuraiPopulationManager populationManager, CherrySamuraiBehaviour behaviour) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
        this.behaviour = behaviour;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!populationManager.isSamuraiEntity(event.getEntity())) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        
        boolean shouldSlideBack = true;
        boolean deflected = false;
        
        if (behaviour != null && event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Projectile projectile) {
                deflected = behaviour.handleProjectileDamage(living, projectile);
                if (deflected) {
                    event.setCancelled(true);
                    projectile.remove();
                    living.getWorld().playSound(living.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                    shouldSlideBack = false; // No slide back on deflected projectiles
                }
            }
            double loggedDamage = deflected ? 0.0 : event.getDamage();
            behaviour.handleDamage(living, damager, loggedDamage);
        }

        // Apply health boost after handling damage to ensure proper health calculation
        if (!living.hasPotionEffect(PotionEffectType.HEALTH_BOOST)) {
            living.addPotionEffect(HEALTH_BOOST);
            living.setHealth(living.getMaxHealth());
        }

        // Always apply slide back effect unless it was a deflected projectile
        if (shouldSlideBack) {
            scheduleDownwardForce(living);
            slideBackFromSource(living, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSamuraiDealDamage(EntityDamageByEntityEvent event) {
        if (behaviour == null) {
            return;
        }
        Entity damager = event.getDamager();
        LivingEntity samurai = null;
        if (populationManager.isSamuraiEntity(damager) && damager instanceof LivingEntity livingDamager) {
            samurai = livingDamager;
        } else if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter && populationManager.isSamuraiEntity(livingShooter)) {
                samurai = livingShooter;
            }
        }
        if (samurai == null) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        behaviour.handleSamuraiAttack(samurai, victim);
    }

    private void scheduleDownwardForce(LivingEntity entity) {
        LivingEntity target = entity;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target == null || !target.isValid()) {
                return;
            }
            Vector adjusted = target.getVelocity().clone();
            if (adjusted.getY() > -0.6) {
                adjusted.setY(-0.6);
                target.setVelocity(adjusted);
            }
        }, 1L);
    }

    private void slideBackFromSource(LivingEntity samurai, EntityDamageEvent event) {
        if (samurai == null || !samurai.isValid()) {
            return;
        }
        Vector away;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Projectile projectile && projectile.getLocation() != null) {
                away = samurai.getLocation().toVector().subtract(projectile.getLocation().toVector());
            } else if (damager != null && damager.getLocation() != null) {
                away = samurai.getLocation().toVector().subtract(damager.getLocation().toVector());
            } else {
                away = samurai.getLocation().getDirection().multiply(-1);
            }
        } else {
            away = samurai.getLocation().getDirection().multiply(-1);
        }
        away.setY(0.0);
        if (away.lengthSquared() < 1.0e-4) {
            away = new Vector(1.0, 0.0, 0.0);
        }
        away.normalize().multiply(0.9);
        Vector velocity = samurai.getVelocity().clone();
        velocity.add(away);
        velocity.setY(samurai.getVelocity().getY());
        samurai.setVelocity(velocity);
    }
}
