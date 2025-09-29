/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby;

import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.LibraryManager;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader.URLClassLoaderHelper;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.adapters.JDKLogAdapter;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

public class BukkitLibraryManager
extends LibraryManager {
    private final URLClassLoaderHelper classLoader;

    public BukkitLibraryManager(Plugin plugin) {
        this(plugin, "lib");
    }

    public BukkitLibraryManager(Plugin plugin, String directoryName) {
        super(new JDKLogAdapter(Objects.requireNonNull(plugin, "plugin").getLogger()), plugin.getDataFolder().toPath(), directoryName);
        this.classLoader = new URLClassLoaderHelper((URLClassLoader)plugin.getClass().getClassLoader(), this);
    }

    @Override
    protected void addToClasspath(Path file) {
        this.classLoader.addToClasspath(file);
    }
}

