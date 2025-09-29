/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.advancements.AdvancementRequirements
 *  net.minecraft.advancements.Criterion
 *  net.minecraft.advancements.CriterionInstance
 *  net.minecraft.advancements.CriterionProgress
 *  net.minecraft.advancements.CriterionTrigger
 *  net.minecraft.advancements.critereon.CriterionTriggerImpossible
 *  net.minecraft.advancements.critereon.CriterionTriggerImpossible$a
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.IChatBaseComponent
 *  net.minecraft.network.protocol.Packet
 *  org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer
 *  org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R1;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.CriterionTriggerImpossible;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class Util {
    @NotNull
    public static Map<String, Criterion<?>> getAdvancementCriteria(@Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Preconditions.checkArgument((maxProgression >= 1 ? 1 : 0) != 0, (Object)"Max progression must be >= 1.");
        HashMap advCriteria = Maps.newHashMapWithExpectedSize((int)maxProgression);
        for (int i = 0; i < maxProgression; ++i) {
            advCriteria.put(String.valueOf(i), new Criterion((CriterionTrigger)new CriterionTriggerImpossible(), (CriterionInstance)new CriterionTriggerImpossible.a()));
        }
        return advCriteria;
    }

    @NotNull
    public static AdvancementRequirements getAdvancementRequirements(@NotNull Map<String, Criterion<?>> advCriteria) {
        Preconditions.checkNotNull(advCriteria, (Object)"Advancement criteria map is null.");
        ArrayList<List<String>> list = new ArrayList<List<String>>(advCriteria.size());
        for (String name : advCriteria.keySet()) {
            list.add(List.of(name));
        }
        return new AdvancementRequirements(list);
    }

    @NotNull
    public static AdvancementProgress getAdvancementProgress(@NotNull AdvancementHolder mcAdv, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        Preconditions.checkNotNull((Object)mcAdv, (Object)"NMS Advancement is null.");
        Preconditions.checkArgument((progression >= 0 ? 1 : 0) != 0, (Object)"Progression must be >= 0.");
        AdvancementProgress advPrg = new AdvancementProgress();
        advPrg.a(mcAdv.b().f());
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
        ((CraftPlayer)player).getHandle().c.b(packet);
    }

    private Util() {
        throw new UnsupportedOperationException("Utility class.");
    }
}

