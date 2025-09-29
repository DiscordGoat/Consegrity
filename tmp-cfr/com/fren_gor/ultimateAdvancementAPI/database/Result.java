/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalOperationException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UnhandledException;
import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Result {
    public static final Result SUCCESSFUL = new Result();
    protected final Exception occurredException;

    public Result() {
        this.occurredException = null;
    }

    public Result(@NotNull Exception occurredException) {
        Preconditions.checkNotNull((Object)occurredException, (Object)"Exception is null.");
        this.occurredException = occurredException;
    }

    public boolean isExceptionOccurred() {
        return this.occurredException != null;
    }

    public boolean isSucceeded() {
        return this.occurredException == null;
    }

    public Exception getOccurredException() throws IllegalOperationException {
        if (!this.isExceptionOccurred()) {
            throw new IllegalOperationException("No exception occurred.");
        }
        return this.occurredException;
    }

    @Contract(value="-> fail")
    public void rethrowException() throws UnhandledException, IllegalOperationException {
        this.rethrowExceptionIfOccurred();
        throw new IllegalOperationException("No exception occurred.");
    }

    public void rethrowExceptionIfOccurred() throws UnhandledException {
        if (this.isExceptionOccurred()) {
            throw new UnhandledException(this.occurredException);
        }
    }

    public void printStackTrace() throws IllegalOperationException {
        if (!this.isExceptionOccurred()) {
            throw new IllegalOperationException("No exception occurred.");
        }
        this.occurredException.printStackTrace();
    }

    public String toString() {
        return "Result{" + (String)(this.isExceptionOccurred() ? "occurredException=" + String.valueOf(this.occurredException) + ", succeeded=false" : "succeeded=true") + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Result result = (Result)o;
        return Objects.equals(this.occurredException, result.occurredException);
    }

    public int hashCode() {
        return this.occurredException != null ? this.occurredException.hashCode() : 0;
    }
}

