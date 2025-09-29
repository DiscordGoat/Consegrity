package goat.projectLinearity.advs.consegrity;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.VanillaVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Freezing extends BaseAdvancement implements VanillaVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.consegrity_NAMESPACE, "freezing");


  public Freezing(Advancement parent, float x, float y) {
    super(KEY.getKey(), new FancyAdvancementDisplay(Material.ICE, "Chilling Prospect", AdvancementFrameType.TASK, true, true, x, y ,"", "Enter a Cold Sector "), parent, 1);
  }
}