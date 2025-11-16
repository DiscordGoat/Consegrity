package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.LingeringProfile;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionItemData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages Lingering potion hazards, including visuals and pulse-based effect delivery.
 */
public final class LingeringHazardManager {

    private static final double BASE_RADIUS = 30.0;
    private static final long BASE_DURATION_TICKS = 30 * 20L;
    private static final long INSTANT_DURATION_TICKS = 10 * 20L;
    private static final long PULSE_INTERVAL_TICKS = 20L;
    private static final int RING_SEGMENTS = 48;
    private static final int AMBIENT_PARTICLES_PER_TICK = 14;
    private static final int PULSE_WAVE_SEGMENTS = 24;

    private final ProjectLinearity plugin;
    private final CustomPotionEffectManager effectManager;
    private final List<Hazard> activeHazards = new ArrayList<>();
    private BukkitTask tickTask;
    private long tickCounter;

    public LingeringHazardManager(ProjectLinearity plugin, CustomPotionEffectManager effectManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.effectManager = Objects.requireNonNull(effectManager, "effectManager");
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        activeHazards.clear();
    }

    public void spawnHazard(PotionItemData data, Location impact) {
        if (data == null || impact == null || impact.getWorld() == null) {
            return;
        }
        Hazard hazard = new Hazard(data, impact.clone());
        activeHazards.add(hazard);
        hazard.initialPulse(tickCounter);
    }

    private void tick() {
        tickCounter++;
        Iterator<Hazard> iterator = activeHazards.iterator();
        while (iterator.hasNext()) {
            Hazard hazard = iterator.next();
            if (hazard.isExpired(tickCounter)) {
                iterator.remove();
                continue;
            }
            hazard.tick(tickCounter);
        }
    }

    private final class Hazard {
        private final PotionItemData data;
        private final Location center;
        private final Color baseColor;
        private final double radius;
        private final double radiusSq;
        private final long expireTick;
        private final boolean positive;
        private final boolean instant;
        private final List<Location> ringPoints;
        private final double surfaceY;
        private long nextPulseTick;

        private Hazard(PotionItemData data, Location center) {
            this.data = data;
            this.center = center;
            this.baseColor = data.getBukkitColor() == null ? Color.WHITE : data.getBukkitColor();
            this.radius = computeRadius(data);
            this.radiusSq = radius * radius;
            this.instant = effectManager.isInstantEffect(data.getDefinition().getId());
            long lifetime = computeLifetimeTicks(data, instant);
            this.expireTick = tickCounter + lifetime;
            this.surfaceY = resolveSurfaceY(center);
            this.ringPoints = buildRingPoints(center.getWorld());
            this.positive = effectManager.isBeneficialDefinition(data.getDefinition().getId());
        }

        private void initialPulse(long currentTick) {
            applyPulse();
            this.nextPulseTick = currentTick + PULSE_INTERVAL_TICKS;
        }

        private boolean isExpired(long currentTick) {
            return currentTick >= expireTick;
        }

        private void tick(long currentTick) {
            renderRing();
            renderAmbient();
            if (currentTick >= nextPulseTick) {
                applyPulse();
                nextPulseTick = currentTick + PULSE_INTERVAL_TICKS;
            }
        }

        private void renderRing() {
            World world = center.getWorld();
            if (world == null) return;
            Particle.DustOptions dust = new Particle.DustOptions(baseColor, 1.1f);
            for (Location point : ringPoints) {
                world.spawnParticle(Particle.DUST, point, 1, 0.01, 0.01, 0.01, 0.0, dust);
            }
        }

        private void renderAmbient() {
            World world = center.getWorld();
            if (world == null) return;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < AMBIENT_PARTICLES_PER_TICK; i++) {
                double distance = Math.sqrt(random.nextDouble()) * radius;
                double angle = random.nextDouble() * Math.PI * 2;
                double x = center.getX() + Math.cos(angle) * distance;
                double z = center.getZ() + Math.sin(angle) * distance;
                double y = surfaceY + random.nextDouble(0.01, 0.08);
                Color variant = jitterColor(baseColor, random, 0.12);
                Particle.DustOptions dust = new Particle.DustOptions(variant, 1.0f);
                world.spawnParticle(Particle.DUST, x, y, z, 1, 0.01, 0.01, 0.01, 0.0, dust);
            }
        }

        private void applyPulse() {
            World world = center.getWorld();
            if (world == null) {
                return;
            }
            emitPulseWave(world);
            Collection<LivingEntity> targets = collectTargets();
            for (LivingEntity living : targets) {
                if (living instanceof ArmorStand) {
                    continue;
                }
                if (positive) {
                    if (!(living instanceof Player player)) {
                        continue;
                    }
                    if (!shouldReceivePositive(player)) {
                        continue;
                    }
                    effectManager.applyEffect(player, data);
                    spawnPositiveImpact(world, player);
                } else {
                    if (living instanceof Player player && !shouldReceiveNegative(player)) {
                        continue;
                    }
                    effectManager.applyEffect(living, data);
                    spawnNegativeImpact(world, living);
                }
            }
        }

