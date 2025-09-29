/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.display;

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplayBuilder;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FancyAdvancementDisplay
extends AdvancementDisplay {
    public static final ChatColor DEFAULT_TITLE_COLOR = ChatColor.WHITE;
    public static final ChatColor DEFAULT_DESCRIPTION_COLOR = ChatColor.GRAY;

    public FancyAdvancementDisplay(@NotNull Material icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, Arrays.asList(description));
    }

    public FancyAdvancementDisplay(@NotNull Material icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull List<String> description) {
        this(new ItemStack(Objects.requireNonNull(icon, "Icon is null.")), title, frame, showToast, announceChat, x, y, description);
    }

    public FancyAdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, Arrays.asList(description));
    }

    public FancyAdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull List<String> description) {
        this(icon, title, frame, showToast, announceChat, x, y, DEFAULT_TITLE_COLOR, DEFAULT_DESCRIPTION_COLOR, description);
    }

    public FancyAdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull ChatColor defaultTitleColor, @NotNull ChatColor defaultDescriptionColor, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, defaultTitleColor, defaultDescriptionColor, Arrays.asList(description));
    }

    public FancyAdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull ChatColor defaultTitleColor, @NotNull ChatColor defaultDescriptionColor, @NotNull List<String> description) {
        super(icon, title, frame, showToast, announceChat, x, y, defaultDescriptionColor, description);
        Preconditions.checkNotNull((Object)defaultTitleColor, (Object)"Default title color is null.");
        this.chatTitle[0] = new TextComponent(String.valueOf(defaultTitleColor) + this.rawTitle);
        this.chatDescription[0] = this.compactDescription.isEmpty() ? new TextComponent(String.valueOf(defaultTitleColor) + this.rawTitle) : new TextComponent(String.valueOf(defaultTitleColor) + this.rawTitle + (AdvancementUtils.startsWithEmptyLine(this.compactDescription) ? "\n" : "\n\n") + this.compactDescription);
    }

    public static class Builder
    extends AdvancementDisplayBuilder<Builder, FancyAdvancementDisplay> {
        protected ChatColor defaultTitleColor = DEFAULT_TITLE_COLOR;
        protected ChatColor defaultDescriptionColor = DEFAULT_DESCRIPTION_COLOR;

        public Builder(@NotNull Material icon, @NotNull String title) {
            super(icon, title);
        }

        public Builder(@NotNull ItemStack icon, @NotNull String title) {
            super(icon, title);
        }

        @NotNull
        public Builder titleColor(@NotNull ChatColor titleColor) {
            this.defaultTitleColor = Objects.requireNonNull(titleColor, "Default title color is null.");
            return this;
        }

        @NotNull
        public Builder descriptionColor(@NotNull ChatColor descriptionColor) {
            this.defaultDescriptionColor = Objects.requireNonNull(descriptionColor, "Default description color is null.");
            return this;
        }

        @Override
        @NotNull
        public FancyAdvancementDisplay build() {
            return new FancyAdvancementDisplay(this.icon, this.title, this.frame, this.showToast, this.announceChat, this.x, this.y, this.defaultTitleColor, this.defaultDescriptionColor, this.description);
        }

        @NotNull
        public ChatColor getDefaultTitleColor() {
            return this.defaultTitleColor;
        }

        @NotNull
        public ChatColor getDefaultDescriptionColor() {
            return this.defaultDescriptionColor;
        }
    }
}

