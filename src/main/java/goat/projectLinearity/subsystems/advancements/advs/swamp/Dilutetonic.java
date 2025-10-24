package goat.projectLinearity.subsystems.advancements.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Dilutetonic extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "dilutetonic");


  public Dilutetonic(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.CAULDRON, "Hypotonic", AdvancementFrameType.GOAL, true, true, x, y , "Use a Water Cauldron to create a Diluted Tonic"), parent, 1);
  }
}