/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.display;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public enum AdvancementFrameType {
    TASK(AdvancementFrameTypeWrapper.TASK, ChatColor.GREEN, "has made the advancement"),
    GOAL(AdvancementFrameTypeWrapper.GOAL, ChatColor.GREEN, "has reached the goal"),
    CHALLENGE(AdvancementFrameTypeWrapper.CHALLENGE, ChatColor.DARK_PURPLE, "has completed the challenge");

    private final AdvancementFrameTypeWrapper wrapper;
    private final ChatColor color;
    private final String chatText;

    private AdvancementFrameType(@NotNull AdvancementFrameTypeWrapper wrapper, ChatColor color, String chatText) {
        this.wrapper = wrapper;
        this.color = color;
        this.chatText = chatText;
    }

    @NotNull
    public static AdvancementFrameType fromNMS(@NotNull AdvancementFrameTypeWrapper nms) {
        for (AdvancementFrameType a : AdvancementFrameType.values()) {
            if (!a.wrapper.equals(nms)) continue;
            return a;
        }
        throw new IllegalArgumentException(String.valueOf(nms) + " isn't a valid enum constant.");
    }

    @NotNull
    public AdvancementFrameTypeWrapper getNMSWrapper() {
        return this.wrapper;
    }

    @NotNull
    public ChatColor getColor() {
        return this.color;
    }

    @NotNull
    public String getChatText() {
        return this.chatText;
    }
}

