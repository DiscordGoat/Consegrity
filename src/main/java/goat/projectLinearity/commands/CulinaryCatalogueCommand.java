package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.culinary.CulinaryCatalogueManager;
import goat.projectLinearity.subsystems.culinary.CulinarySubsystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class CulinaryCatalogueCommand implements CommandExecutor, TabCompleter {

    private final ProjectLinearity plugin;
    private final CulinaryCatalogueManager catalogueManager;
    private final CulinarySubsystem culinarySubsystem;

    public CulinaryCatalogueCommand(ProjectLinearity plugin, CulinaryCatalogueManager catalogueManager, CulinarySubsystem culinarySubsystem) {
        this.plugin = plugin;
        this.catalogueManager = catalogueManager;
        this.culinarySubsystem = culinarySubsystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!player.hasPermission("consegrity.culinary")) {
            player.sendMessage(ChatColor.RED + "You lack permission to access the culinary catalogue.");
            return true;
        }
        if (catalogueManager == null || culinarySubsystem == null) {
            player.sendMessage(ChatColor.RED + "Culinary catalogue is currently unavailable.");
            plugin.getLogger().warning("Culinary catalogue manager was null when command executed.");
            return true;
        }
        catalogueManager.openCatalogue(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
