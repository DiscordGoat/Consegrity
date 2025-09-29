/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.minecraft.advancements.AdvancementDisplay
 *  net.minecraft.advancements.AdvancementFrameType
 *  net.minecraft.network.chat.IChatBaseComponent
 *  net.minecraft.resources.MinecraftKey
 *  net.minecraft.world.item.ItemStack
 *  org.bukkit.craftbukkit.v1_21_R2.inventory.CraftItemStack
 *  org.bukkit.craftbukkit.v1_21_R2.util.CraftChatMessage
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R2.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R2.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import java.util.Optional;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.advancements.AdvancementDisplay;
import net.minecraft.advancements.AdvancementFrameType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import org.bukkit.craftbukkit.v1_21_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R2.util.CraftChatMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancementDisplayWrapper_v1_21_R2
extends AdvancementDisplayWrapper {
    private final AdvancementDisplay display;
    private final AdvancementFrameTypeWrapper frameType;

    public AdvancementDisplayWrapper_v1_21_R2(@NotNull ItemStack icon, @NotNull String title, @NotNull String description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) {
        MinecraftKey background = backgroundTexture == null ? null : MinecraftKey.a((String)backgroundTexture);
        this.display = new AdvancementDisplay(CraftItemStack.asNMSCopy((ItemStack)icon), Util.fromString(title), Util.fromString(description), Optional.ofNullable(background), (AdvancementFrameType)frameType.toNMS(), showToast, announceChat, hidden);
        this.display.a(x, y);
        this.frameType = frameType;
    }

    public AdvancementDisplayWrapper_v1_21_R2(@NotNull ItemStack icon, @NotNull BaseComponent title, @NotNull BaseComponent description, @NotNull AdvancementFrameTypeWrapper frameType, float x, float y, boolean showToast, boolean announceChat, boolean hidden, @Nullable String backgroundTexture) {
        MinecraftKey background = backgroundTexture == null ? null : MinecraftKey.a((String)backgroundTexture);
        this.display = new AdvancementDisplay(CraftItemStack.asNMSCopy((ItemStack)icon), Util.fromComponent(title), Util.fromComponent(description), Optional.ofNullable(background), (AdvancementFrameType)frameType.toNMS(), showToast, announceChat, hidden);
        this.display.a(x, y);
        this.frameType = frameType;
    }

    @Override
    @NotNull
    public ItemStack getIcon() {
        return CraftItemStack.asBukkitCopy((net.minecraft.world.item.ItemStack)this.display.c());
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
        return this.display.f();
    }

    @Override
    public float getY() {
        return this.display.g();
    }

    @Override
    public boolean doesShowToast() {
        return this.display.h();
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
        Optional r = this.display.d();
        return r.isEmpty() ? null : r.toString();
    }

    @NotNull
    public AdvancementDisplay toNMS() {
        return this.display;
    }
}

