package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class SetStatRateCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public SetStatRateCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <multiplier>");
            return true;
        }
        double rate;
        try {
            rate = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Multiplier must be a number.");
            return true;
        }
        if (rate <= 0) {
            sender.sendMessage(ChatColor.RED + "Multiplier must be greater than zero.");
            return true;
        }
        plugin.setStatRate(rate);
        sender.sendMessage(ChatColor.GREEN + "Set stat ticking rate to " + rate + "x.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("0.5", "1", "2", "5", "10");
        }
        return List.of();
    }
}

