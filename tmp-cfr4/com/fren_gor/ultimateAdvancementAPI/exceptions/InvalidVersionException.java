/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

import org.jetbrains.annotations.Nullable;

public class InvalidVersionException
extends RuntimeException {
    @Nullable
    private String expected;
    @Nullable
    private String found;

    public InvalidVersionException() {
    }

    public InvalidVersionException(String message) {
        super(message);
    }

    public InvalidVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidVersionException(Throwable cause) {
        super(cause);
    }

    protected InvalidVersionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidVersionException(@Nullable String expected, @Nullable String found) {
        this.expected = expected;
        this.found = found;
    }

    public InvalidVersionException(@Nullable String expected, @Nullable String found, String message) {
        super(message);
        this.expected = expected;
        this.found = found;
    }

    public InvalidVersionException(@Nullable String expected, @Nullable String found, String message, Throwable cause) {
        super(message, cause);
        this.expected = expected;
        this.found = found;
    }

    public InvalidVersionException(@Nullable String expected, @Nullable String found, Throwable cause) {
        super(cause);
        this.expected = expected;
        this.found = found;
    }

    protected InvalidVersionException(@Nullable String expected, @Nullable String found, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.expected = expected;
        this.found = found;
    }

    @Nullable
    public String getExpected() {
        return this.expected;
    }

    @Nullable
    public String getFound() {
        return this.found;
    }
}

