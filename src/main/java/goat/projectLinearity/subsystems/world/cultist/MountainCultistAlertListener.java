package goat.projectLinearity.subsystems.world.cultist;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

/**
 * Handles auxiliary alert triggers for Mountain Cultists when dealing with invisible players.
 */
public final class MountainCultistAlertListener implements Listener {

    private final CultistPopulationManager populationManager;
    private final MountainCultistBehaviour behaviour;

    public MountainCultistAlertListener(CultistPopulationManager populationManager, MountainCultistBehaviour behaviour) {
        this.populationManager = Objects.requireNonNull(populationManager, "populationManager");
        this.behaviour = Objects.requireNonNull(behaviour, "behaviour");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCultistDamaged(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!populationManager.isCultistEntity(victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (!behaviour.isNight(victim.getWorld())) {
            return;
        }
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY) && !player.isInvisible()) {
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(victim);
        if (npc == null) {
            return;
        }
        behaviour.alertCultist(npc, player, "invisible attack");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY) && !player.isInvisible()) {
            return;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null || !behaviour.isNight(location.getWorld())) {
            return;
        }
        behaviour.alertCultistsNear(player, location, 5.0, "invisible player hurt");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY) && !player.isInvisible()) {
            return;
        }
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null || !behaviour.isNight(location.getWorld())) {
            return;
        }
        behaviour.alertCultistsNear(player, location, 5.0, "invisible player broke block");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY) && !player.isInvisible()) {
            return;
        }
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null || !behaviour.isNight(location.getWorld())) {
            return;
        }
        behaviour.alertCultistsNear(player, location, 5.0, "invisible player placed block");
    }
}
