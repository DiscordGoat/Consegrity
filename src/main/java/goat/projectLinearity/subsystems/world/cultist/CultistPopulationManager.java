package goat.projectLinearity.subsystems.world.cultist;

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
 * Handles creation and cleanup of Mountain Cultist Citizens NPCs.
 * Persistence is intentionally disabled; populations are rebuilt on demand.
 */
public final class CultistPopulationManager {

    public static final int DEFAULT_POPULATION = 50;
    private static final double AVOID_DISTANCE_SQ = 80.0 * 80.0;
    private static final double MIN_SPAWN_SPREAD = 50.0;
    private static final double MIN_SPAWN_SPREAD_SQ = MIN_SPAWN_SPREAD * MIN_SPAWN_SPREAD;
    private static final String SKIN_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTY5NDgzNDE0OTgwMCwKICAicHJvZmlsZUlkIiA6ICI1OGQ2YmFhYTQ5ZjA0Nzg2OGE1MzE1ZWViYmU0YWIwMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJfemFpZGdyaWRfIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE2NDMyMDRhOWQwNzIyODhmNzJkMmZkMDJkMTg5YzVhM2IwNDEzODM1OGMwMjJmYjJjZDUyZTExNTkzODQyZjciCiAgICB9CiAgfQp9";
    private static final String SKIN_SIGNATURE = "tuovYTjtoaoOxyU3aUvRx2MPGzEiZHYftD24/w8GFmFV/z9ykWz9U0vH3Q7VUfaERINKvahhu1ISFczC0ugEsHujQfRHdn4+lJema91KEUqDJfvTtzhf/Bo7w+0VG/IHNgmdEgD51qffroxI073r50STAVICHvEDjeOpH4i74A+TAF4UeEGgVP1p4qxwrsnRQlsUkobzOScHGGiOHL4FB+w2xPuRQcwAd5OztVqOr57wkFvkaQHG5iHVQ9aYXzS3q+dSY7UDYAvPirEhBEfJu0TRMIfgZ8fdnfHJhzKicZ1rEGB9jaKTEgHwiFuSpIJOSC0Qzr5AsCgnrQpicyETe/3BVSo4Hq6GDit0S+bkLq5TBaZ9HdgyY7e10F6ERAR1EiNLL+jWP1lIgXFVaxFbhpqQr4NpH5FDrjgbSjFyl/qv63jAV09YQtZv+53NooOwiGrCwQwv4AH3aH92FD3zlOA2tMwzO1tOW22CwJ2QFAIlsPape0Yfm0UTWjpWUzwoa6yRs1Iz4DYUJEQhjxJ7V8q9y4IGL7xu5KohwmHcGhw6HpKiZTkL3VmXgoJSjcaodBtdk6+cPnXOxZ2UBNTt9FqI9XkCDcfbQCbI4hn4x7Fk0U0iT9jdhNhsdtgN9jlSga+UxvFR4NBMmg66uA++Z3CEL2QbmhRud0xUmH4cPQw=";
    private static final String NPC_NAME = ChatColor.DARK_PURPLE + "Mountain Cultist";

    private final JavaPlugin plugin;
    private final Logger logger;

    private NPCRegistry registry;
    private final Map<UUID, NPC> cultists = new LinkedHashMap<>();
    private int targetPopulation = DEFAULT_POPULATION;

    public CultistPopulationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean startup() {
        if (!ensureRegistry()) {
            logger.warning("Citizens not ready; cultist population will remain empty until retry.");
            return false;
        }
        logger.info("Cultist population manager initialised.");
        return true;
    }

    public void shutdown() {
        despawnAll();
        if (registry != null) {
            registry.deregisterAll();
            registry = null;
        }
        cultists.clear();
    }

    public Collection<NPC> getActiveCultists() {
        return cultists.values();
    }

    public int getCurrentPopulation() {
        return cultists.size();
    }

    public int getTargetPopulation() {
        return targetPopulation;
    }

    public void setTargetPopulation(int target) {
        targetPopulation = Math.max(0, target);
        logger.info(() -> "Cultist population target set to " + targetPopulation + ".");
    }

