package goat.projectLinearity.commands;

import goat.projectLinearity.subsystems.world.desert.CurseManager;
import goat.projectLinearity.subsystems.world.desert.CurseRegistry;
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
import java.util.stream.Collectors;

/**
 * Development command for applying curses to players.
 */
public final class CurseCommand implements CommandExecutor, TabCompleter {

    private final CurseManager curseManager;

    public CurseCommand(CurseManager curseManager) {
        this.curseManager = curseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player> <curse>");
            sender.sendMessage(ChatColor.GRAY + "Available curses: " + String.join(", ", CurseRegistry.getCurseNames()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
            return true;
        }

        String curseName = args[1].toLowerCase().replace(" ", "_");
        CurseRegistry.Curse curse = CurseRegistry.getCurse(curseName);

        if (curse == null) {
            sender.sendMessage(ChatColor.RED + "Unknown curse '" + args[1] + "'.");
            sender.sendMessage(ChatColor.GRAY + "Available curses: " + String.join(", ", CurseRegistry.getCurseNames()));
            return true;
        }

        // Apply curse for 3 minutes (3600 ticks)
        curseManager.applyCurse(target, curse, 3600L);

        sender.sendMessage(ChatColor.GREEN + "Applied " + curse.displayName() + " §ato §e" + target.getName() + "§a for 3 minutes!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Player name completion
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // Curse name completion
            String prefix = args[1].toLowerCase(Locale.ROOT);
            completions.addAll(CurseRegistry.getCurseNames().stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList()));
        }

        return completions;
    }
}
