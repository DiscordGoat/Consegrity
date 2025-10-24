package goat.projectLinearity.subsystems.advancements.advs.swamp;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Potenttonic extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.swamp_NAMESPACE, "potenttonic");


  public Potenttonic(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.REDSTONE_BLOCK, "Hypertonic", AdvancementFrameType.CHALLENGE, true, true, x, y , "Use a Redstone Catalyst to create a Concentrated Tonic"), parent, 1);
  }
}