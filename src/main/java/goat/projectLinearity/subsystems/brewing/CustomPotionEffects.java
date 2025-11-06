package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import goat.projectLinearity.subsystems.mechanics.SidebarManager;
import goat.projectLinearity.subsystems.mining.MiningOxygenManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

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
            case "haste" -> applyHaste(target, potency);
            case "mining_fatigue" -> applyMiningFatigue(target, potency);
            case "strength" -> applyStrength(target, potency);
            case "regeneration" -> applyRegeneration(target, potency);
            case "resistance" -> applyResistance(target, potency);
            case "fire_resistance" -> applyFireResistance(target, potency);
            case "water_breathing" -> applyWaterBreathing(target, potency);
            case "instant_health" -> applyInstantHealth(target, potency);
            case "instant_damage" -> applyInstantDamage(target, potency);
            case "leaping" -> applyLeaping(target, potency);
            case "instant_cooling" -> applyInstantCooling(target, potency);
            case "instant_warming" -> applyInstantWarming(target, potency);
            case "instant_oxygen" -> applyInstantOxygen(target, potency);
            case "instant_air" -> applyInstantAir(target);
            case "nausea" -> applyNausea(target, potency);
            case "invisibility" -> applyInvisibility(target, potency);
            case "blindness" -> applyBlindness(target, potency);
            case "night_vision" -> applyNightVision(target, potency);
            case "saturation" -> applySaturation(target, potency, definition.getStats(brewType).getDurationSeconds());
            case "well_balanced_meal" -> applyWellBalancedMeal(target);
            case "glowing" -> applyGlowing(target, potency);
            case "slow_falling" -> applySlowFalling(target, potency);
            case "conduit_power" -> applyConduitPower(target, potency);
            case "dolphins_grace" -> applyDolphinsGrace(target, potency);
            case "frostbite" -> applyFrostbite(target, potency);
            case "heatstroke" -> applyHeatstroke(target, potency);
            case "instant_lightning" -> applyInstantLightning(target);
            case "wither" -> applyWither(target, potency);
            case "poison" -> applyPoison(target, potency);
            case "weakness" -> applyWeakness(target, potency);
            case "absorption" -> applyAbsorption(target, potency, definition.getStats(brewType).getDurationSeconds());
            default -> { /* other effects pending */ }
        }
    }

    public static void applyTick(PotionDefinition definition, BrewType brewType, int potency, LivingEntity target, int elapsedSeconds) {
        if (target == null) return;
        switch (definition.getId()) {
            case "hunger" -> {
                // Reduce hunger by 1 every 7 seconds
                if (elapsedSeconds % 7 == 0 && target instanceof Player player) {
                    int newFoodLevel = Math.max(0, player.getFoodLevel() - 1);
                    player.setFoodLevel(newFoodLevel);
                }
            }
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
            case "haste" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyHaste(target, potency);
                }
            }
            case "mining_fatigue" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyMiningFatigue(target, potency);
                }
            }
            case "strength" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyStrength(target, potency);
                }
            }
            case "regeneration" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyRegeneration(target, potency);
                }
            }
            case "resistance" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyResistance(target, potency);
                }
            }
            case "fire_resistance" -> {
                if (elapsedSeconds % 2 == 0 && target instanceof Player player) {
                    reduceFireTicks(player, potency);
                }
            }
            case "water_breathing" -> {
                if (elapsedSeconds % 2 == 0 && target instanceof Player player) {
                    restoreAir(player, potency);
                }
            }
            case "weakness" -> {
                // Refresh the effect every 2 seconds (40 ticks) to maintain it
                if (elapsedSeconds % 2 == 0) {
                    applyWeakness(target, potency);
                }
            }
            case "invisibility" -> {
                if (target instanceof Player player) {
                    // Reapply the effect every tick to handle mob targeting
                    applyInvisibility(player, potency);
                }
            }
            case "blindness" -> {
                // Reapply the effect every tick to handle mob targeting
                applyBlindness(target, potency);
            }
            case "night_vision" -> {
                // Apply night vision with extended duration
                applyNightVision(target, potency);
            }
            case "leaping" -> {
                // No periodic refresh needed - handled by BonusJumpManager
            }
            case "instant_health", "instant_damage", "instant_cooling", "instant_warming", "instant_oxygen", "instant_air" -> { /* handled immediately only */ }
            case "nausea" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyNausea(target, potency);
                }
                applyNauseaTick(target, potency, elapsedSeconds);
            }
            case "saturation" -> {
                if (target instanceof Player player) {
                    applySaturation(player, potency, definition.getStats(brewType).getDurationSeconds());
                }
            }
            case "glowing" -> applyGlowing(target, potency);
            case "slow_falling" -> applySlowFalling(target, potency);
            case "conduit_power" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyConduitPower(target, potency);
                }
            }
            case "dolphins_grace" -> {
                if (elapsedSeconds % 2 == 0) {
                    applyDolphinsGrace(target, potency);
                }
            }
            case "frostbite" -> {
                if (elapsedSeconds > 0 && elapsedSeconds % 10 == 0) {
                    applyFrostbite(target, potency);
                }
            }
            case "heatstroke" -> {
                if (elapsedSeconds > 0 && elapsedSeconds % 10 == 0) {
                    applyHeatstroke(target, potency);
                }
            }
            case "wither" -> {
                if (target instanceof Player player) {
                    applyWither(player, potency);
                }
            }
            case "poison" -> {
                if (elapsedSeconds % 2 == 0 && target instanceof Player) {
                    applyPoison(target, potency);
                }
            }
            default -> { /* other effects pending */ }
        }
    }

    private static void applyHaste(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        int durationTicks = 40; // 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, amplifier, true, false, false));
    }

    private static void applyMiningFatigue(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        int durationTicks = 40; // 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, amplifier, true, false, false));
    }

    private static void applySwiftness(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false, false));
    }

    private static void applyStrength(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, amplifier, true, false, false));
    }

    private static void applyRegeneration(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, amplifier, true, false, false));
    }

    private static void applyResistance(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, amplifier, true, false, false));
    }

    private static void applyFireResistance(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            reduceFireTicks(player, potency);
        }
    }

    private static void reduceFireTicks(Player player, int potency) {
        int reduction = 100 * Math.max(0, potency);
        if (reduction <= 0) return;
        int remaining = Math.max(0, player.getFireTicks() - reduction);
        player.setFireTicks(remaining);
    }

    private static void applyWaterBreathing(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            restoreAir(player, potency);
        }
    }

    private static void restoreAir(Player player, int potency) {
        int restore = 3 * Math.max(0, potency);
        if (restore <= 0) return;
        player.setRemainingAir(Math.min(player.getMaximumAir(), player.getRemainingAir() + restore));
    }

    private static void applyInstantHealth(LivingEntity target, int potency) {
        double amount = 4.0 * Math.max(1, potency);
        AttributeInstance attribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = attribute != null ? attribute.getValue() : target.getHealth();
        double newHealth = Math.min(maxHealth, target.getHealth() + amount);
        target.setHealth(newHealth);
    }

    private static void applyInstantDamage(LivingEntity target, int potency) {
        double amount = 4.0 * Math.max(1, potency);
        double newHealth = Math.max(0.0, target.getHealth() - amount);
        if (newHealth <= 0.0) {
            target.setHealth(0.0);
        } else {
            target.setHealth(newHealth);
        }
    }

    private static void applyLeaping(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, amplifier, true, false, false));
    }

    private static void applyInstantCooling(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            double delta = -10.0 * Math.max(1, potency);
            adjustPlayerTemperature(player, delta);
        }
    }

    private static void applyInstantWarming(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            double delta = 10.0 * Math.max(1, potency);
            adjustPlayerTemperature(player, delta);
        }
    }

    private static void applyInstantOxygen(LivingEntity target, int potency) {
        if (!(target instanceof Player player)) {
            return;
        }
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        if (plugin == null) {
            return;
        }
        MiningOxygenManager oxygenManager = plugin.getMiningOxygenManager();
        if (oxygenManager == null) {
            return;
        }
        int current = oxygenManager.getOxygen(player);
        int increase = (int) Math.ceil(15.0 * Math.max(1, potency));
        oxygenManager.setOxygen(player, current + increase, false);
        SidebarManager sidebarManager = plugin.getSidebarManager();
        if (sidebarManager != null) {
            sidebarManager.refreshDisplay(player);
        }
    }

    private static void applyInstantAir(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setRemainingAir(player.getMaximumAir());
    }

    private static void applyConduitPower(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        int durationTicks = 60; // 3 seconds, refreshed every 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, durationTicks, amplifier, true, false, false));
    }

    private static void applyDolphinsGrace(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        int durationTicks = 60; // 3 seconds, refreshed every 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, durationTicks, amplifier, true, false, false));
    }

    private static void applyFrostbite(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            double delta = -5.0 * Math.max(1, potency);
            adjustPlayerTemperature(player, delta);
        }
    }

    private static void applyHeatstroke(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            double delta = 5.0 * Math.max(1, potency);
            adjustPlayerTemperature(player, delta);
        }
    }

    private static void adjustPlayerTemperature(Player player, double delta) {
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        if (plugin == null) {
            return;
        }
        SidebarManager sidebarManager = plugin.getSidebarManager();
        if (sidebarManager == null) {
            return;
        }
        sidebarManager.adjustTemperature(player, delta);
    }

    private static void applyInvisibility(LivingEntity target, int potency) {
        // Apply vanilla invisibility effect
        target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, true));
        
        // Make mobs ignore the player unless they attack
        if (target instanceof Player player) {
            player.getNearbyEntities(16, 16, 16).stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Mob)
                .map(entity -> (org.bukkit.entity.Mob) entity)
                .filter(mob -> mob.getTarget() == player)
                .forEach(mob -> {
                    // Only reset target if the player hasn't attacked recently
                    if (!player.hasMetadata("recently_attacked")) {
                        mob.setTarget(null);
                    }
                });
        }
    }

    private static void applyBlindness(LivingEntity target, int potency) {
        // Apply vanilla blindness effect to all entities
        // 40 ticks = 2 seconds, but we'll reapply it every tick to maintain the effect
        // The actual duration is handled by the potion effect system
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));
        
        // For mobs, handle target clearing when players are too far
        if (target instanceof org.bukkit.entity.Mob mob) {
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget != null && currentTarget.getLocation().distanceSquared(target.getLocation()) > 4) { // 2 blocks squared = 4
                mob.setTarget(null);
            }
        }
    }

    private static void applyNightVision(LivingEntity target, int potency) {
        // Apply night vision effect (13 seconds, will be refreshed every 2 seconds)
        // For players, this gives the night vision visual effect
        // For mobs, we'll handle the increased detection range in the NightVisionListener
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.NIGHT_VISION, 
            13 * 20, // 13 seconds (in ticks)
            0, 
            false, 
            false, 
            true
        ));
        
        // If this is a mob, we'll apply a custom effect to track the increased detection range
        if (target instanceof org.bukkit.entity.Mob) {
            // We'll use metadata to track the original follow range
            if (!target.hasMetadata("originalFollowRange")) {
                double originalRange = ((org.bukkit.entity.Mob) target).getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE).getBaseValue();
                target.setMetadata("originalFollowRange", new org.bukkit.metadata.FixedMetadataValue(ProjectLinearity.getInstance(), originalRange));
                
                // Increase follow range by 50% (or any other multiplier you prefer)
                double newRange = originalRange * 1.5;
                ((org.bukkit.entity.Mob) target).getAttribute(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(newRange);
            }
        }
    }

    
    private static void cleanupHungerEffect(LivingEntity target) {
        // Restore original hunger level if it was stored
        if (target.hasMetadata("originalHungerLevel")) {
            target.removeMetadata("originalHungerLevel", ProjectLinearity.getInstance());
        }
    }
    
    private static void applyNausea(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            int amplifier = Math.max(0, potency - 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, amplifier, true, false, false));
        }
        
        // Clean up hunger effect when the effect expires
        if (target.hasMetadata("originalHungerLevel")) {
            cleanupHungerEffect(target);
        }
    }

    private static void applyNauseaTick(LivingEntity target, int potency, int elapsedSeconds) {
        if (target instanceof Player) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Vector jitter = new Vector(
                    random.nextDouble(-0.08, 0.08) * potency,
                    random.nextDouble(-0.04, 0.06) * potency,
                    random.nextDouble(-0.08, 0.08) * potency
            );
            target.setVelocity(target.getVelocity().add(jitter));
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector jitter = new Vector(
                random.nextDouble(-0.08, 0.08) * potency,
                random.nextDouble(-0.04, 0.06) * potency,
                random.nextDouble(-0.08, 0.08) * potency
        );
        target.setVelocity(target.getVelocity().add(jitter));

        Location loc = target.getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        float yawDelta = (float) (random.nextDouble(-5.0, 5.0) * potency);
        float pitchDelta = (float) (random.nextDouble(-2.5, 2.5) * potency);
        pitch = Math.max(-85f, Math.min(85f, pitch + pitchDelta));
        target.setRotation(yaw + yawDelta, pitch);
    }

    private static void applyWeakness(LivingEntity target, int potency) {
        boolean isPlayer = target instanceof org.bukkit.entity.Player;
        int amplifier = Math.max(0, (isPlayer ? potency : potency * 2) - 1);
        // 2 second duration, will be refreshed by applyTick
        int durationTicks = 40; // 2 seconds
        // Use force:true to ensure the effect is always applied, even if a lower level exists
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, amplifier, true, false, true), true);
    }
    
    private static void applySlowness(LivingEntity target, int potency) {
        boolean isPlayer = target instanceof org.bukkit.entity.Player;
        int amplifier = Math.max(0, (isPlayer ? potency : potency * 2) - 1);
        int durationTicks = 40; // 2 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier, true, false, false));
    }

    private static void applyPoison(LivingEntity target, int potency) {
        int amplifier = Math.max(0, potency - 1);
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, amplifier, true, false, false));
    }

    static void applyAbsorption(LivingEntity target, int potency, int durationSeconds) {
        if (target == null) {
            return;
        }
        if (potency <= 0 || durationSeconds <= 0) {
            target.removePotionEffect(PotionEffectType.ABSORPTION);
            target.setAbsorptionAmount(0.0D);
            return;
        }
        int amplifier = Math.max(0, (5 * Math.max(1, potency)) - 1);
        int durationTicks = Math.max(20, durationSeconds * 20);
        target.removePotionEffect(PotionEffectType.ABSORPTION);
        target.setAbsorptionAmount(0.0D);
        PotionEffect effect = new PotionEffect(PotionEffectType.ABSORPTION, durationTicks, amplifier, true, false, false);
        target.addPotionEffect(effect, true);
    }

    private static void applySaturation(LivingEntity target, int potency, int baseDurationSeconds) {
        if (!(target instanceof Player player)) {
            return;
        }
        int totalSeconds = Math.max(0, baseDurationSeconds) + Math.max(0, potency) * 60;
        totalSeconds = Math.min(totalSeconds, 240);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, totalSeconds * 20, 0, true, false, false));
    }

    private static void applyWellBalancedMeal(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    private static void applyGlowing(LivingEntity target, int potency) {
        int durationTicks = Math.max(1, potency) * 60 * 20;
        if (target instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, true, false, false));
        } else if (!target.hasPotionEffect(PotionEffectType.GLOWING)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    private static void applyInstantLightning(LivingEntity target) {
        Location loc = target.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().strikeLightning(loc);
        }
    }

    private static void applySlowFalling(LivingEntity target, int potency) {
        int durationTicks = 40;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, Math.max(0, potency - 1), true, false, false));
    }

    private static void applyLevitation(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            player.setNoDamageTicks(0);
            player.setFallDistance(0f);
        }
        Vector velocity = target.getVelocity().clone();
        double boost = 1.0 + (Math.max(1, potency) - 1) * 0.4;
        velocity.setY(Math.min(velocity.getY() + boost, 2.0 + potency * 0.5));
        target.setVelocity(velocity);
    }

    private static void applyWither(LivingEntity target, int potency) {
        if (target instanceof Player player) {
            applyWitherPlayerBuffs(player);
        }
    }

    private static void applyWitherPlayerBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, true, false, false));
        int newFood = Math.min(20, player.getFoodLevel() + 2);
        player.setFoodLevel(newFood);
        float newSaturation = Math.min(20f, player.getSaturation() + 1.5f);
        player.setSaturation(newSaturation);
    }
}
