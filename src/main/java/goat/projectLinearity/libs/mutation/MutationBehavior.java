package goat.projectLinearity.libs.mutation;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.libs.mutation.ThreeHeadedFireballListener;
import goat.projectLinearity.libs.mutation.MutationManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Built-in mutation behaviours.
 */
public enum MutationBehavior {
    NONE {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return null;
        }
    },
    RANGER {
        private static final long INTERVAL_TICKS = 30L; // 1.5s
        private static final double MAX_RANGE = 24.0;
        private static final double PROJECTILE_SPEED = 1.6;

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }

                    Player target = findNearestPlayer(entity, MAX_RANGE);
                    if (target == null) {
                        return;
                    }

                    fireArrow(entity, target);
                }
            }.runTaskTimer(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
        }

        private Player findNearestPlayer(LivingEntity entity, double maxRange) {
            Location origin = entity.getLocation();
            return origin.getWorld()
                    .getPlayers()
                    .stream()
                    .filter(Player::isOnline)
                    .filter(p -> !p.isDead())
                    .filter(p -> Objects.equals(p.getWorld(), origin.getWorld()))
                    .filter(p -> p.getLocation().distanceSquared(origin) <= maxRange * maxRange)
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)))
                    .orElse(null);
        }

        private void fireArrow(LivingEntity entity, Player target) {
            Location origin = entity.getLocation().clone().add(0, entity.getHeight() * 0.5, 0);
            Location targetLocation = target.getLocation().clone().add(0, target.getHeight() * 0.5, 0);
            Vector direction = targetLocation.toVector().subtract(origin.toVector());
            if (direction.lengthSquared() == 0) {
                return;
            }
            direction.normalize().multiply(PROJECTILE_SPEED);

            AbstractArrow arrow = entity.launchProjectile(Arrow.class);
            if (arrow == null) {
                arrow = origin.getWorld().spawn(origin, Arrow.class);
                arrow.setShooter(entity);
                arrow.setVelocity(direction);
            } else {
                arrow.teleport(origin);
                arrow.setVelocity(direction);
            }
            arrow.setCritical(true);
        }
    },
    SCAVENGER {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            var equipment = entity.getEquipment();
            if (equipment == null) {
                return null;
            }
            equipment.setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setHelmet(null);
            equipment.setHelmetDropChance(0.0f);
            return null;
        }
    },
    WITHERED {
        private static final Color ARMOR_COLOR = Color.fromRGB(24, 24, 24);

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            var equipment = entity.getEquipment();
            if (equipment == null) {
                return null;
            }
            equipment.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setHelmet(null);
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplate(applyColor(equipment.getChestplate(), Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(applyColor(equipment.getLeggings(), Material.LEATHER_LEGGINGS));
            equipment.setBoots(applyColor(equipment.getBoots(), Material.LEATHER_BOOTS));
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            return null;
        }

        private ItemStack applyColor(ItemStack current, Material fallback) {
            ItemStack piece = current;
            if (piece == null || piece.getType() == Material.AIR) {
                piece = new ItemStack(fallback);
            } else {
                piece = piece.clone();
            }
            if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(ARMOR_COLOR);
                piece.setItemMeta(meta);
            }
            return piece;
        }
    },
    CHARRED {
        private static final Color ARMOR_COLOR = Color.fromRGB(16, 16, 16);

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            var equipment = entity.getEquipment();
            if (equipment == null) {
                return null;
            }
            equipment.setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplate(applyColor(equipment.getChestplate(), Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(applyColor(equipment.getLeggings(), Material.LEATHER_LEGGINGS));
            equipment.setBoots(applyColor(equipment.getBoots(), Material.LEATHER_BOOTS));
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            equipment.setItemInMainHandDropChance(0.0f);
            return null;
        }

        private ItemStack applyColor(ItemStack current, Material fallback) {
            ItemStack piece = current;
            if (piece == null || piece.getType() == Material.AIR) {
                piece = new ItemStack(fallback);
            } else {
                piece = piece.clone();
            }
            if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setColor(ARMOR_COLOR);
                piece.setItemMeta(meta);
            }
            return piece;
        }
    },
    VOLTAIC_CREEPER {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            if (entity instanceof Creeper creeper) {
                creeper.setPowered(true);
            }
            return null;
        }
    },
    STORMCALLER {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return null;
        }
    },
    DUST_DEMON {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return null;
        }
    },
    SUBSONIC {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return null;
        }
    },
    RABID_CUBE {
        private static final long CHECK_INTERVAL_TICKS = 1L;
        private static final long SLAM_DELAY_TICKS = 7L;
        private static final double SLAM_VERTICAL_SPEED = -2.8D;
        private static final double SLAM_RADIUS = 3.0D;
        private static final double SLAM_VERTICAL_RADIUS = 1.5D;
        private static final double SLAM_DAMAGE = 6.0D;
        private static final int RING_POINTS = 32;

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return new BukkitRunnable() {
                private boolean jumpInProgress = false;
                private boolean slammedThisJump = false;
                private boolean slamImpactPending = false;
                private long airborneTicks = 0L;

                @Override
                public void run() {
                    if (!entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }

                    if (entity.isOnGround()) {
                        if (slamImpactPending) {
                            slamImpactPending = false;
                            performSlamImpact(entity);
                        }
                        jumpInProgress = false;
                        slammedThisJump = false;
                        airborneTicks = 0L;
                        return;
                    }

                    if (!jumpInProgress) {
                        jumpInProgress = true;
                        slammedThisJump = false;
                        airborneTicks = 0L;
                        return;
                    }

                    airborneTicks++;
                    if (!slammedThisJump && airborneTicks >= SLAM_DELAY_TICKS) {
                        Vector current = entity.getVelocity();
                        Vector slamVelocity = new Vector(
                                current.getX(),
                                SLAM_VERTICAL_SPEED,
                                current.getZ()
                        );
                        entity.setVelocity(slamVelocity);
                        slammedThisJump = true;
                        slamImpactPending = true;
                    }
                }
            }.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
        }

        private void performSlamImpact(LivingEntity entity) {
            Location center = entity.getLocation().clone();
            World world = center.getWorld();
            if (world == null) {
                return;
            }
            slamNearbyEntities(entity, world, center);
            spawnFlameRing(world, center);
        }

        private void slamNearbyEntities(LivingEntity source, World world, Location center) {
            for (Entity nearby : world.getNearbyEntities(center, SLAM_RADIUS, SLAM_VERTICAL_RADIUS, SLAM_RADIUS)) {
                if (!(nearby instanceof LivingEntity target)) {
                    continue;
                }
                if (target.getUniqueId().equals(source.getUniqueId())) {
                    continue;
                }
                target.damage(SLAM_DAMAGE, source);
            }
        }

        private void spawnFlameRing(World world, Location center) {
            Location particleBase = center.clone().add(0, 0.1D, 0);
            for (int i = 0; i < RING_POINTS; i++) {
                double angle = (Math.PI * 2 * i) / RING_POINTS;
                double x = SLAM_RADIUS * Math.cos(angle);
                double z = SLAM_RADIUS * Math.sin(angle);
                Location particleLocation = particleBase.clone().add(x, 0, z);
                world.spawnParticle(Particle.FLAME, particleLocation, 2, 0.05, 0.01, 0.05, 0.0);
            }
        }
    },
    SNEAK_MITE {
        private static final long AURA_INTERVAL_TICKS = 20L;
        private static final int INVISIBILITY_DURATION_TICKS = 200;

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            entity.setInvisible(true);
            applyInvisibility(entity);
            return new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }
                    if (!entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        applyInvisibility(entity);
                    }
                    spawnAura(entity);
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    super.cancel();
                    if (entity.isValid()) {
                        entity.setInvisible(false);
                        entity.removePotionEffect(PotionEffectType.INVISIBILITY);
                    }
                }
            }.runTaskTimer(plugin, 0L, AURA_INTERVAL_TICKS);
        }

        private void applyInvisibility(LivingEntity entity) {
            PotionEffect effect = new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    INVISIBILITY_DURATION_TICKS,
                    0,
                    true,
                    false,
                    false
            );
            entity.addPotionEffect(effect);
        }

        private void spawnAura(LivingEntity entity) {
            World world = entity.getWorld();
            if (world == null) {
                return;
            }
            Location base = entity.getLocation().clone().add(0, 0.25, 0);
            world.spawnParticle(Particle.PORTAL, base, 8, 0.25, 0.35, 0.25, 0.02);
        }
    },
    VEIL_WARDEN {
        private static final long SPAWN_INTERVAL_TICKS = 2400L;
        private static final int ENDERMITE_CAP = 20;

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }
                    World world = entity.getWorld();
                    if (world == null) {
                        return;
                    }
                    if (world.getEntitiesByClass(Endermite.class).size() >= ENDERMITE_CAP) {
                        return;
                    }
                    spawnSneakMite(plugin, entity);
                }
            }.runTaskTimer(plugin, SPAWN_INTERVAL_TICKS, SPAWN_INTERVAL_TICKS);
        }

        private void spawnSneakMite(ProjectLinearity plugin, LivingEntity source) {
            World world = source.getWorld();
            if (world == null) {
                return;
            }
            Location origin = source.getLocation().clone().add(randomOffset(), 0.2, randomOffset());
            Endermite spawned = world.spawn(origin, Endermite.class, mite -> mite.setPersistent(true));
            if (spawned == null) {
                return;
            }
            MutationManager manager = plugin.getMutationManager();
            if (manager != null) {
                manager.forceApplyMutation(spawned, "sneak_mite");
            }
        }

        private double randomOffset() {
            return ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
        }
    },
    THREE_HEADED_GHAST {
        private static final long TICK_INTERVAL = 5L;
        private static final long SHOT_INTERVAL_TICKS = 20L;
        private static final long AMBIENT_INTERVAL_TICKS = 160L;
        private static final double TARGET_RANGE = 48.0D;
        private static final int FIREBALL_COUNT = 1;
        private static final double FIREBALL_SPEED = 1.3D;
        private static final double FIREBALL_SPREAD = 0.0D;

        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            entity.setSilent(true);
            return new BukkitRunnable() {
                private long shotTimer = SHOT_INTERVAL_TICKS;
                private long ambientTimer = AMBIENT_INTERVAL_TICKS;

                @Override
                public void run() {
                    if (!entity.isValid() || entity.isDead()) {
                        cancel();
                        return;
                    }
                    dampenMovement(entity);
                    shotTimer -= TICK_INTERVAL;
                    ambientTimer -= TICK_INTERVAL;
                    if (shotTimer <= 0) {
                        Player target = findNearestPlayer(entity, TARGET_RANGE);
                        if (target != null) {
                            fireShotgun(entity, target);
                        }
                        shotTimer = SHOT_INTERVAL_TICKS;
                    }
                    if (ambientTimer <= 0) {
                        playAmbient(entity);
                        ambientTimer = AMBIENT_INTERVAL_TICKS;
                    }
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    super.cancel();
                    if (entity.isValid()) {
                        entity.setSilent(false);
                    }
                }
            }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
        }

        private void dampenMovement(LivingEntity entity) {
            Vector velocity = entity.getVelocity();
            Vector drag = velocity.clone().multiply(-0.85);
            Vector reverseDirection = entity.getLocation().getDirection().clone();
            if (reverseDirection.lengthSquared() > 0) {
                reverseDirection.normalize().multiply(-0.05);
            }
            Vector adjusted = velocity.clone().add(drag);
            if (reverseDirection.lengthSquared() > 0) {
                adjusted.add(reverseDirection);
            }
            entity.setVelocity(adjusted);
        }

        private Player findNearestPlayer(LivingEntity entity, double range) {
            Location origin = entity.getLocation();
            return origin.getWorld()
                    .getPlayers()
                    .stream()
                    .filter(Player::isOnline)
                    .filter(p -> !p.isDead())
                    .filter(p -> Objects.equals(p.getWorld(), origin.getWorld()))
                    .filter(p -> p.getLocation().distanceSquared(origin) <= range * range)
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)))
                    .orElse(null);
        }

        private void fireShotgun(LivingEntity entity, Player target) {
            Location origin = entity.getLocation().clone().add(0, entity.getHeight() * 0.5, 0);
            Location targetLocation = target.getLocation().clone().add(0, target.getHeight() * 0.5, 0);
            Vector base = targetLocation.toVector().subtract(origin.toVector());
            Vector forward = base.clone();
            if (forward.lengthSquared() > 0) {
                forward.normalize();
            }
            Location spawnOrigin = origin.clone();
            if (forward.lengthSquared() > 0) {
                spawnOrigin.add(forward.clone().multiply(8.0));
            }
            if (base.lengthSquared() == 0) {
                return;
            }
            World world = entity.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < FIREBALL_COUNT; i++) {
                Vector direction = base.clone();
                direction.add(new Vector(
                        random.nextDouble(-FIREBALL_SPREAD, FIREBALL_SPREAD),
                        random.nextDouble(-FIREBALL_SPREAD, FIREBALL_SPREAD),
                        random.nextDouble(-FIREBALL_SPREAD, FIREBALL_SPREAD)
                ));
                if (direction.lengthSquared() == 0) {
                    continue;
                }
                direction.normalize().multiply(FIREBALL_SPEED);
                world.spawn(spawnOrigin, Fireball.class, fireball -> {
                    fireball.setShooter(entity);
                    fireball.setYield(3f);
                    fireball.setIsIncendiary(true);
                    fireball.setDirection(direction);
                    fireball.setVelocity(direction);
                    ThreeHeadedFireballListener.mark(fireball, ProjectLinearity.getInstance());
                });
            }
        }

        private void playAmbient(LivingEntity entity) {
            World world = entity.getWorld();
            if (world == null) {
                return;
            }
            world.playSound(entity.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 8f, 0.35f);
        }
    };

    public abstract BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation);
}
