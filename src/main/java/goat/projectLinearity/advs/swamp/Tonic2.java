package goat.projectLinearity.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Tonic2 extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "tonic2");


  public Tonic2(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.SPLASH_POTION, "Maybe chase that with some water...", AdvancementFrameType.GOAL, true, true, x, y , "Drink a Tier II Tonic"), parent, 1);
  }
}