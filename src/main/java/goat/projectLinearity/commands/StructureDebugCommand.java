package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.world.structure.StructureManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class StructureDebugCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;

    public StructureDebugCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("consegrity.dev")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        StructureManager manager = plugin.getStructureManager();
        if (manager == null) {
            sender.sendMessage(ChatColor.RED + "Structure manager is not initialised yet.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Structure debug is currently " + (manager.isDebug() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " <on|off|summary>");
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "on" -> {
                manager.setDebug(true);
                sender.sendMessage(ChatColor.GREEN + "Structure debug counters enabled (loot logs only).");
            }
            case "off" -> {
                manager.setDebug(false);
                manager.clearDebug();
                sender.sendMessage(ChatColor.GREEN + "Structure debug logging disabled.");
            }
            case "summary" -> {
                String worldKey;
                if (args.length >= 2) {
                    var world = sender.getServer().getWorld(args[1]);
                    if (world == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown world '" + args[1] + "'.");
                        return true;
                    }
                    worldKey = world.getUID().toString();
                } else {
                    worldKey = sender.getServer().getWorlds().get(0).getUID().toString();
                }
                sender.sendMessage(manager.debugSummary(worldKey));
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown option. Use on, off, or summary.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("on", "off", "summary");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summary")) {
            return sender.getServer().getWorlds().stream().map(org.bukkit.World::getName).toList();
        }
        return java.util.Collections.emptyList();
    }
}