        private void emitPulseWave(World world) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int segment = 0; segment < PULSE_WAVE_SEGMENTS; segment++) {
                double angle = (Math.PI * 2 * segment) / PULSE_WAVE_SEGMENTS;
                for (int step = 1; step <= 6; step++) {
                    double progress = (radius / 6.0) * step;
                    double x = center.getX() + Math.cos(angle) * progress;
                    double z = center.getZ() + Math.sin(angle) * progress;
                    Color variant = jitterColor(baseColor, random, 0.08);
                    world.spawnParticle(
                            Particle.DUST,
                            x,
                            surfaceY + 0.02,
                            z,
                            1,
                            0.02,
                            0.01,
                            0.02,
                            0.0,
                            new Particle.DustOptions(variant, 1.0f)
                    );
                }
            }
        }

        private Collection<LivingEntity> collectTargets() {
            World world = center.getWorld();
            if (world == null) {
                return List.of();
            }
            Collection<Entity> nearby = world.getNearbyEntities(center, radius, world.getMaxHeight(), radius,
                    entity -> entity instanceof LivingEntity);
            List<LivingEntity> matches = new ArrayList<>();
            for (Entity entity : nearby) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                double dx = living.getLocation().getX() - center.getX();
                double dz = living.getLocation().getZ() - center.getZ();
                if ((dx * dx + dz * dz) > radiusSq) {
                    continue;
                }
                matches.add(living);
            }
            return matches;
        }

        private void spawnNegativeImpact(World world, LivingEntity target) {
            Color variant = jitterColor(baseColor, ThreadLocalRandom.current(), 0.18);
            Particle.DustOptions dust = new Particle.DustOptions(variant, 1.6f);
            Location loc = target.getLocation().add(0, target.getHeight() * 0.5, 0);
            world.spawnParticle(Particle.DUST, loc, 18, 0.35, 0.45, 0.35, 0.0, dust);
        }

        private void spawnPositiveImpact(World world, Player player) {
            Color variant = jitterColor(baseColor, ThreadLocalRandom.current(), 0.10);
            Particle.DustOptions dust = new Particle.DustOptions(variant, 1.0f);
            Location loc = player.getLocation().add(0, 0.15, 0);
            world.spawnParticle(Particle.DUST, loc, 8, 0.18, 0.05, 0.18, 0.0, dust);
        }

        private List<Location> buildRingPoints(World world) {
            List<Location> points = new ArrayList<>(RING_SEGMENTS);
            double cx = center.getX();
            double cz = center.getZ();
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double angle = (Math.PI * 2 * i) / RING_SEGMENTS;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                points.add(new Location(world, x, surfaceY + 0.02, z));
            }
            return points;
        }
    }

    private double computeRadius(PotionItemData data) {
        return data.getLingeringProfile()
                .map(profile -> Math.max(2.0, profile.radiusBlocks()))
                .orElse(BASE_RADIUS);
    }

    private long computeLifetimeTicks(PotionItemData data, boolean instant) {
        return data.getLingeringProfile()
                .map(profile -> Math.max(20L, profile.durationSeconds() * 20L))
                .orElse(instant ? INSTANT_DURATION_TICKS : BASE_DURATION_TICKS);
    }

    private double resolveSurfaceY(Location impact) {
        if (impact == null) {
            return 0.0;
        }
        return impact.getY();
    }

    private boolean shouldReceivePositive(Player player) {
        return player != null && !isCultist(player) && !isSamurai(player);
    }

    private boolean shouldReceiveNegative(Player player) {
        return player != null && (isCultist(player) || isSamurai(player));
    }

    private boolean isCultist(Player player) {
        return hasClassTag(player, "cultist");
    }

    private boolean isSamurai(Player player) {
        return hasClassTag(player, "samurai");
    }

    private boolean hasClassTag(Player player, String tag) {
        String normalized = tag.toLowerCase(Locale.ENGLISH);
        for (String scoreboardTag : player.getScoreboardTags()) {
            if (scoreboardTag.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard != null ? scoreboard.getEntryTeam(player.getName()) : null;
        if (team != null && team.getName() != null) {
            if (team.getName().toLowerCase(Locale.ENGLISH).contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private Color jitterColor(Color base, ThreadLocalRandom random, double variation) {
        double clamp = Math.max(0.0, variation);
        int r = jitterChannel(base.getRed(), random, clamp);
        int g = jitterChannel(base.getGreen(), random, clamp);
        int b = jitterChannel(base.getBlue(), random, clamp);
        return Color.fromRGB(r, g, b);
    }

    private int jitterChannel(int channel, ThreadLocalRandom random, double variation) {
        int delta = (int) Math.round(255 * variation);
        int value = channel + random.nextInt(-delta, delta + 1);
        return Math.max(0, Math.min(255, value));
    }
}
