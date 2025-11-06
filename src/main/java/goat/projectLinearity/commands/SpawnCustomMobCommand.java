package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.libs.mutation.MutationDefinition;
import goat.projectLinearity.libs.mutation.MutationManager;
import goat.projectLinearity.util.CustomEntityRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SpawnCustomMobCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public SpawnCustomMobCommand(ProjectLinearity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        CustomEntityRegistry registry = plugin.getCustomEntityRegistry();
        if (registry == null) {
            sender.sendMessage(ChatColor.RED + "Custom entity registry is not ready yet.");
            return true;
        }
        MutationManager mutationManager = plugin.getMutationManager();
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <name>");
            sender.sendMessage(ChatColor.GRAY + "Available: " + String.join(", ", availableKeys(registry, mutationManager)));
            return true;
        }

        String key = args[0];
        Optional<CustomEntityRegistry.CustomEntityEntry> entryOpt = registry.find(key);
        if (entryOpt.isEmpty()) {
            if (mutationManager != null) {
                Optional<MutationDefinition> definitionOpt = mutationManager.find(key);
                if (definitionOpt.isPresent()) {
                    Location spawnLocation = computeSpawnLocation(player);
                    Optional<LivingEntity> spawned = mutationManager.spawnMutation(key, spawnLocation);
                    if (spawned.isPresent()) {
                        MutationDefinition definition = definitionOpt.get();
                        String labelText = definition.name() != null ? definition.name() : definition.id();
                        sender.sendMessage(ChatColor.GREEN + "Spawned mutation " + ChatColor.RESET + labelText);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Failed to spawn mutation '" + key + "'.");
                    }
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "Unknown custom mob or mutation '" + key + "'.");
            sender.sendMessage(ChatColor.GRAY + "Available: " + String.join(", ", availableKeys(registry, mutationManager)));
            return true;
        }

        Location spawnLocation = computeSpawnLocation(player);
        CustomEntityRegistry.CustomEntityEntry entry = entryOpt.get();
        CustomEntityRegistry.SpawnResult result = entry.spawner().spawn(plugin, spawnLocation, sender);
        String message = result.message() != null ? result.message() : (result.success()
                ? "Spawn succeeded."
                : "Spawn failed.");
        sender.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        CustomEntityRegistry registry = plugin.getCustomEntityRegistry();
        if (registry == null) {
            return List.of();
        }
        MutationManager mutationManager = plugin.getMutationManager();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            Stream<String> customKeys = registry.keys().stream();
            Stream<String> mutationKeys = mutationManager != null ? mutationManager.mutationIds().stream() : Stream.empty();
            return Stream.concat(customKeys, mutationKeys)
                    .filter(name -> name.startsWith(prefix))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return List.of();
    }

    private List<String> availableKeys(CustomEntityRegistry registry, MutationManager mutationManager) {
        List<String> keys = new ArrayList<>(registry.primaryIds());
        if (mutationManager != null) {
            keys.addAll(mutationManager.mutationIds());
        }
        keys.sort(String::compareToIgnoreCase);
        return keys;
    }

    private Location computeSpawnLocation(Player player) {
        Location source = player.getLocation();
        Location spawn = source.clone();
        Vector direction = source.getDirection();
        if (direction.lengthSquared() > 1.0e-4) {
            direction.setY(0.0);
            if (direction.lengthSquared() > 1.0e-4) {
                direction.normalize().multiply(1.5);
                spawn.add(direction);
            }
        }
        spawn.setY(source.getY());
        return spawn;
    }
}
