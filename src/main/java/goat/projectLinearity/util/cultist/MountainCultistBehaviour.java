package goat.projectLinearity.util.cultist;

import goat.projectLinearity.world.ConsegrityRegions;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles Mountain Cultist roaming, alert detection, and chase behaviour.
 */
public final class MountainCultistBehaviour implements Runnable {

    private static final long TICK_INTERVAL = 20L;
    private static final double EYE_HEIGHT = 1.62;
    private static final double MAX_TRACK_DISTANCE_SQ = 40.0 * 40.0;
    private static final double ROAM_RANGE = 18.0;
    private static final long ROAM_RETRY_MS = 15_000L;
    private static final double LOOK_FOV_THRESHOLD = Math.cos(Math.toRadians(35.0));
    private static final double LOOK_RANGE = 20.0;
    private static final double LOOK_RANGE_SQ = LOOK_RANGE * LOOK_RANGE;
    private static final double SNEAK_LOOK_RANGE = 10.0;
    private static final double SNEAK_LOOK_RANGE_SQ = SNEAK_LOOK_RANGE * SNEAK_LOOK_RANGE;
    private static final double INVISIBLE_PROXIMITY_RANGE = 2.0;
    private static final double INVISIBLE_PROXIMITY_RANGE_SQ = INVISIBLE_PROXIMITY_RANGE * INVISIBLE_PROXIMITY_RANGE;
    private static final long LOS_CHECK_INTERVAL_MS = 5_000L;
    private static final long SIREN_INTERVAL_MS = 5_000L;
    private static final long MEMORY_DURATION_MS = 10_000L;
    private static final int PATIENCE_START = 100;
    private static final double CLOSE_SIGHT_RANGE = 5.0;
    private static final double CLOSE_SIGHT_RANGE_SQ = CLOSE_SIGHT_RANGE * CLOSE_SIGHT_RANGE;
    private static final double MOVING_SIGHT_RANGE = 6.0;
    private static final double MOVING_SIGHT_RANGE_SQ = MOVING_SIGHT_RANGE * MOVING_SIGHT_RANGE;
    private static final double CHASE_DAMAGE_RANGE = 3.0;
    private static final double CHASE_DAMAGE_RANGE_SQ = CHASE_DAMAGE_RANGE * CHASE_DAMAGE_RANGE;
    private static final long CHASE_DAMAGE_INTERVAL_MS = 1_000L;
    private static final double CHASE_DAMAGE = 2.0;
    private static final double CHASE_MOMENTUM = 0.045;
    private static final double CHASE_MAX_HORIZONTAL_SPEED = 0.35;
    private static final double STALL_DISTANCE = 3.0;
    private static final double STALL_DISTANCE_SQ = STALL_DISTANCE * STALL_DISTANCE;
    private static final long STALL_CHECK_INTERVAL_MS = 1_000L;
    private static final double TELEPORT_BEHIND_DISTANCE = 4.0;

    private final JavaPlugin plugin;
    private final CultistPopulationManager populationManager;
    private final BukkitTask task;
    private final Map<UUID, RoamState> roamStates = new HashMap<>();
    private final Map<UUID, ChaseState> chaseStates = new HashMap<>();
    private final Map<UUID, Set<UUID>> playerToCultists = new HashMap<>();
    private final Map<UUID, Integer> playerPatience = new HashMap<>();
    private long lastPatienceResetDay = Long.MIN_VALUE;

