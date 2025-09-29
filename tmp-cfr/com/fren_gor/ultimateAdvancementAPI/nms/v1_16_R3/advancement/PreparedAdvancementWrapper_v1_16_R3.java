/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.v1_16_R3.Criterion
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R3.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R3.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R3.advancement.AdvancementWrapper_v1_16_R3;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.PreparedAdvancementWrapper;
import java.util.Map;
import net.minecraft.server.v1_16_R3.Criterion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class PreparedAdvancementWrapper_v1_16_R3
extends PreparedAdvancementWrapper {
    private final MinecraftKeyWrapper key;
    private final AdvancementDisplayWrapper display;
    private final Map<String, Criterion> advCriteria;
    private final String[][] advRequirements;

    public PreparedAdvancementWrapper_v1_16_R3(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        this.key = key;
        this.display = display;
        this.advCriteria = Util.getAdvancementCriteria(maxProgression);
        this.advRequirements = Util.getAdvancementRequirements(this.advCriteria);
    }

    @Override
    @NotNull
    public MinecraftKeyWrapper getKey() {
        return this.key;
    }

    @Override
    @NotNull
    public AdvancementDisplayWrapper getDisplay() {
        return this.display;
    }

    @Override
    public @Range(from=1L, to=0x7FFFFFFFL) int getMaxProgression() {
        return this.advRequirements.length;
    }

    @Override
    @NotNull
    public AdvancementWrapper toRootAdvancementWrapper() {
        return new AdvancementWrapper_v1_16_R3(this.key, this.display, this.advCriteria, this.advRequirements);
    }

    @Override
    @NotNull
    public AdvancementWrapper toBaseAdvancementWrapper(@NotNull AdvancementWrapper parent) {
        return new AdvancementWrapper_v1_16_R3(this.key, parent, this.display, this.advCriteria, this.advRequirements);
    }
}

