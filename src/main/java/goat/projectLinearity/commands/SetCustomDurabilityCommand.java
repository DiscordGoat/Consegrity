package goat.projectLinearity.commands;

import goat.projectLinearity.libs.CustomDurabilityManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class SetCustomDurabilityCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <amount>");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().getMaxDurability() <= 0) {
            sender.sendMessage(ChatColor.RED + "Hold a damageable item.");
            return true;
        }

        CustomDurabilityManager manager = CustomDurabilityManager.getInstance();
        manager.ensureTracking(item);
        int max = manager.getMaxDurability(item);
        int clamped = Math.max(0, Math.min(value, max));
        manager.setCustomDurability(item, clamped, max);
        sender.sendMessage(ChatColor.GREEN + "Set durability to " + clamped + "/" + max + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("<amount>");
        }
        return Collections.emptyList();
    }
}

