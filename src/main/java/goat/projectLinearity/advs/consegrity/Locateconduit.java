package goat.projectLinearity.advs.consegrity;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Locateconduit extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.consegrity_NAMESPACE, "locateconduit");


  public Locateconduit(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.HEART_OF_THE_SEA, "PrismaPrize", AdvancementFrameType.GOAL, true, true, x, y , "Fish up a Conduit Component"), parent, 1);
  }
}