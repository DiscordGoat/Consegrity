/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ResourceKeyInvalidException
 *  net.minecraft.resources.MinecraftKey
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_21_R1;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.resources.MinecraftKey;
import org.jetbrains.annotations.NotNull;

public class MinecraftKeyWrapper_v1_21_R1
extends MinecraftKeyWrapper {
    private final MinecraftKey key;

    public MinecraftKeyWrapper_v1_21_R1(@NotNull Object key) {
        this.key = (MinecraftKey)key;
    }

    public MinecraftKeyWrapper_v1_21_R1(@NotNull String namespace, @NotNull String key) {
        try {
            this.key = MinecraftKey.a((String)namespace, (String)key);
        }
        catch (ResourceKeyInvalidException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    @NotNull
    public String getNamespace() {
        return this.key.b();
    }

    @Override
    @NotNull
    public String getKey() {
        return this.key.a();
    }

    @Override
    public int compareTo(@NotNull MinecraftKeyWrapper obj) {
        return this.key.a((MinecraftKey)obj.toNMS());
    }

    @NotNull
    public MinecraftKey toNMS() {
        return this.key;
    }
}

