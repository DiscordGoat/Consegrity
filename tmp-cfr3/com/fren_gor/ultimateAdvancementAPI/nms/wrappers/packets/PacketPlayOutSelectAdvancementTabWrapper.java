/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.packets.ISendable;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;

public abstract class PacketPlayOutSelectAdvancementTabWrapper
implements ISendable {
    private static Constructor<? extends PacketPlayOutSelectAdvancementTabWrapper> selectNoneConstructor;
    private static Constructor<? extends PacketPlayOutSelectAdvancementTabWrapper> selectConstructor;

    @NotNull
    public static PacketPlayOutSelectAdvancementTabWrapper craftSelectNone() throws ReflectiveOperationException {
        return selectNoneConstructor.newInstance(new Object[0]);
    }

    @NotNull
    public static PacketPlayOutSelectAdvancementTabWrapper craftSelect(@NotNull MinecraftKeyWrapper key) throws ReflectiveOperationException {
        return selectConstructor.newInstance(key);
    }

    static {
        Class<PacketPlayOutSelectAdvancementTabWrapper> clazz = ReflectionUtil.getWrapperClass(PacketPlayOutSelectAdvancementTabWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            selectNoneConstructor = clazz.getDeclaredConstructor(new Class[0]);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            selectConstructor = clazz.getDeclaredConstructor(MinecraftKeyWrapper.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

