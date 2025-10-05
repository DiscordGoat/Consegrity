package goat.projectLinearity.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarptoCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("consegrity.warpto")) {
            player.sendMessage("You lack permission: consegrity.warpto");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("Usage: /warpto <worldName>");
            return true;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("World not found: " + worldName);
            return true;
        }

        try {
            player.teleport(world.getSpawnLocation());
            player.sendMessage("Warped to: " + world.getName());
        } catch (Throwable t) {
            player.sendMessage("Teleport failed: " + t.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                String n = w.getName();
                if (prefix.isEmpty() || n.toLowerCase().startsWith(prefix)) names.add(n);
            }
            Collections.sort(names);
            return names;
        }
        return Collections.emptyList();
    }
}

