/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.display;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplayBuilder;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class AdvancementDisplay {
    protected final ItemStack icon;
    protected final BaseComponent[] chatTitle = new BaseComponent[1];
    protected final BaseComponent[] chatDescription = new BaseComponent[1];
    protected final String title;
    protected final String rawTitle;
    protected final @Unmodifiable List<String> description;
    protected final String compactDescription;
    protected final AdvancementFrameType frame;
    protected final boolean showToast;
    protected final boolean announceChat;
    protected final float x;
    protected final float y;

    public AdvancementDisplay(@NotNull Material icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, Arrays.asList(description));
    }

    public AdvancementDisplay(@NotNull Material icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull List<String> description) {
        this(new ItemStack(Objects.requireNonNull(icon, "Icon is null.")), title, frame, showToast, announceChat, x, y, description);
    }

    public AdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, Arrays.asList(description));
    }

    public AdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull List<String> description) {
        this(icon, title, frame, showToast, announceChat, x, y, Objects.requireNonNull(frame, "AdvancementFrameType is null.").getColor(), description);
    }

    public AdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull ChatColor defaultColor, String ... description) {
        this(icon, title, frame, showToast, announceChat, x, y, defaultColor, Arrays.asList(description));
    }

    public AdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, boolean showToast, boolean announceChat, float x, float y, @NotNull ChatColor defaultColor, @NotNull List<String> description) {
        int toSub;
        Preconditions.checkNotNull((Object)icon, (Object)"Icon is null.");
        Preconditions.checkNotNull((Object)title, (Object)"Title is null.");
        Preconditions.checkNotNull((Object)((Object)frame), (Object)"Frame is null.");
        Preconditions.checkNotNull((Object)defaultColor, (Object)"Default color is null.");
        Preconditions.checkNotNull(description, (Object)"Description is null.");
        for (String line : description) {
            Preconditions.checkNotNull((Object)line, (Object)"A line of the description is null.");
        }
        Preconditions.checkArgument((boolean)Float.isFinite(x), (Object)"x is NaN or infinite.");
        Preconditions.checkArgument((boolean)Float.isFinite(y), (Object)"y is NaN or infinite.");
        Preconditions.checkArgument((x >= 0.0f ? 1 : 0) != 0, (Object)"x is not zero or positive.");
        Preconditions.checkArgument((y >= 0.0f ? 1 : 0) != 0, (Object)"y is not zero or positive.");
        this.icon = icon.clone();
        this.title = title;
        this.description = List.copyOf(description);
        String titleTrimmed = title.trim();
        for (toSub = titleTrimmed.length(); toSub > 1 && titleTrimmed.charAt(toSub - 2) == '\u00a7'; toSub -= 2) {
        }
        this.rawTitle = titleTrimmed.substring(0, toSub).trim();
        this.chatTitle[0] = new TextComponent(String.valueOf(defaultColor) + this.rawTitle);
        if (this.description.isEmpty()) {
            this.compactDescription = "";
        } else {
            StringJoiner joiner = new StringJoiner("\n" + String.valueOf(defaultColor), defaultColor.toString(), "");
            for (String s : this.description) {
                joiner.add(s);
            }
            this.compactDescription = joiner.toString();
        }
        this.chatDescription[0] = this.compactDescription.isEmpty() ? new TextComponent(String.valueOf(defaultColor) + this.rawTitle) : new TextComponent(String.valueOf(defaultColor) + this.rawTitle + "\n" + this.compactDescription);
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceChat;
        this.x = x;
        this.y = y;
    }

    public boolean doesShowToast() {
        return this.showToast;
    }

    public boolean doesAnnounceToChat() {
        return this.announceChat;
    }

    @NotNull
    public BaseComponent[] getChatTitle() {
        return (BaseComponent[])this.chatTitle.clone();
    }

    @NotNull
    public BaseComponent[] getChatDescription() {
        return (BaseComponent[])this.chatDescription.clone();
    }

    @NotNull
    public ItemStack getIcon() {
        return this.icon.clone();
    }

    @NotNull
    public AdvancementDisplayWrapper getNMSWrapper(@NotNull Advancement advancement) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        try {
            if (advancement instanceof RootAdvancement) {
                RootAdvancement root = (RootAdvancement)advancement;
                return AdvancementDisplayWrapper.craft(this.icon, this.title, this.compactDescription, this.frame.getNMSWrapper(), this.x, this.y, root.getBackgroundTexture());
            }
            return AdvancementDisplayWrapper.craft(this.icon, this.title, this.compactDescription, this.frame.getNMSWrapper(), this.x, this.y);
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public String getTitle() {
        return this.title;
    }

    @NotNull
    public String getRawTitle() {
        return this.rawTitle;
    }

    public @Unmodifiable List<String> getDescription() {
        return this.description;
    }

    @NotNull
    public String getCompactDescription() {
        return this.compactDescription;
    }

    @NotNull
    public AdvancementFrameType getFrame() {
        return this.frame;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public static class Builder
    extends AdvancementDisplayBuilder<Builder, AdvancementDisplay> {
        protected ChatColor defaultColor;
        private boolean manuallySetDefaultColor;

        public Builder(@NotNull Material icon, @NotNull String title) {
            super(icon, title);
            this.defaultColor = this.frame.getColor();
            this.manuallySetDefaultColor = false;
        }

        public Builder(@NotNull ItemStack icon, @NotNull String title) {
            super(icon, title);
            this.defaultColor = this.frame.getColor();
            this.manuallySetDefaultColor = false;
        }

        @Override
        @NotNull
        public Builder frame(@NotNull AdvancementFrameType frame) {
            super.frame(frame);
            if (!this.manuallySetDefaultColor) {
                this.defaultColor = frame.getColor();
            }
            return this;
        }

        @Override
        @NotNull
        public Builder taskFrame() {
            return (Builder)super.taskFrame();
        }

        @Override
        @NotNull
        public Builder goalFrame() {
            return (Builder)super.goalFrame();
        }

        @Override
        @NotNull
        public Builder challengeFrame() {
            return (Builder)super.challengeFrame();
        }

        @NotNull
        public Builder defaultColor(@NotNull ChatColor defaultColor) {
            this.defaultColor = Objects.requireNonNull(defaultColor, "Default color is null.");
            this.manuallySetDefaultColor = true;
            return this;
        }

        @Override
        @NotNull
        public AdvancementDisplay build() {
            return new AdvancementDisplay(this.icon, this.title, this.frame, this.showToast, this.announceChat, this.x, this.y, this.defaultColor, this.description);
        }

        @NotNull
        public ChatColor getDefaultColor() {
            return this.defaultColor;
        }
    }
}

