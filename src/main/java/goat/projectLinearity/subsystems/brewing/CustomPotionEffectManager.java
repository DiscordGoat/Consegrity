package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.FinalStats;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionItemData;
import goat.projectLinearity.subsystems.mechanics.DamageDisplayManager;
import goat.projectLinearity.subsystems.mechanics.DamageDisplayManager.HighFrequencyStyle;
import goat.projectLinearity.subsystems.mechanics.TablistManager;
import goat.projectLinearity.subsystems.world.samurai.SamuraiPopulationManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks custom potion effects applied by the custom potion system. Effects currently only
 * influence HUD / debug output but maintain groundwork for future gameplay hooks.
 */
public final class CustomPotionEffectManager implements Listener {

    private static final int TICK_INTERVAL = 20; // 1 second
    private static final Set<String> BENEFICIAL_EFFECTS = Set.of(
            "haste",
            "swiftness",
            "night_vision",
            "invisibility",
            "strength",
            "regeneration",
            "resistance",
            "fire_resistance",
            "water_breathing",
            "leaping",
            "instant_health",
            "instant_cooling",
            "instant_warming",
            "instant_oxygen",
            "instant_air",
            "oxygen_recovery",
            "saturation",
            "glowing",
            "levitation",
            "luck",
            "slow_falling",
            "conduit_power",
            "dolphins_grace",
            "charismatic_bartering",
            "frostbite",
            "heatstroke",
            "instant_lightning",
            "well_balanced_meal",
            "absorption",
            "health_boost"
    );
    private static final Set<String> HARMFUL_EFFECTS = Set.of(
            "hunger",
            "weakness",
            "blindness",
            "nausea",
            "slowness",
            "mining_fatigue",
            "poison",
            "instant_damage",
            "wither"
    );
    private static String buildEffectKey(String definitionId, BrewType brewType) {
        if (definitionId == null || brewType == null) {
            return "";
        }
        return definitionId.toLowerCase(Locale.ENGLISH) + ":" + brewType.name();
    }
    private final ProjectLinearity plugin;
    private final TablistManager tablistManager;
    private final DamageDisplayManager damageDisplayManager;
    private final Map<UUID, Map<String, ActiveEffect>> playerEffects = new HashMap<>();
    private final Map<UUID, Map<String, ActiveEffect>> entityEffects = new HashMap<>();
    private final Map<UUID, DamageTask> damageTasks = new HashMap<>();
    private final Map<UUID, Double> healthBoostOriginals = new HashMap<>();
    private final Map<UUID, Integer> slowFallingAirTicks = new HashMap<>();
    private final Set<UUID> charismaticBarterPlayers;
    private final BukkitTask tickTask;
    private final NamespacedKey luckFortuneKey;
    private final NamespacedKey luckLootKey;
    private final File persistenceFile;

    public enum CleanseMode {
        POSITIVE,
        NEGATIVE,
        ALL
    }

    public static final class CleanseResult {
        private final int customRemoved;

        public CleanseResult(int customRemoved) {
            this.customRemoved = customRemoved;
        }

        public int getCustomRemoved() {
            return customRemoved;
        }

        public int getTotalRemoved() {
            return customRemoved;
        }
    }

