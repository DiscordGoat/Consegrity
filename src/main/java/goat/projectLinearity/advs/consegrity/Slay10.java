package goat.projectLinearity.advs.consegrity;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Slay10 extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.consegrity_NAMESPACE, "slay10");


  public Slay10(Advancement parent, float x, float y) {
    super(KEY.getKey(), new FancyAdvancementDisplay(Material.STONE_SWORD, "Sturdy Tactics", AdvancementFrameType.TASK, true, true, x, y ,"", "Slay 10 Monsters"), parent, 1);
  }
}