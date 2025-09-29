/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.ISendable;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public abstract class PacketPlayOutAdvancementsWrapper
implements ISendable {
    private static Constructor<? extends PacketPlayOutAdvancementsWrapper> resetConstructor;
    private static Constructor<? extends PacketPlayOutAdvancementsWrapper> sendConstructor;
    private static Constructor<? extends PacketPlayOutAdvancementsWrapper> removeConstructor;

    @NotNull
    public static PacketPlayOutAdvancementsWrapper craftResetPacket() throws ReflectiveOperationException {
        return resetConstructor.newInstance(new Object[0]);
    }

    @NotNull
    public static PacketPlayOutAdvancementsWrapper craftSendPacket(@NotNull Map<AdvancementWrapper, Integer> toSend) throws ReflectiveOperationException {
        return sendConstructor.newInstance(toSend);
    }

    @NotNull
    public static PacketPlayOutAdvancementsWrapper craftRemovePacket(@NotNull Set<MinecraftKeyWrapper> toRemove) throws ReflectiveOperationException {
        return removeConstructor.newInstance(toRemove);
    }

    static {
        Class<PacketPlayOutAdvancementsWrapper> clazz = ReflectionUtil.getWrapperClass(PacketPlayOutAdvancementsWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            resetConstructor = clazz.getDeclaredConstructor(new Class[0]);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            sendConstructor = clazz.getDeclaredConstructor(Map.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            removeConstructor = clazz.getDeclaredConstructor(Set.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

