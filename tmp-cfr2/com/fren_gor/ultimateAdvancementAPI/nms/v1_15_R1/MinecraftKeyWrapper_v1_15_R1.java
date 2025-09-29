/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.v1_15_R1.MinecraftKey
 *  net.minecraft.server.v1_15_R1.ResourceKeyInvalidException
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.v1_15_R1;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.MinecraftKeyWrapper;
import net.minecraft.server.v1_15_R1.MinecraftKey;
import net.minecraft.server.v1_15_R1.ResourceKeyInvalidException;
import org.jetbrains.annotations.NotNull;

public class MinecraftKeyWrapper_v1_15_R1
extends MinecraftKeyWrapper {
    private final MinecraftKey key;

    public MinecraftKeyWrapper_v1_15_R1(@NotNull Object key) {
        this.key = (MinecraftKey)key;
    }

    public MinecraftKeyWrapper_v1_15_R1(@NotNull String namespace, @NotNull String key) {
        try {
            this.key = new MinecraftKey(namespace, key);
        }
        catch (ResourceKeyInvalidException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    @NotNull
    public String getNamespace() {
        return this.key.getNamespace();
    }

    @Override
    @NotNull
    public String getKey() {
        return this.key.getKey();
    }

    @Override
    public int compareTo(@NotNull MinecraftKeyWrapper obj) {
        return this.key.compareTo((MinecraftKey)obj.toNMS());
    }

    @NotNull
    public MinecraftKey toNMS() {
        return this.key;
    }
}

