/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public final class CacheFreeingOption {
    private static final CacheFreeingOption DONT_CACHE = new CacheFreeingOption(Option.DONT_CACHE, null);
    final Option option;
    final long ticks;
    final Plugin requester;

    private CacheFreeingOption(Option option, Plugin requester) {
        this.option = option;
        this.ticks = -1L;
        this.requester = requester;
    }

    private CacheFreeingOption(Option option, long ticks, Plugin requester) {
        this.option = option;
        this.ticks = ticks < 0L ? 0L : ticks;
        this.requester = requester;
    }

    public static CacheFreeingOption DONT_CACHE() {
        return DONT_CACHE;
    }

    public static CacheFreeingOption AUTOMATIC(@NotNull Plugin requester, @Range(from=0L, to=0x7FFFFFFFFFFFFFFFL) long ticks) {
        Preconditions.checkNotNull((Object)requester, (Object)"Plugin is null.");
        Preconditions.checkArgument((boolean)requester.isEnabled(), (Object)"Plugin isn't enabled.");
        return new CacheFreeingOption(Option.AUTOMATIC, ticks, requester);
    }

    public static CacheFreeingOption MANUAL(@NotNull Plugin requester) {
        Preconditions.checkNotNull((Object)requester, (Object)"Plugin is null.");
        Preconditions.checkArgument((boolean)requester.isEnabled(), (Object)"Plugin isn't enabled.");
        return new CacheFreeingOption(Option.MANUAL, requester);
    }

    public static enum Option {
        DONT_CACHE,
        AUTOMATIC,
        MANUAL;

    }
}

