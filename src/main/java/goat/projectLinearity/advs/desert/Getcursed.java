package goat.projectLinearity.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.HiddenVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Getcursed extends BaseAdvancement implements HiddenVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "getcursed");


  public Getcursed(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.FIRE_CORAL, " #$@&%*!", AdvancementFrameType.TASK, true, true, x, y , "Get Cursed"), parent, 1);
  }
}