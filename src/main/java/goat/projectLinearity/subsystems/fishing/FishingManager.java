package goat.projectLinearity.subsystems.fishing;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles fishing roll outcomes and sea creature encounters.
 */
public final class FishingManager implements Listener {

    private static final double SEA_CREATURE_CHANCE = 1.0; // 100% for testing
    private static final double TREASURE_CHANCE = 0.05;
    private static final double JUNK_CHANCE = 0.05;
    private static final List<Material> DEFAULT_FISH = List.of(
            Material.COD,
            Material.SALMON,
            Material.TROPICAL_FISH
    );

    private final ProjectLinearity plugin;
    private final SeaCreatureRegistry seaCreatureRegistry;

    public FishingManager(ProjectLinearity plugin, SeaCreatureRegistry seaCreatureRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.seaCreatureRegistry = Objects.requireNonNull(seaCreatureRegistry, "seaCreatureRegistry");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        if (player == null || hook == null || hook.getWorld() == null) {
            return;
        }

        event.setExpToDrop(0);

        if (attemptSeaCreature(event, player, hook)) {
            return;
        }
        if (attemptTreasure(event, player, hook)) {
            return;
        }
        if (attemptJunk(event, player, hook)) {
            return;
        }
        awardDefaultFish(event, hook);
    }

    private boolean attemptSeaCreature(PlayerFishEvent event, Player player, FishHook hook) {
        if (!roll(SEA_CREATURE_CHANCE)) {
            return false;
        }
        Location spawnLocation = hook.getLocation();
        var definitionOpt = seaCreatureRegistry.randomDefinition();
        if (definitionOpt.isEmpty()) {
            return false;
        }
        var definition = definitionOpt.get();
        var entityOpt = seaCreatureRegistry.spawnSeaCreature(definition, spawnLocation, player);
        if (entityOpt.isEmpty()) {
            return false;
        }

        SeaCreatureEntity seaCreature = entityOpt.get();
        seaCreature.assignTarget(player);
        seaCreature.launchTowards(player, spawnLocation);

        Entity caught = event.getCaught();
        if (caught != null) {
            caught.remove();
            event.setCancelled(true);
        }
        event.setCancelled(true);

        spawnLocation.getWorld().spawnParticle(Particle.FISHING, spawnLocation, 30, 0.5, 0.5, 0.5, 0.1);
        spawnLocation.getWorld().playSound(spawnLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
        player.sendMessage(ChatColor.AQUA + "A " + definition.displayName() + " is approaching!");
        return true;
    }

    private boolean attemptTreasure(PlayerFishEvent event, Player player, FishHook hook) {
        if (!roll(TREASURE_CHANCE)) {
            return false;
        }
        ItemStack treasure = new ItemStack(Material.HEART_OF_THE_SEA);
        replaceCaughtItem(event, hook, treasure);
        player.sendMessage(ChatColor.GOLD + "You fished up a treasure!");
        return true;
    }

    private boolean attemptJunk(PlayerFishEvent event, Player player, FishHook hook) {
        if (!roll(JUNK_CHANCE)) {
            return false;
        }
        ItemStack junk = new ItemStack(Material.KELP);
        replaceCaughtItem(event, hook, junk);
        player.sendMessage(ChatColor.GRAY + "Just some junk this time.");
        return true;
    }

    private void awardDefaultFish(PlayerFishEvent event, FishHook hook) {
        Material fishMaterial = DEFAULT_FISH.get(ThreadLocalRandom.current().nextInt(DEFAULT_FISH.size()));
        replaceCaughtItem(event, hook, new ItemStack(fishMaterial));
    }

    private void replaceCaughtItem(PlayerFishEvent event, FishHook hook, ItemStack stack) {
        Entity caught = event.getCaught();
        if (caught instanceof Item item) {
            item.setItemStack(stack);
            return;
        }
        hook.getWorld().dropItem(hook.getLocation(), stack);
    }

    private boolean roll(double chance) {
        if (chance <= 0.0) {
            return false;
        }
        if (chance >= 1.0) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
