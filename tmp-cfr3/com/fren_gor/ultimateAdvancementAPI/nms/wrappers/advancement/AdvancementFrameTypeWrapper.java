/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.AbstractWrapper;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;

public abstract class AdvancementFrameTypeWrapper
extends AbstractWrapper {
    public static AdvancementFrameTypeWrapper TASK;
    public static AdvancementFrameTypeWrapper GOAL;
    public static AdvancementFrameTypeWrapper CHALLENGE;

    @NotNull
    public abstract FrameType getFrameType();

    @Override
    @NotNull
    public String toString() {
        return this.getFrameType().name();
    }

    static {
        Class<AdvancementFrameTypeWrapper> clazz = ReflectionUtil.getWrapperClass(AdvancementFrameTypeWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            Constructor<AdvancementFrameTypeWrapper> constructor = clazz.getDeclaredConstructor(FrameType.class);
            TASK = constructor.newInstance(new Object[]{FrameType.TASK});
            GOAL = constructor.newInstance(new Object[]{FrameType.GOAL});
            CHALLENGE = constructor.newInstance(new Object[]{FrameType.CHALLENGE});
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static enum FrameType {
        TASK,
        GOAL,
        CHALLENGE;

    }
}

