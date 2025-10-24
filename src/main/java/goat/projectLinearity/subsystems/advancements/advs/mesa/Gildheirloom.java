package goat.projectLinearity.subsystems.advancements.advs.mesa;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Gildheirloom extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mesa_NAMESPACE, "gildheirloom");


  public Gildheirloom(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.DAMAGED_ANVIL, "Midas", AdvancementFrameType.GOAL, true, true, x, y , "Apply Gold to an Heirloom in an Anvil to increase its Gild"), parent, 1);
  }
}