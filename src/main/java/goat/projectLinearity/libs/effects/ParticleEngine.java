package goat.projectLinearity.libs.effects;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralised helper for spawning ambient particle and sound effects around entities.
 */
public final class ParticleEngine {

    public enum EffectType {
        AMBIENT
    }

    private final ProjectLinearity plugin;
    private final Map<String, AmbientConfig> ambientConfigs = new ConcurrentHashMap<>();
    private final Map<UUID, RunningAmbient> activeAmbient = new ConcurrentHashMap<>();
    private static final boolean DEBUG = true;

    public ParticleEngine(ProjectLinearity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void registerAmbientParticles(String name,
                                         Particle particle,
                                         double frequencySeconds,
                                         EffectType effectType,
                                         Sound linkedSound,
                                         int soundFrequencySeconds,
                                         float soundVolume,
                                         float soundPitch) {
        registerAmbientParticles(name, particle, null, frequencySeconds, effectType, linkedSound,
                soundFrequencySeconds, soundVolume, soundPitch);
    }

    public void registerAmbientParticles(String name,
                                         Particle particle,
                                         Color dustColor,
                                         double frequencySeconds,
                                         EffectType effectType,
                                         Sound linkedSound,
                                         int soundFrequencySeconds,
                                         float soundVolume,
                                         float soundPitch) {
        if (effectType != EffectType.AMBIENT) {
            throw new IllegalArgumentException("Unsupported effect type: " + effectType);
        }
        String key = normalizeKey(name);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Ambient effect name cannot be blank");
        }
        double particleFrequency = Math.max(1, frequencySeconds);
        int soundFrequency = Math.max(1, soundFrequencySeconds);
        Object particleData = resolveDefaultParticleData(particle, dustColor);
        int particleCount = resolveDefaultParticleCount(particle);
        AmbientConfig config = new AmbientConfig(name, key, particle, particleData, particleFrequency,
                linkedSound, soundFrequency, soundVolume, soundPitch, particleCount);
        ambientConfigs.put(key, config);
        debugLog(String.format("Registered ambient effect '%s' (particle=%s, color=%s, freq=%.2fs, sound=%s)",
                name, particle, dustColor, particleFrequency, linkedSound));
    }

    public void attachAmbientEffect(LivingEntity entity, String name) {
        if (entity == null || name == null) {
            return;
        }
        AmbientConfig config = lookupConfig(name);
        if (config == null) {
            debugLog(String.format("attachAmbientEffect skipped: no config for '%s'", name));
            return;
        }
        UUID uuid = entity.getUniqueId();
        RunningAmbient previous = activeAmbient.remove(uuid);
        if (previous != null) {
            previous.stop();
        }
        RunningAmbient instance = new RunningAmbient(entity, config);
        instance.start();
        activeAmbient.put(uuid, instance);
        debugLog(String.format("Attached ambient effect '%s' to entity %s (%s)",
                name, uuid, entity.getType()));
    }

    public void detachAmbientEffect(UUID uuid) {
        if (uuid == null) {
            return;
        }
        RunningAmbient instance = activeAmbient.remove(uuid);
        if (instance != null) {
            instance.stop();
            debugLog(String.format("Detached ambient effect from entity %s", uuid));
        }
    }

    public void shutdown() {
        for (RunningAmbient instance : activeAmbient.values()) {
            instance.stop();
        }
        activeAmbient.clear();
        ambientConfigs.clear();
        debugLog("ParticleEngine shutdown complete");
    }

    private AmbientConfig lookupConfig(String rawName) {
        String key = normalizeKey(rawName);
        if (key.isEmpty()) {
            return null;
        }
        AmbientConfig config = ambientConfigs.get(key);
        if (config != null) {
            return config;
        }
        // Attempt lookup without collapsing whitespace, for defensive coverage.
        return ambientConfigs.get(rawName.toLowerCase(Locale.ENGLISH));
    }

