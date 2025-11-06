package goat.projectLinearity.libs.mutation;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable definition describing how a mutation should behave.
 */
public final class MutationDefinition {
    private final String id;
    private final EntityType entityType;
    private final int percentage;
    private final Color armorColor;
    private final String headTexture;
    private final Stats stats;
    private final String name;
    private final MutationBehavior behavior;
    private final ItemStack drop;
    private final int minDrop;
    private final int maxDrop;
    private final int dropChancePercentage;
    private final Set<Biome> allowedBiomes;
    private final boolean ambient;
    private final Color ambientDustColor;
    private final Particle ambientParticle;
    private final Sound ambientSound;
    private final double ambientIntervalSeconds;
    private final int ambientSoundCooldownSeconds;
    private final float ambientSoundVolume;
    private final float ambientSoundPitch;

    public MutationDefinition(String id,
                              EntityType entityType,
                              int percentage,
                              Color armorColor,
                              String headTexture,
                              Stats stats,
                              String name,
                              MutationBehavior behavior,
                              ItemStack drop,
                              int minDrop,
                              int maxDrop,
                              int dropChancePercentage,
                              Biome... allowedBiomes) {
        this(id, entityType, percentage, armorColor, headTexture, stats, name, behavior,
                drop, minDrop, maxDrop, dropChancePercentage,
                allowedBiomes, false, null, null, null, 0, 0, 0, 0);
    }

    public MutationDefinition(String id,
                              EntityType entityType,
                              int percentage,
                              Color armorColor,
                              String headTexture,
                              Stats stats,
                              String name,
                              MutationBehavior behavior,
                              ItemStack drop,
                              int minDrop,
                              int maxDrop,
                              int dropChancePercentage,
                              Biome[] allowedBiomes,
                              boolean ambient,
                              Color ambientDustColor,
                              Particle ambientParticle,
                              Sound ambientSound,
                              double ambientIntervalSeconds,
                              int ambientSoundCooldownSeconds,
                              float ambientSoundVolume,
                              float ambientSoundPitch) {
        this.id = normalizeId(id);
        this.entityType = entityType;
        this.percentage = percentage;
        this.armorColor = armorColor;
        this.headTexture = headTexture;
        this.stats = stats == null ? Stats.empty() : stats;
        this.name = name == null || name.isBlank() ? null : name.trim();
        this.behavior = behavior == null ? MutationBehavior.NONE : behavior;
        this.drop = drop == null ? null : drop.clone();
        this.minDrop = Math.max(0, minDrop);
        this.maxDrop = Math.max(this.minDrop, maxDrop);
        this.dropChancePercentage = Math.max(0, Math.min(100, dropChancePercentage));
        this.allowedBiomes = sanitizeBiomes(allowedBiomes);
        this.ambient = ambient;
        this.ambientDustColor = ambientDustColor;
        this.ambientParticle = ambientParticle == null ? Particle.DUST : ambientParticle;
        this.ambientSound = ambientSound == null ? Sound.ENTITY_ENDERMAN_AMBIENT : ambientSound;
        this.ambientIntervalSeconds = ambientIntervalSeconds <= 0 ? 3.0 : ambientIntervalSeconds;
        this.ambientSoundCooldownSeconds = ambientSoundCooldownSeconds <= 0 ? 6 : ambientSoundCooldownSeconds;
        this.ambientSoundVolume = ambientSoundVolume <= 0 ? 50f : ambientSoundVolume;
        this.ambientSoundPitch = ambientSoundPitch <= 0 ? 0.4f : ambientSoundPitch;
    }

    public String id() {
        return id;
    }

    public EntityType entityType() {
        return entityType;
    }

    public int percentage() {
        return percentage;
    }

    public Color armorColor() {
        return armorColor;
    }

    public String headTexture() {
        return headTexture;
    }

    public Stats stats() {
        return stats;
    }

    public String name() {
        return name;
    }

    public MutationBehavior behavior() {
        return behavior;
    }

    public ItemStack drop() {
        return drop == null ? null : drop.clone();
    }

    public int minDrop() {
        return minDrop;
    }

    public int maxDrop() {
        return maxDrop;
    }

    public int dropChancePercentage() {
        return dropChancePercentage;
    }

    public Set<Biome> allowedBiomes() {
        return allowedBiomes;
    }

    public boolean hasAmbientEffect() {
        return ambient;
    }

    public Color ambientDustColor() {
        return ambientDustColor;
    }

    public Particle ambientParticle() {
        return ambientParticle;
    }

    public Sound ambientSound() {
        return ambientSound;
    }

    public double ambientIntervalSeconds() {
        return ambientIntervalSeconds;
    }

    public int ambientSoundCooldownSeconds() {
        return ambientSoundCooldownSeconds;
    }

    public float ambientSoundVolume() {
        return ambientSoundVolume;
    }

    public float ambientSoundPitch() {
        return ambientSoundPitch;
    }

    private static String normalizeId(String id) {
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("mutation id cannot be blank");
        }
        return trimmed.toLowerCase(Locale.ENGLISH).replace(' ', '_');
    }

    private static Set<Biome> sanitizeBiomes(Biome... biomes) {
        if (biomes == null || biomes.length == 0) {
            return Collections.emptySet();
        }
        Set<Biome> result = new HashSet<>();
        for (Biome biome : biomes) {
            if (biome != null) {
                result.add(biome);
            }
        }
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }
}
