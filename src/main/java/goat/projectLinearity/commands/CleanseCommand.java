package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager.CleanseMode;
import goat.projectLinearity.subsystems.brewing.CustomPotionEffectManager.CleanseResult;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CleanseCommand implements CommandExecutor, TabCompleter {

    private static final List<String> MODES = List.of("positive", "negative", "all");

    private final ProjectLinearity plugin;

    public CleanseCommand(ProjectLinearity plugin) {
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
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <positive|negative|all>");
            return true;
        }

        CleanseMode mode = parseMode(args[0]);
        if (mode == null) {
            player.sendMessage(ChatColor.RED + "Unknown cleanse mode '" + args[0] + "'. Expected positive, negative, or all.");
            return true;
        }

        CustomPotionEffectManager effectManager = plugin.getCustomPotionEffectManager();
        if (effectManager == null) {
            player.sendMessage(ChatColor.RED + "Potion system is unavailable.");
            return true;
        }

        CleanseResult result = effectManager.cleanse(player, mode);
        int removed = result.getTotalRemoved();
        if (removed == 0) {
            player.sendMessage(ChatColor.GRAY + "No potion effects were removed.");
        } else {
            player.sendMessage(ChatColor.GRAY + "Removed " + ChatColor.YELLOW + removed + ChatColor.GRAY + " custom potion effects.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ENGLISH);
            List<String> suggestions = new ArrayList<>();
            for (String option : MODES) {
                if (option.startsWith(partial)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }
        return List.of();
    }

    private CleanseMode parseMode(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase(Locale.ENGLISH)) {
            case "positive", "pos", "beneficial" -> CleanseMode.POSITIVE;
            case "negative", "neg", "harmful" -> CleanseMode.NEGATIVE;
            case "all", "everything", "any" -> CleanseMode.ALL;
            default -> null;
        };
    }
}
