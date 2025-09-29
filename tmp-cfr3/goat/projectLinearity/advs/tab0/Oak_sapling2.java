/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package goat.projectLinearity.advs.tab0;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.FancyAdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import goat.projectLinearity.advs.AdvancementTabNamespaces;
import org.bukkit.Material;

public class Oak_sapling2
extends BaseAdvancement {
    public static AdvancementKey KEY = new AdvancementKey(AdvancementTabNamespaces.tab0_NAMESPACE, "oak_sapling2");

    public Oak_sapling2(Advancement parent) {
        super(KEY.getKey(), new FancyAdvancementDisplay(Material.OAK_SAPLING, "Get 1 Wood", AdvancementFrameType.TASK, true, true, 1.0f, 0.0f, ""), parent, 1);
    }
}

