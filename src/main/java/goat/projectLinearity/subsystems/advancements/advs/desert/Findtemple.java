package goat.projectLinearity.subsystems.advancements.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Findtemple extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "findtemple");


  public Findtemple(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.BLUE_TERRACOTTA, "Tomb", AdvancementFrameType.GOAL, true, true, x, y , "Find a Desert Pyramid"), parent, 1);
  }
}