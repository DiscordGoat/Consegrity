package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionItemData;
import goat.projectLinearity.subsystems.mechanics.CustomDurabilityManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Handles right-click usage and splash behaviour for custom potions.
 */
public final class PotionUsageListener implements Listener {

    private final ProjectLinearity plugin;
    private final CustomPotionEffectManager effectManager;
    private final CustomDurabilityManager durabilityManager;

    public PotionUsageListener(ProjectLinearity plugin, CustomPotionEffectManager effectManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        this.durabilityManager = CustomDurabilityManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }

        Optional<PotionItemData> maybeData = PotionRegistry.PotionItemData.from(stack);
        if (maybeData.isEmpty()) {
            return;
        }

        PotionItemData data = maybeData.get();
        if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (PotionRegistry.PotionItemData.from(main).isPresent()) {
                return;
            }
        }

        stack = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }

        int current = durabilityManager.getCurrentDurability(stack);
        if (current <= 0) {
            player.sendMessage(ChatColor.RED + "That potion is spent.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (data.isSplash()) {
            handleSplashUse(player, stack, data, hand);
        } else {
            handleDrinkUse(player, stack, data, hand);
        }
    }

    private void handleDrinkUse(Player player, ItemStack stack, PotionItemData data, EquipmentSlot hand) {
        int remaining = consumeCharge(player, stack, data, hand);
        Sound drinkSound = Sound.ENTITY_GENERIC_DRINK;
        float volume = 1.0f;
        float pitch = 1.15f;
        if ("instant_air".equalsIgnoreCase(data.getDefinition().getId())) {
            drinkSound = Sound.ITEM_TRIDENT_RIPTIDE_1;
            volume = 1.2f;
            pitch = 1.0f;
        }
        player.playSound(player.getLocation(), drinkSound, volume, pitch);
        swing(player, hand);

        effectManager.applyEffect(player, data);

        if (remaining <= 0) {
            handleItemDepleted(player, hand);
        }
    }

    private void handleSplashUse(Player player, ItemStack stack, PotionItemData data, EquipmentSlot hand) {
        int remaining = consumeCharge(player, stack, data, hand);
        player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 1.0f);
        swing(player, hand);

        ThrownPotion projectile = player.launchProjectile(ThrownPotion.class);
        if (projectile == null) {
            return;
        }

        ItemStack thrownItem = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) thrownItem.getItemMeta();
        if (meta != null) {
            meta.setColor(data.getBukkitColor());
            PotionRegistry.writePotionData(meta, data.getDefinition(), data.getBrewType(), data.getEnchantTier(), true);
            thrownItem.setItemMeta(meta);
        }
        projectile.setItem(thrownItem);
        projectile.setShooter(player);

        if (remaining <= 0) {
            handleItemDepleted(player, hand);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ItemStack item = event.getPotion().getItem();
        Optional<PotionItemData> maybeData = PotionRegistry.PotionItemData.from(item);
        if (maybeData.isEmpty()) {
            return;
        }

        PotionItemData data = maybeData.get();
        event.setCancelled(true);

        List<String> hits = new ArrayList<>();
        for (LivingEntity entity : event.getAffectedEntities()) {
            effectManager.applyEffect(entity, data);
            hits.add(formatEntityName(entity));
        }

        Player thrower = null;
        if (event.getPotion().getShooter() instanceof Player shooter) {
            thrower = shooter;
        }

        if ("instant_lightning".equalsIgnoreCase(data.getDefinition().getId())) {
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
        }

        if (thrower != null) {
            if (hits.isEmpty()) {
                thrower.sendMessage(ChatColor.DARK_AQUA + "[Potions] " + ChatColor.GRAY + "Your splash potion fizzles without hitting anyone.");
            } else {
                String hitList = String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, hits);
                thrower.sendMessage(ChatColor.DARK_AQUA + "[Potions] " + ChatColor.YELLOW + data.getDisplayName() + ChatColor.GRAY
                        + " splashed " + ChatColor.YELLOW + hitList + ChatColor.GRAY + " for "
                        + ChatColor.YELLOW + data.getFinalStats().durationSeconds() + ChatColor.GRAY + "s at potency "
                        + ChatColor.YELLOW + data.getFinalStats().potency() + ChatColor.GRAY + ".");
            }
        }

        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
        event.getEntity().remove();
    }

    private int consumeCharge(Player player, ItemStack stack, PotionItemData data, EquipmentSlot hand) {
        int max = durabilityManager.getMaxDurability(stack);
        if (max <= 0) {
            max = Math.max(1, data.getFinalStats().charges());
        }
        int current = Math.max(0, durabilityManager.getCurrentDurability(stack) - 1);
        current = Math.min(current, max);
        durabilityManager.setCustomDurability(stack, current, max);
        PotionRegistry.refreshPotionLore(stack);
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(stack);
        } else {
            player.getInventory().setItemInOffHand(stack);
        }
        player.updateInventory();
        return current;
    }

    private void handleItemDepleted(Player player, EquipmentSlot hand) {
        player.sendMessage(ChatColor.GRAY + "The potion bottle crumbles after exhausting its charges.");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.4f);
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInOffHand(null);
        }
        player.updateInventory();
    }

    private void swing(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.HAND) {
            player.swingMainHand();
        } else {
            player.swingOffHand();
        }
    }

    private String formatEntityName(LivingEntity entity) {
        if (entity instanceof Player targetPlayer) {
            return targetPlayer.getName();
        }
        String raw = entity.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            if (i < parts.length - 1) {
                builder.append(' ');
            }
        }
        return builder.length() == 0 ? raw : builder.toString();
    }
}
