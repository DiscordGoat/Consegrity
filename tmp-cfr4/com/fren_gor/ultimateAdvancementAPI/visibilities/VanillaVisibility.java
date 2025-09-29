/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.visibilities;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.multiParents.AbstractMultiParentsAdvancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.visibilities.IVisibility;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

public interface VanillaVisibility
extends IVisibility {
    @Override
    default public boolean isVisible(@NotNull Advancement advancement, @NotNull TeamProgression progression) {
        Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
        Preconditions.checkNotNull((Object)progression, (Object)"TeamProgression is null.");
        if (advancement.getProgression(progression) > 0) {
            return true;
        }
        if (advancement instanceof AbstractMultiParentsAdvancement) {
            AbstractMultiParentsAdvancement multiParent = (AbstractMultiParentsAdvancement)advancement;
            return multiParent.isAnyGrandparentGranted(progression);
        }
        if (advancement instanceof BaseAdvancement) {
            BaseAdvancement base = (BaseAdvancement)advancement;
            Advancement parent = base.getParent();
            if (parent.isGranted(progression)) {
                return true;
            }
            if (parent instanceof AbstractMultiParentsAdvancement) {
                AbstractMultiParentsAdvancement multiParent = (AbstractMultiParentsAdvancement)parent;
                return multiParent.isAnyParentGranted(progression);
            }
            if (parent instanceof BaseAdvancement) {
                BaseAdvancement baseA = (BaseAdvancement)parent;
                return baseA.getParent().isGranted(progression);
            }
            return false;
        }
        return false;
    }
}

