package goat.projectLinearity.subsystems.farming;

import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CropHarvestListener implements Listener {

    private static final Set<Material> TARGETED_CROPS = EnumSet.of(
            Material.CARROTS,
            Material.POTATOES,
            Material.WHEAT,
            Material.BEETROOTS
    );
    private static final double ROOT_VEGETABLE_DROP_CHANCE = 0.30; // 70% reduction versus default

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropDrop(BlockDropItemEvent event) {
        BlockState state = event.getBlockState();
        Material type = state.getType();
        if (!TARGETED_CROPS.contains(type)) {
            return;
        }

        if (!isMature(state)) {
            return;
        }

        List<Item> originalDrops = new ArrayList<>(event.getItems());
        for (Item item : originalDrops) {
            item.remove();
        }
        event.getItems().clear();

        World world = state.getWorld();
        Location dropLocation = state.getLocation().add(0.5, 0.25, 0.5);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        switch (type) {
            case CARROTS -> {
                if (shouldDrop(random)) {
                    drop(world, dropLocation, new ItemStack(Material.CARROT, random.nextInt(1, 4)), event);
                }
                if (shouldDrop(random)) {
                    drop(world, dropLocation, ItemRegistry.getTinyCarrot(), event);
                }
            }
            case POTATOES -> {
                if (shouldDrop(random)) {
                    drop(world, dropLocation, new ItemStack(Material.POTATO, random.nextInt(1, 4)), event);
                }
                if (shouldDrop(random)) {
                    drop(world, dropLocation, ItemRegistry.getBabyPotato(), event);
                }
            }
            case WHEAT -> {
                drop(world, dropLocation, new ItemStack(Material.WHEAT, 1), event);
                drop(world, dropLocation, new ItemStack(Material.WHEAT_SEEDS, 1), event);
            }
            case BEETROOTS -> {
                drop(world, dropLocation, new ItemStack(Material.BEETROOT, random.nextInt(1, 4)), event);
                drop(world, dropLocation, new ItemStack(Material.BEETROOT_SEEDS, 1), event);
            }
            default -> {
            }
        }
    }

    private boolean isMature(BlockState state) {
        if (state == null) {
            return false;
        }
        if (!(state.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private void drop(World world, Location location, ItemStack stack, BlockDropItemEvent event) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        Item entity = world.dropItem(location, stack);
        event.getItems().add(entity);
    }

    private boolean shouldDrop(ThreadLocalRandom random) {
        return random.nextDouble() < ROOT_VEGETABLE_DROP_CHANCE;
    }
}
