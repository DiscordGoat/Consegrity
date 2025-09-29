/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.display;

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.CoordAdapter;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public abstract class AdvancementDisplayBuilder<T extends AdvancementDisplayBuilder<T, R>, R extends AdvancementDisplay> {
    protected final ItemStack icon;
    protected final String title;
    protected @Unmodifiable List<String> description = List.of();
    protected AdvancementFrameType frame = AdvancementFrameType.TASK;
    protected boolean showToast = false;
    protected boolean announceChat = false;
    protected float x = 0.0f;
    protected float y = 0.0f;

    protected AdvancementDisplayBuilder(@NotNull Material icon, @NotNull String title) {
        this.icon = new ItemStack(Objects.requireNonNull(icon, "Icon is null."));
        this.title = Objects.requireNonNull(title, "Title is null.");
    }

    protected AdvancementDisplayBuilder(@NotNull ItemStack icon, @NotNull String title) {
        this.icon = Objects.requireNonNull(icon, "Icon is null.").clone();
        this.title = Objects.requireNonNull(title, "Title is null.");
    }

    @NotNull
    public T coords(@NotNull CoordAdapter adapter, @NotNull AdvancementKey key) {
        Preconditions.checkNotNull((Object)adapter, (Object)"CoordAdapter is null.");
        CoordAdapter.Coord coord = adapter.getXAndY(Objects.requireNonNull(key, "AdvancementKey is null."));
        this.coords(coord.x(), coord.y());
        return (T)this;
    }

    @NotNull
    public T coords(float x, float y) {
        this.x(x);
        this.y(y);
        return (T)this;
    }

    @NotNull
    public T x(float x) {
        Preconditions.checkArgument((x >= 0.0f ? 1 : 0) != 0, (Object)"x is not zero or positive.");
        this.x = x;
        return (T)this;
    }

    @NotNull
    public T y(float y) {
        Preconditions.checkArgument((y >= 0.0f ? 1 : 0) != 0, (Object)"y is not zero or positive.");
        this.y = y;
        return (T)this;
    }

    @NotNull
    public T description(String ... description) {
        this.description = List.of(description);
        return (T)this;
    }

    @NotNull
    public T description(@NotNull List<String> description) {
        this.description = List.copyOf(description);
        return (T)this;
    }

    @NotNull
    public T frame(@NotNull AdvancementFrameType frame) {
        this.frame = Objects.requireNonNull(frame, "Frame is null.");
        return (T)this;
    }

    @NotNull
    public T taskFrame() {
        return this.frame(AdvancementFrameType.TASK);
    }

    @NotNull
    public T goalFrame() {
        return this.frame(AdvancementFrameType.GOAL);
    }

    @NotNull
    public T challengeFrame() {
        return this.frame(AdvancementFrameType.CHALLENGE);
    }

    @NotNull
    public T showToast() {
        return this.showToast(true);
    }

    public T showToast(boolean showToast) {
        this.showToast = showToast;
        return (T)this;
    }

    @NotNull
    public T announceChat() {
        return this.announceChat(true);
    }

    @NotNull
    public T announceChat(boolean announceChat) {
        this.announceChat = announceChat;
        return (T)this;
    }

    @NotNull
    public abstract R build();

    @NotNull
    public ItemStack getIcon() {
        return this.icon.clone();
    }

    @NotNull
    public String getTitle() {
        return this.title;
    }

    @NotNull
    public List<String> getDescription() {
        return this.description;
    }

    @NotNull
    public AdvancementFrameType getFrame() {
        return this.frame;
    }

    public boolean doesShowToast() {
        return this.showToast;
    }

    public boolean doesAnnounceToChat() {
        return this.announceChat;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }
}

