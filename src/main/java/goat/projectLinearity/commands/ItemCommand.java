package goat.projectLinearity.commands;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <name> [vanilla]");
            return true;
        }

        String rawName = args[0];
        String lookupName = rawName.replace('_', ' ').trim();
        boolean vanilla = args.length >= 2 && parseBoolean(args[1]);

        ItemStack item = vanilla ? createVanillaItem(lookupName) : ItemRegistry.getItemByName(lookupName);

        if (item == null && !vanilla) {
            // fallback to vanilla if custom lookup failed
            item = createVanillaItem(lookupName);
        }

        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unable to find item named '" + rawName + "'.");
            return true;
        }

        giveItemToPlayer(player, item);
        sender.sendMessage(ChatColor.GREEN + "Given " + ChatColor.YELLOW + describeItem(item) + ChatColor.GREEN + ".");
        return true;
    }

    private boolean parseBoolean(String input) {
        return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y");
    }

    private ItemStack createVanillaItem(String name) {
        Material material = matchMaterial(name);
        if (material == null) return null;
        return new ItemStack(material);
    }

    private Material matchMaterial(String name) {
        String normalized = name.toUpperCase(Locale.ENGLISH).replace(' ', '_');
        Material material = Material.matchMaterial(normalized, true);
        if (material == null) {
            material = Material.matchMaterial(name, true);
        }
        return material;
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> leftover = inventory.addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }
    }

    private String describeItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            Set<String> suggestions = new LinkedHashSet<>(ItemRegistry.getItemNameSuggestions());
            for (Material material : Material.values()) {
                suggestions.add(material.name().toLowerCase(Locale.ENGLISH));
            }
            return suggestions.stream()
                    .filter(name -> prefix.isEmpty() || name.startsWith(prefix))
                    .limit(50)
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("false");
            options.add("true");
            String prefix = args[1].toLowerCase(Locale.ENGLISH);
            return options.stream()
                    .filter(opt -> prefix.isEmpty() || opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

