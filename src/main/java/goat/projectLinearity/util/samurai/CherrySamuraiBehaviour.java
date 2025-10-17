package goat.projectLinearity.util.samurai;

import goat.projectLinearity.world.ConsegrityRegions;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles high-level Samurai behaviour state management.
 *
 * <p>Current implementation covers Roam, Combat, and Ranged states.</p>
 */
public final class CherrySamuraiBehaviour implements Runnable {

    private static final long TICK_INTERVAL = 20L;

    private static final double ROAM_RANGE = 20.0;
    private static final int ROAM_ATTEMPTS = 20;
    private static final double ARRIVAL_DISTANCE_SQ = 2.25; // 1.5 blocks
    private static final long ROAM_TARGET_TIMEOUT_MS = 25_000L;
    private static final long ROAM_PAUSE_DURATION_MS = 5_000L;
    private static final double MIN_TRAVEL_DISTANCE_SQ = 1.0; // one block squared

    private static final double LOOK_FOV_THRESHOLD = Math.cos(Math.toRadians(35.0));
    private static final double LOOK_RANGE = 20.0;
    private static final double LOOK_RANGE_SQ = LOOK_RANGE * LOOK_RANGE;
    private static final double MOVING_SIGHT_RANGE = 20.0; // Increased from 6.0 to 20.0
    private static final double MOVING_SIGHT_RANGE_SQ = MOVING_SIGHT_RANGE * MOVING_SIGHT_RANGE;

    private static final double MELEE_RANGE_SQ = 9.0; // within 3 blocks
    private static final long MELEE_COOLDOWN_MS = 1_000L;
    private static final long STALL_WINDOW_MS = 3_000L;
    private static final double STALL_DISTANCE_REQUIRED_SQ = 16.0; // 4 blocks squared
    private static final double RANGED_REENTRY_DISTANCE_SQ = 25.0; // 5 blocks squared
    private static final long RANGED_SHOT_INTERVAL_MS = 5_000L;
    private static final long DEFLECTION_DURATION_MS = 10_000L;
    private static final double DEFLECTION_SPEED_MODIFIER = 0.5;

    private static final PotionEffect COMBAT_SPEED = new PotionEffect(
            PotionEffectType.SPEED,
            Integer.MAX_VALUE,
            0,
            true,
            false,
            false
    );

    private final JavaPlugin plugin;
    private final SamuraiPopulationManager populationManager;
    private final Map<UUID, SamuraiContext> contexts = new HashMap<>();
    private final BukkitTask task;

    public CherrySamuraiBehaviour(JavaPlugin plugin, SamuraiPopulationManager populationManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, TICK_INTERVAL, TICK_INTERVAL);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Set<UUID> active = new HashSet<>();

        for (NPC npc : populationManager.getActiveSamurai()) {
            if (!npc.isSpawned()) {
                continue;
            }
            Entity entity = npc.getEntity();
            if (!(entity instanceof LivingEntity living) || !living.isValid()) {
                continue;
            }

            UUID id = npc.getUniqueId();
            active.add(id);
            SamuraiContext context = contexts.computeIfAbsent(id, ignored -> new SamuraiContext());
            if (context.state == null) {
                enterRoamState(npc, living, context);
            }

            context.refreshDeflection(now);

            LivingEntity target = resolveTarget(context);
            if (!ensureTargetValid(npc, context, target)) {
                target = null;
            }
            if (!npc.isSpawned()) {
                contexts.remove(id);
                continue;
            }

            switch (context.state) {
                case ROAM -> tickRoam(npc, living, context, now);
                case COMBAT -> tickCombat(npc, living, context, target, now);
                case RANGED -> tickRanged(npc, living, context, target, now);
                case DEFENSE -> enterRoamState(npc, living, context); // placeholder for future defence state
            }
        }

