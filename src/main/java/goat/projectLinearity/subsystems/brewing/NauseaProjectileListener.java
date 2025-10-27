package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomises projectile trajectories for entities affected by the custom nausea potion.
 */
public final class NauseaProjectileListener implements Listener {

    private final CustomPotionEffectManager effectManager;

    public NauseaProjectileListener(ProjectLinearity plugin, CustomPotionEffectManager effectManager) {
        this.effectManager = effectManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        int potency = effectManager.getEffectPotency(shooter, "nausea");
        if (potency <= 0) {
            return;
        }
        Vector velocity = event.getEntity().getVelocity();
        double speed = velocity.length();
        if (speed <= 0.0) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double yawVariance = Math.toRadians(25 * potency);
        double pitchVariance = Math.toRadians(18 * potency);

        double baseYaw = velocity.getX() == 0 && velocity.getZ() == 0
                ? Math.toRadians(random.nextDouble(0, 360))
                : Math.atan2(velocity.getZ(), velocity.getX());
        double basePitch = Math.asin(Math.max(-1.0, Math.min(1.0, velocity.getY() / speed)));

        double newYaw = baseYaw + random.nextDouble(-yawVariance, yawVariance);
        double newPitch = Math.max(-Math.PI / 2 + 0.1, Math.min(Math.PI / 2 - 0.1, basePitch + random.nextDouble(-pitchVariance, pitchVariance)));

        Vector randomDirection = new Vector(
                Math.cos(newPitch) * Math.cos(newYaw),
                Math.sin(newPitch),
                Math.cos(newPitch) * Math.sin(newYaw)
        ).normalize();

        event.getEntity().setVelocity(randomDirection.multiply(speed));
    }
}
