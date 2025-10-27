package goat.projectLinearity.subsystems.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns short-lived floating damage indicators for high frequency damage sources
 * such as custom poison and wither effects.
 */
public final class DamageDisplayManager {

    private static final double TEXT_SPEED_MIN = 0.18;
    private static final double TEXT_SPEED_MAX = 0.32;
    private static final double TEXT_VERTICAL_BIAS = 0.20;
    private static final long DISPLAY_LIFETIME_TICKS = 30L; // 1.5 seconds

    private final JavaPlugin plugin;
    private final Set<UUID> trackedStands = ConcurrentHashMap.newKeySet();

    public DamageDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public enum HighFrequencyStyle {
        POISON(ChatColor.GREEN),
        WITHER(ChatColor.BLACK);

        private final ChatColor color;

        HighFrequencyStyle(ChatColor color) {
            this.color = color;
        }

        public ChatColor color() {
            return color;
        }
    }

    public void spawnHighFrequencyDamage(LivingEntity target, HighFrequencyStyle style, double amount) {
        if (target == null || style == null) {
            return;
        }
        if (!target.isValid()) {
            return;
        }
        Location base = target.getLocation();
        if (base.getWorld() == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double offsetX = random.nextDouble(-0.4, 0.4);
        double offsetY = random.nextDouble(0.3, Math.max(0.4, target.getHeight()));
        double offsetZ = random.nextDouble(-0.4, 0.4);

        Location spawn = base.clone().add(offsetX, offsetY, offsetZ);
        ArmorStand stand = (ArmorStand) spawn.getWorld().spawnEntity(spawn, EntityType.ARMOR_STAND);
        configureStand(stand, style, amount);

        applyVelocity(stand, random);
        trackedStands.add(stand.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                removeStand(stand.getUniqueId());
            }
        }.runTaskLater(plugin, DISPLAY_LIFETIME_TICKS);
    }

    public void shutdown() {
        for (UUID standId : new ArrayList<>(trackedStands)) {
            removeStand(standId);
        }
        trackedStands.clear();
    }

    private void configureStand(ArmorStand stand, HighFrequencyStyle style, double amount) {
        stand.setPersistent(false);
        stand.setInvisible(true);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setSilent(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCollidable(false);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(style.color() + formatAmount(amount));
    }

    private void applyVelocity(ArmorStand stand, ThreadLocalRandom random) {
        double yaw = random.nextDouble(0.0, Math.PI * 2);
        double speed = random.nextDouble(TEXT_SPEED_MIN, TEXT_SPEED_MAX);
        double horizontal = Math.cos(yaw) * speed;
        double depth = Math.sin(yaw) * speed;
        double vertical = TEXT_VERTICAL_BIAS + random.nextDouble(0.05, 0.16);
        Vector velocity = new Vector(horizontal, vertical, depth);
        stand.setVelocity(velocity);
    }

    private void removeStand(UUID standId) {
        trackedStands.remove(standId);
        Entity entity = Bukkit.getEntity(standId);
        if (entity instanceof ArmorStand armorStand) {
            armorStand.remove();
        }
    }

    private String formatAmount(double amount) {
        int rounded = (int) Math.round(amount);
        if (Math.abs(amount - rounded) < 0.001) {
            return String.valueOf(rounded);
        }
        return String.format("%.1f", amount);
    }
}
