/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.multiParents;

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public abstract class AbstractMultiParentsAdvancement
extends BaseAdvancement {
    public AbstractMultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull BaseAdvancement aParent) {
        super(key, display, aParent);
    }

    public AbstractMultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull BaseAdvancement aParent, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        super(key, display, aParent, maxProgression);
    }

    @NotNull
    public abstract @NotNull Set<@NotNull BaseAdvancement> getParents();

    public boolean isEveryParentGranted(@NotNull Player player) {
        return this.isEveryParentGranted(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isEveryParentGranted(@NotNull UUID uuid) {
        return this.isEveryParentGranted(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public abstract boolean isEveryParentGranted(@NotNull TeamProgression var1);

    public boolean isAnyParentGranted(@NotNull Player player) {
        return this.isAnyParentGranted(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isAnyParentGranted(@NotNull UUID uuid) {
        return this.isAnyParentGranted(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public abstract boolean isAnyParentGranted(@NotNull TeamProgression var1);

    public boolean isEveryGrandparentGranted(@NotNull Player player) {
        return this.isEveryGrandparentGranted(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isEveryGrandparentGranted(@NotNull UUID uuid) {
        return this.isEveryGrandparentGranted(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public abstract boolean isEveryGrandparentGranted(@NotNull TeamProgression var1);

    public boolean isAnyGrandparentGranted(@NotNull Player player) {
        return this.isAnyGrandparentGranted(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isAnyGrandparentGranted(@NotNull UUID uuid) {
        return this.isAnyGrandparentGranted(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public abstract boolean isAnyGrandparentGranted(@NotNull TeamProgression var1);

    @NotNull
    public static <E extends BaseAdvancement> E validateAndGetFirst(@NotNull Set<E> advancements) {
        Preconditions.checkNotNull(advancements, (Object)"Parent advancements are null.");
        Preconditions.checkArgument((advancements.size() > 0 ? 1 : 0) != 0, (Object)"There must be at least 1 parent.");
        BaseAdvancement e = (BaseAdvancement)advancements.iterator().next();
        Preconditions.checkNotNull((Object)e, (Object)"A parent advancement is null.");
        return (E)e;
    }
}

