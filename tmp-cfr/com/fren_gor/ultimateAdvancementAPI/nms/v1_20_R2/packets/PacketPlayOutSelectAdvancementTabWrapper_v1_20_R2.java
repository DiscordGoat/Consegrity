/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.game.PacketPlayOutSelectAdvancementTab
 *  net.minecraft.resources.MinecraftKey
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_20_R2.packets;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_20_R2.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.PacketPlayOutSelectAdvancementTabWrapper;
import net.minecraft.network.protocol.game.PacketPlayOutSelectAdvancementTab;
import net.minecraft.resources.MinecraftKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PacketPlayOutSelectAdvancementTabWrapper_v1_20_R2
extends PacketPlayOutSelectAdvancementTabWrapper {
    private final PacketPlayOutSelectAdvancementTab packet;

    public PacketPlayOutSelectAdvancementTabWrapper_v1_20_R2() {
        this.packet = new PacketPlayOutSelectAdvancementTab((MinecraftKey)null);
    }

    public PacketPlayOutSelectAdvancementTabWrapper_v1_20_R2(@NotNull MinecraftKeyWrapper key) {
        this.packet = new PacketPlayOutSelectAdvancementTab((MinecraftKey)key.toNMS());
    }

    @Override
    public void sendTo(@NotNull Player player) {
        Util.sendTo(player, this.packet);
    }
}