    private enum CleanseCategory {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    public CustomPotionEffectManager(ProjectLinearity plugin, TablistManager tablistManager, DamageDisplayManager damageDisplayManager) {
        this.plugin = plugin;
        this.tablistManager = tablistManager;
        this.damageDisplayManager = damageDisplayManager;
        this.charismaticBarterPlayers = ConcurrentHashMap.newKeySet();
        this.luckFortuneKey = new NamespacedKey(plugin, "luck_fortune_level");
        this.luckLootKey = new NamespacedKey(plugin, "luck_loot_level");
        this.persistenceFile = new File(plugin.getDataFolder(), "custom_effects.yml");

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadPersistedEffects();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                restorePlayerState(online);
            }
        });

        this.tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickEffects();
            }
        }.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    private void handleEffectExpiration(LivingEntity living, ActiveEffect effect) {
        if (living == null || effect == null) return;
        String id = effect.definition.getId();
        if (living instanceof Player player) {
            if ("leaping".equalsIgnoreCase(id)) {
                BonusJumpManager jumpManager = plugin.getBonusJumpManager();
                if (jumpManager != null) {
                    jumpManager.clearPlayer(player);
                }
            }
            if ("luck".equalsIgnoreCase(id)) {
                revertLuckEnhancements(player);
            }
            if ("charismatic_bartering".equalsIgnoreCase(id)) {
                clearCharismaticBonus(player);
            }
            slowFallingAirTicks.remove(player.getUniqueId());
        }
        if ("absorption".equalsIgnoreCase(id)) {
            clearAbsorption(living);
        }
        if ("health_boost".equalsIgnoreCase(id)) {
            clearHealthBoost(living);
        } else if ("levitation".equalsIgnoreCase(id)) {
            clearLevitationSafety(living);
        }
        if ("poison".equalsIgnoreCase(id) || "wither".equalsIgnoreCase(id)) {
            cancelDamageTask(living.getUniqueId());
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (DamageTask task : damageTasks.values()) {
            task.cancel();
        }
        for (UUID uuid : new ArrayList<>(healthBoostOriginals.keySet())) {
            LivingEntity living = getLivingEntity(uuid);
            if (living != null) {
                clearHealthBoost(living);
            } else {
                healthBoostOriginals.remove(uuid);
            }
        }
        damageTasks.clear();
        healthBoostOriginals.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            clearLevitationSafety(online);
            revertLuckEnhancements(online);
        }
        charismaticBarterPlayers.clear();
        slowFallingAirTicks.clear();
        savePersistedEffects();
        playerEffects.clear();
        entityEffects.clear();
    }

    public void applyEffect(LivingEntity target, PotionItemData data) {
        if (target == null || data == null) return;

        FinalStats stats = data.getFinalStats();
        int duration = Math.max(1, stats.durationSeconds());
        int potency = Math.max(1, stats.potency());

        String definitionId = data.getDefinition().getId();
        boolean isPoison = "poison".equalsIgnoreCase(definitionId);
        boolean isWither = "wither".equalsIgnoreCase(definitionId);
        DamageProfile damageProfile = null;
        if ((isPoison || isWither) && !(target instanceof Player) && isHostileTarget(target)) {
            damageProfile = isPoison ? createPoisonProfile(potency) : createWitherProfile(potency);
            duration = Math.max(1, damageProfile.durationSeconds());
        }
        if (isImmediateDefinition(definitionId)) {
            CustomPotionEffects.applyImmediate(data.getDefinition(), data.getBrewType(), potency, target);
            applyInstantEffectExtras(target, definitionId, potency, stats.durationSeconds());
            return;
        }

        if (target instanceof Player player) {
            cancelDamageTask(player.getUniqueId());
            Map<String, ActiveEffect> effects = playerEffects.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
            ActiveEffect effect = effects.computeIfAbsent(data.effectKey(), key -> new ActiveEffect(data.getDefinition(), data.getBrewType(), data.getEnchantTier(), data.getBukkitColor()));
            effect.refresh(duration, potency, player);
            if ("health_boost".equalsIgnoreCase(definitionId)) {
                applyHealthBoostBonus(player, potency);
            }
            if ("charismatic_bartering".equalsIgnoreCase(definitionId)) {
                applyCharismaticBonus(player);
            }
            if ("levitation".equalsIgnoreCase(definitionId)) {
                clearLevitationSafety(player);
            }
            applyInstantEffectExtras(player, definitionId, potency, stats.durationSeconds());
            tablistManager.refreshPlayer(player);
            if ("leaping".equals(definitionId)) {
                BonusJumpManager jumpManager = plugin.getBonusJumpManager();
                if (jumpManager != null) {
                    jumpManager.updatePlayer(player, potency);
                }
            }
        } else {
            UUID uuid = target.getUniqueId();
            cancelDamageTask(uuid);
            Map<String, ActiveEffect> effects = entityEffects.computeIfAbsent(uuid, id -> new HashMap<>());
            ActiveEffect effect = effects.computeIfAbsent(data.effectKey(), key -> new ActiveEffect(data.getDefinition(), data.getBrewType(), data.getEnchantTier(), data.getBukkitColor()));
            effect.refresh(duration, potency, target);
            if ("health_boost".equalsIgnoreCase(definitionId)) {
                applyHealthBoostBonus(target, potency);
            }
            applyInstantEffectExtras(target, definitionId, potency, duration);
            if ("levitation".equalsIgnoreCase(definitionId)) {
                clearLevitationSafety(target);
            }
            if (damageProfile != null) {
                startDamageTask(target, damageProfile);
            }
        }
    }

    public List<String> getTabLines(Player player) {
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null || effects.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        for (ActiveEffect effect : effects.values()) {
            lines.add(effect.tabLabel());
        }
        lines.sort(String::compareToIgnoreCase);
        return lines;
    }

    public List<Color> getActiveColors(Player player) {
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null || effects.isEmpty()) {
            return Collections.emptyList();
        }
        List<Color> colors = new ArrayList<>(effects.size());
        for (ActiveEffect effect : effects.values()) {
            colors.add(effect.color);
        }
        return colors;
    }

    public boolean hasEffect(Player player, String definitionId) {
        if (player == null || definitionId == null) return false;
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null) return false;
        for (ActiveEffect effect : effects.values()) {
            if (effect.definition.getId().equalsIgnoreCase(definitionId)) {
                return true;
            }
        }
        return false;
    }

    public int getEffectPotency(Player player, String definitionId) {
        if (player == null || definitionId == null) return 0;
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null) return 0;
        for (ActiveEffect effect : effects.values()) {
            if (effect.definition.getId().equalsIgnoreCase(definitionId)) {
                return Math.max(0, effect.getPotency());
            }
        }
        return 0;
    }

    public int getEffectPotency(LivingEntity entity, String definitionId) {
        if (entity == null || definitionId == null) return 0;
        if (entity instanceof Player player) {
            return getEffectPotency(player, definitionId);
        }
        Map<String, ActiveEffect> effects = entityEffects.get(entity.getUniqueId());
        if (effects == null) {
            return 0;
        }
        for (ActiveEffect effect : effects.values()) {
            if (effect.definition.getId().equalsIgnoreCase(definitionId)) {
                return Math.max(0, effect.getPotency());
            }
        }
        return 0;
    }

    private int getEffectRemainingSeconds(UUID uuid, String definitionId) {
        if (uuid == null || definitionId == null) {
            return 0;
        }
        Map<String, ActiveEffect> playerMap = playerEffects.get(uuid);
        if (playerMap != null) {
            for (ActiveEffect effect : playerMap.values()) {
                if (definitionId.equalsIgnoreCase(effect.definition.getId())) {
                    return Math.max(0, effect.remainingSeconds);
                }
            }
        }
        Map<String, ActiveEffect> entityMap = entityEffects.get(uuid);
        if (entityMap != null) {
            for (ActiveEffect effect : entityMap.values()) {
                if (definitionId.equalsIgnoreCase(effect.definition.getId())) {
                    return Math.max(0, effect.remainingSeconds);
                }
            }
        }
        return 0;
    }

    public CleanseResult cleanse(Player player, CleanseMode mode) {
        if (player == null || mode == null) {
            return new CleanseResult(0);
        }
        int customRemoved = cleanseCustomEffects(player, mode);
        tablistManager.refreshPlayer(player);
        return new CleanseResult(customRemoved);
    }

    public void applyCustomEffect(LivingEntity target, PotionDefinition definition, BrewType brewType, int durationSeconds, int potency, int enchantTier) {
        if (target == null || definition == null || brewType == null) {
            return;
        }
        int cappedDuration = Math.max(1, durationSeconds);
        int cappedPotency = Math.max(1, potency);
        int cappedEnchant = Math.max(0, enchantTier);

        String definitionId = definition.getId();
        if (isImmediateDefinition(definitionId)) {
            CustomPotionEffects.applyImmediate(definition, brewType, cappedPotency, target);
            return;
        }

        boolean isPoison = "poison".equalsIgnoreCase(definitionId);
        boolean isWither = "wither".equalsIgnoreCase(definitionId);
        DamageProfile profile = null;
        if ((isPoison || isWither) && !(target instanceof Player) && isHostileTarget(target)) {
            profile = isPoison ? createPoisonProfile(cappedPotency) : createWitherProfile(cappedPotency);
            cappedDuration = Math.max(1, profile.durationSeconds());
        }

        if (target instanceof Player player) {
            cancelDamageTask(player.getUniqueId());
            Map<String, ActiveEffect> effects = playerEffects.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
            String key = buildEffectKey(definition.getId(), brewType);
            ActiveEffect effect = effects.computeIfAbsent(key, ignored -> new ActiveEffect(definition, brewType, cappedEnchant, definition.getAccentBukkitColor()));
            effect.refresh(cappedDuration, cappedPotency, player);
            if ("health_boost".equalsIgnoreCase(definitionId)) {
                applyHealthBoostBonus(player, cappedPotency);
            }
            tablistManager.refreshPlayer(player);
            if ("leaping".equalsIgnoreCase(definitionId)) {
                BonusJumpManager jumpManager = plugin.getBonusJumpManager();
                if (jumpManager != null) {
                    jumpManager.updatePlayer(player, cappedPotency);
                }
            }
        } else {
            UUID uuid = target.getUniqueId();
            cancelDamageTask(uuid);
            Map<String, ActiveEffect> effects = entityEffects.computeIfAbsent(uuid, id -> new HashMap<>());
            String key = buildEffectKey(definition.getId(), brewType);
            ActiveEffect effect = effects.computeIfAbsent(key, ignored -> new ActiveEffect(definition, brewType, cappedEnchant, definition.getAccentBukkitColor()));
            effect.refresh(cappedDuration, cappedPotency, target);
            if ("health_boost".equalsIgnoreCase(definitionId)) {
                applyHealthBoostBonus(target, cappedPotency);
            }
            if (profile != null) {
                startDamageTask(target, profile);
            }
        }
    }

    private int cleanseCustomEffects(Player player, CleanseMode mode) {
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null || effects.isEmpty()) {
            return 0;
        }
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, ActiveEffect> entry : effects.entrySet()) {
            ActiveEffect effect = entry.getValue();
            CleanseCategory category = categorizeDefinition(effect.definition.getId());
            boolean remove = switch (mode) {
                case ALL -> true;
                case POSITIVE -> category == CleanseCategory.POSITIVE;
                case NEGATIVE -> category == CleanseCategory.NEGATIVE;
            };
            if (remove) {
                keysToRemove.add(entry.getKey());
            }
        }
        int removed = 0;
        for (String key : keysToRemove) {
            ActiveEffect effect = effects.remove(key);
            if (effect != null) {
                handleEffectExpiration(player, effect);
                removed++;
            }
        }
        if (effects.isEmpty()) {
            playerEffects.remove(player.getUniqueId());
        }
        return removed;
    }

    private CleanseCategory categorizeDefinition(String definitionId) {
        if (definitionId == null) {
            return CleanseCategory.NEUTRAL;
        }
        String id = definitionId.toLowerCase(Locale.ENGLISH);
        if (BENEFICIAL_EFFECTS.contains(id)) {
            return CleanseCategory.POSITIVE;
        }
        if (HARMFUL_EFFECTS.contains(id)) {
            return CleanseCategory.NEGATIVE;
        }
        return CleanseCategory.NEUTRAL;
    }

    private boolean isImmediateDefinition(String definitionId) {
        if (definitionId == null) {
            return false;
        }
        String id = definitionId.toLowerCase(Locale.ENGLISH);
        return switch (id) {
            case "instant_health", "instant_damage", "instant_cooling", "instant_warming", "instant_oxygen", "instant_air" -> true;
            case "instant_lightning" -> true;
            case "well_balanced_meal" -> true;
            default -> false;
        };
    }

    private DamageProfile createPoisonProfile(int potency) {
        int effectivePotency = Math.max(1, potency);
        int pulses = 20 * effectivePotency;
        int intervalTicks = 10; // 0.5s spacing
        double damagePerPulse = 1.0;
        return new DamageProfile("poison", pulses, intervalTicks, damagePerPulse, HighFrequencyStyle.POISON);
    }

    private DamageProfile createWitherProfile(int potency) {
        int effectivePotency = Math.max(1, potency);
        int pulses = 10 * effectivePotency;
        int intervalTicks = 3; // 0.15s spacing
        double damagePerPulse = 1.0;
        return new DamageProfile("wither", pulses, intervalTicks, damagePerPulse, HighFrequencyStyle.WITHER);
    }

    private void startDamageTask(LivingEntity target, DamageProfile profile) {
        if (target == null || profile == null) {
            return;
        }
        if (!isHostileTarget(target)) {
            return;
        }
        UUID uuid = target.getUniqueId();
        cancelDamageTask(uuid);
        DamageTask task = new DamageTask(uuid, profile);
        damageTasks.put(uuid, task);
        task.runTaskTimer(plugin, profile.intervalTicks(), profile.intervalTicks());
    }

    private void cancelDamageTask(UUID uuid) {
        if (uuid == null) {
            return;
        }
        DamageTask task = damageTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean hasActiveEffect(UUID uuid, String effectId) {
        if (effectId == null) {
            return false;
        }
        Map<String, ActiveEffect> playerMap = playerEffects.get(uuid);
        if (playerMap != null) {
            for (ActiveEffect effect : playerMap.values()) {
                if (effectId.equalsIgnoreCase(effect.definition.getId())) {
                    return true;
                }
            }
        }
        Map<String, ActiveEffect> entityMap = entityEffects.get(uuid);
        if (entityMap != null) {
            for (ActiveEffect effect : entityMap.values()) {
                if (effectId.equalsIgnoreCase(effect.definition.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHostileTarget(LivingEntity living) {
        if (living instanceof Monster) {
            return true;
        }
        SamuraiPopulationManager manager = plugin.getSamuraiPopulationManager();
        return manager != null && manager.isSamuraiEntity(living);
    }

    private void applyInstantEffectExtras(LivingEntity target, String definitionId, int potency, int baseDurationSeconds) {
        if (target == null || definitionId == null) {
            return;
        }
        String id = definitionId.toLowerCase(Locale.ENGLISH);
        switch (id) {
            case "well_balanced_meal" -> {
                if (target instanceof Player player) {
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                }
            }
            case "levitation" -> applyLevitationImpulse(target, Math.max(1, potency));
            case "luck" -> {
                if (target instanceof Player player) {
                    applyLuckEnhancements(player, potency);
                }
            }
            case "charismatic_bartering" -> {
                if (target instanceof Player player) {
                    applyCharismaticBonus(player);
                }
            }
        }
    }

    private void applyLevitationImpulse(LivingEntity target, int potency) {
        Vector velocity = target.getVelocity();
        double boost = 1.0 + (potency - 1) * 0.4;
        double newY = Math.min(velocity.getY() + boost, 2.0 + potency * 0.5);
        target.setVelocity(new Vector(velocity.getX(), newY, velocity.getZ()));

        ProjectLinearity plugin = ProjectLinearity.getInstance();
        if (target instanceof Player player) {
            player.setNoDamageTicks(0);
            player.setFallDistance(0f);
            if (plugin != null) {
                player.removeMetadata("pl_levitation_safe", plugin);
                player.setMetadata("pl_levitation_safe", new org.bukkit.metadata.FixedMetadataValue(plugin, Boolean.TRUE));
            }
        } else if (plugin != null && target.hasMetadata("pl_levitation_safe")) {
            target.removeMetadata("pl_levitation_safe", plugin);
        }
    }

    private void handlePlayerSlowFalling(Player player, int potency) {
        if (player.isOnGround()) {
            slowFallingAirTicks.remove(player.getUniqueId());
            return;
        }
        UUID uuid = player.getUniqueId();
        int ticks = slowFallingAirTicks.getOrDefault(uuid, 0) + 1;
        slowFallingAirTicks.put(uuid, ticks);
        if (ticks < 20) {
            return;
        }
        if (!isGroundWithin(player.getLocation(), 2)) {
            return;
        }
        player.setFallDistance(0f);
        Vector velocity = player.getVelocity();
        double descentLimit = -0.15;
        if (velocity.getY() < descentLimit) {
            player.setVelocity(new Vector(velocity.getX(), descentLimit, velocity.getZ()));
        }
    }

    private boolean isGroundWithin(Location loc, int distance) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int startY = loc.getBlockY();
        for (int i = 1; i <= distance; i++) {
            int y = startY - i;
            if (y < world.getMinHeight()) {
                break;
            }
            Material type = world.getBlockAt(x, y, z).getType();
            if (type.isSolid()) {
                return true;
            }
        }
        return false;
    }

    private void applyLuckEnhancements(Player player, int potency) {
        if (player == null || potency <= 0) {
            return;
        }
        for (ItemStack item : collectPlayerItems(player)) {
            enhanceLuckOnItem(item, potency);
        }
    }

    private void revertLuckEnhancements(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack item : collectPlayerItems(player)) {
            revertLuckOnItem(item);
        }
    }

    private ItemStack[] collectPlayerItems(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] base = inv.getContents();
        ItemStack[] armor = inv.getArmorContents();
        ItemStack[] extra = inv.getExtraContents();
        ItemStack offhand = inv.getItemInOffHand();
        int baseLen = base != null ? base.length : 0;
        int armorLen = armor != null ? armor.length : 0;
        int extraLen = extra != null ? extra.length : 0;
        int offhandLen = offhand != null ? 1 : 0;
        ItemStack[] all = new ItemStack[baseLen + armorLen + extraLen + offhandLen];
        int index = 0;
        if (baseLen > 0) {
            System.arraycopy(base, 0, all, index, baseLen);
            index += baseLen;
        }
        if (armorLen > 0) {
            System.arraycopy(armor, 0, all, index, armorLen);
            index += armorLen;
        }
        if (extraLen > 0) {
            System.arraycopy(extra, 0, all, index, extraLen);
            index += extraLen;
        }
        if (offhandLen == 1) {
            all[index] = offhand;
        }
        return all;
    }

    private void enhanceLuckOnItem(ItemStack stack, int potency) {
        if (stack == null || stack.getType() == Material.AIR || potency <= 0) {
            return;
        }
        Material type = stack.getType();
        boolean fortuneEligible = isFortuneTool(type);
        boolean lootingEligible = isLootingWeapon(type);
        if (!fortuneEligible && !lootingEligible) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        boolean updated = false;
        if (fortuneEligible) {
            updated |= applyLuckEnchant(meta, luckFortuneKey, Enchantment.FORTUNE, potency);
        }
        if (lootingEligible) {
            updated |= applyLuckEnchant(meta, luckLootKey, Enchantment.LOOTING, potency);
        }
        if (updated) {
            stack.setItemMeta(meta);
        }
    }

    private boolean applyLuckEnchant(ItemMeta meta, NamespacedKey key, Enchantment enchantment, int potency) {
        if (meta == null || potency <= 0) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer storedLevel = pdc.get(key, PersistentDataType.INTEGER);
        int currentLevel = meta.getEnchantLevel(enchantment);
        int baselineLevel = storedLevel != null ? storedLevel : currentLevel;
        int desiredLevel = Math.max(baselineLevel, potency);
        if (currentLevel == desiredLevel) {
            return false;
        }
        if (storedLevel == null) {
            pdc.set(key, PersistentDataType.INTEGER, currentLevel);
        }
        meta.addEnchant(enchantment, desiredLevel, true);
        return true;
    }

    private void revertLuckOnItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean updated = false;
        Integer fortuneStored = pdc.get(luckFortuneKey, PersistentDataType.INTEGER);
        if (fortuneStored != null) {
            if (fortuneStored <= 0) {
                meta.removeEnchant(Enchantment.FORTUNE);
            } else {
                meta.addEnchant(Enchantment.FORTUNE, fortuneStored, true);
            }
            pdc.remove(luckFortuneKey);
            updated = true;
        }
        Integer lootingStored = pdc.get(luckLootKey, PersistentDataType.INTEGER);
        if (lootingStored != null) {
            if (lootingStored <= 0) {
                meta.removeEnchant(Enchantment.LOOTING);
            } else {
                meta.addEnchant(Enchantment.LOOTING, lootingStored, true);
            }
            pdc.remove(luckLootKey);
            updated = true;
        }
        if (updated) {
            stack.setItemMeta(meta);
        }
    }

    private boolean isFortuneTool(Material type) {
        if (type == null) {
            return false;
        }
        String name = type.name();
        return name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_AXE")
                || name.endsWith("_HOE")
                || type == Material.SHEARS;
    }

    private boolean isLootingWeapon(Material type) {
        if (type == null) {
            return false;
        }
        String name = type.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.equals("TRIDENT")
                || name.equals("BOW")
                || name.equals("CROSSBOW")
                || name.equals("FISHING_ROD");
    }

    private void applyDamagePulse(LivingEntity target, double amount) {
        if (target == null || amount <= 0.0) {
            return;
        }
        double before = target.getHealth();
        if (before <= 0.0) {
            return;
        }
        target.setNoDamageTicks(0);
        double animationDamage = Math.min(amount, 0.001D);
        target.damage(animationDamage);
        if (target.isDead()) {
            return;
        }
        double targetHealth = Math.max(0.0, before - amount);
        if (target.getHealth() > targetHealth) {
            target.setHealth(targetHealth);
        }
    }

    private void savePersistedEffects() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, ActiveEffect>> entry : playerEffects.entrySet()) {
            Map<String, ActiveEffect> effects = entry.getValue();
            if (effects == null || effects.isEmpty()) {
                continue;
            }
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            int index = 0;
            for (ActiveEffect effect : effects.values()) {
                if (effect.getRemainingSeconds() <= 0) {
                    continue;
                }
                ConfigurationSection effectSection = section.createSection(String.valueOf(index++));
                serializeEffect(effectSection, effect);
            }
            if (section.getKeys(false).isEmpty()) {
                config.set(entry.getKey().toString(), null);
            }
        }
        try {
            File parent = persistenceFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Unable to create data folder for custom potion effects.");
            }
            if (config.getKeys(false).isEmpty()) {
                if (persistenceFile.exists() && !persistenceFile.delete()) {
                    plugin.getLogger().warning("Unable to delete empty custom potion effect store.");
                }
                return;
            }
            config.save(persistenceFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save custom potion effects: " + ex.getMessage());
        }
    }

    private void serializeEffect(ConfigurationSection section, ActiveEffect effect) {
        if (section == null || effect == null) {
            return;
        }
        section.set("id", effect.definition.getId());
        section.set("brew_type", effect.getBrewType().name());
        section.set("enchant_tier", effect.getEnchantTier());
        section.set("potency", effect.getPotency());
        section.set("remaining", effect.getRemainingSeconds());
        section.set("duration", effect.getTotalDuration());
    }

    private void loadPersistedEffects() {
        if (!persistenceFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(persistenceFile);
        for (String uuidKey : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection(uuidKey);
            if (section == null) {
                continue;
            }
            for (String childKey : section.getKeys(false)) {
                ConfigurationSection effectSection = section.getConfigurationSection(childKey);
                if (effectSection == null) {
                    continue;
                }
                EffectSnapshot snapshot = deserializeEffect(effectSection);
                if (snapshot == null) {
                    continue;
                }
                PotionDefinition definition = PotionRegistry.getById(snapshot.effectId()).orElse(null);
                if (definition == null) {
                    continue;
                }
                ActiveEffect effect = new ActiveEffect(definition, snapshot.brewType(), snapshot.enchantTier(), definition.getAccentBukkitColor());
                effect.restore(snapshot.remainingSeconds(), snapshot.totalDuration(), snapshot.potency());
                playerEffects.computeIfAbsent(uuid, id -> new HashMap<>()).put(snapshot.effectKey(), effect);
            }
        }
    }

    private EffectSnapshot deserializeEffect(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String effectId = section.getString("id");
        String brewTypeRaw = section.getString("brew_type");
        if (effectId == null || brewTypeRaw == null) {
            return null;
        }
        BrewType brewType;
        try {
            brewType = BrewType.valueOf(brewTypeRaw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        int enchantTier = Math.max(0, section.getInt("enchant_tier", 0));
        int potency = Math.max(0, section.getInt("potency", 0));
        int remaining = Math.max(0, section.getInt("remaining", 0));
        int duration = Math.max(remaining, section.getInt("duration", remaining));
        if (remaining <= 0 || duration <= 0) {
            return null;
        }
        return new EffectSnapshot(effectId, brewType, enchantTier, potency, remaining, duration);
    }

    private record EffectSnapshot(String effectId,
                                  BrewType brewType,
                                  int enchantTier,
                                  int potency,
                                  int remainingSeconds,
                                  int totalDuration) {
        String effectKey() {
            return buildEffectKey(effectId, brewType);
        }
    }

    private void applyHealthBoostBonus(LivingEntity target, int potency) {
        if (target == null) {
            return;
        }
        AttributeInstance attribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        UUID uuid = target.getUniqueId();
        double original = healthBoostOriginals.computeIfAbsent(uuid, id -> attribute.getBaseValue());
        double bonus = 20.0 * Math.max(1, potency);
        double newBase = original + bonus;
        if (Math.abs(attribute.getBaseValue() - newBase) > 0.0001D) {
            attribute.setBaseValue(newBase);
        }
        double current = target.getHealth();
        double healed = Math.min(newBase, current + bonus);
        if (healed > current) {
            target.setHealth(healed);
        } else if (current > newBase) {
            target.setHealth(newBase);
        }
    }

    private void clearAbsorption(LivingEntity target) {
        if (target == null) {
            return;
        }
        target.removePotionEffect(PotionEffectType.ABSORPTION);
        if (target.getAbsorptionAmount() > 0.0D) {
            target.setAbsorptionAmount(0.0D);
        }
    }

    private void clearHealthBoost(LivingEntity target) {
        if (target == null) {
            return;
        }
        UUID uuid = target.getUniqueId();
        Double original = healthBoostOriginals.remove(uuid);
        if (original == null) {
            return;
        }
        AttributeInstance attribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(original);
            double clamped = Math.min(original, target.getHealth());
            target.setHealth(Math.max(0.0, clamped));
        }
    }

    private void clearLevitationSafety(LivingEntity target) {
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        if (plugin == null || target == null) {
            return;
        }
        if (target.hasMetadata("pl_levitation_safe")) {
            target.removeMetadata("pl_levitation_safe", plugin);
        }
    }

    private LivingEntity getLivingEntity(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static final class DamageProfile {
        private final String effectId;
        private final int pulses;
        private final int intervalTicks;
        private final double damagePerPulse;
        private final HighFrequencyStyle style;

        private DamageProfile(String effectId, int pulses, int intervalTicks, double damagePerPulse, HighFrequencyStyle style) {
            this.effectId = effectId;
            this.pulses = Math.max(1, pulses);
            this.intervalTicks = Math.max(1, intervalTicks);
            this.damagePerPulse = Math.max(0.0, damagePerPulse);
            this.style = style;
        }

        private String effectId() {
            return effectId;
        }

        private int intervalTicks() {
            return intervalTicks;
        }

        private double damagePerPulse() {
            return damagePerPulse;
        }

        private HighFrequencyStyle style() {
            return style;
        }

        private int pulses() {
            return pulses;
        }

        private int durationSeconds() {
            int totalTicks = pulses * intervalTicks;
            return Math.max(1, (int) Math.ceil(totalTicks / 20.0));
        }
    }

    private void tickEffects() {
        // Players degrade only while online.
        for (Map.Entry<UUID, Map<String, ActiveEffect>> entry : new ArrayList<>(playerEffects.entrySet())) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            Map<String, ActiveEffect> effects = entry.getValue();
            if (effects.isEmpty()) {
                playerEffects.remove(uuid);
                continue;
            }
            if (player == null || !player.isOnline()) {
                continue;
            }
            boolean hadEffects = !effects.isEmpty();
            if (hadEffects) {
                Iterator<Map.Entry<String, ActiveEffect>> iterator = effects.entrySet().iterator();
                while (iterator.hasNext()) {
                    ActiveEffect effect = iterator.next().getValue();
                    if (effect.tick(player)) {
                        handleEffectExpiration(player, effect);
                        iterator.remove();
                    }
                    if ("luck".equalsIgnoreCase(effect.definition.getId())) {
                        applyLuckEnhancements(player, effect.getPotency());
                    }
                    if ("charismatic_bartering".equalsIgnoreCase(effect.definition.getId())) {
                        applyCharismaticBonus(player);
                    }
                }
                tablistManager.refreshPlayer(player);
            }
            if (effects.isEmpty()) {
                playerEffects.remove(uuid);
            }
        }

        // Entities degrade while loaded; prune stale entries.
        for (Map.Entry<UUID, Map<String, ActiveEffect>> entry : new ArrayList<>(entityEffects.entrySet())) {
            UUID uuid = entry.getKey();
            Map<String, ActiveEffect> effects = entry.getValue();
            if (effects.isEmpty()) {
                entityEffects.remove(uuid);
                continue;
            }

            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                entityEffects.remove(uuid);
                cancelDamageTask(uuid);
                continue;
            }

            Iterator<Map.Entry<String, ActiveEffect>> iterator = effects.entrySet().iterator();
            while (iterator.hasNext()) {
                ActiveEffect effect = iterator.next().getValue();
                if (effect.tick(living)) {
                    handleEffectExpiration(living, effect);
                    iterator.remove();
                }
            }
            if (effects.isEmpty()) {
                entityEffects.remove(uuid);
                cancelDamageTask(uuid);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> restorePlayerState(event.getPlayer()));
    }

    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        bootstrapPlayerEffects(player);
        tablistManager.refreshPlayer(player);
        if (hasEffect(player, "luck")) {
            int luckPotency = getEffectPotency(player, "luck");
            if (luckPotency > 0) {
                applyLuckEnhancements(player, luckPotency);
            }
        }
        if (hasEffect(player, "absorption")) {
            int absorptionPotency = getEffectPotency(player, "absorption");
            if (absorptionPotency > 0) {
                int remaining = getEffectRemainingSeconds(player.getUniqueId(), "absorption");
                if (remaining <= 0) {
                    PotionEffect current = player.getPotionEffect(PotionEffectType.ABSORPTION);
                    if (current != null) {
                        remaining = Math.max(remaining, current.getDuration() / 20);
                    }
                }
                if (remaining <= 0) {
                    remaining = 600;
                }
                CustomPotionEffects.applyAbsorption(player, absorptionPotency, remaining);
            }
        }
        if (hasEffect(player, "charismatic_bartering")) {
            applyCharismaticBonus(player);
        }
    }

    private void bootstrapPlayerEffects(Player player) {
        Map<String, ActiveEffect> effects = playerEffects.get(player.getUniqueId());
        if (effects == null || effects.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, ActiveEffect>> iterator = effects.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next().getValue();
            if (effect.getRemainingSeconds() <= 0) {
                iterator.remove();
                continue;
            }
            effect.reapply(player);
        }
        if (effects.isEmpty()) {
            playerEffects.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        revertLuckEnhancements(player);
        clearLevitationSafety(player);
        clearCharismaticBonus(player);
        // Ensure tablist cleans up next tick when manager runs.
        Bukkit.getScheduler().runTaskLater(plugin, () -> tablistManager.refreshPlayer(player), 1L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            revertLuckEnhancements(player);
            clearCharismaticBonus(player);
        }
        entityEffects.remove(event.getEntity().getUniqueId());
        cancelDamageTask(event.getEntity().getUniqueId());
        clearLevitationSafety(event.getEntity());
    }

    public boolean hasCharismaticBartering(Player player) {
        return player != null && charismaticBarterPlayers.contains(player.getUniqueId());
    }

    public int getCharismaticCost(Player player, int baseCost) {
        if (player == null || baseCost <= 0) {
            return Math.max(1, baseCost);
        }
        if (!hasCharismaticBartering(player)) {
            return baseCost;
        }
        int adjusted = (baseCost + 1) / 2;
        return Math.max(1, adjusted);
    }

    private void applyCharismaticBonus(Player player) {
        if (player == null) {
            return;
        }
        charismaticBarterPlayers.add(player.getUniqueId());
    }

    private void clearCharismaticBonus(Player player) {
        if (player == null) {
            return;
        }
        charismaticBarterPlayers.remove(player.getUniqueId());
    }

    private final class DamageTask extends BukkitRunnable {
        private final UUID targetId;
        private final DamageProfile profile;
        private int pulsesRemaining;

        private DamageTask(UUID targetId, DamageProfile profile) {
            this.targetId = targetId;
            this.profile = profile;
            this.pulsesRemaining = profile.pulses();
        }

        @Override
        public void run() {
            if (pulsesRemaining <= 0) {
                finish();
                return;
            }
            Entity entity = Bukkit.getEntity(targetId);
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                finish();
                return;
            }
            if (!isHostileTarget(living) || !hasActiveEffect(targetId, profile.effectId())) {
                finish();
                return;
            }
            applyDamagePulse(living, profile.damagePerPulse());
            if (damageDisplayManager != null) {
                damageDisplayManager.spawnHighFrequencyDamage(living, profile.style(), profile.damagePerPulse());
            }
            pulsesRemaining--;
            if (pulsesRemaining <= 0) {
                finish();
            }
        }

        private void finish() {
            damageTasks.remove(targetId, this);
            cancel();
        }
    }

    private static final class ActiveEffect {
        private final PotionDefinition definition;
        private final BrewType brewType;
        private final int enchantTier;
        private final Color color;
        private int remainingSeconds;
        private int totalDuration;
        private int potency;
        private int elapsedSeconds;

        private ActiveEffect(PotionDefinition definition, BrewType brewType, int enchantTier, Color color) {
            this.definition = definition;
            this.brewType = brewType;
            this.enchantTier = enchantTier;
            this.color = color == null ? Color.WHITE : color;
        }

        private void refresh(int duration, int potency, LivingEntity target) {
            this.remainingSeconds = duration;
            this.totalDuration = duration;
            this.potency = potency;
            this.elapsedSeconds = 0;
            CustomPotionEffects.applyImmediate(definition, brewType, potency, target);
        }

        private void reapply(LivingEntity target) {
            CustomPotionEffects.applyImmediate(definition, brewType, potency, target);
        }
        private void restore(int remainingSeconds, int totalDuration, int potency) {
            this.remainingSeconds = remainingSeconds;
            this.totalDuration = Math.max(remainingSeconds, totalDuration);
            this.potency = potency;
            this.elapsedSeconds = Math.max(0, this.totalDuration - this.remainingSeconds);
        }

        private boolean tick(LivingEntity target) {
            if (remainingSeconds <= 0) {
                return true;
            }
            elapsedSeconds += 1;
            CustomPotionEffects.applyTick(definition, brewType, potency, target, elapsedSeconds);
            remainingSeconds -= 1;
            return remainingSeconds <= 0;
        }

        private String tabLabel() {
            String accent = definition.getAccentColor();
            String baseName = definition.getDisplayName();
            String potencyStr = potency > 0 ? ChatColor.YELLOW + "P" + potency : ChatColor.YELLOW + "P?";
            String durationStr = ChatColor.YELLOW + String.valueOf(Math.max(0, remainingSeconds)) + ChatColor.GRAY + "s";
            String tierStr = enchantTier > 0 ? ChatColor.LIGHT_PURPLE + roman(enchantTier) + ChatColor.GRAY + " • " : "";
            String source = brewType == BrewType.NETHER ? ChatColor.RED + "Nether" : ChatColor.GREEN + "Overworld";
            return accent + baseName + ChatColor.GRAY + " [" + source + ChatColor.GRAY + "] "
                    + tierStr + potencyStr + ChatColor.GRAY + " • " + durationStr;
        }

        private int getPotency() {
            return potency;
        }

        private int getRemainingSeconds() {
            return remainingSeconds;
        }

        private int getTotalDuration() {
            return totalDuration;
        }

        private BrewType getBrewType() {
            return brewType;
        }

        private int getEnchantTier() {
            return enchantTier;
        }

        private String effectKey() {
            return buildEffectKey(definition.getId(), brewType);
        }
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(tier);
        };
    }
}
