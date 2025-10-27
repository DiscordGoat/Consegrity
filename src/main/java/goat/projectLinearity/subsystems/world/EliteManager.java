package goat.projectLinearity.subsystems.world;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.mechanics.AggressionManager;
import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.inventory.ItemStack;

/**
 * Handles spawning, upkeep, and rewards for Elite monsters.
 */
public final class EliteManager implements Listener {

    private static final String ELITE_TAG = "pl_elite";
    private static final double ELITE_HEALTH = 500.0;
    private static final double ELITE_DAMAGE = 7.0;
    private static final double REGEN_PER_TICK = 4.0;
    private static final double NEARBY_RADIUS = 15.0;
    private static final double PULL_RADIUS = 15.0;
    private static final double RETARGET_RADIUS = 48.0;
    private static final long DURATION_MS = 60_000L;
    private static final long AMBIENT_INTERVAL_MS = 6_000L;
    private static final long MELEE_TIMEOUT_MS = 4_000L;

    private final ProjectLinearity plugin;
    private final List<EntityType> eligibleTypes;
    private final NamespacedKey eliteKey;
    private BukkitTask task;
    private EliteData elite;

    public EliteManager(ProjectLinearity plugin) {
        this.plugin = plugin;
        this.eligibleTypes = List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.CREEPER,
                EntityType.SPIDER,
                EntityType.CAVE_SPIDER,
                EntityType.ENDERMAN,
                EntityType.HUSK,
                EntityType.DROWNED,
                EntityType.STRAY,
                EntityType.PIGLIN,
                EntityType.PIGLIN_BRUTE,
                EntityType.ZOMBIFIED_PIGLIN,
                EntityType.BLAZE,
                EntityType.WITHER_SKELETON,
                EntityType.MAGMA_CUBE,
                EntityType.SLIME,
                EntityType.GHAST
        );
        this.eliteKey = new NamespacedKey(plugin, "elite_flag");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        if (elite != null && !elite.isValid()) {
            demote();
        }
        if (elite == null) {
            tryPromote();
            return;
        }
        updateElite();
    }

    private void tryPromote() {
        EntityType type = eligibleTypes.get(ThreadLocalRandom.current().nextInt(eligibleTypes.size()));
        LivingEntity candidate = selectRandomEntity(type);
        if (candidate != null) {
            promote(candidate);
        }
    }

    private LivingEntity selectRandomEntity(EntityType type) {
        LivingEntity choice = null;
        int seen = 0;
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            if (!("Consegrity".equalsIgnoreCase(name) || world.getEnvironment() == World.Environment.NETHER)) {
                continue;
            }
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.getType() != type) continue;
                if (!isEligible(entity)) continue;
                seen++;
                if (ThreadLocalRandom.current().nextInt(seen) == 0) {
                    choice = entity;
                }
            }
        }
        return choice;
    }

    private boolean isEligible(LivingEntity entity) {
        if (entity.isDead() || !entity.isValid()) return false;
        if (entity instanceof Player) return false;
        if (entity.getScoreboardTags().contains(ELITE_TAG)) return false;
        return true;
    }

    private void promote(LivingEntity entity) {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double previousMax = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : entity.getMaxHealth();
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(ELITE_HEALTH);
        }
        entity.setHealth(ELITE_HEALTH);

        entity.getWorld().strikeLightningEffect(entity.getLocation());

        Double previousDamage = null;
        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            previousDamage = damageAttr.getBaseValue();
            damageAttr.setBaseValue(ELITE_DAMAGE);
        }

        entity.addScoreboardTag(ELITE_TAG);
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(eliteKey, PersistentDataType.BYTE, (byte) 1);

        ProjectLinearity pluginInstance = ProjectLinearity.getInstance();
        AggressionManager aggression = ProjectLinearity.getInstance() != null ? ProjectLinearity.getInstance().getAggressionManager() : null;
        if (aggression != null) {
            aggression.applyNeutral(entity);
        }

        entity.setGlowing(true);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, true, false, false));
        if (entity.getType() == EntityType.MAGMA_CUBE || entity.getType() == EntityType.SLIME) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 2, true, false, false));
        }

        BossBar bossBar = Bukkit.createBossBar(ChatColor.RED + "Elite " + formatName(entity), BarColor.RED, BarStyle.SOLID);
        elite = new EliteData(entity, bossBar, previousMax, previousDamage);
    }

    private String formatName(LivingEntity entity) {
        String custom = entity.getCustomName();
        if (custom != null && !custom.isEmpty()) {
            return ChatColor.stripColor(custom);
        }
        return entity.getType().name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
    }

    private void updateElite() {
        EliteData data = elite;
        if (data == null) {
            return;
        }
        LivingEntity entity = data.entity;
        if (!entity.isValid()) {
            demote();
            return;
        }

        updateBossBar(data);
        if (elite == null) return;
        handleAmbient(data);
        if (elite == null) return;
        handleExpiry(data);
        if (elite == null) return;
        handleTargeting(data);
        if (elite == null) return;
        handleRegeneration(data);
        if (elite == null) return;
        handlePull(data);
    }

    private void updateBossBar(EliteData data) {
        LivingEntity entity = data.entity;
        BossBar bossBar = data.bossBar;
        double max = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                : entity.getMaxHealth();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, entity.getHealth() / max)));

        Set<Player> nearby = new HashSet<>();
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= NEARBY_RADIUS * NEARBY_RADIUS) {
                nearby.add(player);
            }
        }
        for (Player player : bossBar.getPlayers()) {
            if (!nearby.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
        for (Player player : nearby) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private void handleAmbient(EliteData data) {
        long now = System.currentTimeMillis();
        if (now - data.lastAmbient >= AMBIENT_INTERVAL_MS) {
            data.lastAmbient = now;
            data.entity.getWorld().playSound(data.entity.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 1.0f);
        }
    }

    private void handleExpiry(EliteData data) {
        long now = System.currentTimeMillis();
        boolean playerNear = false;
        for (Player player : data.entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(data.entity.getLocation()) <= NEARBY_RADIUS * NEARBY_RADIUS) {
                playerNear = true;
                break;
            }
        }
        if (playerNear) {
            data.expiryTime = now + DURATION_MS;
        } else if (now > data.expiryTime) {
            demote();
        }
    }

    private void handleTargeting(EliteData data) {
        LivingEntity entity = data.entity;
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (!data.aggro) {
            mob.setTarget(null);
            return;
        }
        Player closest = findNearestPlayer(entity.getLocation(), RETARGET_RADIUS);
        mob.setTarget(closest);
    }

    private void handleRegeneration(EliteData data) {
        Player closest = findNearestPlayer(data.entity.getLocation(), Double.MAX_VALUE);
        if (closest == null) {
            healElite(data, REGEN_PER_TICK);
            return;
        }
        double distance = closest.getLocation().distance(data.entity.getLocation());
        if (distance > 20.0) {
            healElite(data, REGEN_PER_TICK);
        }
    }

    private void handlePull(EliteData data) {
        if (!data.aggro) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - data.lastMeleeContact <= MELEE_TIMEOUT_MS) {
            return;
        }
        Player player = findNearestPlayer(data.entity.getLocation(), 10.0);
        if (player == null) {
            return;
        }
        Vector direction = data.entity.getLocation().toVector().subtract(player.getLocation().toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        direction.normalize().multiply(0.9).setY(0.4);
        player.setVelocity(direction);
        data.lastMeleeContact = now;
    }

    private void healElite(EliteData data, double amount) {
        AttributeInstance attr = data.entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = attr != null ? attr.getValue() : data.entity.getMaxHealth();
        data.entity.setHealth(Math.min(max, data.entity.getHealth() + amount));
    }

    private Player findNearestPlayer(Location origin, double radius) {
        Player closest = null;
        double best = radius * radius;
        for (Player player : origin.getWorld().getPlayers()) {
            double dist = player.getLocation().distanceSquared(origin);
            if (dist < best) {
                best = dist;
                closest = player;
            }
        }
        return closest;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (elite != null && event.getEntity().equals(elite.entity)) {
            boolean shouldAggro = false;
            if (event.getDamager() instanceof Player) {
                shouldAggro = true;
            } else if (event.getDamager() instanceof Projectile projectile) {
                Object shooter = projectile.getShooter();
                if (shooter instanceof Player) {
                    shouldAggro = true;
                }
            }
            if (shouldAggro) {
                elite.aggro = true;
                AggressionManager aggression = ProjectLinearity.getInstance() != null ? ProjectLinearity.getInstance().getAggressionManager() : null;
                if (aggression != null) {
                    aggression.markProvoked(elite.entity, 15_000L);
                }
                if (!(event.getDamager() instanceof Projectile)) {
                    elite.lastMeleeContact = System.currentTimeMillis();
                }
            }
        }
        if (elite != null && event.getDamager().equals(elite.entity) && !(event.getEntity() instanceof Projectile)) {
            elite.lastMeleeContact = System.currentTimeMillis();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (elite != null && event.getAffectedEntities().contains(elite.entity)) {
            if (event.getPotion().getShooter() instanceof Player) {
                elite.aggro = true;
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (elite != null && event.getEntity().equals(elite.entity)) {
            dropRewards(event.getEntity());
            demote();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (elite != null) {
            elite.bossBar.removePlayer(event.getPlayer());
        }
    }

    private void dropRewards(LivingEntity entity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack book = ItemRegistry.getItemByName("Enchanted Book");
        if (book == null) {
            book = new ItemStack(Material.ENCHANTED_BOOK);
        }
        ItemStack crown = ItemRegistry.getItemByName("Golden Crown");
        if (crown == null) {
            crown = new ItemStack(Material.GOLDEN_HELMET);
        }
        rollDrop(entity, book, random);
        rollDrop(entity, crown, random);
        rollDrop(entity, new ItemStack(Material.EMERALD, 32), random);
        rollDrop(entity, new ItemStack(Material.DIAMOND, 1), random);
        rollDrop(entity, new ItemStack(Material.NETHER_WART, 1), random);
        rollDrop(entity, new ItemStack(Material.HONEY_BOTTLE, 4), random);
    }

    private void rollDrop(LivingEntity entity, ItemStack stack, ThreadLocalRandom random) {
        if (stack == null) {
            return;
        }
        if (random.nextBoolean()) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), stack);
        }
    }

    private void demote() {
        if (elite == null) return;
        LivingEntity entity = elite.entity;
        entity.getScoreboardTags().remove(ELITE_TAG);
        entity.getPersistentDataContainer().remove(eliteKey);
        entity.setGlowing(false);
        entity.removePotionEffect(PotionEffectType.SPEED);
        entity.removePotionEffect(PotionEffectType.JUMP_BOOST);

        AggressionManager aggression = ProjectLinearity.getInstance() != null ? ProjectLinearity.getInstance().getAggressionManager() : null;
        if (aggression != null) {
            aggression.clearOverride(entity);
        }

        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(elite.previousMaxHealth);
            entity.setHealth(Math.min(entity.getHealth(), elite.previousMaxHealth));
        }
        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null && elite.previousAttackDamage != null) {
            damageAttr.setBaseValue(elite.previousAttackDamage);
        }
        elite.bossBar.removeAll();
        elite = null;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        demote();
        HandlerList.unregisterAll(this);
    }

    public LivingEntity getCurrentElite() {
        return elite != null ? elite.entity : null;
    }

    private static final class EliteData {
        final LivingEntity entity;
        final BossBar bossBar;
        final double previousMaxHealth;
        final Double previousAttackDamage;
        boolean aggro = false;
        long expiryTime = System.currentTimeMillis() + DURATION_MS;
        long lastAmbient = System.currentTimeMillis();
        long lastMeleeContact = System.currentTimeMillis();

        EliteData(LivingEntity entity, BossBar bossBar, double previousMaxHealth, Double previousAttackDamage) {
            this.entity = entity;
            this.bossBar = bossBar;
            this.previousMaxHealth = previousMaxHealth;
            this.previousAttackDamage = previousAttackDamage;
        }

        boolean isValid() {
            return entity.isValid() && !entity.isDead();
        }
    }
}
