package goat.projectLinearity.util.potions;

import goat.projectLinearity.util.potions.PotionRegistry.BrewType;
import goat.projectLinearity.util.potions.PotionRegistry.PotionDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Centralised custom potion behaviour. Handles both immediate application and periodic upkeep.
 */
public final class CustomPotionEffects {

    private CustomPotionEffects() {
    }

    public static void applyImmediate(PotionDefinition definition, BrewType brewType, int potency, LivingEntity target) {
        if (target == null) return;
        switch (definition.getId()) {
            case "swiftness" -> applySwiftness(target, potency);
            case "slowness" -> applySlowness(target, potency);
            default -> { /* other effects pending */ }
        }
    }

    public static void applyTick(PotionDefinition definition, BrewType brewType, int potency, LivingEntity target, int elapsedSeconds) {
        if (target == null) return;
        switch (definition.getId()) {
            case "swiftness" -> {
                if (elapsedSeconds % 2 == 0) {
                    applySwiftness(target, potency);
                }
            }
            case "slowness" -> {
                if (elapsedSeconds % 2 == 0) {
                    applySlowness(target, potency);
                }
            }
            default -> { /* other effects pending */ }
        }
    }

    private static void applySwiftness(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false, false));
    }

    private static void applySlowness(LivingEntity target, int potency) {
        boolean isPlayer = target instanceof org.bukkit.entity.Player;
        int amplifier = Math.max(0, (isPlayer ? potency : potency * 2) - 1);
        int durationTicks = 40; // 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier, true, false, false));
    }
}
