package goat.projectLinearity.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Allcurses extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "allcurses");


  public Allcurses(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.TNT, "Watch your Mouth!", AdvancementFrameType.CHALLENGE, true, true, x, y , "Have every Curse at once"), parent, 1);
  }
}