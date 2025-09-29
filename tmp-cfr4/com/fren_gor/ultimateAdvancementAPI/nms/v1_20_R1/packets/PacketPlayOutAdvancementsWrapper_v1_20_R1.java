/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.network.protocol.game.PacketPlayOutAdvancements
 *  net.minecraft.resources.MinecraftKey
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_20_R1.packets;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ListSet;
import com.fren_gor.ultimateAdvancementAPI.nms.v1_20_R1.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.PacketPlayOutAdvancementsWrapper;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.protocol.game.PacketPlayOutAdvancements;
import net.minecraft.resources.MinecraftKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PacketPlayOutAdvancementsWrapper_v1_20_R1
extends PacketPlayOutAdvancementsWrapper {
    private final PacketPlayOutAdvancements packet;

    public PacketPlayOutAdvancementsWrapper_v1_20_R1() {
        this.packet = new PacketPlayOutAdvancements(true, Collections.emptyList(), Collections.emptySet(), Collections.emptyMap());
    }

    public PacketPlayOutAdvancementsWrapper_v1_20_R1(@NotNull Map<AdvancementWrapper, Integer> toSend) {
        HashMap map = Maps.newHashMapWithExpectedSize((int)toSend.size());
        for (Map.Entry<AdvancementWrapper, Integer> e : toSend.entrySet()) {
            AdvancementWrapper adv = e.getKey();
            map.put((MinecraftKey)adv.getKey().toNMS(), Util.getAdvancementProgress((Advancement)adv.toNMS(), e.getValue()));
        }
        this.packet = new PacketPlayOutAdvancements(false, ListSet.fromWrapperSet(toSend.keySet()), Collections.emptySet(), (Map)map);
    }

    public PacketPlayOutAdvancementsWrapper_v1_20_R1(@NotNull Set<MinecraftKeyWrapper> toRemove) {
        this.packet = new PacketPlayOutAdvancements(false, Collections.emptyList(), ListSet.fromWrapperSet(toRemove), Collections.emptyMap());
    }

    @Override
    public void sendTo(@NotNull Player player) {
        Util.sendTo(player, this.packet);
    }
}

