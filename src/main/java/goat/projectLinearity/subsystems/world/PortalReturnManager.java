package goat.projectLinearity.subsystems.world;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Keeps players anchored to the Consegrity world, redirecting accidental trips to the default "world" dimension
 * and restoring them to the portal entry point when leaving the Nether.
 */
public final class PortalReturnManager implements Listener {
    private static final String CONSEGRITY_WORLD = "Consegrity";
    private static final String OVERWORLD_FALLBACK = "world";
    private static final int PORTAL_COOLDOWN_TICKS = 200;

    private final ProjectLinearity plugin;
    private final File storageFile;
    private final Map<UUID, StoredLocation> entryLocations = new HashMap<>();

    public PortalReturnManager(ProjectLinearity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.storageFile = new File(plugin.getDataFolder(), "portal_entries.yml");
        load();
    }

    public void startup() {
        // Sweep online players after plugin load to ensure no-one is stranded in fallback world.
        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureInConsegrity(player);
        }
    }

    public void shutdown() {
        save();
        entryLocations.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) {
            return;
        }

        switch (event.getCause()) {
            case NETHER_PORTAL -> handleNetherPortal(event, player, fromWorld);
            case END_PORTAL -> handleEndPortal(event, player);
            default -> {
            }
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        ensureInConsegrity(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleEnsure(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location respawn = event.getRespawnLocation();
        if (respawn != null && respawn.getWorld() != null && OVERWORLD_FALLBACK.equalsIgnoreCase(respawn.getWorld().getName())) {
            Location replacement = computeReturnLocation(player, false);
            if (replacement != null) {
                event.setRespawnLocation(replacement);
            }
        }
        scheduleEnsure(player);
    }

    private void handleNetherPortal(PlayerPortalEvent event, Player player, World fromWorld) {
        if (CONSEGRITY_WORLD.equalsIgnoreCase(fromWorld.getName())) {
            entryLocations.put(player.getUniqueId(), StoredLocation.from(event.getFrom()));
            persistEntries();
            return;
        }

        if (fromWorld.getEnvironment() == World.Environment.NETHER && isFallbackWorld(event.getTo())) {
            Location entry = computeReturnLocation(player, true);
            if (entry != null) {
                event.setCanCreatePortal(false);
                event.setTo(offsetForSafety(entry));
                player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
            }
        }
    }

    private void handleEndPortal(PlayerPortalEvent event, Player player) {
        if (isFallbackWorld(event.getTo())) {
            Location target = computeReturnLocation(player, false);
            if (target != null) {
                event.setTo(offsetForSafety(target));
                player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
            }
        }
    }

    private void ensureInConsegrity(Player player) {
        if (player == null) {
            return;
        }
        if (player.getWorld() != null && OVERWORLD_FALLBACK.equalsIgnoreCase(player.getWorld().getName())) {
            Location target = computeReturnLocation(player, false);
            if (target == null) {
                return;
            }
            player.teleport(offsetForSafety(target));
        }
    }

    private void scheduleEnsure(Player player) {
        if (player == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                ensureInConsegrity(player);
            }
        }.runTask(plugin);
    }

    private Location computeReturnLocation(Player player, boolean preferEntry) {
        // Preferred: stored entry point back from Nether
        if (preferEntry) {
            Location entry = getStoredEntry(player.getUniqueId());
            if (entry != null) {
                return entry;
            }
        }

        // Bed spawn, if it exists within Consegrity
        Location bed = player.getBedSpawnLocation();
        if (bed != null && bed.getWorld() != null && CONSEGRITY_WORLD.equalsIgnoreCase(bed.getWorld().getName())) {
            return bed;
        }

        // Stored entry (even if not from this trip)
        Location fallbackEntry = getStoredEntry(player.getUniqueId());
        if (fallbackEntry != null) {
            return fallbackEntry;
        }

        // World spawn for Consegrity
        World consegrity = Bukkit.getWorld(CONSEGRITY_WORLD);
        if (consegrity != null) {
            return consegrity.getSpawnLocation();
        }

        return null;
    }

    private Location offsetForSafety(Location location) {
        Location clone = location.clone();
        clone.setX(clone.getBlockX() + 0.5);
        clone.setZ(clone.getBlockZ() + 0.5);
        World world = clone.getWorld();
        if (world != null) {
            try {
                world.getChunkAt(clone).load();
            } catch (Throwable ignored) {}
            double safeY = world.getHighestBlockYAt(clone) + 1.0;
            clone.setY(Math.max(clone.getY(), safeY));
        }
        return clone;
    }

    private boolean isFallbackWorld(Location location) {
        return location != null && location.getWorld() != null && OVERWORLD_FALLBACK.equalsIgnoreCase(location.getWorld().getName());
    }

    private Location getStoredEntry(UUID id) {
        StoredLocation stored = entryLocations.get(id);
        if (stored == null) {
            return null;
        }
        return stored.asLocation();
    }

    private void load() {
        entryLocations.clear();
        if (!storageFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = config.getConfigurationSection("entries");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) continue;
                StoredLocation stored = StoredLocation.from(section);
                if (stored != null) {
                    entryLocations.put(id, stored);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("entries");
        for (Map.Entry<UUID, StoredLocation> entry : entryLocations.entrySet()) {
            ConfigurationSection child = root.createSection(entry.getKey().toString());
            entry.getValue().save(child);
        }
        try {
            config.save(storageFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save portal entry cache: " + ex.getMessage());
        }
    }

    private void persistEntries() {
        save();
    }

    private record StoredLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        static StoredLocation from(Location location) {
            if (location == null || location.getWorld() == null) {
                return null;
            }
            return new StoredLocation(location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
        }

        static StoredLocation from(ConfigurationSection section) {
            String world = section.getString("world");
            if (world == null) return null;
            return new StoredLocation(
                    world,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch")
            );
        }

        Location asLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }

        void save(ConfigurationSection section) {
            section.set("world", worldName);
            section.set("x", x);
            section.set("y", y);
            section.set("z", z);
            section.set("yaw", yaw);
            section.set("pitch", pitch);
        }
    }
}
