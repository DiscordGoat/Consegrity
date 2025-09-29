/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.advancements.Criterion
 *  net.minecraft.advancements.CriterionInstance
 *  net.minecraft.advancements.CriterionProgress
 *  net.minecraft.advancements.critereon.CriterionTriggerImpossible$a
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.IChatBaseComponent
 *  net.minecraft.network.protocol.Packet
 *  org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
 *  org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_20_R1;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.critereon.CriterionTriggerImpossible;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class Util {
    @NotNull
    public static Map<String, Criterion> getAdvancementCriteria(@Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Preconditions.checkArgument((maxProgression >= 1 ? 1 : 0) != 0, (Object)"Max progression must be >= 1.");
        HashMap advCriteria = Maps.newHashMapWithExpectedSize((int)maxProgression);
        for (int i = 0; i < maxProgression; ++i) {
            advCriteria.put(String.valueOf(i), new Criterion((CriterionInstance)new CriterionTriggerImpossible.a()));
        }
        return advCriteria;
    }

    @NotNull
    public static String[][] getAdvancementRequirements(@NotNull Map<String, Criterion> advCriteria) {
        Preconditions.checkNotNull(advCriteria, (Object)"Advancement criteria map is null.");
        String[][] array = new String[advCriteria.size()][1];
        int index = 0;
        for (String name : advCriteria.keySet()) {
            array[index++][0] = name;
        }
        return array;
    }

    @NotNull
    public static AdvancementProgress getAdvancementProgress(@NotNull Advancement mcAdv, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        Preconditions.checkNotNull((Object)mcAdv, (Object)"NMS Advancement is null.");
        Preconditions.checkArgument((progression >= 0 ? 1 : 0) != 0, (Object)"Progression must be >= 0.");
        AdvancementProgress advPrg = new AdvancementProgress();
        advPrg.a(mcAdv.h(), mcAdv.k());
        for (int i = 0; i < progression; ++i) {
            CriterionProgress criteriaPrg = advPrg.c(String.valueOf(i));
            if (criteriaPrg == null) continue;
            criteriaPrg.b();
        }
        return advPrg;
    }

    @NotNull
    public static IChatBaseComponent fromString(@NotNull String string) {
        if (string == null || string.isEmpty()) {
            return CommonComponents.a;
        }
        return CraftChatMessage.fromStringOrNull((String)string, (boolean)true);
    }

    @NotNull
    public static IChatBaseComponent fromComponent(@NotNull BaseComponent component) {
        if (component == null) {
            return CommonComponents.a;
        }
        IChatBaseComponent base = CraftChatMessage.fromJSONOrNull((String)ComponentSerializer.toString((BaseComponent)component));
        return base == null ? CommonComponents.a : base;
    }

    public static void sendTo(@NotNull Player player, @NotNull Packet<?> packet) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Preconditions.checkNotNull(packet, (Object)"Packet is null.");
        ((CraftPlayer)player).getHandle().c.a(packet);
    }

    private Util() {
        throw new UnsupportedOperationException("Utility class.");
    }
}

