package goat.projectLinearity.subsystems.advancements.advs.desert;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Fulldesertenchant extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.desert_NAMESPACE, "fulldesertenchant");


  public Fulldesertenchant(Advancement parent, float x, float y) {
    super(KEY.getKey(), new AdvancementDisplay(Material.ENCHANTING_TABLE, "Anubis", AdvancementFrameType.CHALLENGE, true, true, x, y , "Acquire a full set of armor enchanted with Hieroglyphic IV"), parent, 1);
  }
}