/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ISendable {
    public void sendTo(@NotNull Player var1);

    default public void sendTo(Player ... players) {
        Preconditions.checkNotNull((Object)players, (Object)"Players is null.");
        for (Player p : players) {
            if (p == null) continue;
            this.sendTo(p);
        }
    }

    default public void sendTo(@NotNull Collection<Player> players) {
        Preconditions.checkNotNull(players, (Object)"Collection<Player> is null.");
        for (Player p : players) {
            if (p == null) continue;
            this.sendTo(p);
        }
    }
}

