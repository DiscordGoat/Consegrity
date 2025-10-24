package goat.projectLinearity.subsystems.world.samurai;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.world.ConsegrityRegions;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Handles spawning and tracking of Cherry sector Samurai NPCs.
 */
public final class SamuraiPopulationManager {

    public static final int DEFAULT_POPULATION = 10;
    private static final double AVOID_DISTANCE_SQ = 80.0 * 80.0;
    private static final double MIN_SPAWN_SPREAD = 40.0;
    private static final double MIN_SPAWN_SPREAD_SQ = MIN_SPAWN_SPREAD * MIN_SPAWN_SPREAD;
    private static final String SKIN_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxODcxODA4ODg3OCwKICAicHJvZmlsZUlkIiA6ICJiZDNhNWRmY2ZkZjg0NDczOTViZDJiZmUwNGY0YzAzMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJwcmVja3Jhc25vIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzU2NzY0OTVlYzg1NjIzNmRmZDlkNWU4NGJkNjBhZjc0MDIyOGMyYzAzNmVmYzIxYTg1ZDEyMTljM2Q2NTVhN2IiCiAgICB9CiAgfQp9";
    private static final String SKIN_SIGNATURE = "DLqlcddbZa9x52Yktr6X4WjekZUmcrJYaEvUON44e1hVcZGI+aEvmEtuhDdO8xMmYu2GkJCGuDSA99p28VXZqgY8eyLFQ11gwPXThAcWEFF2I02RPIa6ArBiAFDZz16lxL53DEMjlZ3XZnww1lmriSPJB+u3PybmbCWecTANx1A5Ync+MLsyX+IlXTas0zLQuCHcZfsbVNs8qJtpys1JkwO42HQ7oCqWcCVofPBvQtXzRJzAltv7a4RCwjXcxjwprXIg2TtMctYE6ZyJL+OZg+qU2kkmK7fkdb23J6vGzwcU3v3XSM21VlFEWYqP+Y/TXq9LfpZyK94A6XxXe5b9xeid/pBWxHkC2DgK8q+ikOyUqKGVSW9vkodMNdiQZ2zyH8yzstHAoaiCX4Vadn3hnk+U1OI5uLnK9Qrepm9PZU+cHG+Y7FTGcu0EByYRrYBWmMXIAB5bPCkkHs/e2BhAS7EEy7EQpLqII/TqmvKK3889CUJ985qnEijt8lS6gbKxCc9AVSY+DsUfQ4qbX9UH13BvqNIhRomgcnVPWituwR4XAmrweFGpBFTD0QpoRObCSZOt400LmsyJwNBKyzwBo3OE/V4n3POGq6tS0YCwGcxpPHVTe3tFyde67ES0NscJfOtLSuIe6juQMUbOZVrsJJH5bmjUiLQoPg5Nk0tYPds=";
    private static final String NPC_NAME = ChatColor.DARK_RED + "Samurai";

    private final JavaPlugin plugin;
    private final Logger logger;
    private NPCRegistry registry;
    private final Map<UUID, NPC> samurai = new LinkedHashMap<>();
    private int targetPopulation = DEFAULT_POPULATION;

    public SamuraiPopulationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean startup() {
        if (!ensureRegistry()) {
            logger.warning("Citizens not ready; samurai population will remain empty until retry.");
            return false;
        }
        logger.info("Samurai population manager initialised.");
        return true;
    }

    public void shutdown() {
        despawnAll();
        if (registry != null) {
            registry.deregisterAll();
            registry = null;
        }
        samurai.clear();
    }

    public Collection<NPC> getActiveSamurai() {
        return samurai.values();
    }

    public int getCurrentPopulation() {
        return samurai.size();
    }

    public int getTargetPopulation() {
        return targetPopulation;
    }

    public void setTargetPopulation(int target) {
        targetPopulation = Math.max(0, target);
        logger.info(() -> "Samurai population target set to " + targetPopulation + ".");
    }

