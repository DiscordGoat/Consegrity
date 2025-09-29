/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.advancements.Advancements
 *  net.minecraft.advancements.Advancements$a
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
 *  org.bukkit.craftbukkit.v1_18_R1.CraftServer
 *  org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_18_R1;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.VanillaAdvancementDisablerWrapper;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Advancements;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAdvancements;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.AdvancementDataWorld;
import net.minecraft.server.level.EntityPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class VanillaAdvancementDisablerWrapper_v1_18_R1
extends VanillaAdvancementDisablerWrapper {
    private static Logger LOGGER = null;
    private static Field listener;
    private static Field firstPacket;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void disableVanillaAdvancements(boolean vanillaAdvancements, boolean vanillaRecipeAdvancements) throws Exception {
        AdvancementDataWorld serverAdvancements = ((CraftServer)Bukkit.getServer()).getServer().ax();
        Advancements registry = serverAdvancements.c;
        if (registry.b.isEmpty()) {
            return;
        }
        final HashSet removed = Sets.newHashSetWithExpectedSize((int)registry.b.size());
        final Advancements.a old = (Advancements.a)listener.get(registry);
        try {
            listener.set(registry, new Advancements.a(){

                public void a(Advancement advancement) {
                    if (old != null) {
                        old.a(advancement);
                    }
                }

                public void b(Advancement advancement) {
                    removed.add(advancement.h());
                    if (old != null) {
                        old.b(advancement);
                    }
                }

                public void c(Advancement advancement) {
                    if (old != null) {
                        old.c(advancement);
                    }
                }

                public void d(Advancement advancement) {
                    removed.add(advancement.h());
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
            for (Advancement root : registry.b()) {
                MinecraftKey key = root.h();
                boolean isRecipe = key.a().startsWith("recipes/");
                if (!key.b().equals("minecraft") || (!vanillaAdvancements || isRecipe) && (!vanillaRecipeAdvancements || !isRecipe)) continue;
                locations.add(key);
            }
            Level oldLevel = VanillaAdvancementDisablerWrapper_v1_18_R1.disableLogger();
            try {
                registry.a(locations);
            }
            finally {
                VanillaAdvancementDisablerWrapper_v1_18_R1.enableLogger(oldLevel);
            }
        }
        finally {
            listener.set(registry, old);
        }
        PacketPlayOutAdvancements removePacket = new PacketPlayOutAdvancements(false, Collections.emptyList(), (Set)removed, Collections.emptyMap());
        for (Player player : Bukkit.getOnlinePlayers()) {
            EntityPlayer mcPlayer = ((CraftPlayer)player).getHandle();
            AdvancementDataPlayer advs = mcPlayer.M();
            advs.a(serverAdvancements);
            firstPacket.setBoolean(advs, false);
            mcPlayer.b.a((Packet)removePacket);
        }
    }

    private static Level disableLogger() {
        Level old = LOGGER.getLevel();
        Logger logger = LOGGER;
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
        Logger logger = LOGGER;
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

    private VanillaAdvancementDisablerWrapper_v1_18_R1() {
        throw new UnsupportedOperationException("Utility class.");
    }

    static {
        try {
            listener = Arrays.stream(Advancements.class.getDeclaredFields()).filter(f -> f.getType() == Advancements.a.class).findFirst().orElseThrow();
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
            Field logger = Arrays.stream(Advancements.class.getDeclaredFields()).filter(f -> f.getType() == Logger.class).findFirst().orElseThrow();
            logger.setAccessible(true);
            LOGGER = (Logger)logger.get(null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

