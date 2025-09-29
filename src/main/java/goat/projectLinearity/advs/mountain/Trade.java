package goat.projectLinearity.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Trade extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "trade");


  public Trade(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.CRAFTING_TABLE, "Stonks", AdvancementFrameType.GOAL, true, true, x, y , "Trade with a Villager"), parent, 1);
  }
}