        contexts.keySet().retainAll(active);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        contexts.clear();
    }

    public void reset() {
        contexts.clear();
    }

    public void handleDamage(LivingEntity samurai, Entity rawDamager, double damage) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(samurai);
        if (npc == null || !populationManager.isSamuraiNpc(npc)) {
            return;
        }
        SamuraiContext context = contexts.computeIfAbsent(npc.getUniqueId(), ignored -> new SamuraiContext());
        LivingEntity attacker = resolveAttacker(rawDamager);
        if (attacker == null || attacker.equals(samurai)) {
            return;
        }
        
        // Always log the damage to track the most recent attacker
        context.logDamage(attacker, damage);
        
        // Always switch to the most recent attacker
        setTarget(context, attacker);
        if (context.state != SamuraiState.COMBAT && context.state != SamuraiState.RANGED) {
            enterCombatState(npc, samurai, context, attacker);
        }
    }

    public void handleSamuraiAttack(LivingEntity samurai, LivingEntity victim) {
        if (victim == null) {
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(samurai);
        if (npc == null || !populationManager.isSamuraiNpc(npc)) {
            return;
        }
        SamuraiContext context = contexts.computeIfAbsent(npc.getUniqueId(), ignored -> new SamuraiContext());
        setTarget(context, victim);
        ensureMonsterTargetsSamurai(samurai, victim);
        if (context.state != SamuraiState.COMBAT && context.state != SamuraiState.RANGED) {
            enterCombatState(npc, samurai, context, victim);
        }
    }

    private boolean isNightTime(World world) {
        long time = world.getTime();
        return time > 13000 && time < 23000; // Night time is between 13000 and 23000 ticks
    }

    private long getPauseDuration(World world) {
        return isNightTime(world) ? 1000L : 5000L; // 1 second at night, 5 seconds during day
    }

    private void tickRoam(NPC npc, LivingEntity living, SamuraiContext context, long now) {
        RoamMemory memory = context.roamMemory;
        World world = living.getWorld();
        long pauseDuration = getPauseDuration(world);

        if (memory.pauseUntilMs > now) {
            if (npc.getNavigator().isNavigating()) {
                npc.getNavigator().cancelNavigation();
            }
            scanForMonsters(npc, living, context, false);
            return;
        }

        if (memory.target != null) {
            double distSq = living.getLocation().distanceSquared(memory.target);
            boolean timeout = (memory.assignedAtMs + ROAM_TARGET_TIMEOUT_MS) < now;
            boolean navigating = npc.getNavigator().isNavigating();
            boolean arrived = distSq <= ARRIVAL_DISTANCE_SQ;
            if (!navigating || arrived || timeout) {
                npc.getNavigator().cancelNavigation();
                double travelledSq = memory.startLocation != null
                        && memory.startLocation.getWorld() != null
                        && memory.startLocation.getWorld().equals(living.getWorld())
                        ? memory.startLocation.distanceSquared(living.getLocation())
                        : Double.POSITIVE_INFINITY;
                boolean movedEnough = travelledSq >= MIN_TRAVEL_DISTANCE_SQ;
                if (arrived || timeout || movedEnough) {
                    memory.pauseUntilMs = now + pauseDuration;
                } else {
                    memory.pauseUntilMs = 0L;
                }
                memory.target = null;
                memory.assignedAtMs = 0L;
                memory.startLocation = null;
            }
        } else {
            Location next = findRoamLocation(living.getLocation());
            if (next == null) {
                memory.pauseUntilMs = now + pauseDuration;
            } else {
                next.getWorld().getChunkAt(next).load();
                memory.target = next;
                memory.assignedAtMs = now;
                memory.startLocation = living.getLocation().clone();
                npc.getNavigator().setTarget(next);
            }
        }

        scanForMonsters(npc, living, context, npc.getNavigator().isNavigating());
    }

    private void findAndSetNewTarget(NPC npc, LivingEntity living, SamuraiContext context) {
        // 1. Check damage log for the most recent attacker
        LivingEntity newTarget = context.getLatestAttacker();
        
        // 2. If no recent attackers, find nearest monster
        if (newTarget == null || newTarget.isDead()) {
            newTarget = findNearestMonster(living.getLocation(), 20.0);
        }
        
        // 3. If found new target, engage. Otherwise, roam.
        if (newTarget != null && !newTarget.isDead()) {
            setTarget(context, newTarget);
            enterCombatState(npc, living, context, newTarget);
        } else {
            context.clearDamageLog();
            enterRoamState(npc, living, context);
        }
    }
    
    private LivingEntity findNearestMonster(Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return null;
        
        return world.getNearbyEntities(location, radius, radius, radius).stream()
            .filter(e -> e instanceof Monster && e.isValid() && !e.isDead())
            .map(e -> (LivingEntity) e)
            .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(location)))
            .orElse(null);
    }
    
    private void tickCombat(NPC npc, LivingEntity living, SamuraiContext context, LivingEntity target, long now) {
        if (target == null || target.isDead()) {
            findAndSetNewTarget(npc, living, context);
            return;
        }

        ensureCombatEquipment(living);
        applyCombatSpeed(living);

        boolean deflecting = context.isDeflecting(now);
        npc.getNavigator().getLocalParameters().speedModifier((float) (deflecting ? DEFLECTION_SPEED_MODIFIER : 1.0));

        CombatMemory memory = context.combatMemory;
        if (memory.stallOrigin == null) {
            memory.reset(now, living.getLocation());
        }

        double distanceSq = living.getLocation().distanceSquared(target.getLocation());
        boolean inMeleeRange = distanceSq <= MELEE_RANGE_SQ;
        boolean hasSight = living.hasLineOfSight(target);

        if (inMeleeRange) {
            npc.getNavigator().cancelNavigation();
            haltForwardMotion(living);
        } else {
            npc.getNavigator().setTarget(target, false);
        }

        npc.faceLocation(target.getLocation());
        ensureMonsterTargetsSamurai(living, target);

        if (inMeleeRange && hasSight) {
            boolean ready = memory.readyForAttack(now);
            if (ready) {
                living.swingMainHand();
                target.damage(7.0, living);
                memory.markAttack(now);
                if (target instanceof Monster) {
                    stepBackFromTarget(living, target);
                }
            }
        }

        if (now - memory.stallWindowStartMs >= STALL_WINDOW_MS) {
            double movedSq = memory.stallOrigin.distanceSquared(living.getLocation());
            if (movedSq < STALL_DISTANCE_REQUIRED_SQ && distanceSq > RANGED_REENTRY_DISTANCE_SQ) {
                enterRangedState(npc, living, context, target, now);
                return;
            }
            memory.stallOrigin = living.getLocation().clone();
            memory.stallWindowStartMs = now;
        }
    }

    private void tickRanged(NPC npc, LivingEntity living, SamuraiContext context, LivingEntity target, long now) {
        if (target == null) {
            clearTarget(context);
            enterRoamState(npc, living, context);
            return;
        }

        ensureRangedEquipment(living);
        clearCombatSpeed(living);
        npc.getNavigator().cancelNavigation();
        npc.faceLocation(target.getLocation());
        ensureMonsterTargetsSamurai(living, target);

        double distanceSq = living.getLocation().distanceSquared(target.getLocation());
        if (distanceSq <= RANGED_REENTRY_DISTANCE_SQ) {
            enterCombatState(npc, living, context, target);
            return;
        }

        RangedMemory memory = context.rangedMemory;
        if (now - memory.lastShotMs >= RANGED_SHOT_INTERVAL_MS) {
            fireArrow(living, target);
            memory.lastShotMs = now;
        }
    }

    private void scanForMonsters(NPC npc, LivingEntity living, SamuraiContext context, boolean moving) {
        World world = living.getWorld();
        if (world == null) {
            return;
        }
        Location origin = living.getLocation();
        Vector facing = origin.getDirection();
        facing.setY(0.0);
        if (facing.lengthSquared() < 1.0e-4) {
            facing = new Vector(0.0, 0.0, 1.0);
        } else {
            facing.normalize();
        }

        double rangeSq = moving ? MOVING_SIGHT_RANGE_SQ : LOOK_RANGE_SQ;
        double range = Math.sqrt(rangeSq);
        for (Entity entity : world.getNearbyEntities(origin, range, 6.0, range)) {
            if (!(entity instanceof Monster monster)) {
                continue;
            }
            if (!monster.isValid() || monster.isDead()) {
                continue;
            }
            Location targetLoc = monster.getLocation();
            double distSq = origin.distanceSquared(targetLoc);
            if (distSq > rangeSq) {
                continue;
            }
            Vector toTarget = targetLoc.toVector().subtract(origin.toVector());
            toTarget.setY(0.0);
            if (toTarget.lengthSquared() < 1.0e-4) {
                continue;
            }
            toTarget.normalize();
            if (facing.dot(toTarget) < LOOK_FOV_THRESHOLD) {
                continue;
            }
            if (!living.hasLineOfSight(monster)) {
                continue;
            }
            setTarget(context, monster);
            enterCombatState(npc, living, context, monster);
            return;
        }
    }

    private void enterRoamState(NPC npc, LivingEntity living, SamuraiContext context) {
        clearTarget(context);
        clearCombatSpeed(living);
        equip(living, Material.LANTERN);
        npc.getNavigator().cancelNavigation();
        npc.getNavigator().getLocalParameters().speedModifier((float) 1.0);
        context.state = SamuraiState.ROAM;
        context.roamMemory.reset();
        context.clearDeflection();
        context.clearDamageLog();
    }

    private void enterCombatState(NPC npc, LivingEntity living, SamuraiContext context, LivingEntity target) {
        ensureCombatEquipment(living);
        applyCombatSpeed(living);
        context.state = SamuraiState.COMBAT;
        context.combatMemory.reset(System.currentTimeMillis(), living.getLocation());
        context.rangedMemory.reset();
        if (target != null) {
            setTarget(context, target);
            npc.getNavigator().setTarget(target, false);
            ensureMonsterTargetsSamurai(living, target);
        }
    }

    private void enterRangedState(NPC npc, LivingEntity living, SamuraiContext context, LivingEntity target, long now) {
        ensureRangedEquipment(living);
        clearCombatSpeed(living);
        context.state = SamuraiState.RANGED;
        context.rangedMemory.reset(now);
        npc.getNavigator().cancelNavigation();
        npc.getNavigator().getLocalParameters().speedModifier((float) 1.0);
        if (target != null) {
            npc.faceLocation(target.getLocation());
        }
    }

    private void fireArrow(LivingEntity living, LivingEntity target) {
        Location eye = living.getEyeLocation();
        Location targetEye = target.getEyeLocation();
        Vector direction = targetEye.toVector().subtract(eye.toVector());
        if (direction.lengthSquared() < 1.0e-4) {
            direction = target.getLocation().toVector().subtract(living.getLocation().toVector());
            direction.setY(0.2);
        }
        if (direction.lengthSquared() < 1.0e-4) {
            return;
        }
        direction.normalize().multiply(1.6);
        Arrow arrow = living.launchProjectile(Arrow.class, direction);
        arrow.setDamage(10.0);
        arrow.setCritical(true);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    private void stepBackFromTarget(LivingEntity samurai, LivingEntity target) {
        if (samurai == null || target == null) {
            return;
        }
        Vector away = samurai.getLocation().toVector().subtract(target.getLocation().toVector());
        away.setY(0.0);
        if (away.lengthSquared() < 1.0e-4) {
            return;
        }
        away.normalize().multiply(0.9);
        Vector velocity = samurai.getVelocity().clone();
        velocity.add(away);
        velocity.setY(samurai.getVelocity().getY());
        samurai.setVelocity(velocity);
    }

    private void haltForwardMotion(LivingEntity living) {
        Vector velocity = living.getVelocity();
        if (Math.abs(velocity.getX()) < 1.0e-3 && Math.abs(velocity.getZ()) < 1.0e-3) {
            return;
        }
        Vector halted = new Vector(0.0, velocity.getY(), 0.0);
        living.setVelocity(halted);
    }

    private void ensureMonsterTargetsSamurai(LivingEntity samurai, LivingEntity target) {
        if (!(target instanceof Monster monster) || target.getType() == EntityType.CREEPER) {
            return;
        }
        LivingEntity current = monster.getTarget();
        if (current == null || !current.isValid() || current.isDead()) {
            monster.setTarget(samurai);
        }
    }

    public boolean handleProjectileDamage(LivingEntity samurai, Projectile projectile) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(samurai);
        if (npc == null || !populationManager.isSamuraiNpc(npc)) {
            return false;
        }
        SamuraiContext context = contexts.computeIfAbsent(npc.getUniqueId(), ignored -> new SamuraiContext());
        long now = System.currentTimeMillis();
        
        // Only activate deflection after the first projectile has been processed
        boolean isFirstProjectile = !context.deflectionActive;
        context.activateDeflection(now);
        
        LivingEntity attacker = resolveAttacker(projectile);
        if (attacker != null && !attacker.equals(samurai)) {
            context.logDamage(attacker, 0.0); // track the shooter
            setTarget(context, attacker);
        }
        
        // Only deflect if it's not the first projectile and it's coming from the front
        return !isFirstProjectile && 
               projectile instanceof AbstractArrow && 
               isProjectileFromFront(samurai, projectile);
    }

    private boolean isProjectileFromFront(LivingEntity samurai, Projectile projectile) {
        Vector facing = samurai.getLocation().getDirection();
        facing.setY(0.0);
        if (facing.lengthSquared() < 1.0e-4) {
            facing = new Vector(0.0, 0.0, 1.0);
        } else {
            facing.normalize();
        }
        Vector velocity = projectile.getVelocity().clone();
        velocity.setY(0.0);
        if (velocity.lengthSquared() < 1.0e-4) {
            velocity = samurai.getLocation().toVector().subtract(projectile.getLocation().toVector());
            velocity.setY(0.0);
        }
        if (velocity.lengthSquared() < 1.0e-4) {
            return false;
        }
        velocity.normalize();
        double dot = facing.dot(velocity);
        return dot <= 0.2; // front or side
    }

    private void ensureCombatEquipment(LivingEntity living) {
        equip(living, Material.IRON_SWORD);
    }

    private void ensureRangedEquipment(LivingEntity living) {
        equip(living, Material.BOW);
    }

    private void equip(LivingEntity living, Material material) {
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return;
        }
        ItemStack item = material == null ? null : new ItemStack(material);
        equipment.setItemInMainHand(item);
        equipment.setItemInOffHand(null);
    }

    private void applyCombatSpeed(LivingEntity living) {
        PotionEffect existing = living.getPotionEffect(PotionEffectType.SPEED);
        if (existing == null || existing.getAmplifier() < COMBAT_SPEED.getAmplifier()) {
            living.addPotionEffect(COMBAT_SPEED);
        }
    }

    private void clearCombatSpeed(LivingEntity living) {
        living.removePotionEffect(PotionEffectType.SPEED);
    }

    private Location findRoamLocation(Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < ROAM_ATTEMPTS; i++) {
            double offsetX = rng.nextDouble(-ROAM_RANGE, ROAM_RANGE);
            double offsetZ = rng.nextDouble(-ROAM_RANGE, ROAM_RANGE);
            double x = origin.getX() + offsetX;
            double z = origin.getZ() + offsetZ;
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            if (ConsegrityRegions.regionAt(world, blockX, blockZ) != ConsegrityRegions.Region.CHERRY) {
                continue;
            }
            int surfaceY = world.getHighestBlockYAt(blockX, blockZ);
            if (surfaceY <= world.getMinHeight()) {
                continue;
            }
            Block surface = world.getBlockAt(blockX, surfaceY, blockZ);
            if (!surface.getType().isSolid() || surface.isLiquid()) {
                continue;
            }
            Block above = world.getBlockAt(blockX, surfaceY + 1, blockZ);
            if (!above.getType().isAir()) {
                continue;
            }
            return new Location(world, blockX + 0.5, surfaceY + 1, blockZ + 0.5);
        }
        return null;
    }

    private LivingEntity resolveTarget(SamuraiContext context) {
        if (context == null) {
            return null;
        }
        WeakReference<LivingEntity> ref = context.targetRef;
        LivingEntity viaReference = ref != null ? ref.get() : null;
        if (viaReference != null && viaReference.isValid() && !viaReference.isDead()) {
            return viaReference;
        }
        if (context.targetId == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(context.targetId);
        if (entity instanceof LivingEntity living && living.isValid() && !living.isDead()) {
            context.targetRef = new WeakReference<>(living);
            return living;
        }
        context.targetRef = null;
        return null;
    }

    private boolean ensureTargetValid(NPC npc, SamuraiContext context, LivingEntity target) {
        if (context.targetId == null) {
            return true;
        }
        if (target == null) {
            clearTarget(context);
            return false;
        }
        if (!target.isValid() || target.isDead()) {
            clearTarget(context);
            return false;
        }
        Location location = target.getLocation();
        if (location == null || location.getWorld() == null) {
            clearTarget(context);
            return false;
        }
        ConsegrityRegions.Region region = ConsegrityRegions.regionAt(location.getWorld(), location.getBlockX(), location.getBlockZ());
        if (region != ConsegrityRegions.Region.CHERRY) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            npc.destroy();
            clearTarget(context);
            contexts.remove(npc.getUniqueId());
            return false;
        }
        return true;
    }

    private void setTarget(SamuraiContext context, LivingEntity target) {
        if (target == null || !target.isValid() || target.isDead()) {
            clearTarget(context);
            return;
        }
        UUID newId = target.getUniqueId();
        if (!newId.equals(context.targetId)) {
            context.combatMemory.instantAttack = true;
        }
        context.targetId = newId;
        context.targetRef = new WeakReference<>(target);
    }

    private void clearTarget(SamuraiContext context) {
        context.targetId = null;
        context.targetRef = null;
        context.combatMemory.instantAttack = true;
    }

    private LivingEntity resolveAttacker(Entity rawDamager) {
        if (rawDamager instanceof LivingEntity living) {
            return living;
        }
        if (rawDamager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter) {
                return livingShooter;
            }
        }
        return null;
    }

    private enum SamuraiState {
        ROAM,
        COMBAT,
        DEFENSE,
        RANGED
    }

    private static final class DamageRecord {
        final LivingEntity attacker;
        long timestamp;
        double damage;

        DamageRecord(LivingEntity attacker, double damage) {
            this.attacker = attacker;
            this.damage = damage;
            this.timestamp = System.currentTimeMillis();
        }
        
        void update(double newDamage) {
            this.damage = newDamage;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static final class SamuraiContext {
        SamuraiState state;
        final RoamMemory roamMemory = new RoamMemory();
        final CombatMemory combatMemory = new CombatMemory();
        final RangedMemory rangedMemory = new RangedMemory();
        final Map<UUID, DamageRecord> damageLog = new HashMap<>();
        UUID targetId;
        WeakReference<LivingEntity> targetRef;
        boolean deflectionActive;
        long deflectionUntilMs;

        void logDamage(LivingEntity attacker, double damage) {
            if (attacker != null && attacker.getUniqueId() != null) {
                damageLog.compute(attacker.getUniqueId(), 
                    (k, v) -> {
                        if (v == null) {
                            return new DamageRecord(attacker, damage);
                        } else {
                            v.update(damage);
                            return v;
                        }
                    });
            }
        }

        void clearDamageLog() {
            damageLog.clear();
        }

        LivingEntity getLatestAttacker() {
            return damageLog.values().stream()
                .filter(record -> record.attacker != null && record.attacker.isValid() && !record.attacker.isDead())
                .max(Comparator.comparingLong(record -> record.timestamp))
                .map(record -> record.attacker)
                .orElse(null);
        }

        void activateDeflection(long now) {
            deflectionActive = true;
            long desired = now + DEFLECTION_DURATION_MS;
            if (desired > deflectionUntilMs) {
                deflectionUntilMs = desired;
            }
        }

        void refreshDeflection(long now) {
            if (deflectionActive && now > deflectionUntilMs) {
                deflectionActive = false;
                deflectionUntilMs = 0L;
            }
        }

        boolean isDeflecting(long now) {
            refreshDeflection(now);
            return deflectionActive;
        }

        void clearDeflection() {
            deflectionActive = false;
            deflectionUntilMs = 0L;
        }
    }

    private static final class RoamMemory {
        Location target;
        long assignedAtMs;
        long pauseUntilMs;
        Location startLocation;

        void reset() {
            target = null;
            assignedAtMs = 0L;
            pauseUntilMs = 0L;
            startLocation = null;
        }
    }

    private static final class CombatMemory {
        long lastAttackMs;
        long stallWindowStartMs;
        Location stallOrigin;
        boolean instantAttack;

        void reset(long now, Location origin) {
            lastAttackMs = now;
            stallWindowStartMs = now;
            stallOrigin = origin == null ? null : origin.clone();
            instantAttack = true;
        }

        boolean readyForAttack(long now) {
            return instantAttack || now - lastAttackMs >= MELEE_COOLDOWN_MS;
        }

        void markAttack(long now) {
            lastAttackMs = now;
            instantAttack = false;
        }
    }

    private static final class RangedMemory {
        long lastShotMs;

        void reset(long now) {
            lastShotMs = now;
        }

        void reset() {
            lastShotMs = 0L;
        }
    }
}
