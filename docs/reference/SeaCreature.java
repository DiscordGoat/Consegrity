package goat.minecraft.minecraftnew.subsystems.fishing;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class SeaCreature {
    private String displayName;
    private Rarity rarity;
    private EntityType entityType;
    private List<DropItem> dropItems; // List of possible drops with chance and quantity range
    private Color armorColor; // Color for dyed armor
    private String playerHeadName; // Display name for player head
    private int level; // Level based on rarity
    private String skullName; // Holds the base64 URL string
    private Random random = new Random();

    public SeaCreature(String displayName, Rarity rarity, EntityType entityType, List<DropItem> dropItems,
                       Color armorColor, String skullName, int level) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.entityType = entityType;
        this.dropItems = dropItems;
        this.armorColor = armorColor;
        this.level = level;
        this.skullName = skullName;
    }

    // Add a getter for the texture
    public String getSkullName() {
        return skullName;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<>();

        for (DropItem dropItem : dropItems) {
            int quantity = 0;
            for (int i = 0; i < dropItem.getRollCount(); i++) {
                if (random.nextInt(dropItem.getRollDenominator()) < dropItem.getRollNumerator()) {
                    quantity++;
                }
            }
            if (quantity > 0) {
                ItemStack item = dropItem.getItemStack().clone();
                item.setAmount(quantity);
                drops.add(item);
            }
        }

        return drops;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public int getLevel() {
        return level;
    }

    public static ItemStack createDyedLeatherArmor(Material material, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }

    public String getColoredDisplayName() {
        // Prepend color codes based on rarity
        switch (rarity) {
            case COMMON:
                return ChatColor.WHITE + displayName;
            case UNCOMMON:
                return ChatColor.GREEN + displayName;
            case RARE:
                return ChatColor.BLUE + displayName;
            case EPIC:
                return ChatColor.DARK_PURPLE + displayName;
            case LEGENDARY:
                return ChatColor.GOLD + displayName;
            case MYTHIC:
                return ChatColor.DARK_AQUA + displayName;
            default:
                return displayName;
        }
    }

    // Inner class to represent a drop item with chance and quantity range
    public static class DropItem {
        private ItemStack itemStack;
        private int rollCount;
        private int rollNumerator;
        private int rollDenominator;

        public DropItem(ItemStack itemStack, int rollCount, int rollNumerator, int rollDenominator) {
            this.itemStack = itemStack;
            this.rollCount = rollCount;
            this.rollNumerator = rollNumerator;
            this.rollDenominator = rollDenominator;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public int getRollCount() {
            return rollCount;
        }

        public int getRollNumerator() {
            return rollNumerator;
        }

        public int getRollDenominator() {
            return rollDenominator;
        }
    }
}
