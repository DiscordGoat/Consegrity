package goat.projectLinearity.subsystems.advancements.advs.cherry;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Makerosegoldequipment extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.cherry_NAMESPACE, "makerosegoldequipment");


  public Makerosegoldequipment(Advancement parent) {
    super(KEY.getKey(), new AdvancementDisplay(Material.PINK_BANNER, "Truly Marvelous", AdvancementFrameType.TASK, true, true, 3f, 0f , "Combine Rosegold, Copper, and Golden Equipment to make Rosegold Equipment"), parent, 1);
  }
}