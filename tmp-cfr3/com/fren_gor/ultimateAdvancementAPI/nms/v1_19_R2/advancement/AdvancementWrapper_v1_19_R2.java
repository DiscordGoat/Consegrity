/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.advancements.Advancement
 *  net.minecraft.advancements.AdvancementDisplay
 *  net.minecraft.advancements.AdvancementRewards
 *  net.minecraft.advancements.Criterion
 *  net.minecraft.resources.MinecraftKey
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_19_R2.advancement;

import com.fren_gor.ultimateAdvancementAPI.nms.v1_19_R2.Util;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementDisplay;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.MinecraftKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class AdvancementWrapper_v1_19_R2
extends AdvancementWrapper {
    private final Advancement advancement;
    private final MinecraftKeyWrapper key;
    private final AdvancementWrapper parent;
    private final AdvancementDisplayWrapper display;

    public AdvancementWrapper_v1_19_R2(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Map<String, Criterion> advCriteria = Util.getAdvancementCriteria(maxProgression);
        this.advancement = new Advancement((MinecraftKey)key.toNMS(), null, (AdvancementDisplay)display.toNMS(), AdvancementRewards.a, advCriteria, Util.getAdvancementRequirements(advCriteria));
        this.key = key;
        this.parent = null;
        this.display = display;
    }

    public AdvancementWrapper_v1_19_R2(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementWrapper parent, @NotNull AdvancementDisplayWrapper display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        Map<String, Criterion> advCriteria = Util.getAdvancementCriteria(maxProgression);
        this.advancement = new Advancement((MinecraftKey)key.toNMS(), (Advancement)parent.toNMS(), (AdvancementDisplay)display.toNMS(), AdvancementRewards.a, advCriteria, Util.getAdvancementRequirements(advCriteria));
        this.key = key;
        this.parent = parent;
        this.display = display;
    }

    protected AdvancementWrapper_v1_19_R2(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementDisplayWrapper display, @NotNull Map<String, Criterion> advCriteria, @NotNull String[][] advRequirements) {
        this.advancement = new Advancement((MinecraftKey)key.toNMS(), null, (AdvancementDisplay)display.toNMS(), AdvancementRewards.a, advCriteria, advRequirements);
        this.key = key;
        this.parent = null;
        this.display = display;
    }

    protected AdvancementWrapper_v1_19_R2(@NotNull MinecraftKeyWrapper key, @NotNull AdvancementWrapper parent, @NotNull AdvancementDisplayWrapper display, @NotNull Map<String, Criterion> advCriteria, @NotNull String[][] advRequirements) {
        this.advancement = new Advancement((MinecraftKey)key.toNMS(), (Advancement)parent.toNMS(), (AdvancementDisplay)display.toNMS(), AdvancementRewards.a, advCriteria, advRequirements);
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
        return this.advancement.i().length;
    }

    @NotNull
    public Advancement toNMS() {
        return this.advancement;
    }
}

