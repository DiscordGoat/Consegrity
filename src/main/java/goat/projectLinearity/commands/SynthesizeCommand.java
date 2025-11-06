package goat.projectLinearity.commands;

import goat.projectLinearity.subsystems.brewing.PotionRegistry;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Developer tooling for synthesizing bespoke potions on demand.
 */
public final class SynthesizeCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_SUBCOMMAND = "admin_potion";

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
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " admin_potion <effect> <durationSeconds> <potency> <charges>");
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ENGLISH);
        if (ADMIN_SUBCOMMAND.equals(mode)) {
            return handleAdminPotion(player, label, args);
        }

        player.sendMessage(ChatColor.RED + "Unknown synthesis mode '" + args[0] + "'.");
        return true;
    }

    private boolean handleAdminPotion(Player player, String label, String[] args) {
        if (args.length != 5) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " admin_potion <effect> <durationSeconds> <potency> <charges>");
            return true;
        }

        String effectId = args[1].toLowerCase(Locale.ENGLISH);
        PotionDefinition definition = resolveDefinition(effectId);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Unknown potion effect '" + args[1] + "'.");
            List<String> suggestions = effectSuggestions(effectId);
            if (!suggestions.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Try: " + ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, suggestions));
            }
            return true;
        }

        Integer duration = parsePositiveInt(args[2]);
        if (duration == null) {
            player.sendMessage(ChatColor.RED + "Duration must be a positive number.");
            return true;
        }

        Integer potency = parsePositiveInt(args[3]);
        if (potency == null) {
            player.sendMessage(ChatColor.RED + "Potency must be a positive number.");
            return true;
        }

        Integer charges = parsePositiveInt(args[4]);
        if (charges == null) {
            player.sendMessage(ChatColor.RED + "Charges must be a positive number.");
            return true;
        }

        ItemStack potion = PotionRegistry.createAdminPotion(definition, BrewType.OVERWORLD, false, duration, potency, charges);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(potion);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.updateInventory();

        player.sendMessage(ChatColor.DARK_AQUA + "[Synthesize] " + ChatColor.GRAY + "Created "
                + definition.getAccentColor() + definition.getDisplayName() + ChatColor.GRAY + " ("
                + ChatColor.YELLOW + potency + ChatColor.GRAY + " potency, "
                + ChatColor.YELLOW + duration + ChatColor.GRAY + "s, "
                + ChatColor.YELLOW + charges + ChatColor.GRAY + " charges).");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of(ADMIN_SUBCOMMAND), args[0]);
        }
        if (args.length == 2 && ADMIN_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            List<String> options = PotionRegistry.getAll().stream()
                    .map(PotionDefinition::getId)
                    .sorted()
                    .collect(Collectors.toList());
            return filterByPrefix(options, args[1]);
        }
        if (args.length == 3 && ADMIN_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            return filterByPrefix(List.of("60", "120", "180", "300"), args[2]);
        }
        if (args.length == 4 && ADMIN_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            return filterByPrefix(List.of("1", "2", "3", "4"), args[3]);
        }
        if (args.length == 5 && ADMIN_SUBCOMMAND.equalsIgnoreCase(args[0])) {
            return filterByPrefix(List.of("5", "10", "15", "20"), args[4]);
        }
        return List.of();
    }

    private PotionDefinition resolveDefinition(String id) {
        if (id == null) {
            return null;
        }
        return PotionRegistry.getById(id.toLowerCase(Locale.ENGLISH)).orElse(null);
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> effectSuggestions(String prefix) {
        return PotionRegistry.getAll().stream()
                .map(PotionDefinition::getId)
                .filter(id -> prefix == null || prefix.isBlank() || id.startsWith(prefix.toLowerCase(Locale.ENGLISH)))
                .sorted()
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<String> filterByPrefix(Collection<String> options, String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option == null) {
                continue;
            }
            String lower = option.toLowerCase(Locale.ENGLISH);
            if (prefix.isEmpty() || lower.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
