/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

public class DisposedException
extends RuntimeException {
    public DisposedException() {
    }

    public DisposedException(String message) {
        super(message);
    }

    public DisposedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DisposedException(Throwable cause) {
        super(cause);
    }

    protected DisposedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

