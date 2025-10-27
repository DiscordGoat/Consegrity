package goat.projectLinearity.subsystems.mechanics;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AggressionManager implements Listener {

    public enum AggressionState {
        PASSIVE,
        NEUTRAL,
        HOSTILE
    }

    private final Map<EntityType, AggressionState> defaults = new EnumMap<>(EntityType.class);
    private final Map<UUID, AggressionState> overrides = new HashMap<>();
    private final Map<UUID, Long> provokedUntil = new HashMap<>();

    public AggressionManager(ProjectLinearity plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        setDefault(EntityType.ZOMBIE, AggressionState.HOSTILE);
        setDefault(EntityType.SKELETON, AggressionState.HOSTILE);
        setDefault(EntityType.CREEPER, AggressionState.HOSTILE);
        setDefault(EntityType.SPIDER, AggressionState.NEUTRAL);
        setDefault(EntityType.ENDERMAN, AggressionState.NEUTRAL);
        setDefault(EntityType.PIGLIN, AggressionState.NEUTRAL);
        setDefault(EntityType.PIGLIN_BRUTE, AggressionState.HOSTILE);
        setDefault(EntityType.ZOMBIFIED_PIGLIN, AggressionState.NEUTRAL);
        setDefault(EntityType.HUSK, AggressionState.HOSTILE);
        setDefault(EntityType.DROWNED, AggressionState.HOSTILE);
        setDefault(EntityType.STRAY, AggressionState.HOSTILE);
        setDefault(EntityType.MAGMA_CUBE, AggressionState.HOSTILE);
        setDefault(EntityType.SLIME, AggressionState.NEUTRAL);
        setDefault(EntityType.BLAZE, AggressionState.HOSTILE);
        setDefault(EntityType.WITHER_SKELETON, AggressionState.HOSTILE);
        setDefault(EntityType.GHAST, AggressionState.HOSTILE);
    }

    public void setDefault(EntityType type, AggressionState state) {
        defaults.put(type, state);
    }

    public AggressionState getState(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return AggressionState.PASSIVE;
        }
        UUID id = living.getUniqueId();
        AggressionState override = overrides.get(id);
        if (override != null) {
            return override;
        }
        return defaults.getOrDefault(living.getType(), AggressionState.PASSIVE);
    }

    public boolean isNeutral(Entity entity) {
        return getState(entity) == AggressionState.NEUTRAL;
    }

    public boolean isPassive(Entity entity) {
        return getState(entity) == AggressionState.PASSIVE;
    }

    public void applyNeutral(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }
        overrides.put(entity.getUniqueId(), AggressionState.NEUTRAL);
    }

    public void clearOverride(LivingEntity entity) {
        overrides.remove(entity.getUniqueId());
        provokedUntil.remove(entity.getUniqueId());
    }

    public void markProvoked(LivingEntity entity, long durationMillis) {
        provokedUntil.put(entity.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    private boolean isProvoked(LivingEntity entity) {
        Long expiry = provokedUntil.get(entity.getUniqueId());
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) {
            provokedUntil.remove(entity.getUniqueId());
            return false;
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        AggressionState state = getState(living);
        if (state == AggressionState.PASSIVE) {
            event.setCancelled(true);
            event.setTarget(null);
            return;
        }
        if (state == AggressionState.NEUTRAL) {
            if (!isProvoked(living)) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (getState(victim) != AggressionState.NEUTRAL) {
            return;
        }
        boolean provoked = false;
        if (event.getDamager() instanceof LivingEntity damager && damager instanceof Player) {
            provoked = true;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter && livingShooter instanceof Player) {
                provoked = true;
            }
        }
        if (provoked) {
            markProvoked(victim, 15_000L);
        }
    }
}
