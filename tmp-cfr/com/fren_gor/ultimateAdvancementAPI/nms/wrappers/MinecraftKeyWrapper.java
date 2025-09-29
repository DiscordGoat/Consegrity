/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.AbstractWrapper;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;

public abstract class MinecraftKeyWrapper
extends AbstractWrapper
implements Comparable<MinecraftKeyWrapper> {
    private static Constructor<? extends MinecraftKeyWrapper> minecraftKeyConstructor;
    private static Constructor<? extends MinecraftKeyWrapper> namespacedKeyConstructor;

    @NotNull
    public static MinecraftKeyWrapper craft(@NotNull Object minecraftKey) throws ReflectiveOperationException, ClassCastException {
        return minecraftKeyConstructor.newInstance(minecraftKey);
    }

    @NotNull
    public static MinecraftKeyWrapper craft(@NotNull String namespace, @NotNull String key) throws ReflectiveOperationException, IllegalArgumentException {
        return namespacedKeyConstructor.newInstance(namespace, key);
    }

    @NotNull
    public abstract String getNamespace();

    @NotNull
    public abstract String getKey();

    @Override
    public String toString() {
        return this.getNamespace() + ":" + this.getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MinecraftKeyWrapper that = (MinecraftKeyWrapper)o;
        if (!this.getNamespace().equals(that.getNamespace())) {
            return false;
        }
        return this.getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        int result = this.getNamespace().hashCode();
        result = 31 * result + this.getKey().hashCode();
        return result;
    }

    static {
        Class<MinecraftKeyWrapper> clazz = ReflectionUtil.getWrapperClass(MinecraftKeyWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            minecraftKeyConstructor = clazz.getDeclaredConstructor(Object.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            namespacedKeyConstructor = clazz.getDeclaredConstructor(String.class, String.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

