package goat.projectLinearity.subsystems.advancements.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Slaywitch extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "slaywitch");


  public Slaywitch(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.BREWING_STAND, "Fabled", AdvancementFrameType.GOAL, true, true, x, y , "Kill a Witch"), parent, 1);
  }
}