package goat.projectLinearity.util;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.components.FoodComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of bespoke entity spawn handlers used for developer utilities.
 */
public final class CustomEntityRegistry {

    private final ProjectLinearity plugin;
    private final Map<String, CustomEntityEntry> entries = new LinkedHashMap<>();
    private final Map<String, CustomEntityEntry> lookup = new LinkedHashMap<>();

    public CustomEntityRegistry(ProjectLinearity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public ProjectLinearity getPlugin() {
        return plugin;
    }

    public void register(CustomEntityEntry entry) {
        Objects.requireNonNull(entry, "entry");
        String id = normalise(entry.id());
        entries.put(id, entry);
        lookup.put(id, entry);
        for (String alias : entry.aliases()) {
            lookup.put(normalise(alias), entry);
        }

    }

    public Optional<CustomEntityEntry> find(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lookup.get(normalise(key)));
    }

    public Collection<CustomEntityEntry> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(lookup.keySet());
    }

    public List<String> primaryIds() {
        return entries.keySet().stream().collect(Collectors.toUnmodifiableList());
    }

    private static String normalise(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    public record CustomEntityEntry(
            String id,
            String displayName,
            String description,
            List<String> aliases,
            CustomEntitySpawner spawner
    ) {
        public CustomEntityEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(aliases, "aliases");
            Objects.requireNonNull(spawner, "spawner");
            aliases = List.copyOf(aliases);
        }
    }

    @FunctionalInterface
    public interface CustomEntitySpawner {
        SpawnResult spawn(ProjectLinearity plugin, Location location, CommandSender sender);
    }

    public record SpawnResult(boolean success, String message) {
        public static SpawnResult success(String message) {
            return new SpawnResult(true, message);
        }

        public static SpawnResult failure(String message) {
            return new SpawnResult(false, message);
        }
    }
}
