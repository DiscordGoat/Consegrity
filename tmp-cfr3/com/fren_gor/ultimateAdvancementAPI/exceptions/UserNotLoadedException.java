/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

import java.util.UUID;

public class UserNotLoadedException
extends RuntimeException {
    public UserNotLoadedException() {
    }

    public UserNotLoadedException(UUID user) {
        super((String)(user == null ? "User is not currently loaded. May it be offline?" : "User" + String.valueOf(user) + " is not currently loaded. May it be offline?"));
    }

    public UserNotLoadedException(String message) {
        super(message);
    }

    public UserNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotLoadedException(Throwable cause) {
        super(cause);
    }

    protected UserNotLoadedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

