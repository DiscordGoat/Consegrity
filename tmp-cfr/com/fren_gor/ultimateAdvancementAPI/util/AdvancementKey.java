/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.NamespacedKey
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.util;

import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalKeyException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class AdvancementKey
implements Comparable<AdvancementKey> {
    public static final Pattern VALID_ADVANCEMENT_KEY = Pattern.compile("[a-z0-9_.-]{1,127}:[a-z0-9_./\\-]{1,127}");
    public static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]{1,127}");
    public static final Pattern VALID_KEY = Pattern.compile("[a-z0-9_./\\-]{1,127}");
    @NotNull
    private final MinecraftKeyWrapper minecraftKey;

    public AdvancementKey(@NotNull Plugin plugin, @NotNull String key) {
        this(plugin.getName().toLowerCase(Locale.ROOT), key);
    }

    public AdvancementKey(@NotNull String namespace, @NotNull String key) throws IllegalKeyException {
        AdvancementKey.checkNamespace(namespace);
        AdvancementKey.checkKey(key);
        try {
            this.minecraftKey = MinecraftKeyWrapper.craft(namespace, key);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalKeyException(e.getMessage());
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public AdvancementKey(@NotNull NamespacedKey key) {
        this(Objects.requireNonNull(key, "NamespacedKey is null.").getNamespace(), key.getKey());
    }

    public AdvancementKey(@NotNull MinecraftKeyWrapper key) throws IllegalKeyException {
        this(Objects.requireNonNull(key, "MinecraftKey is null.").getNamespace(), key.getKey());
    }

    @NotNull
    public String getNamespace() {
        return this.minecraftKey.getNamespace();
    }

    @NotNull
    public String getKey() {
        return this.minecraftKey.getKey();
    }

    @NotNull
    public MinecraftKeyWrapper getNMSWrapper() {
        return this.minecraftKey;
    }

    @NotNull
    public NamespacedKey toNamespacedKey() {
        return new NamespacedKey(this.minecraftKey.getNamespace(), this.minecraftKey.getKey());
    }

    public static AdvancementKey fromString(@NotNull String string) throws IllegalKeyException {
        int colon;
        if (string == null || string.isEmpty() || (colon = string.indexOf(58)) <= 0 || colon == string.length() - 1) {
            throw new IllegalKeyException("Illegal key '" + string + "'");
        }
        return new AdvancementKey(string.substring(0, colon), string.substring(colon + 1));
    }

    public static void checkNamespace(String namespace) throws IllegalArgumentException, IllegalKeyException {
        Preconditions.checkNotNull((Object)namespace, (Object)"Namespace is null.");
        Preconditions.checkArgument((!namespace.isEmpty() ? 1 : 0) != 0, (Object)"Namespace is empty.");
        if (namespace.length() > 127) {
            throw new IllegalKeyException("Too long namespace (max allowed is 127 chars).");
        }
    }

    public static void checkKey(String key) throws IllegalArgumentException, IllegalKeyException {
        Preconditions.checkNotNull((Object)key, (Object)"Key is null.");
        Preconditions.checkArgument((!key.isEmpty() ? 1 : 0) != 0, (Object)"Key is empty.");
        if (key.length() > 127) {
            throw new IllegalKeyException("Too long key (max allowed is 127 chars).");
        }
    }

    @Override
    public int compareTo(@NotNull AdvancementKey key) {
        return this.minecraftKey.compareTo(key.minecraftKey);
    }

    public String toString() {
        return this.minecraftKey.getNamespace() + ":" + this.minecraftKey.getKey();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AdvancementKey that = (AdvancementKey)o;
        return this.minecraftKey.equals(that.minecraftKey);
    }

    public int hashCode() {
        return this.minecraftKey.hashCode();
    }
}

