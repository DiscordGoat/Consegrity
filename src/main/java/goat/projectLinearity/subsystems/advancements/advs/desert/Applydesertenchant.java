package goat.projectLinearity.subsystems.advancements.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Applydesertenchant extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "applydesertenchant");


  public Applydesertenchant(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.CHISELED_SANDSTONE, "Hieroglyphic", AdvancementFrameType.GOAL, true, true, x, y , "Combine Cursed Gold with Armor to apply the Hieroglyphic Enchantment"), parent, 1);
  }
}