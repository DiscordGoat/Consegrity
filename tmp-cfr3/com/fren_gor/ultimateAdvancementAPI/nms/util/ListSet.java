/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.nms.util;

import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.AbstractWrapper;
import com.google.common.base.Preconditions;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public final class ListSet<E>
extends AbstractSet<E>
implements Set<E> {
    private final E[] elements;
    private final int size;

    public ListSet(@NotNull Set<E> elements) {
        Preconditions.checkNotNull(elements, (Object)"Set is null.");
        Object[] array = new Object[elements.size()];
        int i = 0;
        for (E e : elements) {
            if (e == null) continue;
            array[i++] = e;
        }
        this.elements = array;
        this.size = i;
    }

    private ListSet(@NotNull E[] elements, @Range(from=0L, to=0x7FFFFFFFL) int size) {
        this.elements = elements;
        this.size = size;
    }

    @NotNull
    @Contract(pure=true, value="_ -> new")
    public static <T extends AbstractWrapper> ListSet<?> fromWrapperSet(@NotNull Set<T> elements) {
        Preconditions.checkNotNull(elements, (Object)"Set is null.");
        Object[] array = new Object[elements.size()];
        int i = 0;
        for (AbstractWrapper t : elements) {
            Object nms;
            if (t == null || (nms = t.toNMS()) == null) continue;
            array[i++] = nms;
        }
        return new ListSet<Object>(array, i);
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return new Iterator<E>(){
            private final AtomicInteger current = new AtomicInteger(0);

            @Override
            public boolean hasNext() {
                return this.current.get() < ListSet.this.size;
            }

            @Override
            public E next() {
                return ListSet.this.elements[this.current.getAndIncrement()];
            }
        };
    }

    @Override
    public int size() {
        return this.size;
    }
}

