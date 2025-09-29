/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.AbstractWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class AdvancementWrapper
extends AbstractWrapper {
    private static Constructor<? extends AdvancementWrapper> rootAdvancementWrapperConstructor;
    private static Constructor<? extends AdvancementWrapper> baseAdvancementWrapperConstructor;
    private MinecraftKeyWrapper key;

    @NotNull
    public static AdvancementWrapper craftRootAdvancement(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) throws ReflectiveOperationException {
        return rootAdvancementWrapperConstructor.newInstance(key, display, maxProgression);
    }

    @NotNull
    public static AdvancementWrapper craftBaseAdvancement(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementWrapper parent, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) throws ReflectiveOperationException {
        return baseAdvancementWrapperConstructor.newInstance(key, parent, display, maxProgression);
    }

    @NotNull
    public abstract MinecraftKeyWrapper getKey();

    @Nullable
    public abstract AdvancementWrapper getParent();

    @NotNull
    public abstract AdvancementDisplayWrapper getDisplay();

    public abstract @Range(from=1L, to=0x7FFFFFFFL) int getMaxProgression();

    @Override
    public String toString() {
        return "AdvancementWrapper{key=" + String.valueOf(this.getKey()) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AdvancementWrapper that = (AdvancementWrapper)o;
        return this.getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    static {
        Class<AdvancementWrapper> clazz = ReflectionUtil.getWrapperClass(AdvancementWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            rootAdvancementWrapperConstructor = clazz.getDeclaredConstructor(MinecraftKeyWrapper.class, AdvancementDisplayWrapper.class, Integer.TYPE);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            baseAdvancementWrapperConstructor = clazz.getDeclaredConstructor(MinecraftKeyWrapper.class, AdvancementWrapper.class, AdvancementDisplayWrapper.class, Integer.TYPE);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

