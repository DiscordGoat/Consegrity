/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.minecraft.server.v1_16_R1.AdvancementDisplay
 *  net.minecraft.server.v1_16_R1.AdvancementFrameType
 *  net.minecraft.server.v1_16_R1.IChatBaseComponent
 *  net.minecraft.server.v1_16_R1.ItemStack
 *  net.minecraft.server.v1_16_R1.MinecraftKey
 *  org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack
 *  org.bukkit.craftbukkit.v1_16_R1.util.CraftChatMessage
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R1.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R1.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import java.lang.reflect.Field;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_16_R1.AdvancementDisplay;
import net.minecraft.server.v1_16_R1.AdvancementFrameType;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import net.minecraft.server.v1_16_R1.ItemStack;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancementDisplayWrapper_v1_16_R1
extends AdvancementDisplayWrapper {
    private static Field iconField;
    private static Field xField;
    private static Field yField;
    private static Field keyField;
    private static Field showToastField;
    private final AdvancementDisplay display;
    private final AdvancementFrameTypeWrapper frameType;

    public AdvancementDisplayWrapper_v1_16_R1(@NotNull org.bukkit.inventory.ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) {
        MinecraftKey background = backgroundTexture == null ? null : new MinecraftKey(backgroundTexture);
        this.display = new AdvancementDisplay(CraftItemStack.asNMSCopy((org.bukkit.inventory.ItemStack)icon), Util.fromString(title), Util.fromString(description), background, (AdvancementFrameType)frameType.toNMS(), showToast, announceChat, hidden);
        this.display.a(x, y);
        this.frameType = frameType;
    }

    public AdvancementDisplayWrapper_v1_16_R1(@NotNull org.bukkit.inventory.ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) {
        MinecraftKey background = backgroundTexture == null ? null : new MinecraftKey(backgroundTexture);
        this.display = new AdvancementDisplay(CraftItemStack.asNMSCopy((org.bukkit.inventory.ItemStack)icon), Util.fromComponent(title), Util.fromComponent(description), background, (AdvancementFrameType)frameType.toNMS(), showToast, announceChat, hidden);
        this.display.a(x, y);
        this.frameType = frameType;
    }

    @Override
    @NotNull
    public org.bukkit.inventory.ItemStack getIcon() {
        try {
            ItemStack item = (ItemStack)iconField.get(this.display);
            return CraftItemStack.asBukkitCopy((ItemStack)item);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @NotNull
    public String getTitle() {
        return CraftChatMessage.fromComponent((IChatBaseComponent)this.display.a());
    }

    @Override
    @NotNull
    public String getDescription() {
        return CraftChatMessage.fromComponent((IChatBaseComponent)this.display.b());
    }

    @Override
    @NotNull
    public AdvancementFrameTypeWrapper getAdvancementFrameType() {
        return this.frameType;
    }

    @Override
    public float getX() {
        try {
            return xField.getFloat(this.display);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getY() {
        try {
            return yField.getFloat(this.display);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean doesShowToast() {
        try {
            return showToastField.getBoolean(this.display);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean doesAnnounceToChat() {
        return this.display.i();
    }

    @Override
    public boolean isHidden() {
        return this.display.j();
    }

    @Override
    @Nullable
    public String getBackgroundTexture() {
        try {
            Object mckey = keyField.get(this.display);
            return mckey == null ? null : mckey.toString();
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public AdvancementDisplay toNMS() {
        return this.display;
    }

    static {
        try {
            iconField = AdvancementDisplay.class.getDeclaredField("c");
            iconField.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            xField = AdvancementDisplay.class.getDeclaredField("i");
            xField.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            yField = AdvancementDisplay.class.getDeclaredField("j");
            yField.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            keyField = AdvancementDisplay.class.getDeclaredField("d");
            keyField.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            showToastField = AdvancementDisplay.class.getDeclaredField("f");
            showToastField.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}

