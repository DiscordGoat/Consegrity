package goat.projectLinearity.util;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Applies gameplay side effects for active curses.
 */
public final class CurseEffectController implements Listener {

    private static final int POTION_REFRESH_TICKS = 120;

    private static final CurseRegistry.Curse CURSE_FREEZING = requireCurse("curse_of_freezing");
    private static final CurseRegistry.Curse CURSE_MICROWAVING = requireCurse("curse_of_microwaving");
    private static final CurseRegistry.Curse CURSE_SUFFOCATION = requireCurse("curse_of_suffocation");
    private static final CurseRegistry.Curse CURSE_INSOMNIA = requireCurse("curse_of_insomnia");
    private static final CurseRegistry.Curse CURSE_SHATTERING = requireCurse("curse_of_shattering");
    private static final CurseRegistry.Curse CURSE_CLUMSINESS = requireCurse("curse_of_clumsiness");
    private static final CurseRegistry.Curse CURSE_SILENCE = requireCurse("curse_of_silence");
    private static final CurseRegistry.Curse CURSE_FRAILTY = requireCurse("curse_of_frailty");
    private static final CurseRegistry.Curse CURSE_ANOREXIA = requireCurse("curse_of_anorexia");
    private static final CurseRegistry.Curse CURSE_GLUTTONY = requireCurse("curse_of_gluttony");
    private static final CurseRegistry.Curse CURSE_WEAKNESS = requireCurse("curse_of_weakness");
    private static final CurseRegistry.Curse CURSE_SLOWNESS = requireCurse("curse_of_slowness");
    private static final CurseRegistry.Curse CURSE_FATIGUE = requireCurse("curse_of_fatigue");
    private static final CurseRegistry.Curse CURSE_BLINDNESS = requireCurse("curse_of_blindness");
    private static final CurseRegistry.Curse CURSE_PEACE = requireCurse("curse_of_peace");

    private final ProjectLinearity plugin;
    private final CurseManager curseManager;
    private final MiningOxygenManager oxygenManager;
    private final SidebarManager sidebarManager;

    private final Map<UUID, Double> frailtyOriginalHealth = new HashMap<>();
    private final Map<UUID, Long> peaceMessageCooldown = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask tickTask;

    public CurseEffectController(ProjectLinearity plugin,
                                 CurseManager curseManager,
                                 MiningOxygenManager oxygenManager,
                                 SidebarManager sidebarManager) {
        this.plugin = plugin;
        this.curseManager = curseManager;
        this.oxygenManager = oxygenManager;
        this.sidebarManager = sidebarManager;
    }

