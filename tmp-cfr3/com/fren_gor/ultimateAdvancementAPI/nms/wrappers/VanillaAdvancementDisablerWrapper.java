/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.google.common.base.Preconditions;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class VanillaAdvancementDisablerWrapper {
    private static Method method;

    @Deprecated(forRemoval=true)
    public static void disableVanillaAdvancements() throws Exception {
        VanillaAdvancementDisablerWrapper.disableVanillaAdvancements(true, false);
    }

    public static void disableVanillaAdvancements(boolean vanillaAdvancements, boolean vanillaRecipeAdvancements) throws Exception {
        if (vanillaAdvancements || vanillaRecipeAdvancements) {
            method.invoke(null, vanillaAdvancements, vanillaRecipeAdvancements);
        }
    }

    static {
        Class<VanillaAdvancementDisablerWrapper> clazz = ReflectionUtil.getWrapperClass(VanillaAdvancementDisablerWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            method = clazz.getDeclaredMethod("disableVanillaAdvancements", Boolean.TYPE, Boolean.TYPE);
            Preconditions.checkArgument((boolean)Modifier.isPublic(method.getModifiers()), (Object)"Method disableVanillaAdvancements(boolean, boolean) is not public.");
            Preconditions.checkArgument((boolean)Modifier.isStatic(method.getModifiers()), (Object)"Method disableVanillaAdvancements(boolean, boolean) is not static.");
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

