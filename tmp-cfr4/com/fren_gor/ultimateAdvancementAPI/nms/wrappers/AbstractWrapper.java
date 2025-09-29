/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.nms.wrappers;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractWrapper {
    @NotNull
    public abstract Object toNMS();

    public String toString() {
        return this.toNMS().toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AbstractWrapper that = (AbstractWrapper)o;
        return this.toNMS().equals(that.toNMS());
    }

    public int hashCode() {
        return this.toNMS().hashCode();
    }
}

