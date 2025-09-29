/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public abstract class PreparedAdvancementWrapper {
    private static Constructor<? extends PreparedAdvancementWrapper> constructor;

    @NotNull
    public static PreparedAdvancementWrapper craft(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) throws ReflectiveOperationException {
        return constructor.newInstance(key, display, maxProgression);
    }

    @NotNull
    public abstract MinecraftKeyWrapper getKey();

    @NotNull
    public abstract AdvancementDisplayWrapper getDisplay();

    public abstract @Range(from=1L, to=0x7FFFFFFFL) int getMaxProgression();

    @NotNull
    public abstract AdvancementWrapper toRootAdvancementWrapper();

    @NotNull
    public abstract AdvancementWrapper toBaseAdvancementWrapper(@NotNull AdvancementWrapper var1);

    public String toString() {
        return "PreparedAdvancementWrapper{key=" + String.valueOf(this.getKey()) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        PreparedAdvancementWrapper that = (PreparedAdvancementWrapper)o;
        return this.getKey().equals(that.getKey());
    }

    public int hashCode() {
        return this.getKey().hashCode();
    }

    static {
        Class<PreparedAdvancementWrapper> clazz = ReflectionUtil.getWrapperClass(PreparedAdvancementWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            constructor = clazz.getDeclaredConstructor(MinecraftKeyWrapper.class, AdvancementDisplayWrapper.class, Integer.TYPE);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

