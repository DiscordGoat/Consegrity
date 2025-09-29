/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  net.minecraft.server.v1_16_R2.Advancement
 *  net.minecraft.server.v1_16_R2.AdvancementProgress
 *  net.minecraft.server.v1_16_R2.ChatComponentText
 *  net.minecraft.server.v1_16_R2.Criterion
 *  net.minecraft.server.v1_16_R2.CriterionInstance
 *  net.minecraft.server.v1_16_R2.CriterionProgress
 *  net.minecraft.server.v1_16_R2.CriterionTriggerImpossible$a
 *  net.minecraft.server.v1_16_R2.IChatBaseComponent
 *  net.minecraft.server.v1_16_R2.IChatBaseComponent$ChatSerializer
 *  net.minecraft.server.v1_16_R2.IChatMutableComponent
 *  net.minecraft.server.v1_16_R2.Packet
 *  org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer
 *  org.bukkit.craftbukkit.v1_16_R2.util.CraftChatMessage
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R2;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R2.Advancement;
import net.minecraft.server.v1_16_R2.AdvancementProgress;
import net.minecraft.server.v1_16_R2.ChatComponentText;
import net.minecraft.server.v1_16_R2.Criterion;
import net.minecraft.server.v1_16_R2.CriterionInstance;
import net.minecraft.server.v1_16_R2.CriterionProgress;
import net.minecraft.server.v1_16_R2.CriterionTriggerImpossible;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;
import net.minecraft.server.v1_16_R2.IChatMutableComponent;
import net.minecraft.server.v1_16_R2.Packet;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftChatMessage;
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
        advPrg.a(mcAdv.getCriteria(), mcAdv.i());
        for (int i = 0; i < progression; ++i) {
            CriterionProgress criteriaPrg = advPrg.getCriterionProgress(String.valueOf(i));
            if (criteriaPrg == null) continue;
            criteriaPrg.b();
        }
        return advPrg;
    }

    @NotNull
    public static IChatBaseComponent fromString(@NotNull String string) {
        if (string == null || string.isEmpty()) {
            return ChatComponentText.d;
        }
        return CraftChatMessage.fromStringOrNull((String)string, (boolean)true);
    }

    @NotNull
    public static IChatBaseComponent fromComponent(@NotNull BaseComponent component) {
        if (component == null) {
            return ChatComponentText.d;
        }
        try {
            IChatMutableComponent base = IChatBaseComponent.ChatSerializer.a((String)ComponentSerializer.toString((BaseComponent)component));
            return base == null ? ChatComponentText.d : base;
        }
        catch (Exception e) {
            return ChatComponentText.d;
        }
    }

    public static void sendTo(@NotNull Player player, @NotNull Packet<?> packet) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Preconditions.checkNotNull(packet, (Object)"Packet is null.");
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }

    private Util() {
        throw new UnsupportedOperationException("Utility class.");
    }
}

