package goat.projectLinearity.subsystems.farming;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class CropPlantingListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Material placedType = event.getBlockPlaced().getType();
        if (placedType != Material.CARROTS && placedType != Material.POTATOES) {
            return;
        }

        ItemStack inHand = event.getItemInHand();
        if (placedType == Material.CARROTS) {
            if (ItemRegistry.isTinyCarrot(inHand)) {
                return;
            }
            cancelPlacement(event, ChatColor.RED + "Only Tiny Carrots can be planted.");
            return;
        }

        if (ItemRegistry.isBabyPotato(inHand)) {
            return;
        }
        cancelPlacement(event, ChatColor.RED + "Only Baby Potatoes can be planted.");
    }

    private void cancelPlacement(BlockPlaceEvent event, String message) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}
