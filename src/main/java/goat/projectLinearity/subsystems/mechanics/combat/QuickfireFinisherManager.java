package goat.projectLinearity.subsystems.mechanics.combat;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the bow quickfire + finisher loop.
 */
public final class QuickfireFinisherManager implements Listener {
    private static final String QUICKFIRE_METADATA = "consegrity_quickfire";
    private static final long QUICKFIRE_COOLDOWN_MS = 5000L;
    private static final long STUN_DURATION_TICKS = 100L;
    private static final int INVINCIBILITY_TICKS = 40;
    private static final Set<Material> ARROW_TYPES = EnumSet.of(
            Material.ARROW,
            Material.TIPPED_ARROW,
            Material.SPECTRAL_ARROW
    );

    private final ProjectLinearity plugin;
    private final Map<UUID, QuickfireShot> quickfireProjectiles = new HashMap<>();
    private final Map<UUID, StunRecord> stunnedEntities = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> recentInteract = new HashMap<>();

    public QuickfireFinisherManager(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        boolean trigger = switch (action) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR -> true;
            default -> false;
        };
        if (!trigger) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BOW) {
            return;
        }
        Player player = event.getPlayer();
        if (attemptQuickfire(player, true)) {
            recentInteract.put(player.getUniqueId(), System.currentTimeMillis());
            event.setCancelled(true);
            if (action == Action.RIGHT_CLICK_AIR) {
                player.swingMainHand();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBowMeleeSwing(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.BOW) {
            return;
        }
        if (attemptQuickfire(player, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        QuickfireShot shot = quickfireProjectiles.remove(arrow.getUniqueId());
        if (shot == null && !arrow.hasMetadata(QUICKFIRE_METADATA)) {
            return;
        }

        Entity hit = event.getHitEntity();
        if (!(hit instanceof LivingEntity living)) {
            return;
        }
        applyStun(living, shot != null ? shot.ownerId : findShooterId(arrow));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        StunRecord stun = stunnedEntities.get(target.getUniqueId());
        if (stun == null) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Arrow arrow) {
            quickfireProjectiles.remove(arrow.getUniqueId());
            ProjectileSource source = arrow.getShooter();
            if (source instanceof Player shooter && shooter.getUniqueId().equals(stun.ownerId)) {
                handleRangedFinisher(shooter, target, event);
            } else {
                releaseStun(target.getUniqueId(), true, false);
            }
            return;
        }

        if (damager instanceof Player player) {
            if (player.getUniqueId().equals(stun.ownerId)) {
                handleMeleeFinisher(player, target, event);
            } else {
                releaseStun(target.getUniqueId(), true, false);
            }
            return;
        }

        releaseStun(target.getUniqueId(), true, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAmbientDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (stunnedEntities.containsKey(living.getUniqueId())) {
            releaseStun(living.getUniqueId(), true, false);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        releaseStun(event.getEntity().getUniqueId(), false, false);
    }

    private void handleRangedFinisher(Player shooter, LivingEntity target, EntityDamageByEntityEvent event) {
        event.setDamage(20.0);
        performFinisherFeedback(shooter, target, AttackMode.RANGED);
        releaseStun(target.getUniqueId(), true, false);
    }

    private void handleMeleeFinisher(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 2.0);
        player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), INVINCIBILITY_TICKS));
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector velocity = target.getVelocity();
                target.setVelocity(velocity.multiply(2.0));
            }
        }.runTask(plugin);
        performFinisherFeedback(player, target, AttackMode.MELEE);
        releaseStun(target.getUniqueId(), true, false);
    }

    private void performFinisherFeedback(Player player, LivingEntity target, AttackMode mode) {
        FinisherStyle style = FinisherStyle.random();
        player.sendMessage(ChatColor.GOLD + "Finisher: " + style.display + ChatColor.YELLOW + " [" + mode.label + "]");
        Location center = target.getLocation().add(0, target.getHeight() * 0.5, 0);
        World world = target.getWorld();
        world.spawnParticle(style.particle, center, 24, 0.4, 0.6, 0.4, 0.1);
        world.playSound(center, style.sound, 1.0f, style.pitch);
    }

    private void applyStun(LivingEntity target, UUID ownerId) {
        if (target == null || ownerId == null) {
            return;
        }
        releaseStun(target.getUniqueId(), true, false);
        boolean hadAI = target.hasAI();
        target.setAI(false);
        spawnCritBurst(target.getLocation().add(0, target.getHeight() * 0.5, 0), target.getWorld());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                releaseStun(target.getUniqueId(), true, true);
            }
        };
        int taskId = runnable.runTaskLater(plugin, STUN_DURATION_TICKS).getTaskId();
        stunnedEntities.put(target.getUniqueId(), new StunRecord(new WeakReference<>(target), ownerId, hadAI, taskId));
    }

    private void releaseStun(UUID entityId, boolean restoreAI, boolean fromExpiry) {
        if (entityId == null) return;
        StunRecord record = stunnedEntities.remove(entityId);
        if (record == null) return;
        if (!fromExpiry && record.taskId != -1) {
            Bukkit.getScheduler().cancelTask(record.taskId);
        }
        LivingEntity entity = record.entityRef.get();
        if (entity != null && entity.isValid() && restoreAI) {
            entity.setAI(record.hadAI);
            entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation().add(0, entity.getHeight() * 0.5, 0), 12, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private void spawnCritBurst(Location location, World world) {
        world.spawnParticle(Particle.CRIT, location, 40, 0.5, 0.5, 0.5, 0.15);
        world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.2f);
    }

    private UUID findShooterId(Arrow arrow) {
        ProjectileSource source = arrow.getShooter();
        if (source instanceof Player player) {
            return player.getUniqueId();
        }
        return null;
    }

    private boolean isCooldownReady(Player player) {
        long now = System.currentTimeMillis();
        Long ready = cooldowns.get(player.getUniqueId());
        return ready == null || ready <= now;
    }

    private boolean attemptQuickfire(Player player, boolean notifyCooldown) {
        if (!isCooldownReady(player)) {
            if (notifyCooldown) {
                long remaining = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
                long seconds = Math.max(1L, (remaining + 999L) / 1000L);
                player.sendMessage(ChatColor.RED + "Quickfire recharging (" + seconds + "s)");
            }
            return false;
        }
        if (!consumeArrow(player)) {
            player.sendMessage(ChatColor.RED + "You need an arrow to quickfire.");
            return false;
        }
        Arrow arrow = player.launchProjectile(Arrow.class);
        Vector velocity = player.getLocation().getDirection().normalize().multiply(2.8);
        arrow.setVelocity(velocity);
        arrow.setCritical(true);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setMetadata(QUICKFIRE_METADATA, new FixedMetadataValue(plugin, true));
        quickfireProjectiles.put(arrow.getUniqueId(), new QuickfireShot(player.getUniqueId()));
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + QUICKFIRE_COOLDOWN_MS);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
        return true;
    }

    private boolean consumeArrow(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isArrow(stack)) {
                decrementStack(inventory, slot, stack);
                return true;
            }
        }
        ItemStack offhand = inventory.getItemInOffHand();
        if (isArrow(offhand)) {
            if (offhand.getAmount() <= 1) {
                inventory.setItemInOffHand(null);
            } else {
                offhand.setAmount(offhand.getAmount() - 1);
                inventory.setItemInOffHand(offhand);
            }
            return true;
        }
        return false;
    }

    private void decrementStack(PlayerInventory inventory, int slot, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            inventory.setItem(slot, stack);
        }
    }

    private boolean isArrow(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && ARROW_TYPES.contains(stack.getType());
    }

    @EventHandler
    public void onBowSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.BOW) {
            return;
        }
        long last = recentInteract.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (now - last < 100L) {
            return;
        }
        if (attemptQuickfire(player, true)) {
            recentInteract.put(player.getUniqueId(), now);
            player.swingMainHand();
        }
    }

    private record QuickfireShot(UUID ownerId) {}

    private static final class StunRecord {
        final WeakReference<LivingEntity> entityRef;
        final UUID ownerId;
        final boolean hadAI;
        final int taskId;

        StunRecord(WeakReference<LivingEntity> entityRef, UUID ownerId, boolean hadAI, int taskId) {
            this.entityRef = entityRef;
            this.ownerId = ownerId;
            this.hadAI = hadAI;
            this.taskId = taskId;
        }
    }

    private enum AttackMode {
        MELEE("Melee"),
        RANGED("Ranged");

        final String label;

        AttackMode(String label) {
            this.label = label;
        }
    }

    private enum FinisherStyle {
        SKYFALL("Skyfall", Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f,
                (player, target, mode) -> {
                    Location loc = target.getLocation();
                    loc.getWorld().strikeLightningEffect(loc);
                    Vector v = target.getVelocity();
                    target.setVelocity(new Vector(v.getX(), 1.0, v.getZ()));
                }),
        HEARTSEEKER("Heartseeker", Particle.CRIT, Sound.ENTITY_ARROW_HIT_PLAYER, 1.6f,
                (player, target, mode) -> {
                    double max = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(Math.min(max, player.getHealth() + 8.0));
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 80, 1, true, false, true));
                }),
        ECHO_BREAK("Echo Break", Particle.CRIT, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.3f,
                (player, target, mode) -> {
                    Location center = target.getLocation();
                    center.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1, 0, 0, 0, 0);
                    for (Entity nearby : target.getNearbyEntities(4.0, 2.0, 4.0)) {
                        if (nearby instanceof LivingEntity living && !living.equals(target)) {
                            living.damage(6.0, player);
                            Vector push = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.0);
                            push.setY(0.35);
                            living.setVelocity(push);
                        }
                    }
                });

        final String display;
        final Particle particle;
        final Sound sound;
        final float pitch;
        final FinisherEffect effect;

        FinisherStyle(String display, Particle particle, Sound sound, float pitch, FinisherEffect effect) {
            this.display = display;
            this.particle = particle;
            this.sound = sound;
            this.pitch = pitch;
            this.effect = effect;
        }

        static FinisherStyle random() {
            FinisherStyle[] values = values();
            return values[ThreadLocalRandom.current().nextInt(values.length)];
        }
    }

    @FunctionalInterface
    private interface FinisherEffect {
        void apply(Player player, LivingEntity target, AttackMode mode);
    }
}
