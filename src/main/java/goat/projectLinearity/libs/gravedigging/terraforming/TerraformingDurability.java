package goat.projectLinearity.libs.gravedigging.terraforming;

import goat.minecraft.minecraftnew.other.skilltree.Skill;
import goat.minecraft.minecraftnew.other.skilltree.SkillTreeManager;
import goat.minecraft.minecraftnew.other.skilltree.Talent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Grants a chance to prevent durability loss based on the player's Terraforming level.
 */
public class TerraformingDurability implements Listener {

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        int level = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(),
                Skill.TERRAFORMING, Talent.PROSPEKT);
        double chance = level * 0.01; // 1% per talent level

        if (Math.random() < chance) {
            event.setCancelled(true);
        }
    }
}
