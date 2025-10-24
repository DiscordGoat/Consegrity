package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.FinalStats;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionItemData;
import goat.projectLinearity.subsystems.mechanics.TablistManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks custom potion effects applied by the custom potion system. Effects currently only
 * influence HUD / debug output but maintain groundwork for future gameplay hooks.
 */
public final class CustomPotionEffectManager implements Listener {

    private static final int TICK_INTERVAL = 20; // 1 second
    private final ProjectLinearity plugin;
    private final TablistManager tablistManager;
    private final Map<UUID, Map<String, ActiveEffect>> playerEffects = new HashMap<>();
    private final Map<UUID, Map<String, ActiveEffect>> entityEffects = new HashMap<>();
    private final BukkitTask tickTask;

    public CustomPotionEffectManager(ProjectLinearity plugin, TablistManager tablistManager) {
        this.plugin = plugin;
        this.tablistManager = tablistManager;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickEffects();
            }
        }.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        playerEffects.clear();
        entityEffects.clear();
    }

    public void applyEffect(LivingEntity target, PotionItemData data) {
        if (target == null || data == null) return;

        FinalStats stats = data.getFinalStats();
        int duration = Math.max(1, stats.durationSeconds());
        int potency = Math.max(1, stats.potency());

        if (target instanceof Player player) {
            Map<String, ActiveEffect> effects = playerEffects.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
            ActiveEffect effect = effects.computeIfAbsent(data.effectKey(), key -> new ActiveEffect(data.getDefinition(), data.getBrewType(), data.getEnchantTier(), data.getBukkitColor()));
            effect.refresh(duration, potency, player);
            tablistManager.refreshPlayer(player);
        } else {
            UUID uuid = target.getUniqueId();
            Map<String, ActiveEffect> effects = entityEffects.computeIfAbsent(uuid, id -> new HashMap<>());
            ActiveEffect effect = effects.computeIfAbsent(data.effectKey(), key -> new ActiveEffect(data.getDefinition(), data.getBrewType(), data.getEnchantTier(), data.getBukkitColor()));
            effect.refresh(duration, potency, target);
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
                        iterator.remove();
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
                continue;
            }

            Iterator<Map.Entry<String, ActiveEffect>> iterator = effects.entrySet().iterator();
            while (iterator.hasNext()) {
                ActiveEffect effect = iterator.next().getValue();
                if (effect.tick(living)) {
                    iterator.remove();
                }
            }
            if (effects.isEmpty()) {
                entityEffects.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> tablistManager.refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Ensure tablist cleans up next tick when manager runs.
        Bukkit.getScheduler().runTaskLater(plugin, () -> tablistManager.refreshPlayer(player), 1L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        entityEffects.remove(event.getEntity().getUniqueId());
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
            ChatColor accent = definition.getAccentColor();
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
