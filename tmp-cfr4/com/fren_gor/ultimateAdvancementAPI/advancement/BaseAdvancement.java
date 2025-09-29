/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.LazyValue;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class BaseAdvancement
extends Advancement {
    @NotNull
    protected final Advancement parent;
    @LazyValue
    private AdvancementWrapper wrapper;

    public BaseAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Advancement parent) {
        this(key, display, parent, 1);
    }

    public BaseAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Advancement parent, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(Objects.requireNonNull(parent, (String)"Parent advancement is null.").advancementTab, key, display, maxProgression);
        this.parent = parent;
    }

    @Override
    public void validateRegister() throws InvalidAdvancementException {
        if (!this.parent.isValid()) {
            throw new InvalidAdvancementException("Parent advancement is not valid (" + String.valueOf(this.parent.getKey()) + ").");
        }
    }

    @Override
    @NotNull
    public AdvancementWrapper getNMSWrapper() {
        if (this.wrapper != null) {
            return this.wrapper;
        }
        try {
            this.wrapper = AdvancementWrapper.craftBaseAdvancement(this.key.getNMSWrapper(), this.parent.getNMSWrapper(), this.display.getNMSWrapper(this), this.maxProgression);
            return this.wrapper;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public Advancement getParent() {
        return this.parent;
    }
}

