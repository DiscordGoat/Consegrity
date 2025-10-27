package goat.projectLinearity.commands;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.world.EliteManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class WarptoEliteCommand implements CommandExecutor {

    private final ProjectLinearity plugin;

    public WarptoEliteCommand(ProjectLinearity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage("You lack permission: consegrity.dev");
            return true;
        }

        EliteManager manager = plugin.getEliteManager();
        if (manager == null) {
            player.sendMessage("Elite manager unavailable.");
            return true;
        }
        LivingEntity elite = manager.getCurrentElite();
        if (elite == null || !elite.isValid()) {
            player.sendMessage("No active elite found.");
            return true;
        }

        Location target = elite.getLocation().clone();
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        target.add(0, 1.0, 0);

        player.teleport(target);
        player.sendMessage("Warped to elite: " + elite.getType().name());
        return true;
    }
}

