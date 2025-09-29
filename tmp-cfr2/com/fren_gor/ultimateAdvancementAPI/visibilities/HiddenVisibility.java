/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.visibilities;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.visibilities.IVisibility;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

public interface HiddenVisibility
extends IVisibility {
    @Override
    default public boolean isVisible(@NotNull Advancement advancement, @NotNull TeamProgression progression) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkNotNull((Object)progression, (Object)"TeamProgression is null.");
        return advancement.getProgression(progression) > 0;
    }
}

