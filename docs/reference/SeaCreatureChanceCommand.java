package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.other.additionalfunctionality.BlessingUtils;
import goat.minecraft.minecraftnew.other.beacon.Catalyst;
import goat.minecraft.minecraftnew.other.beacon.CatalystManager;
import goat.minecraft.minecraftnew.other.beacon.CatalystType;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionManager;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionEffectPreferences;
import goat.minecraft.minecraftnew.other.enchanting.CustomEnchantmentManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetTrait;
import goat.minecraft.minecraftnew.utils.devtools.PlayerMeritManager;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import goat.minecraft.minecraftnew.other.skilltree.SkillTreeManager;
import goat.minecraft.minecraftnew.other.skilltree.Talent;
import goat.minecraft.minecraftnew.other.skilltree.Skill;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SeaCreatureChanceCommand implements CommandExecutor {
    private final MinecraftNew plugin;
    private final XPManager xpManager;

    public SeaCreatureChanceCommand(MinecraftNew plugin, XPManager xpManager) {
        this.plugin = plugin;
        this.xpManager = xpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        sendSeaCreatureChanceBreakdown(player);
        return true;
    }

    private void sendSeaCreatureChanceBreakdown(Player player) {
        double base = 0.0;
        int scc1 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_I);
        int scc2 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_II);
        int scc3 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_III);
        int scc4 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_IV);
        int scc5 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_V);
        double talentBonus =   scc1 * 0.5 + scc2 * 1.0 + scc3 * 1.5 + scc4 * 2.0 + scc5 * 2.5;

        int callOfTheVoidLevel = CustomEnchantmentManager.getEnchantmentLevel(player.getInventory().getItemInMainHand(), "Call of the Void");
        double callOfTheVoidBonus = callOfTheVoidLevel;

        double fountainBonus = PotionManager.isActive("Potion of Fountains", player)
                && PotionEffectPreferences.isEnabled(player, "Potion of Fountains") ? 10.0 : 0.0;
        double fountainMastery = SkillTreeManager.getInstance().hasTalent(player, Talent.FOUNTAIN_MASTERY) ? 5.0 : 0.0;

        CatalystManager catalystManager = CatalystManager.getInstance();
        double depthBonus = 0.0;
        if (catalystManager != null && catalystManager.isNearCatalyst(player.getLocation(), CatalystType.DEPTH)) {
            Catalyst nearest = catalystManager.findNearestCatalyst(player.getLocation(), CatalystType.DEPTH);
            if (nearest != null) {
                int tier = catalystManager.getCatalystTier(nearest);
                depthBonus = 5 + tier;
            }
        }

        double talismanBonus = isReforgedForSeaCreatures(player.getInventory().getItemInMainHand()) ? 5.0 : 0.0;

        PlayerMeritManager meritManager = PlayerMeritManager.getInstance(plugin);

        double fathmicPenalty = BlessingUtils.hasFullSetBonus(player, "Fathmic Iron") ? -20.0 : 0.0;

        ItemStack rod = player.getInventory().getItemInMainHand();
        int sonarLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.SONAR);
        double sonarBonus = sonarLevel;
        double nauticalBonus = 0;
        PetManager petManager = PetManager.getInstance(plugin);
        PetManager.Pet activePet = petManager.getActivePet(player);
        double petBonus = 0.0;
        if (activePet != null) {
            if (activePet.hasPerk(PetManager.PetPerk.ANGLER)) {
                petBonus += 5;
            }
            if (activePet.hasPerk(PetManager.PetPerk.HEART_OF_THE_SEA)) {
                petBonus += 10;
            }
            if (activePet.hasPerk(PetManager.PetPerk.BUDDY_SYSTEM)) {
                for (Player other : player.getWorld().getPlayers()) {
                    if (!other.equals(player) && other.getLocation().distance(player.getLocation()) <= 20) {
                        petBonus += 5;
                        break;
                    }
                }
            }
            if (activePet.hasPerk(PetManager.PetPerk.BAIT)) {
                petBonus += (double) activePet.getLevel() / 10.0;
            }
            if(activePet.getTrait().equals(PetTrait.NAUTICAL)){
                nauticalBonus += (int) activePet.getTrait().getValueForRarity(activePet.getTraitRarity());
            }
        }

        double total = base + nauticalBonus + talentBonus + callOfTheVoidBonus + fountainBonus + fountainMastery + depthBonus + talismanBonus + fathmicPenalty + sonarBonus + petBonus;

        player.sendMessage(ChatColor.AQUA + "Sea Creature Chance Breakdown:");
        player.sendMessage(ChatColor.AQUA + "Base SCC: " + ChatColor.YELLOW + "0%");
        player.sendMessage(ChatColor.AQUA + "SCC from Fishing Talents: " + ChatColor.YELLOW + String.format("%.2f", talentBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from COTV: " + ChatColor.YELLOW + String.format("%.2f", callOfTheVoidBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from Potion of Fountains: " + ChatColor.YELLOW + String.format("%.2f", fountainBonus) + "%");
        if(fountainMastery > 0){
            player.sendMessage(ChatColor.AQUA + "SCC from Fountain Mastery: " + ChatColor.YELLOW + String.format("%.2f", fountainMastery) + "%");
        }
        player.sendMessage(ChatColor.AQUA + "SCC from Depth Catalyst: " + ChatColor.YELLOW + String.format("%.2f", depthBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from Sea Creature Talisman: " + ChatColor.YELLOW + String.format("%.2f", talismanBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC Reduction from Fathmic Iron: " + ChatColor.YELLOW + String.format("%.2f", fathmicPenalty) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from Sonor Upgrade: " + ChatColor.YELLOW + String.format("%.2f", sonarBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from Pet Perks: " + ChatColor.YELLOW + String.format("%.2f", petBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "SCC from Nautical: " + ChatColor.YELLOW + String.format("%.2f", nauticalBonus) + "%");
        player.sendMessage(ChatColor.AQUA + "Total Sea Creature Chance: " + ChatColor.YELLOW + String.format("%.2f", total) + "%");
    }

    private boolean isReforgedForSeaCreatures(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String lore : meta.getLore()) {
                if (ChatColor.stripColor(lore).equals("Talisman: Sea Creature Chance")) {
                    return true;
                }
            }
        }
        return false;
    }
}
