package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class DebugOxygenCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public DebugOxygenCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean newState;
        if (args.length == 0) {
            newState = !plugin.isDebugOxygen();
        } else {
            String arg = args[0].toLowerCase(Locale.ENGLISH);
            if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
                newState = true;
            } else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
                newState = false;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [on|off]");
                return true;
            }
        }
        plugin.setDebugOxygen(newState);
        sender.sendMessage(ChatColor.YELLOW + "Oxygen debug " + (newState ? "enabled" : "disabled") + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}

