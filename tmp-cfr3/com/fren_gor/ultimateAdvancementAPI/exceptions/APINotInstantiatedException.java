/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

public class APINotInstantiatedException
extends RuntimeException {
    public APINotInstantiatedException() {
    }

    public APINotInstantiatedException(String message) {
        super(message);
    }

    public APINotInstantiatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public APINotInstantiatedException(Throwable cause) {
        super(cause);
    }

    protected APINotInstantiatedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

