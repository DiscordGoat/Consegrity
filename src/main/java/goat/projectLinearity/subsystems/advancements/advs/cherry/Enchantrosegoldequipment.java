package goat.projectLinearity.subsystems.advancements.advs.cherry;

import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.visibilities.ParentGrantedVisibility;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import org.bukkit.Material;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;

public class Enchantrosegoldequipment extends BaseAdvancement implements ParentGrantedVisibility {

  public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.cherry_NAMESPACE, "enchantrosegoldequipment");


  public Enchantrosegoldequipment(Advancement parent) {
    super(KEY.getKey(), new AdvancementDisplay(Material.ENCHANTED_GOLDEN_APPLE, "More than Myth", AdvancementFrameType.TASK, true, true, 4f, 0f , "Enchant Rosegold Equipment to acquire the strongest enchanted effect"), parent, 1);
  }
}