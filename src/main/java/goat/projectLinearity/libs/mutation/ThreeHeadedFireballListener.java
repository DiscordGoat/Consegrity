package goat.projectLinearity.libs.mutation;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Fireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

/**
 * Ensures Three Headed Ghast fireballs cannot explode immediately after spawning.
 */
public final class ThreeHeadedFireballListener implements Listener {

    static final String TAG = "project_linearity_three_headed_fireball";
    private static final String META_SAFE_UNTIL = "project_linearity_three_headed_fireball_safe_until";
    private static final long SAFE_WINDOW_MILLIS = 500L;

    private final ProjectLinearity plugin;

    public ThreeHeadedFireballListener(ProjectLinearity plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void mark(Fireball fireball, ProjectLinearity plugin) {
        fireball.addScoreboardTag(TAG);
        fireball.setMetadata(META_SAFE_UNTIL, new FixedMetadataValue(plugin, System.currentTimeMillis() + SAFE_WINDOW_MILLIS));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) {
            return;
        }
        if (!fireball.getScoreboardTags().contains(TAG)) {
            return;
        }
        long safeUntil = getSafeUntil(fireball);
        if (safeUntil <= 0 || System.currentTimeMillis() >= safeUntil) {
            return;
        }
        event.setCancelled(true);
        relaunchFireball(fireball, safeUntil);
    }

    private long getSafeUntil(Fireball fireball) {
        if (!fireball.hasMetadata(META_SAFE_UNTIL)) {
            return -1;
        }
        for (MetadataValue value : fireball.getMetadata(META_SAFE_UNTIL)) {
            if (value.getOwningPlugin() == plugin) {
                return value.asLong();
            }
        }
        return -1;
    }

    private void relaunchFireball(Fireball original, long safeUntil) {
        Vector velocity = original.getVelocity().clone();
        if (velocity.lengthSquared() == 0) {
            velocity = original.getLocation().getDirection().clone();
        }
        if (velocity.lengthSquared() == 0) {
            velocity = new Vector(0, 0.2, 0);
        }
        double speed = Math.max(1.3, velocity.length());
        Vector direction = velocity.clone().normalize().multiply(speed);

        ProjectileSource shooter = original.getShooter();
        float yield = original.getYield();
        boolean incendiary = original.isIncendiary();
        var world = original.getWorld();
        var spawnLocation = original.getLocation().clone().add(direction.clone().normalize().multiply(0.5));
        original.remove();

        world.spawn(spawnLocation, Fireball.class, replacement -> {
            replacement.setYield(yield);
            replacement.setIsIncendiary(incendiary);
            replacement.setVelocity(direction);
            replacement.setDirection(direction);
            if (shooter != null) {
                replacement.setShooter(shooter);
            }
            replacement.addScoreboardTag(TAG);
            replacement.setMetadata(META_SAFE_UNTIL, new FixedMetadataValue(plugin, safeUntil));
        });
    }
}
