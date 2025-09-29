package goat.projectLinearity.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.VanillaVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Getcoal extends BaseAdvancement implements VanillaVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "getcoal");


  public Getcoal(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.COAL, "Mining away", AdvancementFrameType.TASK, true, true, x, y , "Mine an Ore"), parent, 1);
  }
}