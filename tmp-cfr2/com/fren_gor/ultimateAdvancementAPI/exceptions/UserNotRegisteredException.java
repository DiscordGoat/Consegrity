/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

public class UserNotRegisteredException
extends RuntimeException {
    public UserNotRegisteredException() {
    }

    public UserNotRegisteredException(String message) {
        super(message);
    }

    public UserNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotRegisteredException(Throwable cause) {
        super(cause);
    }

    protected UserNotRegisteredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

