/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.advancements.AdvancementFrameType
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R2.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import net.minecraft.advancements.AdvancementFrameType;
import org.jetbrains.annotations.NotNull;

public class AdvancementFrameTypeWrapper_v1_21_R2
extends AdvancementFrameTypeWrapper {
    private final AdvancementFrameType mcFrameType;
    private final AdvancementFrameTypeWrapper.FrameType frameType;

    public AdvancementFrameTypeWrapper_v1_21_R2(@NotNull AdvancementFrameTypeWrapper.FrameType frameType) {
        this.frameType = frameType;
        this.mcFrameType = switch (frameType) {
            default -> throw new IncompatibleClassChangeError();
            case AdvancementFrameTypeWrapper.FrameType.TASK -> AdvancementFrameType.a;
            case AdvancementFrameTypeWrapper.FrameType.GOAL -> AdvancementFrameType.c;
            case AdvancementFrameTypeWrapper.FrameType.CHALLENGE -> AdvancementFrameType.b;
        };
    }

    @Override
    @NotNull
    public AdvancementFrameTypeWrapper.FrameType getFrameType() {
        return this.frameType;
    }

    @NotNull
    public AdvancementFrameType toNMS() {
        return this.mcFrameType;
    }
}