    public void respawnPopulation(Player avoidPlayer) {
        if (plugin instanceof ProjectLinearity pl && pl.getMountainCultistBehaviour() != null) {
            pl.getMountainCultistBehaviour().clearRoamStates();
        }
        if (!ensureRegistry()) {
            logger.warning("Citizens not available; unable to spawn cultists.");
            return;
        }
        World world = resolveCultistWorld();
        if (world == null) {
            logger.warning("Unable to resolve cultist world.");
            return;
        }

        despawnAll();

        Location avoid = avoidPlayer != null ? avoidPlayer.getLocation() : null;
        double minDistanceSq = avoid != null ? AVOID_DISTANCE_SQ : 0.0;
        int spawned = 0;
        java.util.ArrayList<Location> placed = new java.util.ArrayList<>();
        int attempts = 0;
        int maxAttempts = Math.max(targetPopulation * 40, 200);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        while (spawned < targetPopulation && attempts++ < maxAttempts) {
            Location candidate = findRandomMountainLocation(world, rng);
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
            NPC npc = createCultist(candidate);
            if (npc != null) {
                placed.add(candidate.clone());
                spawned++;
            }
        }

        if (spawned < targetPopulation) {
            logger.warning("Spawned " + spawned + " of " + targetPopulation + " requested cultists.");
        } else {
            logger.info("Spawned " + spawned + " cultists.");
        }
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

    public Optional<NPC> spawnCultistEntity(Location location) {
        if (location == null) {
            logger.warning("Cannot spawn cultist at null location.");
            return Optional.empty();
        }
        if (!ensureRegistry()) {
            logger.warning("Citizens not available; unable to spawn cultist.");
            return Optional.empty();
        }
        NPC npc = createCultist(location.clone());
        return Optional.ofNullable(npc);
    }

    private NPC createCultist(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        world.getChunkAt(location).load();

        NPC npc = registry.createNPC(EntityType.PLAYER, NPC_NAME);
        npc.setProtected(false);
        npc.spawn(location);
        npc.setName("");
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent("cultist", SKIN_SIGNATURE, SKIN_TEXTURE);
        LivingEntity cultist = (LivingEntity) npc.getEntity();
        PotionEffect resistanceEffect = new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 4, true, false);
        boolean applied = cultist.addPotionEffect(resistanceEffect);
        PotionEffect activeResistance = cultist.getPotionEffect(PotionEffectType.RESISTANCE);
        int resistanceLevel = activeResistance != null ? activeResistance.getAmplifier() + 1 : 0;
        String resistanceDebug = activeResistance != null
                ? String.format("Resistance %d (amplifier=%d)", resistanceLevel, activeResistance.getAmplifier())
                : "none";
        String durationDebug = activeResistance != null ? String.valueOf(activeResistance.getDuration()) : "n/a";
        String locationDebug = String.format(
                "world=%s,x=%.1f,y=%.1f,z=%.1f",
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ());
        logger.info(() -> String.format(
                "Cultist %s damage immunity ready at %s (resistance=%s, duration=%s)",
                npc.getUniqueId(),
                applied ? "applied" : "not applied",
                locationDebug,
                resistanceDebug,
                durationDebug));

        cultists.put(npc.getUniqueId(), npc);
        return npc;
    }

    public boolean isCultistNpc(NPC npc) {
        if (npc == null) {
            return false;
        }
        return cultists.containsKey(npc.getUniqueId());
    }

    public boolean isCultistEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return isCultistNpc(npc);
    }

    private void despawnAll() {
        for (NPC npc : cultists.values()) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            npc.destroy();
        }
        cultists.clear();
    }

    private World resolveCultistWorld() {
        World world = Bukkit.getWorld("Consegrity");
        if (world != null) {
            return world;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private Location findRandomMountainLocation(World world, ThreadLocalRandom rng) {
        double radius = Math.max(32.0, ConsegrityRegions.LANDMASS_RADIUS - 32.0);
        for (int i = 0; i < 256; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rng.nextDouble()) * radius;
            int x = (int) Math.round(Math.cos(angle) * dist);
            int z = (int) Math.round(Math.sin(angle) * dist);
            if (ConsegrityRegions.regionAt(world, x, z) != ConsegrityRegions.Region.MOUNTAIN) {
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
