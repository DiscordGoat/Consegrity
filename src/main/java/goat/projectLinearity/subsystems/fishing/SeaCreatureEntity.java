package goat.projectLinearity.subsystems.fishing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.MovementSpeedUtil;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime controller for a Citizens-backed sea creature NPC.
 */
final class SeaCreatureEntity {

    private static final double MELEE_RANGE_SQ = 4.0; // 2 blocks squared
    private static final long TICK_INTERVAL = 20L;

    private final ProjectLinearity plugin;
    private final SeaCreatureRegistry registry;
    private final SeaCreatureRegistry.SeaCreatureDefinition definition;
    private final NPC npc;
    private final UUID npcId;
    private final Location spawnLocation;
    private final LivingEntity living;
    private BukkitTask behaviourTask;
    private Player currentTarget;
    private boolean destroyed;

    private SeaCreatureEntity(
            ProjectLinearity plugin,
            SeaCreatureRegistry registry,
            SeaCreatureRegistry.SeaCreatureDefinition definition,
            NPC npc,
            LivingEntity living,
            Location spawnLocation,
            Player initialTarget
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.definition = definition;
        this.npc = npc;
        this.npcId = npc.getUniqueId();
        this.spawnLocation = spawnLocation.clone();
        this.living = living;
        this.currentTarget = initialTarget;
    }

    static Optional<SeaCreatureEntity> spawn(
            ProjectLinearity plugin,
            SeaCreatureRegistry registry,
            NPCRegistry npcRegistry,
            SeaCreatureRegistry.SeaCreatureDefinition definition,
            Location location,
            Player caster
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(npcRegistry, "npcRegistry");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(location, "location");

        World world = location.getWorld();
        if (world == null) {
            plugin.getLogger().warning("Cannot spawn sea creature without a valid world.");
            return Optional.empty();
        }
        try {
            world.getChunkAt(location).load();
        } catch (Throwable ignored) {
        }

        String name = ChatColor.DARK_AQUA + definition.displayName();
        NPC npc = npcRegistry.createNPC(EntityType.PLAYER, name);
        npc.setProtected(false);
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, true);
        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(
                definition.id().toLowerCase(Locale.ROOT),
                definition.skinSignature(),
                definition.skinValue()
        );

        boolean spawned = npc.spawn(location);
        if (!spawned) {
            npc.destroy();
            plugin.getLogger().warning("Failed to spawn sea creature NPC '" + definition.id() + "'.");
            return Optional.empty();
        }