    private Object resolveDefaultParticleData(Particle particle, Color overrideColor) {
        if (particle == Particle.DUST) {
            Color color = overrideColor != null ? overrideColor : Color.fromRGB(78, 168, 255);
            return new Particle.DustOptions(color, 1.8f);
        }
        return null;
    }

    private int resolveDefaultParticleCount(Particle particle) {
        return switch (particle) {
            case DUST, DUST_COLOR_TRANSITION, DUST_PLUME, SOUL_FIRE_FLAME -> 12;
            case END_ROD, ELECTRIC_SPARK, WARPED_SPORE, CRIMSON_SPORE -> 8;
            default -> 6;
        };
    }

    private String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(raw).replace('_', ' ').replace('-', ' ');
        stripped = stripped.trim();
        if (stripped.isEmpty()) {
            return "";
        }
        return stripped.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
    }

    private final class RunningAmbient {
        private final LivingEntity entity;
        private final AmbientConfig config;
        private BukkitTask particleTask;
        private BukkitTask soundTask;
        private boolean spawnedOnce;

        private RunningAmbient(LivingEntity entity, AmbientConfig config) {
            this.entity = entity;
            this.config = config;
        }

        private void start() {
            double particleInterval = Math.max(1, config.frequencySeconds()) * 20;
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::emitParticles, 0L, (long) particleInterval);
            debugLog(String.format("Particle task started for %s (%s) interval=%.1fticks",
                    entity.getUniqueId(), config.name(), particleInterval));
            if (config.sound() != null) {
                int soundInterval = Math.max(1, config.soundFrequencySeconds()) * 20;
                soundTask = Bukkit.getScheduler().runTaskTimer(plugin, this::emitSound, 0L, soundInterval);
                debugLog(String.format("Sound task started for %s (%s) interval=%dticks",
                        entity.getUniqueId(), config.name(), soundInterval));
            }
        }

        private void emitParticles() {
            if (!isEntityActive()) {
                stopAndRemove();
                return;
            }
            Location origin = entity.getLocation();
            if (origin.getWorld() == null) {
                return;
            }
            double midY = origin.getY() + entity.getHeight() * 0.5;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double offsetX = random.nextDouble(-1.0, 1.0);
            double offsetY = random.nextDouble(-0.5, 0.5);
            double offsetZ = random.nextDouble(-1.0, 1.0);
            Location spawnLocation = new Location(origin.getWorld(),
                    origin.getX() + offsetX,
                    midY + offsetY,
                    origin.getZ() + offsetZ);
        origin.getWorld().spawnParticle(
                config.particle(),
                spawnLocation,
                config.particleCount(),
                0.35,
                0.4,
                0.35,
                0.005,
                config.particleData());
            if (!spawnedOnce) {
                spawnedOnce = true;
                debugLog(String.format("Emitting ambient particles for %s (%s) - count=%d",
                        entity.getUniqueId(), config.name(), config.particleCount()));
            }
        }

        private void emitSound() {
            if (!isEntityActive()) {
                stopAndRemove();
                return;
            }
            if (config.sound() == null) {
                return;
            }
            Location origin = entity.getLocation();
            if (origin.getWorld() == null) {
                return;
            }
            origin.getWorld().playSound(origin, config.sound(), SoundCategory.HOSTILE, config.soundVolume(), config.soundPitch());
        }

        private boolean isEntityActive() {
            return entity.isValid() && !entity.isDead();
        }

        private void stopAndRemove() {
            stop();
            activeAmbient.remove(entity.getUniqueId(), this);
        }

        private void stop() {
            if (particleTask != null) {
                particleTask.cancel();
            }
            if (soundTask != null) {
                soundTask.cancel();
            }
        }
    }

    private record AmbientConfig(String name,
                                 String key,
                                 Particle particle,
                                 Object particleData,
                                 double frequencySeconds,
                                 Sound sound,
                                 int soundFrequencySeconds,
                                 float soundVolume,
                                 float soundPitch,
                                 int particleCount) {
    }

    private void debugLog(String message) {
        if (!DEBUG) {
            return;
        }
        plugin.getLogger().info("[ParticleEngine] " + message);
    }
}
