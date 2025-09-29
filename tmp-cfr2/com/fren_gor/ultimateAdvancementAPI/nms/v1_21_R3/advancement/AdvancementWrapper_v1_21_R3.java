/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.advancements.AdvancementDisplay
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementRequirements
 *  net.minecraft.advancements.AdvancementRewards
 *  net.minecraft.advancements.Criterion
 *  net.minecraft.resources.MinecraftKey
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R3.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R3.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementDisplay;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.MinecraftKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class AdvancementWrapper_v1_21_R3
extends AdvancementWrapper {
    private final AdvancementHolder advancementHolder;
    private final MinecraftKeyWrapper key;
    private final AdvancementWrapper parent;
    private final AdvancementDisplayWrapper display;

    public AdvancementWrapper_v1_21_R3(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Map<String, Criterion<?>> advCriteria = Util.getAdvancementCriteria(maxProgression);
        Advancement advancement = new Advancement(Optional.empty(), Optional.of((AdvancementDisplay)display.toNMS()), AdvancementRewards.b, advCriteria, Util.getAdvancementRequirements(advCriteria), false, Optional.empty());
        this.advancementHolder = new AdvancementHolder((MinecraftKey)key.toNMS(), advancement);
        this.key = key;
        this.parent = null;
        this.display = display;
    }

    public AdvancementWrapper_v1_21_R3(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementWrapper parent, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Map<String, Criterion<?>> advCriteria = Util.getAdvancementCriteria(maxProgression);
        Advancement advancement = new Advancement(Optional.of((MinecraftKey)parent.getKey().toNMS()), Optional.of((AdvancementDisplay)display.toNMS()), AdvancementRewards.b, advCriteria, Util.getAdvancementRequirements(advCriteria), false, Optional.empty());
        this.advancementHolder = new AdvancementHolder((MinecraftKey)key.toNMS(), advancement);
        this.key = key;
        this.parent = parent;
        this.display = display;
    }

    protected AdvancementWrapper_v1_21_R3(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @NotNull Map<String, Criterion<?>> advCriteria, @NotNull AdvancementRequirements advRequirements) {
        Advancement advancement = new Advancement(Optional.empty(), Optional.of((AdvancementDisplay)display.toNMS()), AdvancementRewards.b, advCriteria, advRequirements, false, Optional.empty());
        this.advancementHolder = new AdvancementHolder((MinecraftKey)key.toNMS(), advancement);
        this.key = key;
        this.parent = null;
        this.display = display;
    }

    protected AdvancementWrapper_v1_21_R3(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementWrapper parent, @NotNull AdvancementDisplayWrapper display, @NotNull Map<String, Criterion<?>> advCriteria, @NotNull AdvancementRequirements advRequirements) {
        Advancement advancement = new Advancement(Optional.of((MinecraftKey)parent.getKey().toNMS()), Optional.of((AdvancementDisplay)display.toNMS()), AdvancementRewards.b, advCriteria, advRequirements, false, Optional.empty());
        this.advancementHolder = new AdvancementHolder((MinecraftKey)key.toNMS(), advancement);
        this.key = key;
        this.parent = parent;
        this.display = display;
    }

    @Override
    @NotNull
    public MinecraftKeyWrapper getKey() {
        return this.key;
    }

    @Override
    @Nullable
    public AdvancementWrapper getParent() {
        return this.parent;
    }

    @Override
    @NotNull
    public AdvancementDisplayWrapper getDisplay() {
        return this.display;
    }

    @Override
    public @Range(from=1L, to=0x7FFFFFFFL) int getMaxProgression() {
        return this.advancementHolder.b().f().a();
    }

    @NotNull
    public AdvancementHolder toNMS() {
        return this.advancementHolder;
    }
}

