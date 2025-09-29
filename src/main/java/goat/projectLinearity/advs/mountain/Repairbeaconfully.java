package goat.projectLinearity.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Repairbeaconfully extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "repairbeaconfully");


  public Repairbeaconfully(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.BEACON, "A Really Expensive Fan", AdvancementFrameType.CHALLENGE, true, true, x, y , "Fully Repair The Beacon"), parent, 1);
  }
}