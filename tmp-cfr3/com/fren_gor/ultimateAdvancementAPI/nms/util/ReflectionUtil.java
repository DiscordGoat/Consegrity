/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.fren_gor.ultimateAdvancementAPI.nms.util;

import com.fren_gor.ultimateAdvancementAPI.util.Versions;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReflectionUtil {
    public static final String MINECRAFT_VERSION = Bukkit.getBukkitVersion().split("-")[0];
    public static final int MINOR_VERSION;
    private static final String CRAFTBUKKIT_PACKAGE;
    public static final int VERSION;
    private static final boolean IS_1_17;

    public static boolean classExists(@NotNull String className) {
        Objects.requireNonNull(className, "ClassName cannot be null.");
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nullable
    public static Class<?> getNMSClass(@NotNull String name, @NotNull String mcPackage) {
        String path;
        if (IS_1_17) {
            path = "net.minecraft." + mcPackage + "." + name;
        } else {
            Optional<String> version = Versions.getNMSVersion();
            if (version.isEmpty()) {
                Bukkit.getLogger().severe("[UltimateAdvancementAPI] Unsupported Minecraft version! (" + MINECRAFT_VERSION + ")");
                return null;
            }
            path = "net.minecraft.server." + version.get() + "." + name;
        }
        try {
            return Class.forName(path);
        }
        catch (ClassNotFoundException e) {
            Bukkit.getLogger().severe("[UltimateAdvancementAPI] Can't find NMS Class! (" + path + ")");
            return null;
        }
    }

    @Nullable
    public static Class<?> getCBClass(@NotNull String name) {
        String cb = CRAFTBUKKIT_PACKAGE + "." + name;
        try {
            return Class.forName(cb);
        }
        catch (ClassNotFoundException e) {
            Bukkit.getLogger().severe("[UltimateAdvancementAPI] Can't find CB Class! (" + cb + ")");
            return null;
        }
    }

    @Nullable
    public static <T> Class<? extends T> getWrapperClass(@NotNull Class<T> clazz) {
        String validPackage;
        Optional<String> version = Versions.getNMSVersion();
        if (version.isEmpty()) {
            Bukkit.getLogger().severe("[UltimateAdvancementAPI] Unsupported Minecraft version! (" + MINECRAFT_VERSION + ")");
            return null;
        }
        String name = clazz.getName();
        if (!name.startsWith(validPackage = "com.fren_gor.ultimateAdvancementAPI.nms.wrappers.")) {
            throw new IllegalArgumentException("Invalid class " + name + ".");
        }
        String wrapper = "com.fren_gor.ultimateAdvancementAPI.nms." + version.get() + "." + name.substring(validPackage.length()) + "_" + version.get();
        try {
            return Class.forName(wrapper).asSubclass(clazz);
        }
        catch (ClassNotFoundException e) {
            Bukkit.getLogger().severe("[UltimateAdvancementAPI] Can't find Wrapper Class! (" + wrapper + ")");
            return null;
        }
    }

    private ReflectionUtil() {
        throw new UnsupportedOperationException("Utility class.");
    }

    static {
        String[] splitted = MINECRAFT_VERSION.split("\\.");
        MINOR_VERSION = splitted.length > 2 ? Integer.parseInt(splitted[2]) : 0;
        CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = Integer.parseInt(MINECRAFT_VERSION.split("\\.")[1]);
        IS_1_17 = VERSION >= 17;
    }
}