    public void respawnPopulation(Player avoidPlayer) {
        if (plugin instanceof ProjectLinearity pl && pl.getCherrySamuraiBehaviour() != null) {
            pl.getCherrySamuraiBehaviour().reset();
        }
        if (!ensureRegistry()) {
            logger.warning("Citizens not available; unable to spawn samurai.");
            return;
        }
        World world = resolveSamuraiWorld();
        if (world == null) {
            logger.warning("Unable to resolve samurai world.");
            return;
        }

        despawnAll();

        Location avoid = avoidPlayer != null ? avoidPlayer.getLocation() : null;
        double minDistanceSq = avoid != null ? AVOID_DISTANCE_SQ : 0.0;
        int spawned = 0;
        java.util.ArrayList<Location> placed = new java.util.ArrayList<>();
        int attempts = 0;
        int maxAttempts = Math.max(targetPopulation * 30, 120);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        while (spawned < targetPopulation && attempts++ < maxAttempts) {
            Location candidate = findRandomCherryLocation(world, rng);
            if (candidate == null) {
                break;
            }
            if (avoid != null && candidate.getWorld().equals(avoid.getWorld())
                    && candidate.distanceSquared(avoid) < minDistanceSq) {
                continue;
            }
            boolean tooClose = false;
            for (Location existing : placed) {
                if (existing.distanceSquared(candidate) < MIN_SPAWN_SPREAD_SQ) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }
            NPC npc = createSamurai(candidate);
            if (npc != null) {
                placed.add(candidate.clone());
                spawned++;
            }
        }

        if (spawned < targetPopulation) {
            logger.warning("Spawned " + spawned + " of " + targetPopulation + " requested samurai.");
        } else {
            logger.info("Spawned " + spawned + " samurai.");
        }
    }

    public Optional<NPC> spawnSamuraiEntity(Location location) {
        if (location == null) {
            logger.warning("Cannot spawn samurai at null location.");
            return Optional.empty();
        }
        if (!ensureRegistry()) {
            logger.warning("Citizens not available; unable to spawn samurai.");
            return Optional.empty();
        }
        NPC npc = createSamurai(location.clone());
        return Optional.ofNullable(npc);
    }

    public boolean isSamuraiNpc(NPC npc) {
        if (npc == null) {
            return false;
        }
        return samurai.containsKey(npc.getUniqueId());
    }

    public boolean isSamuraiEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return isSamuraiNpc(npc);
    }

    private boolean ensureRegistry() {
        if (registry != null) {
            return true;
        }
        if (!CitizensAPI.hasImplementation()) {
            return false;
        }
        registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        return true;
    }

    private NPC createSamurai(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        world.getChunkAt(location).load();

        NPC npc = registry.createNPC(EntityType.PLAYER, NPC_NAME);
        npc.setProtected(false);
        npc.spawn(location);
        npc.setName(NPC_NAME);
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, true);
        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent("samurai", SKIN_SIGNATURE, SKIN_TEXTURE);
        if (npc.getEntity() instanceof LivingEntity living) {
            PotionEffect healthBoost = new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 19, true, false, false);
            living.addPotionEffect(healthBoost);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (living.isValid()) {
                    living.setHealth(Math.min(100.0D, living.getMaxHealth()));
                }
            });
        }
        samurai.put(npc.getUniqueId(), npc);
        return npc;
    }

    private void despawnAll() {
        for (NPC npc : samurai.values()) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            npc.destroy();
        }
        samurai.clear();
    }

    private World resolveSamuraiWorld() {
        World world = Bukkit.getWorld("Consegrity");
        if (world != null) {
            return world;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private Location findRandomCherryLocation(World world, ThreadLocalRandom rng) {
        double radius = Math.max(32.0, ConsegrityRegions.LANDMASS_RADIUS - 32.0);
        for (int i = 0; i < 256; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rng.nextDouble()) * radius;
            int x = (int) Math.round(Math.cos(angle) * dist);
            int z = (int) Math.round(Math.sin(angle) * dist);
            if (ConsegrityRegions.regionAt(world, x, z) != ConsegrityRegions.Region.CHERRY) {
                continue;
            }

            int surfaceY = world.getHighestBlockYAt(x, z);
            if (surfaceY <= world.getMinHeight()) {
                continue;
            }
            Block surface = world.getBlockAt(x, surfaceY, z);
            Block above = world.getBlockAt(x, surfaceY + 1, z);
            if (!surface.getType().isSolid() || surface.isLiquid()) {
                continue;
            }
            if (!above.getType().isAir()) {
                continue;
            }
            float yaw = (float) (rng.nextDouble() * 360.0 - 180.0);
            return new Location(world, x + 0.5, surfaceY + 1, z + 0.5, yaw, 0.0f);
        }
        return null;
    }
}
