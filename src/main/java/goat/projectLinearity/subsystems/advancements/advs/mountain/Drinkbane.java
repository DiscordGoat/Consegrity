package goat.projectLinearity.subsystems.advancements.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Drinkbane extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "drinkbane");


  public Drinkbane(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.HONEY_BOTTLE, "Time to Climb!", AdvancementFrameType.CHALLENGE, true, true, x, y , "Drink Bane of The Mountain to pause Oxygen"), parent, 1);
  }
}