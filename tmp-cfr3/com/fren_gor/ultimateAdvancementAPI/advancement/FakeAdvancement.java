/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.chat.BaseComponent
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement;

import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementDisplayWrapper;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AfterHandle;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public final class FakeAdvancement
extends BaseAdvancement {
    private static final AtomicInteger FAKE_NUMBER = new AtomicInteger(1);

    public FakeAdvancement(@NotNull Advancement parent, float x, float y) {
        this(parent, new FakeAdvancementDisplay(Material.GRASS_BLOCK, "FakeAdvancement", AdvancementFrameType.TASK, x, y));
    }

    public FakeAdvancement(@NotNull Advancement parent, @NotNull FakeAdvancementDisplay display) {
        super("fakeadvancement._-.-_." + FAKE_NUMBER.getAndIncrement(), display, parent);
    }

    @Override
    @NotNull
    public FakeAdvancementDisplay getDisplay() {
        return (FakeAdvancementDisplay)super.getDisplay();
    }

    @Override
    @NotNull
    public AdvancementWrapper getNMSWrapper() {
        return super.getNMSWrapper();
    }

    @Override
    public int getProgression(@NotNull Player player) {
        return 0;
    }

    @Override
    public int getProgression(@NotNull UUID uuid) {
        return 0;
    }

    @Override
    public int getProgression(@NotNull TeamProgression progression) {
        return 0;
    }

    @Override
    @Contract(value="_ -> true")
    public boolean isVisible(@NotNull Player player) {
        return true;
    }

    @Override
    @Contract(value="_ -> true")
    public boolean isVisible(@NotNull UUID uuid) {
        return true;
    }

    @Override
    @Contract(value="_ -> true")
    public boolean isVisible(@NotNull TeamProgression progression) {
        return true;
    }

    @Override
    public void onUpdate(@NotNull TeamProgression teamProgression, @NotNull Map<AdvancementWrapper, Integer> addedAdvancements) {
        super.onUpdate(teamProgression, addedAdvancements);
    }

    @Override
    public boolean isGranted(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGranted(@NotNull UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGranted(@NotNull TeamProgression progression) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public BaseComponent[] getAnnounceMessage(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int increment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int increment, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int increment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int increment, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull TeamProgression pro, @Nullable Player player, @Range(from=0L, to=0x7FFFFFFFL) int increment, boolean giveRewards) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveReward) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setProgression(@NotNull TeamProgression pro, @Nullable Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveRewards) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handlePlayer(@NotNull TeamProgression pro, @Nullable Player player, int newProgression, int oldProgression, boolean giveRewards, @Nullable AfterHandle afterHandle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void displayToastToPlayer(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onGrant(@NotNull Player player, boolean giveRewards) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void giveReward(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void grant(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void grant(@NotNull Player player, boolean giveRewards) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revoke(@NotNull Player player) {
        throw new UnsupportedOperationException();
    }

    public static final class FakeAdvancementDisplay
    extends AdvancementDisplay {
        public FakeAdvancementDisplay(@NotNull Material icon, @NotNull String title, @NotNull AdvancementFrameType frame, float x, float y) {
            super(icon, title, frame, false, false, x, y, Collections.emptyList());
        }

        public FakeAdvancementDisplay(@NotNull ItemStack icon, @NotNull String title, @NotNull AdvancementFrameType frame, float x, float y) {
            super(icon, title, frame, false, false, x, y, Collections.emptyList());
        }

        @Override
        @NotNull
        public AdvancementDisplayWrapper getNMSWrapper(@NotNull Advancement advancement) {
            Preconditions.checkNotNull((Object)advancement, (Object)"Advancement is null.");
            try {
                return AdvancementDisplayWrapper.craft(this.icon, this.title, this.compactDescription, this.frame.getNMSWrapper(), this.x, this.y, false, false, true);
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

