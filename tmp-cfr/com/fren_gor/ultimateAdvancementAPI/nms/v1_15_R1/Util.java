/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  net.minecraft.server.v1_15_R1.Advancement
 *  net.minecraft.server.v1_15_R1.AdvancementProgress
 *  net.minecraft.server.v1_15_R1.ChatComponentText
 *  net.minecraft.server.v1_15_R1.Criterion
 *  net.minecraft.server.v1_15_R1.CriterionInstance
 *  net.minecraft.server.v1_15_R1.CriterionProgress
 *  net.minecraft.server.v1_15_R1.CriterionTriggerImpossible$a
 *  net.minecraft.server.v1_15_R1.IChatBaseComponent
 *  net.minecraft.server.v1_15_R1.IChatBaseComponent$ChatSerializer
 *  net.minecraft.server.v1_15_R1.Packet
 *  org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer
 *  org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_15_R1;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_15_R1.Advancement;
import net.minecraft.server.v1_15_R1.AdvancementProgress;
import net.minecraft.server.v1_15_R1.ChatComponentText;
import net.minecraft.server.v1_15_R1.Criterion;
import net.minecraft.server.v1_15_R1.CriterionInstance;
import net.minecraft.server.v1_15_R1.CriterionProgress;
import net.minecraft.server.v1_15_R1.CriterionTriggerImpossible;
import net.minecraft.server.v1_15_R1.IChatBaseComponent;
import net.minecraft.server.v1_15_R1.Packet;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class Util {
    private static final IChatBaseComponent EMPTY = new ChatComponentText("");

    @NotNull
    public static Map<String, Criterion> getAdvancementCriteria(@Range(from=1L, to=0x7FFFFFFFL) int maxProgressions) {
        Preconditions.checkArgument((maxProgressions >= 1 ? 1 : 0) != 0, (Object)"Max progressions must be >= 1.");
        HashMap advCriteria = Maps.newHashMapWithExpectedSize((int)maxProgressions);
        for (int i = 0; i < maxProgressions; ++i) {
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

    public static void sendTo(@NotNull Player player, @NotNull Packet<?> packet) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Preconditions.checkNotNull(packet, (Object)"Packet is null.");
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }

    @NotNull
    public static IChatBaseComponent fromString(@NotNull String string) {
        if (string == null || string.isEmpty()) {
            return EMPTY;
        }
        return CraftChatMessage.fromStringOrNull((String)string, (boolean)true);
    }

    @NotNull
    public static IChatBaseComponent fromComponent(@NotNull BaseComponent component) {
        if (component == null) {
            return EMPTY;
        }
        try {
            IChatBaseComponent base = IChatBaseComponent.ChatSerializer.a((String)ComponentSerializer.toString((BaseComponent)component));
            return base == null ? EMPTY : base;
        }
        catch (Exception e) {
            return EMPTY;
        }
    }

    private Util() {
        throw new UnsupportedOperationException("Utility class.");
    }
}

