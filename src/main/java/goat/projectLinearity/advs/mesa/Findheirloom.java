package goat.projectLinearity.advs.mesa;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.HiddenVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Findheirloom extends BaseAdvancement implements HiddenVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mesa_NAMESPACE, "findheirloom");


  public Findheirloom(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.GOLD_BLOCK, "Relics of the Past", AdvancementFrameType.TASK, true, true, x, y , "Acquire a Gilded Heirloom"), parent, 1);
  }
}