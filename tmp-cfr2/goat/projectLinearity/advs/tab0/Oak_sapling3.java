/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package goat.projectLinearity.subsystems.advancements.advs.tab0;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.subsystems.advancements.advs.AdvancementTabNamespaces;
import org.bukkit.Material;

public class Oak_sapling3
extends BaseAdvancement {
    public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.tab0_NAMESPACE, "oak_sapling3");

    public Oak_sapling3(Advancement parent) {
        super(KEY.getKey(), new FancyAdvancementDisplay(Material.OAK_SAPLING, "oak_sapling3", AdvancementFrameType.TASK, true, true, 2.0f, 0.0f, ""), parent, 1);
    }
}

