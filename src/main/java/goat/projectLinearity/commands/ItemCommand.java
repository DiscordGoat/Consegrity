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
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <name> [amount] [vanilla]");
            return true;
        }

        String rawName = args[0];
        String customLookup = rawName.replace('_', ' ').trim();
        String vanillaLookup = rawName.trim();
        int amount = 1;
        boolean vanilla = false;
        int nextIndex = 1;

        if (args.length >= 2) {
            Integer maybeAmount = parsePositiveInt(args[1]);
            if (maybeAmount != null) {
                amount = maybeAmount;
                nextIndex = 2;
            } else if (isBoolean(args[1])) {
                vanilla = parseBoolean(args[1]);
                nextIndex = 2;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid amount '" + args[1] + "'.");
                return true;
            }
        }

        if (args.length >= nextIndex + 1) {
            String flagArg = args[nextIndex];
            if (!isBoolean(flagArg)) {
                sender.sendMessage(ChatColor.RED + "Invalid vanilla flag '" + flagArg + "'.");
                return true;
            }
            vanilla = parseBoolean(flagArg);
            nextIndex++;
        }

        if (args.length > nextIndex) {
            sender.sendMessage(ChatColor.RED + "Too many arguments.");
            return true;
        }

        ItemStack item;
        if (vanilla) {
            item = createVanillaItem(sender, vanillaLookup);
        } else {
            item = ItemRegistry.getItemByName(customLookup);
            if (item == null) {
                // fallback to vanilla if custom lookup failed
                item = createVanillaItem(sender, vanillaLookup);
            }
        }

        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unable to find item named '" + rawName + "'.");
            return true;
        }

        giveItemToPlayer(player, item, amount);
        ItemStack preview = item.clone();
        preview.setAmount(Math.min(amount, preview.getMaxStackSize()));
        sender.sendMessage(ChatColor.GREEN + "Given " + ChatColor.YELLOW + amount + "x " + describeItem(preview) + ChatColor.GREEN + ".");
        return true;
    }

    private Integer parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private boolean isBoolean(String input) {
        return input.equalsIgnoreCase("true")
                || input.equalsIgnoreCase("false")
                || input.equalsIgnoreCase("yes")
                || input.equalsIgnoreCase("no")
                || input.equalsIgnoreCase("y")
                || input.equalsIgnoreCase("n");
    }

    private boolean parseBoolean(String input) {
        return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y");
    }

    private ItemStack createVanillaItem(CommandSender debugRecipient, String name) {
        Material material = matchMaterial(debugRecipient, name);
        if (material == null) return null;
        return new ItemStack(material);
    }

    private Material matchMaterial(CommandSender debugRecipient, String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;

        String withUnderscores = trimmed.replace(' ', '_');
        String upper = withUnderscores.toUpperCase(Locale.ENGLISH);
        String lower = withUnderscores.toLowerCase(Locale.ENGLISH);
        String namespaced = "minecraft:" + lower;

        Material material = attemptMaterialLookup(debugRecipient, trimmed, "raw");
        if (material != null) return material;

        if (!trimmed.equals(withUnderscores)) {
            material = attemptMaterialLookup(debugRecipient, withUnderscores, "underscored");
            if (material != null) return material;
        }

        material = attemptMaterialLookup(debugRecipient, upper, "upper");
        if (material != null) return material;

        material = attemptMaterialLookup(debugRecipient, lower, "lower");
        if (material != null) return material;

        material = attemptMaterialLookup(debugRecipient, namespaced, "namespaced");
        if (material != null) return material;

        // Final fallback: manual scan through known materials
        String normalized = upper;
        for (Material candidate : Material.values()) {
            if (candidate.name().equalsIgnoreCase(normalized)) {
                if (debugRecipient != null) {
                    debugRecipient.sendMessage(ChatColor.DARK_GRAY + "[/i debug] Matched vanilla item by scanning enum '" + candidate.name() + "'.");
                }
                return candidate;
            }
        }

        if (debugRecipient != null) {
            debugRecipient.sendMessage(ChatColor.DARK_GRAY + "[/i debug] Failed vanilla lookup. raw='" + name + "', normalized='" + normalized + "'.");
        }
        return null;
    }

    private Material attemptMaterialLookup(CommandSender debugRecipient, String key, String variant) {
        Material material = Material.matchMaterial(key, true);
        if (material != null && debugRecipient != null) {
            debugRecipient.sendMessage(ChatColor.DARK_GRAY + "[/i debug] Matched vanilla item using " + variant + " key '" + key + "'.");
        }
        return material;
    }

    private void giveItemToPlayer(Player player, ItemStack prototype, int totalAmount) {
        PlayerInventory inventory = player.getInventory();
        int remaining = totalAmount;
        int maxStack = Math.max(1, prototype.getMaxStackSize());
        while (remaining > 0) {
            ItemStack stack = prototype.clone();
            stack.setAmount(Math.min(maxStack, remaining));
            remaining -= stack.getAmount();
            Map<Integer, ItemStack> leftover = inventory.addItem(stack);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(remainingStack ->
                        player.getWorld().dropItemNaturally(player.getLocation(), remainingStack));
            }
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
            List<String> amounts = List.of("1", "16", "32", "64");
            String prefix = args[1].toLowerCase(Locale.ENGLISH);
            return amounts.stream()
                    .filter(opt -> prefix.isEmpty() || opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            List<String> options = new ArrayList<>();
            options.add("false");
            options.add("true");
            String prefix = args[2].toLowerCase(Locale.ENGLISH);
            return options.stream()
                    .filter(opt -> prefix.isEmpty() || opt.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
