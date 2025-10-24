package goat.projectLinearity.commands;

import goat.projectLinearity.subsystems.world.loot.InventorySerializer;
import goat.projectLinearity.subsystems.world.loot.LootRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Developer-only helper command that captures the next chest inventory a player opens
 * and writes it to disk for later curation.
 */
public final class SaveInventoryCommand implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private final LootRegistry lootRegistry;
    private final Map<UUID, PendingCapture> pendingCaptures = new HashMap<>();

    public SaveInventoryCommand(JavaPlugin plugin, LootRegistry lootRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lootRegistry = Objects.requireNonNull(lootRegistry, "lootRegistry");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }
        if (!player.hasPermission("consegrity.dev")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <structure> <name>");
            return true;
        }

        LootRegistry.StructureLootDefinition definition = resolveDefinition(args[0]);
        if (definition == null) {
            player.sendMessage(ChatColor.RED + "Unknown structure '" + args[0] + "'.");
            return true;
        }

        String sanitizedName = InventorySerializer.sanitizeFileName(args[1]);
        if (sanitizedName == null) {
            player.sendMessage(ChatColor.RED + "Name must contain letters, numbers, underscores, or hyphens.");
            return true;
        }

        pendingCaptures.put(player.getUniqueId(), new PendingCapture(definition, sanitizedName));
        player.sendMessage(ChatColor.GRAY + "Saving next chest as " + ChatColor.YELLOW + sanitizedName + ChatColor.GRAY +
                " for " + ChatColor.YELLOW + definition.getDirectoryName() + ChatColor.GRAY + ". Open the target chest to capture.");
        return true;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        PendingCapture capture = pendingCaptures.get(player.getUniqueId());
        if (capture == null) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof org.bukkit.block.Chest) && !(holder instanceof org.bukkit.block.DoubleChest)) {
            player.sendMessage(ChatColor.RED + "Next chest not captured: that inventory is not a chest.");
            return;
        }

        pendingCaptures.remove(player.getUniqueId());

        ItemStack[] snapshot = cloneContents(inventory);
        File dir = new File(plugin.getDataFolder(), "structureloot/" + capture.definition().getDirectoryName());
        if (!dir.exists() && !dir.mkdirs()) {
            player.sendMessage(ChatColor.RED + "Failed to create target directory. Check console for details.");
            pendingCaptures.put(player.getUniqueId(), capture);
            return;
        }

        File target = new File(dir, capture.fileName() + ".yml");
        if (target.exists()) {
            player.sendMessage(ChatColor.RED + "A file named '" + target.getName() + "' already exists. Delete or rename it first.");
            pendingCaptures.put(player.getUniqueId(), capture);
            return;
        }

        try {
            InventorySerializer.save(snapshot, target);
            lootRegistry.reload(capture.definition().getPrimaryKey());
            player.sendMessage(ChatColor.GREEN + "Saved chest inventory to " + target.getName() + ".");
        } catch (IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to save inventory snapshot", ex);
            player.sendMessage(ChatColor.RED + "Failed to save inventory. Check console for details.");
            pendingCaptures.put(player.getUniqueId(), capture);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            Set<String> suggestions = new LinkedHashSet<>();
            for (LootRegistry.StructureLootDefinition def : lootRegistry.getDefinitions()) {
                suggestions.add(def.getPrimaryKey());
                suggestions.add(def.getDirectoryName());
            }
            List<String> completions = new ArrayList<>();
            for (String option : suggestions) {
                if (option.toLowerCase(Locale.ROOT).startsWith(partial)) {
                    completions.add(option);
                }
            }
            return completions;
        }
        if (args.length == 2) {
            return java.util.Collections.emptyList();
        }
        return null;
    }

    private LootRegistry.StructureLootDefinition resolveDefinition(String input) {
        if (input == null) return null;
        LootRegistry.StructureLootDefinition direct = lootRegistry.get(input);
        if (direct != null) {
            return direct;
        }
        String normalized = input.replaceAll("[^A-Za-z0-9]", "");
        for (LootRegistry.StructureLootDefinition definition : lootRegistry.getDefinitions()) {
            if (definition.getDirectoryName().equalsIgnoreCase(input) ||
                definition.getDirectoryName().replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(normalized)) {
                return definition;
            }
        }
        return null;
    }

    private ItemStack[] cloneContents(Inventory inventory) {
        ItemStack[] source = inventory.getContents();
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            ItemStack stack = source[i];
            clone[i] = stack == null ? null : stack.clone();
        }
        return clone;
    }

    private record PendingCapture(LootRegistry.StructureLootDefinition definition, String fileName) { }
}