        Entity backing = npc.getEntity();
        if (!(backing instanceof LivingEntity living)) {
            npc.destroy();
            plugin.getLogger().warning("Sea creature NPC '" + definition.id() + "' is not a LivingEntity.");
            return Optional.empty();
        }
        living.setRemoveWhenFarAway(false);
        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false, false));
        double desiredHealth = definition.maxHealth();
        AttributeInstance maxHealthAttr = living.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(desiredHealth);
        }
        double appliedMax = maxHealthAttr != null ? maxHealthAttr.getValue() : living.getMaxHealth();
        living.setHealth(Math.min(desiredHealth, appliedMax));

        SeaCreatureEntity entity = new SeaCreatureEntity(plugin, registry, definition, npc, living, living.getLocation(), caster);
        entity.configureNavigator();
        entity.startBehaviour();
        return Optional.of(entity);
    }

    UUID getNpcId() {
        return npcId;
    }

    Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    LivingEntity getLivingEntity() {
        return living;
    }

    SeaCreatureRegistry.SeaCreatureDefinition getDefinition() {
        return definition;
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (behaviourTask != null) {
            behaviourTask.cancel();
            behaviourTask = null;
        }
        registry.unregisterActive(npcId);
        try {
            if (npc.isSpawned()) {
                npc.despawn();
            }
        } catch (Throwable ignored) {
        }
        try {
            npc.destroy();
        } catch (Throwable ignored) {
        }
    }

    private void configureNavigator() {
        NavigatorParameters params = npc.getNavigator().getLocalParameters();
        params.baseSpeed(1.1F);
        params.avoidWater(false);
        params.useNewPathfinder(true);
    }

    private void startBehaviour() {
        behaviourTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    private void tick() {
        if (destroyed || !npc.isSpawned()) {
            destroy();
            return;
        }
        Entity backing = npc.getEntity();
        if (!(backing instanceof LivingEntity living) || !living.isValid()) {
            destroy();
            return;
        }
        Player target = currentTarget;
        if (!isValidTarget(target, living.getWorld())) {
            target = findNearestTarget(living.getLocation());
            updateTarget(target);
        }
        boolean hovering = handleWaterHover(living);
        if (target == null) {
            npc.getNavigator().cancelNavigation();
            return;
        }

        ensureChasing(target);
        faceTarget(living, target);
        if (hovering) {
            applyForwardMomentum(living, target);
        }
        if (!target.getWorld().equals(living.getWorld())) {
            return;
        }
        double distanceSq = target.getLocation().distanceSquared(living.getLocation());
        if (distanceSq <= MELEE_RANGE_SQ) {
            living.swingMainHand();
            target.damage(definition.baseDamage(), living);
        }
    }


    private void ensureChasing(Player target) {
        if (target == null) {
            return;
        }
        LivingEntity living = npc.getEntity() instanceof LivingEntity le ? le : null;
        float modifier = living != null ? MovementSpeedUtil.applySlowness(living, 1.0f) : 1.0f;
        try {
            npc.getNavigator().getLocalParameters().speedModifier(modifier);
            npc.getNavigator().setTarget(target, true);
        } catch (Throwable ignored) {
        }
    }

    private void updateTarget(Player target) {
        if (Objects.equals(this.currentTarget, target)) {
            return;
        }
        this.currentTarget = target;
        if (target == null) {
            try {
                npc.getNavigator().cancelNavigation();
            } catch (Throwable ignored) {
            }
            return;
        }
        ensureChasing(target);
    }

    void assignTarget(Player player) {
        updateTarget(player);
    }

    void launchTowards(Player player, Location origin) {
        if (player == null || origin == null) {
            return;
        }
        if (!npc.isSpawned()) {
            return;
        }
        Vector direction = player.getLocation().toVector().subtract(origin.toVector());
        if (direction.lengthSquared() < 1.0e-6) {
            direction = player.getLocation().getDirection();
        }
        if (direction.lengthSquared() < 1.0e-6) {
            return;
        }
        direction.normalize().setY(0.0);
        Vector current = living.getVelocity();
        Vector newVelocity = direction.multiply(1.2);
        if (current != null) {
            newVelocity.add(current.multiply(0.06));
        }
        double multiplier = MovementSpeedUtil.getSlownessMultiplier(living);
        if (multiplier <= 0.0) {
            living.setVelocity(new Vector(0.0, current != null ? current.getY() : 0.0, 0.0));
            return;
        }
        newVelocity.multiply(multiplier);
        try {
            living.setVelocity(newVelocity);
        } catch (Throwable ignored) {
        }
    }

    private boolean handleWaterHover(LivingEntity living) {
        Location location = living.getLocation();
        Block feet = location.clone().subtract(0.0, 0.1, 0.0).getBlock();
        boolean inLiquid = isLiquid(feet) || isLiquid(feet.getRelative(0, -1, 0));
        if (!inLiquid) {
            living.setGravity(true);
            return false;
        }

        living.setGravity(false);
        Block surface = feet;
        int iterations = 6;
        while (iterations-- > 0 && isLiquid(surface.getRelative(0, 1, 0))) {
            surface = surface.getRelative(0, 1, 0);
        }
        double targetY = surface.getY() + 1.0;
        double currentY = location.getY();
        if (currentY < targetY - 0.05 || currentY > targetY + 0.15) {
            Location adjusted = location.clone();
            adjusted.setY(targetY);
            living.teleport(adjusted);
        }
        Vector velocity = living.getVelocity();
        if (velocity.getY() < -0.01 || velocity.getY() > 0.1) {
            living.setVelocity(new Vector(velocity.getX(), 0.0, velocity.getZ()));
        }
        living.setFallDistance(0.0f);
        return true;
    }

    private void applyForwardMomentum(LivingEntity living, Player target) {
        if (target == null) {
            return;
        }
        Vector offset = target.getLocation().toVector().subtract(living.getLocation().toVector());
        offset.setY(0.0);
        double lengthSq = offset.lengthSquared();
        if (lengthSq < 1.0e-4) {
            return;
        }
        double distance = Math.sqrt(lengthSq);
        double speed = Math.min(0.9, 0.25 + distance * 0.12);
        double multiplier = MovementSpeedUtil.getSlownessMultiplier(living);
        Vector current = living.getVelocity();
        if (multiplier <= 0.0) {
            Vector velocity = current != null ? current : new Vector(0.0, 0.0, 0.0);
            living.setVelocity(new Vector(0.0, velocity.getY(), 0.0));
            living.setFallDistance(0.0f);
            return;
        }
        speed *= multiplier;
        Vector direction = offset.normalize().multiply(speed);
        if (current != null) {
            direction.add(current.multiply(0.02 * multiplier));
        }
        direction.setY(0.0);
        try {
            living.setVelocity(direction);
        } catch (Throwable ignored) {
        }
        living.setFallDistance(0.0f);
    }

    private void faceTarget(LivingEntity living, Player target) {
        if (target == null || living == null) {
            return;
        }
        if (!target.getWorld().equals(living.getWorld())) {
            return;
        }
        Location livingLoc = living.getLocation();
        Location targetLoc = target.getLocation();
        double dx = targetLoc.getX() - livingLoc.getX();
        double dz = targetLoc.getZ() - livingLoc.getZ();
        if (Math.abs(dx) < 1.0e-4 && Math.abs(dz) < 1.0e-4) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        living.setRotation(yaw, livingLoc.getPitch());
    }

    private boolean isLiquid(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        return type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }

    private boolean isValidTarget(Player player, World world) {
        return player != null
                && player.isOnline()
                && !player.isDead()
                && player.getWorld().equals(world);
    }

    private Player findNearestTarget(Location origin) {
        if (origin.getWorld() == null) {
            return null;
        }
        double bestDistance = Double.MAX_VALUE;
        Player bestPlayer = null;
        for (Player candidate : origin.getWorld().getPlayers()) {
            if (!candidate.isOnline() || candidate.isDead()) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = candidate;
            }
        }
        return bestPlayer;
    }
}
