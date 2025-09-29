/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.exceptions;

public class AsyncExecutionException
extends RuntimeException {
    public AsyncExecutionException() {
    }

    public AsyncExecutionException(String message) {
        super(message);
    }

    public AsyncExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncExecutionException(Throwable cause) {
        super(cause);
    }

    protected AsyncExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

