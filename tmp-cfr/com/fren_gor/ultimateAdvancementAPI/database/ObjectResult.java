/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.fren_gor.ultimateAdvancementAPI.database.Result;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UnhandledException;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ObjectResult<T>
extends Result {
    protected final T result;

    public ObjectResult() {
        this(null);
    }

    public ObjectResult(T result) {
        this.result = result;
    }

    public ObjectResult(@NotNull Exception occurredException) {
        super(occurredException);
        this.result = null;
    }

    public boolean hasResult() {
        return this.isSucceeded() && this.result != null;
    }

    @Contract(pure=true)
    public T getResult() throws UnhandledException {
        this.rethrowExceptionIfOccurred();
        return this.result;
    }

    @Override
    public String toString() {
        return "ObjectResult{" + (this.isExceptionOccurred() ? "occurredException=" + String.valueOf(this.occurredException) + ", succeeded=false" : "result=" + String.valueOf(this.result) + ", succeeded=true") + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ObjectResult that = (ObjectResult)o;
        return Objects.equals(this.result, that.result);
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (this.result != null ? this.result.hashCode() : 0);
        return result1;
    }
}

