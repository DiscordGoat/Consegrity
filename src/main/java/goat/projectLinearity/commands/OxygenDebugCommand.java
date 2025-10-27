package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.mechanics.TablistManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OxygenDebugCommand implements CommandExecutor {

    private final ProjectLinearity plugin;

    public OxygenDebugCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        TablistManager manager = plugin.getTablistManager();
        if (manager == null) {
            player.sendMessage("Tablist manager unavailable.");
            return true;
        }

        boolean enabled = manager.toggleOxygenDebug(player);
        manager.refreshPlayer(player);
        player.sendMessage(enabled
                ? "Oxygen debug enabled."
                : "Oxygen debug disabled.");
        return true;
    }
}

