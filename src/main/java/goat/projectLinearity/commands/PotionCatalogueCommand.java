package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionGuiManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Opens the custom potion catalogue for comfortable testing.
 */
public final class PotionCatalogueCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;
    private final PotionGuiManager guiManager;

    public PotionCatalogueCommand(ProjectLinearity plugin, PotionGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!player.hasPermission("consegrity.potions")) {
            player.sendMessage(ChatColor.RED + "You lack permission to access the potion catalogue.");
            return true;
        }
        if (guiManager == null) {
            player.sendMessage(ChatColor.RED + "Potion GUI is currently unavailable.");
            plugin.getLogger().warning("Potion GUI manager was null when command executed.");
            return true;
        }
        guiManager.openCatalogue(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
