package goat.projectLinearity.subsystems.advancements.advs.mountain;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.VanillaVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Startsuffocate extends BaseAdvancement implements VanillaVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mountain_NAMESPACE, "startsuffocate");


  public Startsuffocate(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.GLASS_BOTTLE, "Deep Breath", AdvancementFrameType.TASK, true, true, x, y , "Enter an area with low Oxygen"), parent, 1);
  }
}