    public void startup() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllPlayers, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        restoreFrailtyForAll();
        frailtyOriginalHealth.clear();
        peaceMessageCooldown.clear();
    }

    private void tickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPersistentEffects(player);
        }
    }

    private void applyPersistentEffects(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (hasCurse(player, CURSE_FREEZING)) {
            sidebarManager.setTemperature(player, -40.0);
        } else if (hasCurse(player, CURSE_MICROWAVING)) {
            sidebarManager.setTemperature(player, 140.0);
        }

        if (hasCurse(player, CURSE_SUFFOCATION) && oxygenManager.getOxygen(player) > 0) {
            oxygenManager.setOxygen(player, 0, false);
        }

        if (hasCurse(player, CURSE_SILENCE)) {
            player.stopAllSounds();
        }

        applyPotion(player, PotionEffectType.HUNGER, 1, hasCurse(player, CURSE_GLUTTONY));
        applyPotion(player, PotionEffectType.WEAKNESS, 1, hasCurse(player, CURSE_WEAKNESS));
        applyPotion(player, PotionEffectType.SLOWNESS, 1, hasCurse(player, CURSE_SLOWNESS));
        applyPotion(player, PotionEffectType.MINING_FATIGUE, 2, hasCurse(player, CURSE_FATIGUE));
        applyPotion(player, PotionEffectType.BLINDNESS, 0, hasCurse(player, CURSE_BLINDNESS));

        applyFrailty(player, hasCurse(player, CURSE_FRAILTY));

        if (!hasCurse(player, CURSE_PEACE)) {
            peaceMessageCooldown.remove(player.getUniqueId());
        }
    }

    private void applyPotion(Player player, PotionEffectType type, int amplifier, boolean active) {
        if (active) {
            PotionEffect current = player.getPotionEffect(type);
            if (current == null || current.getAmplifier() != amplifier || current.getDuration() < POTION_REFRESH_TICKS / 2) {
                player.addPotionEffect(new PotionEffect(type, POTION_REFRESH_TICKS, amplifier, true, false, false));
            }
        } else if (player.hasPotionEffect(type)) {
            player.removePotionEffect(type);
        }
    }

    private void applyFrailty(Player player, boolean active) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (active) {
            frailtyOriginalHealth.computeIfAbsent(uuid, id -> attribute.getBaseValue());
            double original = frailtyOriginalHealth.get(uuid);
            double target = Math.max(2.0, original / 2.0);
            if (Math.abs(attribute.getBaseValue() - target) > 0.01) {
                attribute.setBaseValue(target);
                if (player.getHealth() > target) {
                    player.setHealth(target);
                }
            }
        } else {
            Double original = frailtyOriginalHealth.remove(uuid);
            if (original != null && Math.abs(attribute.getBaseValue() - original) > 0.01) {
                attribute.setBaseValue(original);
                if (player.getHealth() > original) {
                    player.setHealth(original);
                }
            }
        }
    }

    private void restoreFrailtyForAll() {
        for (Map.Entry<UUID, Double> entry : frailtyOriginalHealth.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                attribute.setBaseValue(entry.getValue());
                if (player.getHealth() > entry.getValue()) {
                    player.setHealth(entry.getValue());
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (hasCurse(player, CURSE_INSOMNIA)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "A lingering curse stops you from falling asleep.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!hasCurse(player, CURSE_SHATTERING)) {
            return;
        }
        int damage = Math.max(1, event.getDamage() * 4);
        event.setDamage(damage);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasCurse(player, CURSE_CLUMSINESS)) {
            return;
        }
        if (event.getFinalDamage() <= 0) {
            return;
        }
        dropRandomInventoryItem(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!hasCurse(player, CURSE_PEACE)) {
            return;
        }
        Entity victim = event.getEntity();
        if (!(victim instanceof LivingEntity)) {
            return;
        }
        if (!(victim instanceof Monster)) {
            return;
        }
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        long nextAllowed = peaceMessageCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now >= nextAllowed) {
            player.sendMessage(ChatColor.RED + "Your curse of peace stops you from harming monsters.");
            peaceMessageCooldown.put(player.getUniqueId(), now + 1500L);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasCurse(player, CURSE_ANOREXIA)) {
            return;
        }
        ItemStack item = event.getItem();
        int delta = event.getFoodLevel() - player.getFoodLevel();
        if (delta <= 0 || item == null || !item.getType().isEdible()) {
            return;
        }
        event.setCancelled(true);
        double heal = delta * 0.5D;
        healPlayer(player, heal);
        player.sendMessage(ChatColor.YELLOW + "The curse twists nourishment into vitality instead of satiation.");
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!hasCurse(player, CURSE_ANOREXIA)) {
            return;
        }
        ItemStack consumed = event.getItem();
        if (consumed == null || !consumed.getType().isEdible()) {
            return;
        }
        // Remove saturation granted by the food item since hunger was cancelled.
        player.setSaturation(Math.max(0.0F, player.getSaturation() - 2.0F));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applyPersistentEffects(event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applyPersistentEffects(event.getPlayer()));
    }

    private void dropRandomInventoryItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<Integer> occupiedSlots = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                occupiedSlots.add(slot);
            }
        }
        if (inventory.getSize() > 40) {
            ItemStack offhand = inventory.getItem(40);
            if (offhand != null && offhand.getType() != Material.AIR) {
                occupiedSlots.add(40);
            }
        }
        if (occupiedSlots.isEmpty()) {
            return;
        }
        int chosenIndex = occupiedSlots.get(random.nextInt(occupiedSlots.size()));
        ItemStack stack = inventory.getItem(chosenIndex);
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        ItemStack drop = stack.clone();
        drop.setAmount(1);

        int newAmount = stack.getAmount() - 1;
        if (newAmount <= 0) {
            inventory.clear(chosenIndex);
        } else {
            stack.setAmount(newAmount);
        }

        player.getWorld().dropItemNaturally(player.getLocation(), drop);
        player.sendMessage(ChatColor.RED + "You fumble and drop an item due to the curse of clumsiness!");
    }

    private void healPlayer(Player player, double amount) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = attr != null ? attr.getValue() : player.getHealthScale();
        if (max <= 0) {
            max = 20.0;
        }
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    private boolean hasCurse(Player player, CurseRegistry.Curse curse) {
        return curse != null && curseManager.hasCurse(player, curse);
    }

    private static CurseRegistry.Curse requireCurse(String id) {
        return Objects.requireNonNull(CurseRegistry.getCurse(id), "Missing curse definition for " + id);
    }
}
