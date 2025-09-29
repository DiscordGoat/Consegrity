package goat.projectLinearity.advs.mesa;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Barterkali extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.mesa_NAMESPACE, "barterkali");


  public Barterkali(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.CHEST, "Gambling", AdvancementFrameType.GOAL, true, true, x, y , "Offer Kali a Golden Item"), parent, 1);
  }
}