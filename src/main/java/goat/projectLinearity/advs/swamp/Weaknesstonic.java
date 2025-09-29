package goat.projectLinearity.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Weaknesstonic extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "weaknesstonic");


  public Weaknesstonic(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.WOODEN_SWORD, "Unmotivated", AdvancementFrameType.GOAL, true, true, x, y , "Brew a Weakness Tonic"), parent, 1);
  }
}