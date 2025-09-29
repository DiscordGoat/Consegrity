/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 *  org.jetbrains.annotations.Unmodifiable
 */
package com.fren_gor.ultimateAdvancementAPI.advancement.multiParents;

import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.FakeAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.multiParents.AbstractMultiParentsAdvancement;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.PreparedAdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.fren_gor.ultimateAdvancementAPI.util.LazyValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

public class MultiParentsAdvancement
extends AbstractMultiParentsAdvancement {
    private final Map<BaseAdvancement, FakeAdvancement> parents;
    @LazyValue
    private PreparedAdvancementWrapper wrapper;

    public MultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, BaseAdvancement ... parents) {
        this(key, display, 1, parents);
    }

    public MultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression, BaseAdvancement ... parents) {
        this(key, display, maxProgression, Sets.newHashSet((Object[])Objects.requireNonNull(parents)));
    }

    public MultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @NotNull Set<BaseAdvancement> parents) {
        this(key, display, 1, parents);
    }

    public MultiParentsAdvancement(@NotNull String key, @NotNull AdvancementDisplay display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression, @NotNull Set<BaseAdvancement> parents) {
        super(key, display, MultiParentsAdvancement.validateAndGetFirst(parents), maxProgression);
        this.parents = Maps.newHashMapWithExpectedSize((int)parents.size());
        for (BaseAdvancement advancement : parents) {
            if (advancement == null) {
                this.parents.clear();
                throw new IllegalArgumentException("A parent advancement is null.");
            }
            if (!this.advancementTab.isOwnedByThisTab(advancement)) {
                this.parents.clear();
                throw new IllegalArgumentException("A parent advancement (" + String.valueOf(advancement.getKey()) + ") is not owned by this tab (" + String.valueOf(this.advancementTab) + ").");
            }
            FakeAdvancement adv = new FakeAdvancement(advancement, display.getX(), display.getY());
            this.parents.put(advancement, adv);
        }
    }

    @Override
    public void onUpdate(@NotNull TeamProgression teamProgression, @NotNull Map<AdvancementWrapper, Integer> addedAdvancements) {
        if (this.isVisible(teamProgression)) {
            BaseAdvancement tmp = null;
            for (Map.Entry<BaseAdvancement, FakeAdvancement> e : this.parents.entrySet()) {
                if (!e.getKey().isVisible(teamProgression)) continue;
                if (tmp == null) {
                    tmp = e.getKey();
                    continue;
                }
                e.getValue().onUpdate(teamProgression, addedAdvancements);
            }
            if (tmp == null) {
                tmp = this.getParent();
            }
            addedAdvancements.put(this.getNMSWrapper(tmp), this.getProgression(teamProgression));
        }
    }

    @Override
    @NotNull
    public @NotNull @Unmodifiable Set<@NotNull BaseAdvancement> getParents() {
        return Collections.unmodifiableSet(this.parents.keySet());
    }

    @Override
    public boolean isEveryParentGranted(@NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)pro, (Object)"TeamProgression cannot be null.");
        for (BaseAdvancement advancement : this.parents.keySet()) {
            if (advancement.isGranted(pro)) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean isAnyParentGranted(@NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)pro, (Object)"TeamProgression cannot be null.");
        for (BaseAdvancement advancement : this.parents.keySet()) {
            if (!advancement.isGranted(pro)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean isEveryGrandparentGranted(@NotNull TeamProgression pro) {
        Preconditions.checkNotNull((Object)pro, (Object)"TeamProgression cannot be null.");
        for (BaseAdvancement advancement : this.parents.keySet()) {
            AbstractMultiParentsAdvancement multiParent;
            if (advancement.isGranted(pro) || !(advancement instanceof AbstractMultiParentsAdvancement ? !(multiParent = (AbstractMultiParentsAdvancement)advancement).isEveryParentGranted(pro) : !advancement.getParent().isGranted(pro))) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean isAnyGrandparentGranted(@NotNull TeamProgression pro) {
        AdvancementUtils.validateTeamProgression(pro);
        for (BaseAdvancement advancement : this.parents.keySet()) {
            AbstractMultiParentsAdvancement multiParent;
            if (advancement.isGranted(pro)) {
                return true;
            }
            if (!(advancement instanceof AbstractMultiParentsAdvancement ? (multiParent = (AbstractMultiParentsAdvancement)advancement).isAnyParentGranted(pro) : advancement.getParent().isGranted(pro))) continue;
            return true;
        }
        return false;
    }

    @Override
    public void validateRegister() throws InvalidAdvancementException {
        for (BaseAdvancement advancement : this.parents.keySet()) {
            if (advancement.isValid()) continue;
            throw new InvalidAdvancementException("A parent advancement is not valid (" + String.valueOf(advancement.getKey()) + ").");
        }
    }

    @Override
    public void onDispose() {
        for (FakeAdvancement f : this.parents.values()) {
            f.onDispose();
        }
        super.onDispose();
    }

    @Override
    @NotNull
    public BaseAdvancement getParent() {
        return (BaseAdvancement)this.parent;
    }

    @Override
    @NotNull
    public AdvancementWrapper getNMSWrapper() {
        this.setUpWrapper();
        return this.wrapper.toBaseAdvancementWrapper(this.parent.getNMSWrapper());
    }

    @NotNull
    protected AdvancementWrapper getNMSWrapper(@NotNull BaseAdvancement advancement) {
        this.setUpWrapper();
        return this.wrapper.toBaseAdvancementWrapper(advancement.getNMSWrapper());
    }

    private void setUpWrapper() {
        if (this.wrapper == null) {
            try {
                this.wrapper = PreparedAdvancementWrapper.craft(this.key.getNMSWrapper(), this.display.getNMSWrapper(this), this.maxProgression);
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

