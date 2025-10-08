package goat.projectLinearity.commands;

import goat.projectLinearity.libs.HeirloomManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class SetGildCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <amount> [max]");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        Integer max = null;
        if (args.length >= 2) {
            try {
                max = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Max must be a number.");
                return true;
            }
            if (max <= 0) {
                sender.sendMessage(ChatColor.RED + "Max must be greater than zero.");
                return true;
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "Hold the heirloom you wish to modify.");
            return true;
        }

        HeirloomManager manager = HeirloomManager.getInstance();
        if (manager == null) {
            sender.sendMessage(ChatColor.RED + "Heirloom manager is not initialised.");
            return true;
        }

        if (!manager.isHeirloom(item) && max == null) {
            sender.sendMessage(ChatColor.RED + "That item is not recognised as an heirloom. Provide a max value to initialise it.");
            return true;
        }

        int resolvedMax = max != null ? max : manager.getMaxGild(item);
        if (resolvedMax <= 0) {
            resolvedMax = Math.max(amount, 0);
            if (resolvedMax <= 0) {
                sender.sendMessage(ChatColor.RED + "Unable to determine max gild for this item.");
                return true;
            }
        }

        manager.setGild(item, amount, resolvedMax);
        sender.sendMessage(ChatColor.GOLD + "Set gild to " + Math.max(0, Math.min(amount, resolvedMax)) + "/" + resolvedMax + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("<amount>", "0", "25", "50", "100");
        }
        if (args.length == 2) {
            return Arrays.asList("<max>", "100", "250", "500");
        }
        return List.of();
    }
}
