/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.adapters;

import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.LogLevel;

public interface LogAdapter {
    public void log(LogLevel var1, String var2);

    public void log(LogLevel var1, String var2, Throwable var3);
}

