/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.util.ReflectionUtil;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.AbstractWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import java.lang.reflect.Constructor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AdvancementDisplayWrapper
extends AbstractWrapper {
    private static Constructor<? extends AdvancementDisplayWrapper> constructor;
    private static Constructor<? extends AdvancementDisplayWrapper> componentConstructor;

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, null);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, @Nullable String backgroundTexture) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, false, false, false, backgroundTexture);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, showToast, announceChat, hidden, null);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) throws ReflectiveOperationException {
        return constructor.newInstance(icon.clone(), title, description, frameType, Float.valueOf(x), Float.valueOf(y), showToast, announceChat, hidden, backgroundTexture);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, null);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, @Nullable String backgroundTexture) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, false, false, false, backgroundTexture);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden) throws ReflectiveOperationException {
        return AdvancementDisplayWrapper.craft(icon, title, description, frameType, x, y, showToast, announceChat, hidden, null);
    }

    @NotNull
    public static AdvancementDisplayWrapper craft(@NotNull ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) throws ReflectiveOperationException {
        return componentConstructor.newInstance(icon.clone(), title, description, frameType, Float.valueOf(x), Float.valueOf(y), showToast, announceChat, hidden, backgroundTexture);
    }

    @NotNull
    public abstract ItemStack getIcon();

    @NotNull
    public abstract String getTitle();

    @NotNull
    public abstract String getDescription();

    @NotNull
    public abstract AdvancementFrameTypeWrapper getAdvancementFrameType();

    public abstract float getX();

    public abstract float getY();

    public abstract boolean doesShowToast();

    public abstract boolean doesAnnounceToChat();

    public abstract boolean isHidden();

    @Nullable
    public abstract String getBackgroundTexture();

    static {
        Class<AdvancementDisplayWrapper> clazz = ReflectionUtil.getWrapperClass(AdvancementDisplayWrapper.class);
        assert (clazz != null) : "Wrapper class is null.";
        try {
            constructor = clazz.getDeclaredConstructor(ItemStack.class, String.class, String.class, AdvancementFrameTypeWrapper.class, Float.TYPE, Float.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, String.class);
            componentConstructor = clazz.getDeclaredConstructor(ItemStack.class, BaseComponent.class, BaseComponent.class, AdvancementFrameTypeWrapper.class, Float.TYPE, Float.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, String.class);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

