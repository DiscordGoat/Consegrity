package goat.projectLinearity.libs.gravedigging.terraforming;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;

/**
 * Awards Terraforming XP when players mine blocks that are exposed to the sky.
 */
public class Terraforming implements Listener {
    private final XPManager xpManager = new XPManager(MinecraftNew.getInstance());

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();
        World world = loc.getWorld();
        ArrayList<Material> topographicBlocksWhitelist = new ArrayList<>();
        topographicBlocksWhitelist.add(Material.GRASS_BLOCK);
        topographicBlocksWhitelist.add(Material.DIRT);
        topographicBlocksWhitelist.add(Material.STONE);
        if (world == null) return;
        int highest = world.getHighestBlockYAt(loc);
        if (loc.getBlockY() >= highest && topographicBlocksWhitelist.contains(block.getType())) {
            xpManager.addXP(player, "Terraforming", 1);
        }
    }
}
