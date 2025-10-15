package goat.projectLinearity.world;

import goat.projectLinearity.world.KeystoneManager.KeystoneInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class KeystoneListener implements Listener {

    private final KeystoneManager manager;

    public KeystoneListener(KeystoneManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        // Only accept right-click interactions
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        Location clicked = event.getClickedBlock().getLocation();
        Optional<KeystoneInstance> instance = manager.findInstance(player.getWorld(), clicked, false);
        if (instance.isEmpty()) {
            return;
        }
        boolean contributed = manager.tryConsumeContribution(player, instance.get(), stack);
        if (contributed) {
            event.setCancelled(true);
        }
    }

    private boolean isProtected(Player player, Location location) {
        Optional<KeystoneInstance> instance = manager.findInstance(player.getWorld(), location, true);
        return instance.isPresent();
    }
}
