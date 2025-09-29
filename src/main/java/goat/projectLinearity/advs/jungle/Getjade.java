package goat.projectLinearity.advs.jungle;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Getjade extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.jungle_NAMESPACE, "getjade");


  public Getjade(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.EMERALD, "Jaded", AdvancementFrameType.TASK, true, true, x, y , "Obtain a Jade Gemstone"), parent, 1);
  }
}