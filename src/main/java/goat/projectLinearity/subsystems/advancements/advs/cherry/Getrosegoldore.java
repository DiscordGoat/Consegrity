package goat.projectLinearity.subsystems.advancements.advs.cherry;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Getrosegoldore extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.cherry_NAMESPACE, "getrosegoldore");


  public Getrosegoldore(Advancement parent) {
    super(KEY.getKey(), new AdvancementDisplay(Material.GOLD_NUGGET, "Strong as Iron, Fast as Gold", AdvancementFrameType.TASK, true, true, 2f, 0f , "Acquire Rosegold Ore from Samurai"), parent, 1);
  }
}