/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.v1_16_R1.AdvancementFrameType
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_16_R1.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementFrameTypeWrapper;
import net.minecraft.server.v1_16_R1.AdvancementFrameType;
import org.jetbrains.annotations.NotNull;

public class AdvancementFrameTypeWrapper_v1_16_R1
extends AdvancementFrameTypeWrapper {
    private final AdvancementFrameType mcFrameType;
    private final AdvancementFrameTypeWrapper.FrameType frameType;

    public AdvancementFrameTypeWrapper_v1_16_R1(@NotNull AdvancementFrameTypeWrapper.FrameType frameType) {
        this.frameType = frameType;
        this.mcFrameType = switch (frameType) {
            default -> throw new IncompatibleClassChangeError();
            case AdvancementFrameTypeWrapper.FrameType.TASK -> AdvancementFrameType.TASK;
            case AdvancementFrameTypeWrapper.FrameType.GOAL -> AdvancementFrameType.GOAL;
            case AdvancementFrameTypeWrapper.FrameType.CHALLENGE -> AdvancementFrameType.CHALLENGE;
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

