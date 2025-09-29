/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.LazyValue;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class RootAdvancement
extends Advancement {
    @NotNull
    private final String backgroundTexture;
    @LazyValue
    private AdvancementWrapper wrapper;

    public RootAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @NotNull String backgroundTexture) {
        this(advancementTab, key, display, backgroundTexture, 1);
    }

    public RootAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @NotNull String backgroundTexture, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(advancementTab, key, display, maxProgression);
        this.backgroundTexture = Objects.requireNonNull(backgroundTexture, "Background texture is null.");
    }

    @Override
    @NotNull
    public AdvancementWrapper getNMSWrapper() {
        if (this.wrapper != null) {
            return this.wrapper;
        }
        try {
            this.wrapper = AdvancementWrapper.craftRootAdvancement(this.key.getNMSWrapper(), this.display.getNMSWrapper(this), this.maxProgression);
            return this.wrapper;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Contract(value="_ -> true")
    public final boolean isVisible(@NotNull Player player) {
        return true;
    }

    @Override
    @Contract(value="_ -> true")
    public final boolean isVisible(@NotNull UUID uuid) {
        return true;
    }

    @Override
    @Contract(value="_ -> true")
    public final boolean isVisible(@NotNull TeamProgression progression) {
        return true;
    }

    @NotNull
    public String getBackgroundTexture() {
        return this.backgroundTexture;
    }
}

