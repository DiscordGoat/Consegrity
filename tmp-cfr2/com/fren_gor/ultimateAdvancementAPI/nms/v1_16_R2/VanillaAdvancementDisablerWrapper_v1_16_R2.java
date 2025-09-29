/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  net.minecraft.server.v1_16_R2.Advancement
 *  net.minecraft.server.v1_16_R2.AdvancementDataPlayer
 *  net.minecraft.server.v1_16_R2.AdvancementDataWorld
 *  net.minecraft.server.v1_16_R2.Advancements
 *  net.minecraft.server.v1_16_R2.EntityPlayer
 *  net.minecraft.server.v1_16_R2.MinecraftKey
 *  net.minecraft.server.v1_16_R2.Packet
 *  net.minecraft.server.v1_16_R2.PacketPlayOutAdvancements
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.v1_16_R2.CraftServer
 *  org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R2;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.VanillaAdvancementDisablerWrapper;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.v1_16_R2.Advancement;
import net.minecraft.server.v1_16_R2.AdvancementDataPlayer;
import net.minecraft.server.v1_16_R2.AdvancementDataWorld;
import net.minecraft.server.v1_16_R2.Advancements;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.Packet;
import net.minecraft.server.v1_16_R2.PacketPlayOutAdvancements;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class VanillaAdvancementDisablerWrapper_v1_16_R2
extends VanillaAdvancementDisablerWrapper {
    private static Field advancementRoots;
    private static Field advancementTasks;
    private static Field firstPacket;

    public static void disableVanillaAdvancements(boolean vanillaAdvancements, boolean vanillaRecipeAdvancements) throws Exception {
        AdvancementDataWorld serverAdvancements = ((CraftServer)Bukkit.getServer()).getServer().getAdvancementData();
        Advancements registry = serverAdvancements.REGISTRY;
        if (registry.advancements.isEmpty()) {
            return;
        }
        Set advRoots = (Set)advancementRoots.get(registry);
        Set advTasks = (Set)advancementTasks.get(registry);
        HashSet removed = Sets.newHashSetWithExpectedSize((int)registry.advancements.size());
        Iterator it = registry.advancements.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = it.next();
            MinecraftKey key = (MinecraftKey)e.getKey();
            boolean isRecipe = key.getKey().startsWith("recipes/");
            if (!key.getNamespace().equals("minecraft") || (!vanillaAdvancements || isRecipe) && (!vanillaRecipeAdvancements || !isRecipe)) continue;
            Advancement adv = (Advancement)e.getValue();
            if (adv.b() == null) {
                advRoots.remove(adv);
            } else {
                advTasks.remove(adv);
            }
            it.remove();
            removed.add((MinecraftKey)e.getKey());
        }
        PacketPlayOutAdvancements removePacket = new PacketPlayOutAdvancements(false, Collections.emptySet(), (Set)removed, Collections.emptyMap());
        for (Player player : Bukkit.getOnlinePlayers()) {
            EntityPlayer mcPlayer = ((CraftPlayer)player).getHandle();
            AdvancementDataPlayer advs = mcPlayer.getAdvancementData();
            advs.a(serverAdvancements);
            firstPacket.setBoolean(advs, false);
            mcPlayer.playerConnection.sendPacket((Packet)removePacket);
        }
    }

    private VanillaAdvancementDisablerWrapper_v1_16_R2() {
        throw new UnsupportedOperationException("Utility class.");
    }

    static {
        try {
            advancementRoots = Advancements.class.getDeclaredField("c");
            advancementRoots.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            advancementTasks = Advancements.class.getDeclaredField("d");
            advancementTasks.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            firstPacket = AdvancementDataPlayer.class.getDeclaredField("m");
            firstPacket.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

