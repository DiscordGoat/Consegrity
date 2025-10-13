package goat.projectLinearity.libs.gravedigging.corpses;

import goat.minecraft.minecraftnew.subsystems.fishing.Rarity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Represents a hostile Corpse NPC definition.
 */
public class Corpse {
    private final String displayName;
    private final String skinValue;      // the “eyJ…” blob
    private final String skinSignature;  // Mojang’s signature
    private final Rarity rarity;
    private final int level;
    private final Material weaponMaterial;
    private final boolean usesBow;
    private final List<DropItem> dropItems;

    private final Random random = new Random();

    public Corpse(String displayName, Rarity rarity, int level,
                  Material weaponMaterial,
                  boolean usesBow, List<DropItem> dropItems,
                  String skinValue,
                  String skinSignature) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.level = level;
        this.weaponMaterial = weaponMaterial;
        this.usesBow = usesBow;
        this.dropItems = dropItems;
        this.skinValue     = Objects.requireNonNull(skinValue,     "skinValue");
        this.skinSignature = Objects.requireNonNull(skinSignature, "skinSignature");
    }
    public String getSkinTexture()   { return skinValue; }
    public String getSkinSignature() { return skinSignature; }
    public String getDisplayName() {
        // Pick the right ChatColor for this corpse’s rarity
        ChatColor color = getColorForRarityStatic(rarity);
        // Build "[Lv X] Name" in that color
        return color + "[Lv: " + level + "] " + displayName;
    }

    // (You already have this helper; make sure it returns the ChatColor you want)
    public static ChatColor getColorForRarityStatic(Rarity rarity) {
        return switch (rarity) {
            case COMMON     -> ChatColor.WHITE;
            case UNCOMMON   -> ChatColor.GREEN;
            case RARE       -> ChatColor.BLUE;
            case EPIC       -> ChatColor.DARK_PURPLE;
            case LEGENDARY,
                 MYTHIC    -> ChatColor.GOLD;
        };
    }
    public Rarity getRarity() { return rarity; }
    public int getLevel() { return level; }
    public Material getWeaponMaterial() { return weaponMaterial; }
    public boolean usesBow() { return usesBow; }

    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<>();
        if (dropItems == null) return drops;
        for (DropItem drop : dropItems) {
            int qty = 0;
            for (int i = 0; i < drop.getRollCount(); i++) {
                if (random.nextInt(drop.getRollDenominator()) < drop.getRollNumerator()) {
                    qty++;
                }
            }
            if (qty > 0) {
                ItemStack item = drop.getItemStack().clone();
                item.setAmount(qty);
                drops.add(item);
            }
        }
        return drops;
    }

    /** Represents an item drop with roll chance information. */
    public static class DropItem {
        private final ItemStack itemStack;
        private final int rollCount;
        private final int rollNumerator;
        private final int rollDenominator;

        public DropItem(ItemStack itemStack, int rollCount, int rollNumerator, int rollDenominator) {
            this.itemStack = itemStack;
            this.rollCount = rollCount;
            this.rollNumerator = rollNumerator;
            this.rollDenominator = rollDenominator;
        }

        public ItemStack getItemStack() { return itemStack; }
        public int getRollCount() { return rollCount; }
        public int getRollNumerator() { return rollNumerator; }
        public int getRollDenominator() { return rollDenominator; }
    }
}
