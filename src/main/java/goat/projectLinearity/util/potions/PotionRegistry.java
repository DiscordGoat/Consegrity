package goat.projectLinearity.util.potions;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.CustomDurabilityManager;
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

    static {
        register(
                new PotionDefinition(
                        "swiftness",
                        ChatColor.AQUA,
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
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "strength",
                        ChatColor.DARK_RED,
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                Material.BLAZE_POWDER
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                Material.BLAZE_ROD
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "oxygen_recovery",
                        ChatColor.DARK_AQUA,
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.HONEY_BOTTLE,
                                "Forbidden Book"
                        ),
                        new PotionRecipe(
                                Material.GLASS_BOTTLE,
                                Material.NETHER_WART,
                                "Forbidden Book"
                        ),
                        new PotionStats(60, 1, 20),
                        new PotionStats(120, 2, 10)
                )
        );
        register(
                new PotionDefinition(
                        "slowness",
                        ChatColor.BLUE,
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

    public static void incrementEnchantTier(ItemStack stack, int newTier) {
        PotionItemData.from(stack).ifPresent(data -> {
            int capped = Math.max(0, Math.min(3, newTier));
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                writePotionData(meta, data.getDefinition(), data.getBrewType(), capped, data.isSplash());
                stack.setItemMeta(meta);
            }
            FinalStats stats = computeFinalStats(data.getDefinition().getStats(data.getBrewType()), capped);
            CustomDurabilityManager manager = CustomDurabilityManager.getInstance();
            manager.setCustomDurability(stack, Math.min(manager.getCurrentDurability(stack), stats.charges()), stats.charges());
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
            if (!durabilityManager.isTracked(item)) {
                durabilityManager.setCustomDurability(item, stats.charges(), stats.charges());
            } else {
                int max = durabilityManager.getMaxDurability(item);
                if (max <= 0) {
                    durabilityManager.setCustomDurability(item, stats.charges(), stats.charges());
                } else {
                    int current = Math.max(0, Math.min(durabilityManager.getCurrentDurability(item), max));
                    durabilityManager.setCustomDurability(item, current, max);
                }
            }
            int current = durabilityManager.getCurrentDurability(item);
            int max = durabilityManager.getMaxDurability(item);
            applyPotionLore(item, data, current, max);
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

    private static Color convertColor(ChatColor chatColor) {
        if (chatColor == null) {
            return Color.WHITE;
        }
        return switch (chatColor) {
            case AQUA -> Color.fromRGB(0, 194, 255);
            case DARK_AQUA -> Color.fromRGB(0, 124, 148);
            case DARK_RED -> Color.fromRGB(153, 0, 0);
            case RED -> Color.fromRGB(255, 64, 64);
            case GOLD -> Color.fromRGB(255, 170, 0);
            case YELLOW -> Color.fromRGB(255, 252, 61);
            case GREEN -> Color.fromRGB(64, 204, 64);
            case DARK_GREEN -> Color.fromRGB(0, 102, 0);
            case BLUE -> Color.fromRGB(64, 64, 255);
            case DARK_BLUE -> Color.fromRGB(0, 0, 170);
            case LIGHT_PURPLE -> Color.fromRGB(255, 128, 255);
            case DARK_PURPLE -> Color.fromRGB(128, 0, 128);
            case WHITE -> Color.fromRGB(255, 255, 255);
            case GRAY, DARK_GRAY -> Color.fromRGB(96, 96, 96);
            default -> Color.WHITE;
        };
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
        private final ChatColor accentColor;
        private final Color accentBukkitColor;
        private final PotionRecipe overworldRecipe;
        private final PotionRecipe netherRecipe;
        private final PotionStats overworldStats;
        private final PotionStats netherStats;
        private final NamespacedKey discoveryKey;
        private final Map<BrewType, NamespacedKey> discoveryKeys;

        private PotionDefinition(String id,
                                 ChatColor accentColor,
                                 PotionRecipe overworldRecipe,
                                 PotionRecipe netherRecipe,
                                 PotionStats overworldStats,
                                 PotionStats netherStats) {
            this.id = Objects.requireNonNull(id, "id").toLowerCase(Locale.ENGLISH);
            this.displayName = formatDisplayName(id);
            this.accentColor = Objects.requireNonNullElse(accentColor, ChatColor.WHITE);
            this.accentBukkitColor = convertColor(this.accentColor);
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

        public ChatColor getAccentColor() {
            return accentColor;
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
        private final FinalStats finalStats;

        private PotionItemData(PotionDefinition definition, BrewType brewType, boolean splash, int enchantTier) {
            this.definition = definition;
            this.brewType = brewType;
            this.splash = splash;
            this.enchantTier = enchantTier;
            this.finalStats = computeFinalStats(definition.getStats(brewType), enchantTier);
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

        public ChatColor getAccentColor() {
            return definition.getAccentColor();
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

            return Optional.of(new PotionItemData(definition, brewType, isSplash, enchantTier));
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
}
