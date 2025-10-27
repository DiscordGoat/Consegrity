package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager;
import goat.projectLinearity.subsystems.brewing.PotionRegistry;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EffectCustomCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public EffectCustomCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <effectId> <potency> <durationSeconds>");
            return true;
        }

        PotionDefinition definition = resolveDefinition(args[0]);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Unknown potion effect '" + args[0] + "'.");
            return true;
        }

        int potency;
        int duration;
        try {
            potency = Integer.parseInt(args[1]);
            duration = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Potency and duration must be numbers.");
            return true;
        }

        if (potency <= 0 || duration <= 0) {
            player.sendMessage(ChatColor.RED + "Potency and duration must be positive.");
            return true;
        }

        CustomPotionEffectManager manager = plugin.getCustomPotionEffectManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "Potion system is unavailable.");
            return true;
        }

        manager.applyCustomEffect(player, definition, BrewType.OVERWORLD, duration, potency, 0);
        player.sendMessage(ChatColor.GRAY + "Applied " + definition.getAccentColor() + definition.getDisplayName() + ChatColor.GRAY +
                " (P" + potency + ", " + duration + "s).");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ENGLISH);
            List<String> suggestions = new ArrayList<>();
            for (PotionDefinition definition : PotionRegistry.getAll()) {
                String id = definition.getId();
                if (id.startsWith(partial)) {
                    suggestions.add(id);
                }
            }
            return suggestions;
        }
        if (args.length == 2) {
            return List.of("1", "2", "3", "4", "5");
        }
        if (args.length == 3) {
            return List.of("1", "5", "10", "30", "60");
        }
        return List.of();
    }

    private PotionDefinition resolveDefinition(String input) {
        if (input == null) {
            return null;
        }
        Optional<PotionDefinition> direct = PotionRegistry.getById(input.toLowerCase(Locale.ENGLISH));
        return direct.orElse(null);
    }
}
