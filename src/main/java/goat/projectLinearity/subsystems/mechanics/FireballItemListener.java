package goat.projectLinearity.subsystems.mechanics;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Handles right-click usage of custom Fireball charges.
 */
public final class FireballItemListener implements Listener {

    public FireballItemListener(ProjectLinearity plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!ItemRegistry.isFireball(stack)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        launchFireball(player);
        consumeOne(player, stack);
    }

    private void launchFireball(Player player) {
        Vector direction = player.getLocation().getDirection().clone();
        if (direction.lengthSquared() == 0) {
            direction = new Vector(0, 1, 0);
        }
        direction.normalize().multiply(1.5);
        Vector finalDirection = direction;
        player.getWorld().spawn(player.getEyeLocation().add(direction.clone().multiply(0.5)), Fireball.class, fireball -> {
            fireball.setShooter(player);
            fireball.setYield(2f);
            fireball.setIsIncendiary(true);
            fireball.setDirection(finalDirection);
            fireball.setVelocity(finalDirection);
        });
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.6f, 0.7f);
    }

    private void consumeOne(Player player, ItemStack stack) {
        int remaining = stack.getAmount() - 1;
        if (remaining > 0) {
            stack.setAmount(remaining);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
