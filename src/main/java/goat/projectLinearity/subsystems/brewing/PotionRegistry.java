package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.mechanics.CustomDurabilityManager;
import goat.projectLinearity.util.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Central registry for custom potion definitions, recipes, and discovery state.
 */
public final class PotionRegistry {

    public enum BrewType {
        OVERWORLD(ChatColor.GREEN + "Overworld Brew"),
        NETHER(ChatColor.RED + "Nether Brew");

        private final String display;

        BrewType(String display) {
            this.display = display;
        }

        public String displayName() {
            return display;
        }
    }

    private static final Map<String, PotionDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final NamespacedKey KEY_ID = key("id");
    private static final NamespacedKey KEY_TYPE = key("brew_type");
    private static final NamespacedKey KEY_ENCHANT = key("enchant_tier");
    private static final NamespacedKey KEY_SPLASH = key("is_splash");
    private static final NamespacedKey KEY_OVERRIDE_DURATION = key("override_duration");
    private static final NamespacedKey KEY_OVERRIDE_POTENCY = key("override_potency");
    private static final NamespacedKey KEY_OVERRIDE_CHARGES = key("override_charges");

    static {
        register(
                new PotionDefinition(
                        "haste", "#F3B23E",  // Display color
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.HONEYCOMB  // Core ingredient
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Golden Tear"  // Nether variant
                        ),
                        new PotionStats(60, 1, 20),  // Overworld: 60s duration, potency 1, 20 charges
                        new PotionStats(120, 2, 20)  // Nether: 120s duration, potency 2, 10 charges
                )
        );
        register(
                new PotionDefinition(
                        "swiftness", "#58BFFE",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SUGAR
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.WEEPING_VINES
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 5)
                )
        );
        register(
                new PotionDefinition(
                        "night_vision", "#2EE68D",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.GOLDEN_CARROT
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Golden Eye"
                        ),
                        new PotionStats(60, 1, 10),  // Overworld: 60s duration, potency 1, 15 charges
                        new PotionStats(180, 1, 30)   // Nether: 180s duration, potency 2, 5 charges
                )
                );
        register(
                new PotionDefinition(
                        "hunger", "#857729",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                "Molding Flesh"
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Charred Flesh"
                        ),
                        new PotionStats(30, 1, 10),  // Overworld: 30s duration, potency 1, 10 charges
                        new PotionStats(60, 2, 5)    // Nether: 60s duration, potency 2, 5 charges
                )
        );

        register(
                new PotionDefinition(
                        "weakness", "#B391C8",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.FERMENTED_SPIDER_EYE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Weakened Marrow"
                        ),
                        new PotionStats(30, 1, 10),  // Overworld: 30s duration, potency 1, 10 charges
                        new PotionStats(60, 2, 5)    // Nether: 60s duration, potency 2, 5 charges
                )
        );
        register(
                new PotionDefinition(
                        "poison", "#47FF6D",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SPIDER_EYE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.WARPED_FUNGUS
                        ),
                        new PotionStats(45, 1, 15),  // Overworld: 45s duration, potency 1, 15 charges
                        new PotionStats(90, 2, 8)    // Nether: 90s duration, potency 2, 8 charges
                )
        );
        register(
                new PotionDefinition(
                        "wither", "#3B3344",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.WITHER_ROSE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.WITHER_SKELETON_SKULL
                        ),
                        new PotionStats(60, 1, 12),  // Overworld: 60s duration, potency 1, 12 charges
                        new PotionStats(120, 2, 10)    // Nether: 120s duration, potency 2, 6 charges
                )
        );

        register(
                new PotionDefinition(
                        "absorption", "#FFC659",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.GOLDEN_APPLE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Warped Apple"
                        ),
                        new PotionStats(600, 1, 4),  // Overworld: 10 minutes, potency 1, 10 charges
                        new PotionStats(900, 2, 4)    // Nether: 15 minutes, potency 2, 6 charges
                )
        );

        register(
                new PotionDefinition(
                        "health_boost", "#FF6F82",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.APPLE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Red Sugar Cane"
                        ),
                        new PotionStats(600, 1, 2),   // Overworld: 10 minutes, potency 1, 8 charges
                        new PotionStats(900, 2, 5)    // Nether: 15 minutes, potency 2, 5 charges
                )
        );

        register(
                new PotionDefinition(
                        "saturation", "#D7B356",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.JACK_O_LANTERN
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.GLOWSTONE_DUST
                        ),
                        new PotionStats(60*20, 1, 1),   // Overworld: 60s base, scales with potency
                        new PotionStats(60*60, 2, 1)     // Nether: same base duration with higher potency
                )
        );

        register(
                new PotionDefinition(
                        "well_balanced_meal", "#B47A45",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.CAKE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Hoglin Roast"
                        ),
                        new PotionStats(1, 1, 8),   // Instant effect, 20 uses
                        new PotionStats(1, 1, 20)
                )
        );

        register(
                new PotionDefinition(
                        "glowing", "#FFF45C",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.GLOW_INK_SAC
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.SPECTRAL_ARROW
                        ),
                        new PotionStats(60, 1, 12),   // Overworld: 60s duration, potency 1, 12 charges
                        new PotionStats(120, 2, 8)    // Nether: 120s duration, potency 2, 8 charges
                )
        );

        register(
                new PotionDefinition(
                        "levitation", "#C9ACFF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.PHANTOM_MEMBRANE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.SHULKER_SHELL
                        ),
                        new PotionStats(20, 1, 8),   // Overworld: short duration, potency 1, 8 charges
                        new PotionStats(30, 2, 6)    // Nether: longer duration, potency 2, 6 charges
                )
        );

        register(
                new PotionDefinition(
                        "luck", "#1FAE5D",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.EMERALD
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.RABBIT_FOOT
                        ),
                        new PotionStats(180, 1, 10),  // Overworld: 3 minutes, potency 1, 10 charges
                        new PotionStats(240, 2, 6)    // Nether: 4 minutes, potency 2, 6 charges
                )
        );

        register(
                new PotionDefinition(
                        "slow_falling", "#F7C7A5",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.FEATHER
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.GHAST_TEAR
                        ),
                        new PotionStats(120, 1, 12),
                        new PotionStats(240, 2, 8)
                )
        );

        register(
                new PotionDefinition(
                        "instant_lightning", "#3B7CFF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.AMETHYST_SHARD
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.LIGHTNING_ROD
                        ),
                        new PotionStats(1, 1, 12),
                        new PotionStats(1, 1, 12)
                )
        );

        register(
                new PotionDefinition(
                        "blindness", "#2B2E3A",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.BLACK_DYE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.ENDER_EYE
                        ),
                        new PotionStats(30, 1, 10),  // Overworld: 30s duration, potency 1, 10 charges
                        new PotionStats(60, 2, 5)    // Nether: 60s duration, potency 2, 5 charges
                )
        );
        register(
                new PotionDefinition(
                        "invisibility", "#DAE0EA",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.INK_SAC
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.ENDER_PEARL
                        ),
                        new PotionStats(60, 1, 15),  // Overworld: 60s duration, potency 1, 15 charges
                        new PotionStats(180, 2, 5)   // Nether: 180s duration, potency 2, 5 charges
                )
        );
        register(
                new PotionDefinition(
                        "strength", "#C83C32",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.OBSIDIAN
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BLAZE_POWDER
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "oxygen_recovery", "#3ED2CA",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SPONGE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.CRYING_OBSIDIAN
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "slowness", "#6F7F8F",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SLIME_BALL
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.SOUL_SAND
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "mining_fatigue", "#8A7558",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.DEEPSLATE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BASALT
                        ),
                        new PotionStats(60, 1, 18),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_health", "#FF4FA8",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.GLISTERING_MELON_SLICE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.GHAST_TEAR
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_damage", "#7A245A",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SPIDER_EYE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.FERMENTED_SPIDER_EYE
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_cooling", "#7CE6FF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.PACKED_ICE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BLUE_ICE
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_warming", "#FF6633",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.BLAZE_ROD
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.MAGMA_CREAM
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_oxygen", "#51C9FF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.KELP
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.TUBE_CORAL_FAN
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "instant_air", "#A6E3FF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.PHANTOM_MEMBRANE
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BREEZE_ROD
                        ),
                        new PotionStats(1, 1, 16),
                        new PotionStats(1, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "frostbite", "#71B9FF",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.SNOW_BLOCK
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BLUE_ICE
                        ),
                        new PotionStats(120, 1, 12),
                        new PotionStats(180, 2, 8)
                )
        );
        register(
                new PotionDefinition(
                        "heatstroke", "#FF945C",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.BLAZE_POWDER
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.MAGMA_BLOCK
                        ),
                        new PotionStats(120, 1, 12),
                        new PotionStats(180, 2, 8)
                )
        );
        register(
                new PotionDefinition(
                        "leaping", "#7EDB5A",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.RABBIT_FOOT
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.PHANTOM_MEMBRANE
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "nausea", "#C963B8",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.POISONOUS_POTATO
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.ROTTEN_FLESH
                        ),
                        new PotionStats(60, 1, 16),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "regeneration", "#EB5E87",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.GHAST_TEAR
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.SHULKER_SHELL
                        ),
                        new PotionStats(60, 1, 16),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "resistance", "#5464D0",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.IRON_INGOT
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BLAZE_ROD
                        ),
                        new PotionStats(60, 1, 16),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "fire_resistance", "#FF7A2B",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.MAGMA_CREAM
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.MAGMA_BLOCK
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "water_breathing", "#2F9ED1",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.PUFFERFISH
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.PRISMARINE_CRYSTALS
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 12)
                )
        );
        register(
                new PotionDefinition(
                        "conduit_power", "#2B8DD8",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.NAUTILUS_SHELL
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.HEART_OF_THE_SEA
                        ),
                        new PotionStats(120, 1, 12),
                        new PotionStats(240, 2, 8)
                )
        );
        register(
                new PotionDefinition(
                        "dolphins_grace", "#2FC0E0",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.TROPICAL_FISH
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.PRISMARINE_BRICKS
                        ),
                        new PotionStats(120, 1, 12),
                        new PotionStats(240, 2, 8)
                )
        );
        register(
                new PotionDefinition(
                        "charismatic_bartering", "#7CE078",
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.EMERALD
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.EMERALD_BLOCK
                        ),
                        new PotionStats(180, 1, 10),
                        new PotionStats(240, 2, 6)
                )
        );
    }

    private PotionRegistry() {
    }

    private static void register(PotionDefinition definition) {
        DEFINITIONS.put(definition.getId(), definition);
    }

    public static Collection<PotionDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static Optional<PotionDefinition> getById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(DEFINITIONS.get(id.toLowerCase(Locale.ENGLISH)));
    }

    public static boolean hasDiscovered(Player player, PotionDefinition definition) {
        if (player == null || definition == null) return false;
        return readFlag(player.getPersistentDataContainer(), definition.discoveryKey);
    }

    public static boolean hasDiscovered(Player player, PotionDefinition definition, BrewType type) {
        if (player == null || definition == null || type == null) return false;
        NamespacedKey key = definition.discoveryKeys.get(type);
        return readFlag(player.getPersistentDataContainer(), key);
    }

    public static void markDiscovered(Player player, PotionDefinition definition, BrewType type) {
        if (player == null || definition == null || type == null) return;
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        writeFlag(pdc, definition.discoveryKey, true);
        NamespacedKey tierKey = definition.discoveryKeys.get(type);
        writeFlag(pdc, tierKey, true);
    }

    private static boolean readFlag(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc == null || key == null) return false;
        Byte value = pdc.get(key, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private static void writeFlag(PersistentDataContainer pdc, NamespacedKey key, boolean value) {
        if (pdc == null || key == null) return;
        if (value) {
            pdc.set(key, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(key);
        }
    }

    private static FinalStats computeFinalStats(PotionStats stats, int enchantTier) {
        int charges = stats.getCharges();
        int duration = stats.getDurationSeconds();
        int potency = stats.getPotency();

        if (enchantTier >= 1) {
            charges = (int) Math.ceil(charges * 1.5d);
        }
        if (enchantTier >= 2) {
            duration = (int) Math.ceil(duration * 1.5d);
        }
        if (enchantTier >= 3) {
            potency += 1;
        }

        return new FinalStats(charges, duration, potency);
    }

    public static ItemStack createResultItem(PotionDefinition definition, BrewType type, boolean splash, int enchantTier) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(type, "type");
        int tier = Math.max(0, Math.min(3, enchantTier));
        FinalStats stats = computeFinalStats(definition.getStats(type), tier);

        ItemStack result = new ItemStack(Material.STONE_HOE);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(definition.getAccentColor() + definition.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(false);
            writePotionData(meta, definition, type, tier, splash);
            result.setItemMeta(meta);
        }

        CustomDurabilityManager manager = CustomDurabilityManager.getInstance();
        manager.setCustomDurability(result, stats.charges(), stats.charges());
        refreshPotionLore(result);
        return result;
    }

    public static ItemStack createAdminPotion(PotionDefinition definition,
                                              BrewType type,
                                              boolean splash,
                                              int durationSeconds,
                                              int potency,
                                              int charges,
                                              int enchantTier) {
        ItemStack result = createResultItem(definition, type, splash, Math.max(0, Math.min(3, enchantTier)));
        ItemMeta meta = result.getItemMeta();
        int safeDuration = Math.max(1, durationSeconds);
        int safePotency = Math.max(1, potency);
        int safeCharges = Math.max(1, charges);
        if (meta != null) {
            applyPotionOverrides(meta, safeDuration, safePotency, safeCharges);
            result.setItemMeta(meta);
        }
        CustomDurabilityManager manager = CustomDurabilityManager.getInstance();
        manager.setCustomDurability(result, safeCharges, safeCharges);
        refreshPotionLore(result);
        return result;
    }

    public static ItemStack createAdminPotion(PotionDefinition definition,
                                              BrewType type,
                                              boolean splash,
                                              int durationSeconds,
                                              int potency,
                                              int charges) {
        return createAdminPotion(definition, type, splash, durationSeconds, potency, charges, 0);
    }

    public static void writePotionData(ItemMeta meta, PotionDefinition definition, BrewType type, int enchantTier, boolean splash) {
        if (meta == null || definition == null || type == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(KEY_ID, PersistentDataType.STRING, definition.getId());
        container.set(KEY_TYPE, PersistentDataType.STRING, type.name());
        container.set(KEY_ENCHANT, PersistentDataType.INTEGER, Math.max(0, Math.min(3, enchantTier)));
        container.set(KEY_SPLASH, PersistentDataType.BYTE, (byte) (splash ? 1 : 0));
    }

    public static void writePotionData(ItemMeta meta, PotionDefinition definition, BrewType type, int enchantTier, boolean splash,
                                       Integer durationOverride, Integer potencyOverride, Integer chargesOverride) {
        writePotionData(meta, definition, type, enchantTier, splash);
        applyPotionOverrides(meta, durationOverride, potencyOverride, chargesOverride);
    }

    public static void applyPotionOverrides(ItemMeta meta, Integer durationOverride, Integer potencyOverride, Integer chargesOverride) {
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        applyOverride(container, KEY_OVERRIDE_DURATION, durationOverride);
        applyOverride(container, KEY_OVERRIDE_POTENCY, potencyOverride);
        applyOverride(container, KEY_OVERRIDE_CHARGES, chargesOverride);
    }

    private static void applyOverride(PersistentDataContainer container, NamespacedKey key, Integer value) {
        if (container == null || key == null) {
            return;
        }
        if (value != null && value > 0) {
            container.set(key, PersistentDataType.INTEGER, value);
        } else {
            container.remove(key);
        }
    }

    public static void incrementEnchantTier(ItemStack stack, int newTier) {
        PotionItemData.from(stack).ifPresent(data -> {
            int capped = Math.max(0, Math.min(3, newTier));
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                writePotionData(meta, data.getDefinition(), data.getBrewType(), capped, data.isSplash(),
                        data.getDurationOverride(), data.getPotencyOverride(), data.getChargesOverride());
                stack.setItemMeta(meta);
            }
            refreshPotionLore(stack);
        });
    }

    public static List<String> describeRecipe(PotionRecipe recipe) {
        if (recipe == null) return List.of();
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.WHITE + "Bottle: " + ChatColor.GRAY + recipe.getBottle().friendlyName());
        lines.add(ChatColor.YELLOW + "Enzyme: " + ChatColor.GRAY + recipe.getEnzyme().friendlyName());
        lines.add(ChatColor.LIGHT_PURPLE + "Core: " + ChatColor.GRAY + recipe.getMain().friendlyName());
        return lines;
    }

    public static void refreshPotionLore(ItemStack item) {
        PotionItemData.from(item).ifPresent(data -> {
            CustomDurabilityManager durabilityManager = CustomDurabilityManager.getInstance();
            FinalStats stats = data.getFinalStats();
            int targetMax = Math.max(1, stats.charges());
            int current;
            if (!durabilityManager.isTracked(item)) {
                current = targetMax;
            } else {
                current = Math.max(0, Math.min(durabilityManager.getCurrentDurability(item), targetMax));
            }
            durabilityManager.setCustomDurability(item, current, targetMax);
            int refreshedCurrent = durabilityManager.getCurrentDurability(item);
            int refreshedMax = durabilityManager.getMaxDurability(item);
            applyPotionLore(item, data, refreshedCurrent, refreshedMax);
        });
    }

    private static void applyPotionLore(ItemStack item, PotionItemData data, int current, int max) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + (data.isSplash() ? "Splash Brew" : "Drinkable Brew"));
        lore.add(ChatColor.DARK_GRAY + data.getBrewType().displayName());
        if (data.getEnchantTier() > 0) {
            lore.add(ChatColor.LIGHT_PURPLE + "Enchanted " + roman(data.getEnchantTier()));
        }
        lore.add(ChatColor.GRAY + "Potency: " + ChatColor.YELLOW + data.getFinalStats().potency());
        lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + data.getFinalStats().durationSeconds() + "s");
        lore.add(ChatColor.GRAY + "Charges: " + ChatColor.YELLOW + current + ChatColor.GRAY + "/" + ChatColor.YELLOW + max);

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "0";
        };
    }

    private static String sanitizeHex(String raw) {
        String candidate = (raw == null || raw.isBlank()) ? "#FFFFFF" : raw.trim();
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        if (!candidate.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color: " + raw);
        }
        return candidate.toUpperCase(Locale.ENGLISH);
    }

    private static String toColorCode(String hex) {
        return net.md_5.bungee.api.ChatColor.of(hex).toString();
    }

    private static Color toBukkitColor(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return Color.fromRGB(rgb);
    }

    private static NamespacedKey key(String suffix) {
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        return new NamespacedKey(plugin, "potion_" + suffix);
    }

    private static NamespacedKey key(String id, String suffix) {
        ProjectLinearity plugin = ProjectLinearity.getInstance();
        return new NamespacedKey(plugin, "potion_" + id + "_" + suffix);
    }

    // --- Model Classes ---

    public static final class PotionDefinition {
        private final String id;
        private final String displayName;
        private final String accentHex;
        private final String accentColorCode;
        private final Color accentBukkitColor;
        private final PotionRecipe overworldRecipe;
        private final PotionRecipe netherRecipe;
        private final PotionStats overworldStats;
        private final PotionStats netherStats;
        private final NamespacedKey discoveryKey;
        private final Map<BrewType, NamespacedKey> discoveryKeys;

        private PotionDefinition(String id,
                                 String accentHex,
                                 PotionRecipe overworldRecipe,
                                 PotionRecipe netherRecipe,
                                 PotionStats overworldStats,
                                 PotionStats netherStats) {
            this.id = Objects.requireNonNull(id, "id").toLowerCase(Locale.ENGLISH);
            this.displayName = formatDisplayName(id);
            this.accentHex = sanitizeHex(accentHex);
            this.accentColorCode = toColorCode(this.accentHex);
            this.accentBukkitColor = toBukkitColor(this.accentHex);
            this.overworldRecipe = Objects.requireNonNull(overworldRecipe, "overworldRecipe");
            this.netherRecipe = Objects.requireNonNull(netherRecipe, "netherRecipe");
            this.overworldStats = Objects.requireNonNull(overworldStats, "overworldStats");
            this.netherStats = Objects.requireNonNull(netherStats, "netherStats");
            this.discoveryKey = key(this.id, "discovered");
            this.discoveryKeys = Map.of(
                    BrewType.OVERWORLD, key(this.id, "overworld"),
                    BrewType.NETHER, key(this.id, "nether")
            );
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAccentColor() {
            return accentColorCode;
        }

        public PotionRecipe getRecipe(BrewType type) {
            return type == BrewType.NETHER ? netherRecipe : overworldRecipe;
        }

        public Color getAccentBukkitColor() {
            return accentBukkitColor;
        }

        public PotionStats getStats(BrewType type) {
            return type == BrewType.NETHER ? netherStats : overworldStats;
        }

        public String getAccentHex() {
            return accentHex;
        }

        private static String formatDisplayName(String id) {
            String[] parts = id.split("[_\\-]");
            StringBuilder builder = new StringBuilder("Potion of ");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;
                builder.append(capitalize(part));
                if (i < parts.length - 1) {
                    builder.append(' ');
                }
            }
            return builder.toString();
        }

        private static String capitalize(String input) {
            if (input.isEmpty()) return input;
            return input.substring(0, 1).toUpperCase(Locale.ENGLISH) + input.substring(1).toLowerCase(Locale.ENGLISH);
        }
    }

    public static final class PotionStats {
        private final int durationSeconds;
        private final int potency;
        private final int charges;

        public PotionStats(int durationSeconds, int potency, int charges) {
            this.durationSeconds = Math.max(1, durationSeconds);
            this.potency = Math.max(1, potency);
            this.charges = Math.max(1, charges);
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public int getPotency() {
            return potency;
        }

        public int getCharges() {
            return charges;
        }
    }

    public static final class PotionRecipe {
        private final PotionIngredient bottle;
        private final PotionIngredient enzyme;
        private final PotionIngredient main;

        public PotionRecipe(Object bottle, Object enzyme, Object main) {
            this.bottle = PotionIngredient.fromSource("bottle", bottle);
            this.enzyme = PotionIngredient.fromSource("enzyme", enzyme);
            this.main = PotionIngredient.fromSource("core", main);
        }

        public PotionIngredient getBottle() {
            return bottle;
        }

        public PotionIngredient getEnzyme() {
            return enzyme;
        }

        public PotionIngredient getMain() {
            return main;
        }
    }

    public record FinalStats(int charges, int durationSeconds, int potency) { }

    public static final class PotionItemData {
        private final PotionDefinition definition;
        private final BrewType brewType;
        private final boolean splash;
        private final int enchantTier;
        private final Integer durationOverride;
        private final Integer potencyOverride;
        private final Integer chargesOverride;
        private final FinalStats finalStats;

        private PotionItemData(PotionDefinition definition,
                               BrewType brewType,
                               boolean splash,
                               int enchantTier,
                               Integer durationOverride,
                               Integer potencyOverride,
                               Integer chargesOverride) {
            this.definition = definition;
            this.brewType = brewType;
            this.splash = splash;
            this.enchantTier = enchantTier;
            this.durationOverride = sanitizeOverride(durationOverride);
            this.potencyOverride = sanitizeOverride(potencyOverride);
            this.chargesOverride = sanitizeOverride(chargesOverride);

            FinalStats baseStats = computeFinalStats(definition.getStats(brewType), enchantTier);
            int charges = applyOverride(this.chargesOverride, baseStats.charges());
            int duration = applyOverride(this.durationOverride, baseStats.durationSeconds());
            int potency = applyOverride(this.potencyOverride, baseStats.potency());
            this.finalStats = new FinalStats(charges, duration, potency);
        }

        public PotionDefinition getDefinition() {
            return definition;
        }

        public BrewType getBrewType() {
            return brewType;
        }

        public boolean isSplash() {
            return splash;
        }

        public int getEnchantTier() {
            return enchantTier;
        }

        public FinalStats getFinalStats() {
            return finalStats;
        }

        public String getAccentColor() {
            return definition.getAccentColor();
        }

        public Integer getDurationOverride() {
            return durationOverride;
        }

        public Integer getPotencyOverride() {
            return potencyOverride;
        }

        public Integer getChargesOverride() {
            return chargesOverride;
        }

        public Color getBukkitColor() {
            return definition.getAccentBukkitColor();
        }

        public String getDisplayName() {
            return definition.getDisplayName();
        }

        public String effectKey() {
            return definition.getId() + ":" + brewType.name();
        }

        public static Optional<PotionItemData> from(ItemStack stack) {
            if (stack == null) return Optional.empty();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return Optional.empty();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            String id = container.get(KEY_ID, PersistentDataType.STRING);
            String typeName = container.get(KEY_TYPE, PersistentDataType.STRING);
            Integer enchant = container.get(KEY_ENCHANT, PersistentDataType.INTEGER);
            Byte splash = container.get(KEY_SPLASH, PersistentDataType.BYTE);

            if (id == null || typeName == null) {
                return Optional.empty();
            }

            PotionDefinition definition = getById(id).orElse(null);
            if (definition == null) {
                return Optional.empty();
            }

            BrewType brewType;
            try {
                brewType = BrewType.valueOf(typeName);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            int enchantTier = enchant != null ? Math.max(0, Math.min(3, enchant)) : 0;
            boolean isSplash = splash != null && splash == 1;

            Integer durationOverride = container.get(KEY_OVERRIDE_DURATION, PersistentDataType.INTEGER);
            Integer potencyOverride = container.get(KEY_OVERRIDE_POTENCY, PersistentDataType.INTEGER);
            Integer chargesOverride = container.get(KEY_OVERRIDE_CHARGES, PersistentDataType.INTEGER);

            return Optional.of(new PotionItemData(definition, brewType, isSplash, enchantTier,
                    durationOverride, potencyOverride, chargesOverride));
        }
    }

    public static final class PotionIngredient {
        private final Material material;
        private final String customKey;
        private final String friendlyName;
        private ItemStack cachedTemplate;
        private boolean templateLookupFailed;

        private PotionIngredient(Material material, String customKey, String friendlyName) {
            this.material = material;
            this.customKey = customKey == null ? null : customKey.trim();
            this.friendlyName = friendlyName;
        }

        public static PotionIngredient fromSource(String slot, Object source) {
            if (source instanceof PotionIngredient ingredient) {
                return ingredient;
            }
            if (source instanceof Material material) {
                return new PotionIngredient(material, null, null);
            }
            if (source instanceof String key) {
                return new PotionIngredient(null, key, null);
            }
            throw new IllegalArgumentException("Unsupported ingredient type for " + slot + ": " + source);
        }

        public String friendlyName() {
            if (friendlyName != null && !friendlyName.isEmpty()) {
                return ChatColor.stripColor(friendlyName);
            }
            if (material != null) {
                return beautify(material.name());
            }
            ItemStack template = getTemplate();
            if (template != null) {
                ItemMeta meta = template.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    return ChatColor.stripColor(meta.getDisplayName());
                }
                return beautify(template.getType().name());
            }
            if (customKey != null) {
                return beautify(customKey);
            }
            return "Ingredient";
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.getType() == Material.AIR) {
                return false;
            }
            if (material != null) {
                return stack.getType() == material;
            }
            ItemStack template = getTemplate();
            if (template == null) {
                return false;
            }
            ItemStack normalized = normalizeAmount(stack);
            return isSimilarCustom(template, normalized);
        }

        public ItemStack displayItem() {
            if (material != null) {
                return new ItemStack(material);
            }
            ItemStack template = getTemplate();
            if (template != null) {
                return template;
            }
            return new ItemStack(Material.BARRIER);
        }

        private ItemStack getTemplate() {
            if (customKey == null) {
                return null;
            }
            if (cachedTemplate != null) {
                return cachedTemplate.clone();
            }
            if (templateLookupFailed) {
                return null;
            }
            ItemStack item = ItemRegistry.getItemByName(customKey);
            if (item == null) {
                templateLookupFailed = true;
                return null;
            }
            ItemStack normalized = normalizeAmount(item);
            cachedTemplate = normalized;
            return normalized.clone();
        }

        private static String beautify(String raw) {
            if (raw == null || raw.isEmpty()) {
                return "Ingredient";
            }
            String lower = ChatColor.stripColor(raw);
            if (lower == null || lower.isEmpty()) {
                lower = raw;
            }
            lower = lower.replaceAll("[^a-zA-Z0-9_ ]", "").toLowerCase(Locale.ENGLISH);
            String[] pieces = lower.split("[_ ]+");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < pieces.length; i++) {
                String piece = pieces[i];
                if (piece.isEmpty()) continue;
                builder.append(piece.substring(0, 1).toUpperCase(Locale.ENGLISH));
                if (piece.length() > 1) {
                    builder.append(piece.substring(1));
                }
                if (i < pieces.length - 1) {
                    builder.append(' ');
                }
            }
            return builder.length() == 0 ? "Ingredient" : builder.toString();
        }

        private static ItemStack normalizeAmount(ItemStack stack) {
            ItemStack clone = stack.clone();
            clone.setAmount(1);
            return clone;
        }

        private static boolean isSimilarCustom(ItemStack template, ItemStack target) {
            if (template == null || target == null) {
                return false;
            }
            if (template.getType() != target.getType()) {
                return false;
            }

            ItemMeta templateMeta = template.getItemMeta();
            ItemMeta targetMeta = target.getItemMeta();
            if (templateMeta == null && targetMeta == null) {
                return true;
            }
            if (templateMeta == null || targetMeta == null) {
                return false;
            }

            if (templateMeta.hasDisplayName() || targetMeta.hasDisplayName()) {
                String templateName = ChatColor.stripColor(templateMeta.getDisplayName());
                String targetName = ChatColor.stripColor(targetMeta.getDisplayName());
                if (!Objects.equals(templateName, targetName)) {
                    return false;
                }
            }

            if (templateMeta.hasLore() || targetMeta.hasLore()) {
                List<String> templateLore = templateMeta.getLore();
                List<String> targetLore = targetMeta.getLore();
                if ((templateLore == null) != (targetLore == null)) {
                    return false;
                }
                if (templateLore != null && targetLore != null) {
                    if (templateLore.size() != targetLore.size()) {
                        return false;
                    }
                    for (int i = 0; i < templateLore.size(); i++) {
                        String lhs = ChatColor.stripColor(templateLore.get(i));
                        String rhs = ChatColor.stripColor(targetLore.get(i));
                        if (!Objects.equals(lhs, rhs)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    private static Integer sanitizeOverride(Integer value) {
        if (value == null) {
            return null;
        }
        return value > 0 ? value : null;
    }

    private static int applyOverride(Integer override, int fallback) {
        if (override == null) {
            return Math.max(1, fallback);
        }
        return Math.max(1, override);
    }
}
