package goat.projectLinearity.libs.mutation;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Objects;

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
    SUBSONIC {
        @Override
        public BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation) {
            return null;
        }
    };

    public abstract BukkitTask start(ProjectLinearity plugin, LivingEntity entity, ActiveMutation activeMutation);
}
