package goat.projectLinearity.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Tonic1 extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "tonic1");


  public Tonic1(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.GLASS_BOTTLE, "Performance Enhancer", AdvancementFrameType.TASK, true, true, x, y , "Drink a Tier I Tonic"), parent, 1);
  }
}