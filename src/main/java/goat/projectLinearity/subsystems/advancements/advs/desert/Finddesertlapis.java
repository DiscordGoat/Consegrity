package goat.projectLinearity.subsystems.advancements.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Finddesertlapis extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "finddesertlapis");


  public Finddesertlapis(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.LAPIS_LAZULI, "Enchantedness", AdvancementFrameType.GOAL, true, true, x, y , "Locate Lapis Lazuli"), parent, 1);
  }
}