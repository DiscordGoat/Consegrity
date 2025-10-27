package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies additional combat-side effects for custom potions. Handles the mining fatigue stun logic.
 */
public final class CustomPotionCombatListener implements Listener {

    private final ProjectLinearity plugin;
    private final CustomPotionEffectManager effectManager;
    private final Map<UUID, BukkitTask> restorationTasks = new HashMap<>();
    private final Map<UUID, Boolean> originalAiStates = new HashMap<>();

    public CustomPotionCombatListener(ProjectLinearity plugin, CustomPotionEffectManager effectManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && player.hasMetadata("pl_levitation_safe")) {
            event.setCancelled(true);
            ProjectLinearity plugin = ProjectLinearity.getInstance();
            if (plugin != null) {
                player.removeMetadata("pl_levitation_safe", plugin);
            }
            player.setFallDistance(0f);
            return;
        }
        int potency = effectManager.getEffectPotency(player, "absorption");
        if (potency <= 0) {
            return;
        }
        event.setDamage(event.getDamage() * 0.5);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker instanceof Player) {
            return;
        }
        int potency = effectManager.getEffectPotency(attacker, "mining_fatigue");
        if (potency <= 0) {
            return;
        }
        applyStun(attacker, potency);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        clearRestoration(event.getEntity());
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, BukkitTask> entry : restorationTasks.entrySet()) {
            entry.getValue().cancel();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living) {
                Boolean hadAI = originalAiStates.remove(entry.getKey());
                if (Boolean.TRUE.equals(hadAI)) {
                    living.setAI(true);
                }
            } else {
                originalAiStates.remove(entry.getKey());
            }
        }
        restorationTasks.clear();
        originalAiStates.clear();
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return living;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private void applyStun(LivingEntity entity, int potency) {
        if (!entity.isValid() || entity.isDead()) {
            return;
        }
        UUID uuid = entity.getUniqueId();
        if (!originalAiStates.containsKey(uuid)) {
            originalAiStates.put(uuid, entity.hasAI());
        }
        entity.setAI(false);

        BukkitTask existing = restorationTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        long delay = Math.max(1, potency) * 40L; // potency * 2 seconds
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tryRestoreAi(entity, uuid);
        }, delay);
        restorationTasks.put(uuid, task);
    }

    private void clearRestoration(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        BukkitTask task = restorationTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        tryRestoreAi(entity, uuid);
    }

    private void tryRestoreAi(LivingEntity entity, UUID uuid) {
        Boolean hadAI = originalAiStates.remove(uuid);
        if (entity.isValid() && !entity.isDead()) {
            if (Boolean.TRUE.equals(hadAI)) {
                entity.setAI(true);
            }
        }
        restorationTasks.remove(uuid);
    }
}
