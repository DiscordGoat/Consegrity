package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.cultist.CultistPopulationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public final class SetCultistPopulationCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public SetCultistPopulationCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("consegrity.cultist")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <population>");
            return true;
        }

        int target;
        try {
            target = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Population must be a whole number.");
            return true;
        }
        if (target < 0) {
            sender.sendMessage(ChatColor.RED + "Population cannot be negative.");
            return true;
        }

        CultistPopulationManager manager = plugin.getCultistPopulationManager();
        if (manager == null) {
            sender.sendMessage(ChatColor.RED + "Cultist population manager is not initialised.");
            return true;
        }

        manager.setTargetPopulation(target);
        int finalTarget = manager.getTargetPopulation();
        if (finalTarget != target) {
            sender.sendMessage(ChatColor.RED + "Unable to reach requested population. Target adjusted to " + finalTarget + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Cultist population target set to " + finalTarget + ".");
        }
        manager.respawnPopulation(null);
        int current = manager.getCurrentPopulation();
        if (current == finalTarget) {
            sender.sendMessage(ChatColor.GREEN + "Population is now " + current + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Population is " + current + "; " +
                    Math.max(0, finalTarget - current) + " additional spawn attempt(s) required.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            CultistPopulationManager manager = plugin.getCultistPopulationManager();
            if (manager != null) {
                return Collections.singletonList(Integer.toString(manager.getTargetPopulation()));
            }
        }
        return Collections.emptyList();
    }
}
