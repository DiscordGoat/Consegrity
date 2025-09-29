package goat.projectLinearity.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Dropcursedgold extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "dropcursedgold");


  public Dropcursedgold(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.GOLD_INGOT, "Two Wrongs Make a Right", AdvancementFrameType.TASK, true, true, x, y , "Kill a Cursed Monster that drops Cursed Gold"), parent, 1);
  }
}