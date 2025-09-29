/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;

public class IsolatedClassLoader
extends URLClassLoader {
    public IsolatedClassLoader(URL ... urls) {
        super(Objects.requireNonNull(urls, "urls"), ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addPath(Path path) {
        try {
            this.addURL(Objects.requireNonNull(path, "path").toUri().toURL());
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}

