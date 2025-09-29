package goat.projectLinearity.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.VanillaVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Slayhusk extends BaseAdvancement implements VanillaVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "slayhusk");


  public Slayhusk(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.ZOMBIE_HEAD, "Desert Zombie", AdvancementFrameType.TASK, true, true, x, y , "Slay a Husk"), parent, 1);
  }
}