package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionManager;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionEffectPreferences;
import goat.minecraft.minecraftnew.other.enchanting.CustomEnchantmentManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetManager;
import goat.minecraft.minecraftnew.other.beacon.Catalyst;
import goat.minecraft.minecraftnew.other.beacon.CatalystManager;
import goat.minecraft.minecraftnew.other.beacon.CatalystType;
import goat.minecraft.minecraftnew.subsystems.pets.PetTrait;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TreasureChanceCommand implements CommandExecutor {
    private final MinecraftNew plugin;

    public TreasureChanceCommand(MinecraftNew plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        sendTreasureChanceBreakdown(player);
        return true;
    }

    private void sendTreasureChanceBreakdown(Player player) {
        double base = 5.0;

        ItemStack rod = player.getInventory().getItemInMainHand();
        int upgradeLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.TREASURE_HUNTER);
        double upgradeBonus = upgradeLevel;
        double treasureChance = 0.0;

        PetManager petManager = PetManager.getInstance(plugin);
        PetManager.Pet activePet = petManager.getActivePet(player);
        double petBonus = 0.0;
        if (activePet != null && activePet.hasPerk(PetManager.PetPerk.TREASURE_HUNTER)) {
            petBonus = activePet.getLevel() * 0.1;
        }

        double potionBonus = PotionManager.isActive("Potion of Liquid Luck", player)
                && PotionEffectPreferences.isEnabled(player, "Potion of Liquid Luck") ? 20.0 : 0.0;

        int piracyLevel = CustomEnchantmentManager.getEnchantmentLevel(player.getInventory().getItemInMainHand(), "Piracy");
        double piracyBonus = piracyLevel;

        CatalystManager catalystManager = CatalystManager.getInstance();
        double depthBonus = 0.0;
        if (catalystManager != null && catalystManager.isNearCatalyst(player.getLocation(), CatalystType.DEPTH)) {
            Catalyst nearest = catalystManager.findNearestCatalyst(player.getLocation(), CatalystType.DEPTH);
            if (nearest != null) {
                int tier = catalystManager.getCatalystTier(nearest);
                depthBonus = 5 + tier;
            }
        }
        if(petManager.getActivePet(player).getTrait().equals(PetTrait.TREASURED)){
            treasureChance += (petManager.getActivePet(player).getTrait().getValueForRarity(petManager.getActivePet(player).getTraitRarity()) / 100);
        }
        double total = base + treasureChance + upgradeBonus + petBonus + potionBonus + piracyBonus + depthBonus;

        player.sendMessage(ChatColor.GOLD + "Treasure Chance Breakdown:");
        player.sendMessage(ChatColor.AQUA + "Base TC: " + ChatColor.YELLOW + String.format("%.2f", base) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Treasure Hunter Upgrade: " + ChatColor.YELLOW + String.format("%.2f", upgradeBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Treasure Hunter Pet: " + ChatColor.YELLOW + String.format("%.2f", petBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Potion of Liquid Luck: " + ChatColor.YELLOW + String.format("%.2f", potionBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Piracy: " + ChatColor.YELLOW + String.format("%.2f", piracyBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Depth Catalyst: " + ChatColor.YELLOW + String.format("%.2f", depthBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "TC from Treasured Trait: " + ChatColor.YELLOW + String.format("%.2f", treasureChance) + "%");
        player.sendMessage(ChatColor.GOLD + "Total Treasure Chance: " + ChatColor.YELLOW + String.format("%.2f", total) + "%");
    }
}
