package goat.projectLinearity.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Shared helpers for adapting custom movement logic to vanilla status effects.
 */
public final class MovementSpeedUtil {

    private static final double SLOWNESS_FACTOR_PER_LEVEL = 0.15;

    private MovementSpeedUtil() {
    }

    public static double applySlowness(LivingEntity entity, double baseSpeed) {
        return baseSpeed * getSlownessMultiplier(entity);
    }

    public static float applySlowness(LivingEntity entity, float baseSpeed) {
        return (float) applySlowness(entity, (double) baseSpeed);
    }

    public static double getSlownessMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 1.0;
        }
        PotionEffect effect = entity.getPotionEffect(PotionEffectType.SLOWNESS);
        if (effect == null) {
            return 1.0;
        }
        int amplifier = Math.max(0, effect.getAmplifier());
        double reduction = SLOWNESS_FACTOR_PER_LEVEL * (amplifier + 1);
        double multiplier = 1.0 - reduction;
        if (multiplier <= 0.0) {
            return 0.0;
        }
        return multiplier;
    }
}
