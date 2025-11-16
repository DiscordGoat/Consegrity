package goat.projectLinearity.subsystems.world.loot;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;

/**
 * Logs loot template information whenever a populated chest inventory is opened.
 */
public final class LootChestOpenListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey templateIdKey;
    private final NamespacedKey structureIdKey;

    public LootChestOpenListener(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.templateIdKey = new NamespacedKey(plugin, "loot_template_id");
        this.structureIdKey = new NamespacedKey(plugin, "loot_structure_key");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        TemplateInfo info = resolveTemplateInfo(event.getInventory().getHolder());
        if (info == null) {
            return;
        }
        HumanEntity viewer = event.getPlayer();
        String viewerName = (viewer instanceof Player player) ? player.getName() : viewer.getName();
        Location loc = info.location();
        String structure = info.structureKey().isBlank() ? "unknown" : info.structureKey();
        plugin.getLogger().info(() -> String.format(Locale.ROOT,
                "[LootChest] %s opened chest at %s (%d,%d,%d) using template %s (structure=%s)",
                viewerName,
                loc.getWorld() == null ? "UnknownWorld" : loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                info.templateId(),
                structure));
    }

    private TemplateInfo resolveTemplateInfo(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            // Convert InventoryHolder to Chest if possible
            if (doubleChest.getLeftSide() instanceof Chest leftChest) {
                TemplateInfo left = extractTemplate(leftChest);
                if (left != null) {
                    return left;
                }
            }
            if (doubleChest.getRightSide() instanceof Chest rightChest) {
                return extractTemplate(rightChest);
            }
            return null;
        }
        if (holder instanceof Chest chest) {
            return extractTemplate(chest);
        }
        return null;
    }

    private TemplateInfo extractTemplate(Chest chest) {
        if (chest == null) {
            return null;
        }
        return extractTemplate(chest.getBlock());
    }

    private TemplateInfo extractTemplate(Block block) {
        if (block == null) {
            return null;
        }
        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) {
            return null;
        }
        PersistentDataContainer container = tileState.getPersistentDataContainer();
        String template = container.get(templateIdKey, PersistentDataType.STRING);
        if (template == null || template.isBlank()) {
            return null;
        }
        String structure = container.get(structureIdKey, PersistentDataType.STRING);
        if (structure == null) {
            structure = "";
        }
        return new TemplateInfo(template, structure, block.getLocation());
    }

    private record TemplateInfo(String templateId, String structureKey, Location location) { }
}