    public MountainCultistBehaviour(JavaPlugin plugin, CultistPopulationManager populationManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, TICK_INTERVAL, TICK_INTERVAL);
    }

    @Override
    public void run() {
        Collection<NPC> cultists = populationManager.getActiveCultists();
        if (cultists.isEmpty()) {
            roamStates.clear();
            chaseStates.clear();
            playerToCultists.clear();
            return;
        }

        Set<UUID> activeIds = new HashSet<>();

        for (NPC npc : cultists) {
            if (!npc.isSpawned()) {
                continue;
            }
            Entity entity = npc.getEntity();
            if (entity == null) {
                continue;
            }
            World world = entity.getWorld();
            if (world == null) {
                continue;
            }
            ensurePatienceReset(world);
            UUID id = npc.getUniqueId();
            activeIds.add(id);

            if (isDaytime(world)) {
                ChaseState chaseState = chaseStates.get(id);
                if (chaseState != null) {
                    endChaseForPlayer(chaseState.playerId, "daytime");
                    continue;
                }
                handleDaytime(npc, entity);
                continue;
            }

            if (isChasing(id)) {
                tickChase(npc, entity, chaseStates.get(id));
                continue;
            }

            handleNightRoam(npc, entity);
            checkVisibleProximity(npc, entity);
            checkInvisibleProximity(npc, entity);
        }

        pruneStates(activeIds);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        roamStates.clear();
        chaseStates.clear();
        playerToCultists.clear();
    }

    public void clearRoamStates() {
        roamStates.clear();
    }

    public void alertCultist(NPC npc, Player player, String reason) {
        if (npc == null || player == null || !npc.isSpawned()) {
            return;
        }
        Entity entity = npc.getEntity();
        if (entity == null || entity.getWorld() == null) {
            return;
        }
        if (isDaytime(entity.getWorld())) {
            return;
        }
        startChase(npc, player, reason);
    }

    public void alertCultistsNear(Player player, Location source, double radius, String reason) {
        if (player == null || source == null) {
            return;
        }
        World world = source.getWorld();
        if (world == null || isDaytime(world)) {
            return;
        }
        double rangeSq = radius * radius;
        for (NPC npc : populationManager.getActiveCultists()) {
            if (!npc.isSpawned()) {
                continue;
            }
            Entity entity = npc.getEntity();
            if (entity == null || !entity.getWorld().equals(world)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(source) > rangeSq) {
                continue;
            }
            startChase(npc, player, reason);
        }
    }

    public boolean isNight(World world) {
        return !isDaytime(world);
    }

    private void pruneStates(Set<UUID> activeIds) {
        Iterator<UUID> roamIt = roamStates.keySet().iterator();
        while (roamIt.hasNext()) {
            if (!activeIds.contains(roamIt.next())) {
                roamIt.remove();
            }
        }
        Iterator<Map.Entry<UUID, ChaseState>> chaseIt = chaseStates.entrySet().iterator();
        while (chaseIt.hasNext()) {
            Map.Entry<UUID, ChaseState> entry = chaseIt.next();
            if (!activeIds.contains(entry.getKey())) {
                removeCultistFromPlayerMap(entry.getValue().playerId, entry.getKey());
                chaseIt.remove();
            }
        }
        playerToCultists.values().removeIf(Set::isEmpty);
    }

    private void handleDaytime(NPC npc, Entity entity) {
        npc.getNavigator().cancelNavigation();
        roamStates.remove(npc.getUniqueId());
        Player target = findNearestPlayer(entity, MAX_TRACK_DISTANCE_SQ);
        if (target != null) {
            npc.faceLocation(target.getLocation());
        }
    }

    private void handleNightRoam(NPC npc, Entity entity) {
        UUID id = npc.getUniqueId();
        RoamState state = roamStates.get(id);
        long now = System.currentTimeMillis();
        boolean needsTarget = state == null || state.target == null || now >= state.nextUpdateMs;
        boolean reachedTarget = false;

        if (!needsTarget && state.target != null) {
            double distSq = entity.getLocation().distanceSquared(state.target);
            if (!npc.getNavigator().isNavigating() || distSq <= 4.0) {
                needsTarget = true;
                reachedTarget = true;
            }
        }

        if (reachedTarget) {
            performLookAround(npc, entity);
        }

        if (!needsTarget) {
            scanWhileMoving(npc, entity);
            return;
        }

        Location target = findRoamLocation(entity.getLocation(), ROAM_RANGE, 20);
        if (target != null) {
            target.getWorld().getChunkAt(target).load();
            npc.getNavigator().setTarget(target);
            roamStates.put(id, new RoamState(target, now + ROAM_RETRY_MS));
        } else {
            roamStates.put(id, new RoamState(null, now + 5_000L));
        }
    }

    private void performLookAround(NPC npc, Entity entity) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float baseYaw = normalizeYaw(entity.getLocation().getYaw());
        float offsetYaw = selectFacingOffset(rng);
        float finalYaw = normalizeYaw(baseYaw + offsetYaw);
        Location lookTarget = entity.getLocation().clone();
        Vector direction = directionFromYaw(finalYaw);
        lookTarget.add(direction.multiply(5.0));
        npc.faceLocation(lookTarget);
        scanForTargets(npc, entity, direction);
    }

    private void scanForTargets(NPC npc, Entity entity, Vector facing) {
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        Location origin = entity.getLocation();
        for (Player player : world.getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            boolean invisible = isInvisible(player);
            if (invisible) {
                continue;
            }
            double distanceSq = origin.distanceSquared(player.getLocation());
            boolean sneaking = player.isSneaking();
            double maxRangeSq = sneaking ? SNEAK_LOOK_RANGE_SQ : LOOK_RANGE_SQ;
            if (distanceSq > maxRangeSq) {
                continue;
            }
            Vector toPlayer = player.getLocation().toVector().subtract(origin.toVector());
            toPlayer.setY(0.0);
            if (toPlayer.lengthSquared() == 0.0) {
                continue;
            }
            toPlayer.normalize();
            if (facing.dot(toPlayer) < LOOK_FOV_THRESHOLD) {
                continue;
            }
            if (!hasLineOfSight(entity, player)) {
                continue;
            }
            npc.faceLocation(player.getLocation());
            startChase(npc, player, "sighted");
            return;
        }
    }

    private void scanWhileMoving(NPC npc, Entity entity) {
        Vector facing = directionFromYaw(normalizeYaw(entity.getLocation().getYaw()));
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        Location origin = entity.getLocation();
        for (Player player : world.getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            if (isInvisible(player)) {
                continue;
            }
            double distanceSq = origin.distanceSquared(player.getLocation());
            if (distanceSq > MOVING_SIGHT_RANGE_SQ) {
                continue;
            }
            Vector toPlayer = player.getLocation().toVector().subtract(origin.toVector());
            toPlayer.setY(0.0);
            if (toPlayer.lengthSquared() == 0.0) {
                continue;
            }
            toPlayer.normalize();
            if (facing.dot(toPlayer) < LOOK_FOV_THRESHOLD) {
                continue;
            }
            if (!hasLineOfSight(entity, player)) {
                continue;
            }
            npc.faceLocation(player.getLocation());
            startChase(npc, player, "moving sight");
            return;
        }
    }

    private void checkInvisibleProximity(NPC npc, Entity entity) {
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        Location origin = entity.getLocation();
        for (Player player : world.getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            if (!isInvisible(player)) {
                continue;
            }
            double distSq = origin.distanceSquared(player.getLocation());
            if (distSq <= INVISIBLE_PROXIMITY_RANGE_SQ) {
                // Only detect if player is actually moving (not just standing still)
                if (isPlayerMoving(player)) {
                    startChase(npc, player, "invisible proximity");
                    return;
                }
            }
        }
    }

    private void checkVisibleProximity(NPC npc, Entity entity) {
        World world = entity.getWorld();
        if (world == null) {
            return;
        }
        Location origin = entity.getLocation();
        for (Player player : world.getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            if (isInvisible(player)) {
                continue;
            }
            double distSq = origin.distanceSquared(player.getLocation());
            if (distSq <= CLOSE_SIGHT_RANGE_SQ) {
                startChase(npc, player, "close proximity");
                return;
            }
        }
    }

    private boolean isValidTarget(Player player) {
        if (player == null || !player.isValid() || player.isDead()) {
            return false;
        }
        return player.getGameMode() == GameMode.SURVIVAL;
    }

    private boolean isInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible();
    }

    private void cleanseInvisibility(Player player, String detectionMethod) {
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.sendMessage(ChatColor.RED + "Your invisibility has been cleansed by cultist detection!");
        }
        if (player.isInvisible()) {
            player.setInvisible(false);
        }
    }

    private boolean isPlayerMoving(Player player) {
        // Check if player has significant movement velocity (not just floating point errors)
        // Using a threshold of 0.05 to detect actual player movement
        return player.getVelocity().lengthSquared() > 0.05;
    }

    private boolean hasLineOfSight(Entity source, Player player) {
        Location eye = source.getLocation().clone().add(0, EYE_HEIGHT, 0);
        Location targetEye = player.getLocation().clone().add(0, player.getEyeHeight(), 0);
        if (eye.getWorld() == null) {
            return false;
        }
        RayTraceResult result = eye.getWorld().rayTraceBlocks(
                eye,
                targetEye.toVector().subtract(eye.toVector()),
                eye.distance(targetEye),
                FluidCollisionMode.NEVER);
        return result == null || result.getHitBlock() == null;
    }

    private void tickChase(NPC npc, Entity entity, ChaseState state) {
        Player player = plugin.getServer().getPlayer(state.playerId);
        if (player == null || !player.isOnline()) {
            endChaseForPlayer(state.playerId, "player missing");
            return;
        }
        if (player.isDead()) {
            endChaseForPlayer(state.playerId, "player dead");
            return;
        }
        if (entity.getWorld() == null || isDaytime(entity.getWorld())) {
            endChaseForPlayer(state.playerId, "daytime");
            return;
        }
        if (ConsegrityRegions.regionAt(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockZ())
                != ConsegrityRegions.Region.MOUNTAIN) {
            endChaseForPlayer(state.playerId, "player left mountain");
            return;
        }

        long now = System.currentTimeMillis();

        if (state.nextHallucinationMs == 0L) {
            state.nextHallucinationMs = now + 30_000L;
        } else if (now >= state.nextHallucinationMs) {
            showHallucination(player, state.patience, false);
            state.nextHallucinationMs = now + 30_000L;
        }

        if (now >= state.nextSirenMs) {
            state.nextSirenMs = now + SIREN_INTERVAL_MS;
            if (state.patience > 0) {
                state.patience--;
                if (state.patience < 0) {
                    state.patience = 0;
                }
                storePatience(state.playerId, state.patience);
            } else {
                storePatience(state.playerId, state.patience);
            }
            Set<UUID> chasers = playerToCultists.get(state.playerId);
            boolean isLead = chasers != null && !chasers.isEmpty() && chasers.iterator().next().equals(npc.getUniqueId());
            if (isLead) {
                float pitch = 0.3f + (state.patience / (float) PATIENCE_START) * 0.7f;
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 100, pitch);
            }
        }

        if (now >= state.nextLosCheckMs) {
            state.nextLosCheckMs = now + LOS_CHECK_INTERVAL_MS;
            if (hasLineOfSight(entity, player)) {
                state.lastSeenMs = now;
            }
        }

        if (now - state.lastSeenMs >= MEMORY_DURATION_MS) {
            endChaseForPlayer(state.playerId, "lost sight");
            return;
        }

        Location playerLocation = player.getLocation();
        World targetWorld = playerLocation.getWorld();
        if (targetWorld != null && !targetWorld.isChunkLoaded(playerLocation.getBlockX() >> 4, playerLocation.getBlockZ() >> 4)) {
            targetWorld.getChunkAt(playerLocation);
        }
        npc.getNavigator().getLocalParameters().speedModifier(computeChaseSpeed(state.patience));
        npc.getNavigator().setTarget(player, true);
        npc.faceLocation(playerLocation);

        Location currentLocation = entity.getLocation();
        if (now - state.lastTeleportCheckMs >= STALL_CHECK_INTERVAL_MS) {
            boolean teleported = false;
            if (state.lastRecordedLocation != null
                    && currentLocation.getWorld() != null
                    && state.lastRecordedLocation.getWorld() != null
                    && currentLocation.getWorld().equals(state.lastRecordedLocation.getWorld())
                    && currentLocation.distanceSquared(state.lastRecordedLocation) <= STALL_DISTANCE_SQ) {
                teleported = teleportBehindPlayer(npc, entity, player);
                if (teleported) {
                    currentLocation = entity.getLocation();
                }
            }
            state.lastRecordedLocation = currentLocation.clone();
            state.lastTeleportCheckMs = now;
            if (teleported) {
                state.lastSeenMs = now;
            }
        }

        if (currentLocation.distanceSquared(playerLocation) <= CHASE_DAMAGE_RANGE_SQ) {
            if (now - state.lastDamageMs >= CHASE_DAMAGE_INTERVAL_MS) {
                state.lastDamageMs = now;
                double newHealth = Math.max(0.0, player.getHealth() - CHASE_DAMAGE);
                player.setHealth(newHealth);
            }
        }

        applyForwardMomentum(entity);
    }

    private void startChase(NPC npc, Player player, String reason) {
        UUID npcId = npc.getUniqueId();
        UUID playerId = player.getUniqueId();

        // Cleanse invisibility when chase starts
        cleanseInvisibility(player, reason);

        ChaseState current = chaseStates.get(npcId);
        if (current != null && current.playerId.equals(playerId)) {
            current.lastSeenMs = System.currentTimeMillis();
            current.lastDamageMs = System.currentTimeMillis();
            current.lastTeleportCheckMs = System.currentTimeMillis();
            current.lastRecordedLocation = npc.getEntity() != null ? npc.getEntity().getLocation().clone() : current.lastRecordedLocation;
            current.nextHallucinationMs = System.currentTimeMillis() + 30_000L;
            current.patience = getPatience(player);
            storePatience(playerId, current.patience);
            return;
        }

        stopChase(npc, "switch target");
        playerPatience.putIfAbsent(playerId, PATIENCE_START);

        ChaseState state = new ChaseState(playerId);
        state.patience = getPatience(player);
        storePatience(playerId, state.patience);
        state.lastSeenMs = System.currentTimeMillis();
        state.nextSirenMs = System.currentTimeMillis();
        state.nextLosCheckMs = System.currentTimeMillis();
        state.lastDamageMs = System.currentTimeMillis();
        state.lastTeleportCheckMs = System.currentTimeMillis();
        state.nextHallucinationMs = System.currentTimeMillis() + 30_000L;
        Entity npcEntity = npc.getEntity();
        state.lastRecordedLocation = npcEntity != null ? npcEntity.getLocation().clone() : null;
        chaseStates.put(npcId, state);
        Set<UUID> currentChasers = playerToCultists.computeIfAbsent(playerId, k -> new LinkedHashSet<>());
        boolean alreadyChasing = !currentChasers.isEmpty();
        currentChasers.add(npcId);

        Location playerLocation = player.getLocation();
        World targetWorld = playerLocation.getWorld();
        if (targetWorld != null && !targetWorld.isChunkLoaded(playerLocation.getBlockX() >> 4, playerLocation.getBlockZ() >> 4)) {
            targetWorld.getChunkAt(playerLocation);
        }
        npc.getNavigator().getLocalParameters().speedModifier(computeChaseSpeed(state.patience));
        npc.getNavigator().setTarget(player, true);
        npc.faceLocation(playerLocation);
        if (!alreadyChasing) {
            showHallucination(player, state.patience, true);
        }
    }

    private void stopChase(NPC npc, String reason) {
        UUID npcId = npc.getUniqueId();
        ChaseState state = chaseStates.remove(npcId);
        if (state != null) {
            removeCultistFromPlayerMap(state.playerId, npcId);
        }
        npc.getNavigator().getLocalParameters().speedModifier(1.0f);
        npc.getNavigator().cancelNavigation();
        roamStates.remove(npcId);
    }

    private void endChaseForPlayer(UUID playerId, String reason) {
        Set<UUID> cultistIds = playerToCultists.remove(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            String message = chaseEndMessage(reason);
            if (message != null) {
                player.sendMessage(message);
            }
        }
        if (cultistIds == null || cultistIds.isEmpty()) {
            return;
        }
        for (UUID npcId : new HashSet<>(cultistIds)) {
            NPC npc = findNpc(npcId);
            if (npc != null) {
                stopChase(npc, reason);
            } else {
                chaseStates.remove(npcId);
            }
        }
    }

    private NPC findNpc(UUID npcId) {
        for (NPC npc : populationManager.getActiveCultists()) {
            if (npc.getUniqueId().equals(npcId)) {
                return npc;
            }
        }
        return null;
    }

    private void removeCultistFromPlayerMap(UUID playerId, UUID npcId) {
        Set<UUID> cultists = playerToCultists.get(playerId);
        if (cultists != null) {
            cultists.remove(npcId);
            if (cultists.isEmpty()) {
                playerToCultists.remove(playerId);
            }
        }
    }

    private String chaseEndMessage(String reason) {
        return switch (reason) {
            case "daytime" -> ChatColor.GREEN + "Chase Ended" + ChatColor.GRAY + " (sunrise)";
            case "lost sight" -> ChatColor.GREEN + "Chase Ended" + ChatColor.GRAY + " (cultists lost sight of you)";
            case "player left mountain" -> ChatColor.GREEN + "Chase Ended" + ChatColor.GRAY + " (you escaped the mountain)";
            case "player dead" -> ChatColor.GREEN + "Chase Ended" + ChatColor.GRAY + " (death)";
            case "player missing" -> null;
            default -> ChatColor.GREEN + "Chase Ended";
        };
    }

    private boolean isChasing(UUID npcId) {
        return chaseStates.containsKey(npcId);
    }

    private boolean isDaytime(World world) {
        long time = world.getTime() % 24000;
        return time < 12300 || time > 23850;
    }

    private Player findNearestPlayer(Entity source, double maxDistanceSq) {
        Player closest = null;
        double closestDistSq = maxDistanceSq;
        for (Player player : source.getWorld().getPlayers()) {
            if (!player.isValid() || player.isDead()) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double distSq = source.getLocation().distanceSquared(player.getLocation());
            if (distSq > closestDistSq) {
                continue;
            }
            if (!hasLineOfSight(source, player)) {
                continue;
            }
            closest = player;
            closestDistSq = distSq;
        }
        return closest;
    }

    private Location findRoamLocation(Location origin, double range, int attempts) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < attempts; i++) {
            double offsetX = rng.nextDouble(-range, range);
            double offsetZ = rng.nextDouble(-range, range);
            double x = origin.getX() + offsetX;
            double z = origin.getZ() + offsetZ;
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            if (ConsegrityRegions.regionAt(world, blockX, blockZ) != ConsegrityRegions.Region.MOUNTAIN) {
                continue;
            }
            int surfaceY = world.getHighestBlockYAt(blockX, blockZ);
            if (surfaceY <= world.getMinHeight()) {
                continue;
            }
            Block surface = world.getBlockAt(blockX, surfaceY, blockZ);
            Block above = world.getBlockAt(blockX, surfaceY + 1, blockZ);
            if (surface.isLiquid()) {
                continue;
            }
            if (!surface.getType().isSolid()) {
                continue;
            }
            if (!above.getType().isAir() && above.getType() != Material.CAVE_AIR) {
                continue;
            }
            return new Location(world, blockX + 0.5, surfaceY + 1, blockZ + 0.5);
        }
        return null;
    }

    private float normalizeYaw(float yaw) {
        yaw %= 360.0f;
        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    private float selectFacingOffset(ThreadLocalRandom rng) {
        double roll = rng.nextDouble();
        if (roll < 0.50) {
            return 0.0f; // straight
        }
        if (roll < 0.70) {
            return -90.0f; // left
        }
        if (roll < 0.90) {
            return 90.0f; // right
        }
        return 180.0f; // behind
    }

    private Vector directionFromYaw(float yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        return new Vector(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
    }

    private static final class RoamState {
        final Location target;
        final long nextUpdateMs;

        RoamState(Location target, long nextUpdateMs) {
            this.target = target == null ? null : target.clone();
            this.nextUpdateMs = nextUpdateMs;
        }
    }

    private static final class ChaseState {
        final UUID playerId;
        int patience = PATIENCE_START;
        long nextSirenMs;
        long nextLosCheckMs;
        long lastSeenMs;
        long lastDamageMs;
        long lastTeleportCheckMs;
        long nextHallucinationMs;
        Location lastRecordedLocation;

        ChaseState(UUID playerId) {
            this.playerId = playerId;
        }
    }

    private void applyForwardMomentum(Entity entity) {
        Vector direction = entity.getLocation().getDirection();
        if (direction.lengthSquared() < 1.0e-4) {
            return;
        }
        direction.setY(0.0);
        if (direction.lengthSquared() < 1.0e-4) {
            return;
        }
        direction.normalize().multiply(CHASE_MOMENTUM);
        Vector current = entity.getVelocity();
        Vector boosted = current.clone().add(direction);
        double horizontal = Math.hypot(boosted.getX(), boosted.getZ());
        if (horizontal > CHASE_MAX_HORIZONTAL_SPEED && horizontal > 0.0) {
            double scale = CHASE_MAX_HORIZONTAL_SPEED / horizontal;
            boosted.setX(boosted.getX() * scale);
            boosted.setZ(boosted.getZ() * scale);
        }
        entity.setVelocity(boosted);
    }

    private void showHallucination(Player player, int patience, boolean extended) {
        if (player == null || !player.isOnline()) {
            return;
        }
        double normalized = Math.max(0.0, Math.min(1.0, patience / (double) PATIENCE_START));
        double intensity = 1.0 - normalized;
        java.util.List<String> frames = new java.util.ArrayList<>();
        frames.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + "I SEE YOU.");
        if (intensity > 0.2) {
            frames.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + "I " + ChatColor.MAGIC + "SEE" + ChatColor.DARK_RED + ChatColor.BOLD + " YOU.");
        }
        if (intensity > 0.5) {
            frames.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + ChatColor.MAGIC + "I SEE YOU");
        }
        if (intensity > 0.8) {
            frames.add(ChatColor.MAGIC + "I SEE YOU.");
        }
        int stay = extended ? 24 : 12;
        int fade = 4;
        for (int i = 0; i < frames.size(); i++) {
            String title = frames.get(i);
            long delay = (long) i * (stay + fade);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendTitle(title, "", 0, stay, fade);
                }
            }, delay);
        }
        long resetDelay = (long) frames.size() * (stay + fade) + 2L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.resetTitle();
            }
        }, resetDelay);
    }

    private void ensurePatienceReset(World world) {
        long day = world.getFullTime() / 24000L;
        if (day != lastPatienceResetDay) {
            playerPatience.clear();
            lastPatienceResetDay = day;
            for (ChaseState state : chaseStates.values()) {
                state.patience = PATIENCE_START;
                storePatience(state.playerId, state.patience);
                state.nextHallucinationMs = System.currentTimeMillis() + 30_000L;
            }
        }
    }

    private boolean teleportBehindPlayer(NPC npc, Entity entity, Player player) {
        if (entity == null) {
            return false;
        }
        Location playerLocation = player.getLocation();
        World world = playerLocation.getWorld();
        if (world == null) {
            return false;
        }

        Vector back = playerLocation.getDirection().clone();
        if (back.lengthSquared() < 1.0e-4) {
            back = new Vector(0.0, 0.0, 1.0);
        }
        back.setY(0.0);
        if (back.lengthSquared() < 1.0e-4) {
            back = new Vector(0.0, 0.0, 1.0);
        }
        back.normalize();

        double[] offsets = {TELEPORT_BEHIND_DISTANCE, TELEPORT_BEHIND_DISTANCE - 1.0, TELEPORT_BEHIND_DISTANCE + 1.0};
        for (double distance : offsets) {
            Location candidate = playerLocation.clone().subtract(back.clone().multiply(distance));
            candidate.setYaw(playerLocation.getYaw());
            candidate.setPitch(playerLocation.getPitch());
            candidate.setY(playerLocation.getY());
            if (!world.isChunkLoaded(candidate.getBlockX() >> 4, candidate.getBlockZ() >> 4)) {
                world.getChunkAt(candidate);
            }
            Location safe = findSafeTeleportLocation(candidate);
            if (safe == null) {
                continue;
            }
            safe.setYaw(playerLocation.getYaw());
            safe.setPitch(playerLocation.getPitch());
            boolean teleported = entity.teleport(safe);
            if (teleported) {
                world.playSound(safe, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
                npc.faceLocation(playerLocation);
                applyForwardMomentum(entity);
                return true;
            }
        }

        return false;
    }

    private Location findSafeTeleportLocation(Location base) {
        World world = base.getWorld();
        if (world == null) {
            return null;
        }
        double centerX = base.getBlockX() + 0.5;
        double centerZ = base.getBlockZ() + 0.5;
        int startY = Math.max(world.getMinHeight() + 2, Math.min(world.getMaxHeight() - 2, base.getBlockY()));

        // Search downward first for a safe spot, then upward if none found immediately.
        for (int y = startY; y >= world.getMinHeight() + 1 && y >= startY - 6; y--) {
            Location candidate = new Location(world, centerX, y, centerZ);
            if (isSafeTeleportSpot(candidate)) {
                return candidate;
            }
        }
        for (int y = startY + 1; y <= world.getMaxHeight() - 2 && y <= startY + 4; y++) {
            Location candidate = new Location(world, centerX, y, centerZ);
            if (isSafeTeleportSpot(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSafeTeleportSpot(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Location below = location.clone().add(0, -1, 0);
        Location head = location.clone().add(0, 1, 0);
        return world.getBlockAt(location).getType().isAir()
                && world.getBlockAt(head).getType().isAir()
                && world.getBlockAt(below).getType().isSolid();
    }

    private int getPatience(Player player) {
        return playerPatience.getOrDefault(player.getUniqueId(), PATIENCE_START);
    }

    private void storePatience(UUID playerId, int patience) {
        playerPatience.put(playerId, Math.max(0, Math.min(PATIENCE_START, patience)));
    }

    private float computeChaseSpeed(int patience) {
        float norm = Math.max(0.0f, Math.min(1.0f, patience / (float) PATIENCE_START));
        return 1.0f + (1.0f - norm);
    }
}
