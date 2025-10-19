package goat.projectLinearity.util.listeners;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.CurseManager;
import goat.projectLinearity.util.CurseRegistry;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Random;

/**
 * Handles curse application from cursed monsters and particle effects.
 */
public final class CurseListener implements Listener {

    private static final Random random = new Random();
    private final ProjectLinearity plugin;
    private final CurseManager curseManager;

    public CurseListener(ProjectLinearity plugin, CurseManager curseManager) {
        this.plugin = plugin;
        this.curseManager = curseManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if a player is taking damage from a cursed monster
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getDamager() instanceof Monster monster)) {
            return;
        }

        // Check if the monster is cursed
        if (!isCursedMonster(monster)) {
            return;
        }

        // 50% chance to apply curse
        if (random.nextDouble() < 0.5) {
            // Apply a random curse for 3 minutes (3600 ticks)
            CurseRegistry.getRandomCurse().ifPresent(curse -> {
                curseManager.applyCurse(player, curse, 3600L); // 3 minutes in ticks
            });
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }

        // Check if this monster should be cursed and mark it
        if (shouldBeCursed(monster)) {
            markMonsterAsCursed(monster);
            startCurseParticleEffect(monster);
        }
    }

    private boolean isCursedMonster(Monster monster) {
        // Check if monster has the cursed metadata
        return monster.hasMetadata("cursed_monster");
    }

    private void startCurseParticleEffect(Monster monster) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!monster.isValid() || monster.isDead()) {
                    cancel();
                    return;
                }

                Location location = monster.getLocation().add(0, 1, 0);

                // Spawn enchantment particles around the monster
                monster.getWorld().spawnParticle(Particle.ENCHANT, location, 10, 0, 0, 0, 0);

                // Also spawn some portal particles for extra cursed effect
                for (int i = 0; i < 3; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 2;
                    double offsetY = random.nextDouble() * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * 2;
                    Location particleLoc = location.clone().add(offsetX, offsetY, offsetZ);
                    monster.getWorld().playEffect(particleLoc, Effect.ELECTRIC_SPARK, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 5 ticks (quarter second)
    }

    /**
     * Marks a monster as cursed.
     */
    public static void markMonsterAsCursed(Monster monster) {
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        if (plugin == null) {
            return;
        }
        monster.setMetadata("cursed_monster", new FixedMetadataValue(plugin, true));
    }

    /**
     * Checks if a monster should be cursed based on spawn location and type.
     */
    public static boolean shouldBeCursed(Monster monster) {
        // All pillagers in Mesa are cursed
        if (monster.getType() == EntityType.PILLAGER) {
            return isInMesaSector(monster.getLocation());
        }

        // 10% chance for monsters in Desert or Mesa sectors
        return (isInDesertSector(monster.getLocation()) || isInMesaSector(monster.getLocation()))
                && random.nextDouble() < 0.1;
    }

    private static boolean isInDesertSector(Location location) {
        // Use ConsegrityRegions to check if location is in desert
        return goat.projectLinearity.world.ConsegrityRegions.regionAt(
                location.getWorld(), location.getBlockX(), location.getBlockZ()
        ) == goat.projectLinearity.world.ConsegrityRegions.Region.DESERT;
    }

    private static boolean isInMesaSector(Location location) {
        // Use ConsegrityRegions to check if location is in mesa
        return goat.projectLinearity.world.ConsegrityRegions.regionAt(
                location.getWorld(), location.getBlockX(), location.getBlockZ()
        ) == goat.projectLinearity.world.ConsegrityRegions.Region.MESA;
    }
}
