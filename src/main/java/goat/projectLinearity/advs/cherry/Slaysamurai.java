package goat.projectLinearity.advs.cherry;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Slaysamurai extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.cherry_NAMESPACE, "slaysamurai");


  public Slaysamurai(Advancement parent) {
    super(KEY.getKey(), new AdvancementDisplay(Material.PINK_DYE, "Duelist", AdvancementFrameType.GOAL, true, true, 1f, 0f , "Defeat a Samurai"), parent, 1);
  }
}