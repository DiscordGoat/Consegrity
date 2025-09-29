/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Sets
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementNode
 *  net.minecraft.advancements.AdvancementTree
 *  net.minecraft.advancements.AdvancementTree$a
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.PacketPlayOutAdvancements
 *  net.minecraft.resources.MinecraftKey
 *  net.minecraft.server.AdvancementDataPlayer
 *  net.minecraft.server.AdvancementDataWorld
 *  net.minecraft.server.level.EntityPlayer
 *  org.apache.logging.log4j.Level
 *  org.apache.logging.log4j.Logger
 *  org.apache.logging.log4j.core.Logger
 *  org.apache.logging.log4j.simple.SimpleLogger
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.v1_21_R4.CraftServer
 *  org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer
 *  org.bukkit.entity.Player
 *  org.slf4j.Logger
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R4;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.VanillaAdvancementDisablerWrapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAdvancements;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.AdvancementDataWorld;
import net.minecraft.server.level.EntityPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

public class VanillaAdvancementDisablerWrapper_v1_21_R4
extends VanillaAdvancementDisablerWrapper {
    private static org.apache.logging.log4j.Logger LOGGER = null;
    private static Field listener;
    private static Field firstPacket;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void disableVanillaAdvancements(boolean vanillaAdvancements, boolean vanillaRecipeAdvancements) throws Exception {
        AdvancementDataWorld serverAdvancements = ((CraftServer)Bukkit.getServer()).getServer().aD();
        AdvancementTree tree = serverAdvancements.a();
        if (serverAdvancements.b.isEmpty()) {
            return;
        }
        final HashSet removed = Sets.newHashSetWithExpectedSize((int)serverAdvancements.b.size());
        final AdvancementTree.a old = (AdvancementTree.a)listener.get(tree);
        try {
            listener.set(tree, new AdvancementTree.a(){

                public void a(AdvancementNode advancement) {
                    if (old != null) {
                        old.a(advancement);
                    }
                }

                public void b(AdvancementNode advancement) {
                    removed.add(advancement.b().a());
                    if (old != null) {
                        old.b(advancement);
                    }
                }

                public void c(AdvancementNode advancement) {
                    if (old != null) {
                        old.c(advancement);
                    }
                }

                public void d(AdvancementNode advancement) {
                    removed.add(advancement.b().a());
                    if (old != null) {
                        old.d(advancement);
                    }
                }

                public void a() {
                    if (old != null) {
                        old.a();
                    }
                }
            });
            HashSet<MinecraftKey> locations = new HashSet<MinecraftKey>();
            ImmutableMap.Builder builder = ImmutableMap.builder();
            for (Map.Entry entry : serverAdvancements.b.entrySet()) {
                MinecraftKey key = (MinecraftKey)entry.getKey();
                boolean isRecipe = key.a().startsWith("recipes/");
                if (key.b().equals("minecraft") && (vanillaAdvancements && !isRecipe || vanillaRecipeAdvancements && isRecipe)) {
                    locations.add(key);
                    continue;
                }
                builder.put((Object)key, (Object)((AdvancementHolder)entry.getValue()));
            }
            serverAdvancements.b = builder.buildOrThrow();
            Level oldLevel = VanillaAdvancementDisablerWrapper_v1_21_R4.disableLogger();
            try {
                tree.a(locations);
            }
            finally {
                VanillaAdvancementDisablerWrapper_v1_21_R4.enableLogger(oldLevel);
            }
        }
        finally {
            listener.set(tree, old);
        }
        PacketPlayOutAdvancements removePacket = new PacketPlayOutAdvancements(false, Collections.emptyList(), (Set)removed, Collections.emptyMap(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            EntityPlayer mcPlayer = ((CraftPlayer)player).getHandle();
            AdvancementDataPlayer advs = mcPlayer.R();
            advs.a(serverAdvancements);
            firstPacket.setBoolean(advs, false);
            mcPlayer.f.b((Packet)removePacket);
        }
    }

    private static Level disableLogger() {
        if (LOGGER == null) {
            return null;
        }
        Level old = LOGGER.getLevel();
        org.apache.logging.log4j.Logger logger = LOGGER;
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)logger;
            coreLogger.setLevel(Level.OFF);
        } else {
            logger = LOGGER;
            if (logger instanceof SimpleLogger) {
                SimpleLogger simple = (SimpleLogger)logger;
                simple.setLevel(Level.OFF);
            }
        }
        return old;
    }

    private static void enableLogger(Level toSet) {
        if (LOGGER == null || toSet == null) {
            return;
        }
        org.apache.logging.log4j.Logger logger = LOGGER;
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)logger;
            coreLogger.setLevel(toSet);
        } else {
            logger = LOGGER;
            if (logger instanceof SimpleLogger) {
                SimpleLogger simple = (SimpleLogger)logger;
                simple.setLevel(toSet);
            }
        }
    }

    private VanillaAdvancementDisablerWrapper_v1_21_R4() {
        throw new UnsupportedOperationException("Utility class.");
    }

    static {
        try {
            listener = Arrays.stream(AdvancementTree.class.getDeclaredFields()).filter(f -> f.getType() == AdvancementTree.a.class).findFirst().orElseThrow();
            listener.setAccessible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            firstPacket = Arrays.stream(AdvancementDataPlayer.class.getDeclaredFields()).filter(f -> f.getType() == Boolean.TYPE).findFirst().orElseThrow();
            firstPacket.setAccessible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Field logger = Arrays.stream(AdvancementTree.class.getDeclaredFields()).filter(f -> f.getType() == Logger.class).findFirst().orElseThrow();
            logger.setAccessible(true);
            Logger slf4jLogger = (Logger)logger.get(null);
            LOGGER = Arrays.stream(slf4jLogger.getClass().getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).map(f -> {
                try {
                    f.setAccessible(true);
                    Object patt0$temp = f.get(slf4jLogger);
                    if (patt0$temp instanceof org.apache.logging.log4j.Logger) {
                        org.apache.logging.log4j.Logger log = (org.apache.logging.log4j.Logger)patt0$temp;
                        return log;
                    }
                }
                catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).findFirst().orElseThrow();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

