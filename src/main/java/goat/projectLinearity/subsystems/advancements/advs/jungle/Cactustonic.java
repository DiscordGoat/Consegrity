package goat.projectLinearity.subsystems.advancements.advs.jungle;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Cactustonic extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.jungle_NAMESPACE, "cactustonic");


  public Cactustonic(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.CACTUS, "Cactus32", AdvancementFrameType.GOAL, true, true, x, y , "Drink a Tonic of Immunity"), parent, 1);
  }
}