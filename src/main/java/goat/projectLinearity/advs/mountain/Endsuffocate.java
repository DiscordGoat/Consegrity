package goat.projectLinearity.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Endsuffocate extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "endsuffocate");


  public Endsuffocate(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.DEEPSLATE_TILE_WALL, "Deep Trouble", AdvancementFrameType.GOAL, true, true, x, y , "Run out of Oxygen and start Suffocating"), parent, 1);
  }
}