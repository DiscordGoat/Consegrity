package goat.projectLinearity.subsystems.advancements.advs.jungle;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Getinfected extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.jungle_NAMESPACE, "getinfected");


  public Getinfected(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.VINE, "Virus", AdvancementFrameType.TASK, true, true, x, y , "Get Infected by The Disease in The Jungle"), parent, 1);
  }
}