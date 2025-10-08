package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.SidebarManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SetStatCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public SetStatCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <oxygen|temperature> <value> [player]");
            return true;
        }

        String type = args[0].toLowerCase(Locale.ENGLISH);
        String valueArg = args[1];

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Unable to find player '" + args[2] + "'.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ChatColor.RED + "Console must specify a player.");
            return true;
        }

        double parsedValue;
        try {
            parsedValue = Double.parseDouble(valueArg);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Value must be numeric.");
            return true;
        }

        int oxygenValue = (int) Math.round(parsedValue);

        switch (type) {
            case "oxygen", "o2" -> {
                plugin.getMiningOxygenManager().setOxygen(target, oxygenValue, true);
                sender.sendMessage(ChatColor.GREEN + "Set oxygen for " + target.getName() + " to " + oxygenValue + ".");
            }
            case "temperature", "temp" -> {
                SidebarManager sidebar = plugin.getSidebarManager();
                if (sidebar == null) {
                    sender.sendMessage(ChatColor.RED + "Sidebar manager is not initialised.");
                    return true;
                }
                sidebar.setTemperature(target, parsedValue);
                sender.sendMessage(ChatColor.GREEN + "Set temperature for " + target.getName() + " to " + parsedValue + "°F.");
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown stat '" + type + "'.");
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("oxygen", "temperature");
        }
        if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return List.of();
    }
}
