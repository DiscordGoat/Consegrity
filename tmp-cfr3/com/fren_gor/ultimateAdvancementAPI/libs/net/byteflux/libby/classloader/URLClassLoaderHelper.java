/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader;

import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.Library;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.LibraryManager;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader.IsolatedClassLoader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import sun.misc.Unsafe;

public class URLClassLoaderHelper {
    private static final Unsafe theUnsafe;
    private final URLClassLoader classLoader;
    private MethodHandle addURLMethodHandle = null;

    public URLClassLoaderHelper(URLClassLoader classLoader, LibraryManager libraryManager) {
        Objects.requireNonNull(libraryManager, "libraryManager");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        try {
            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            try {
                URLClassLoaderHelper.openUrlClassLoaderModule();
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                addURLMethod.setAccessible(true);
            }
            catch (Exception exception) {
                if (exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    if (theUnsafe != null) {
                        try {
                            this.addURLMethodHandle = this.getPrivilegedMethodHandle(addURLMethod).bindTo(classLoader);
                            return;
                        }
                        catch (Exception ignored) {
                            this.addURLMethodHandle = null;
                        }
                    }
                    try {
                        this.addOpensWithAgent(libraryManager);
                        addURLMethod.setAccessible(true);
                    }
                    catch (Exception e) {
                        System.err.println("Cannot access URLClassLoader#addURL(URL), if you are using Java 9+ try to add the following option to your java command: --add-opens java.base/java.net=ALL-UNNAMED");
                        throw new RuntimeException("Cannot access URLClassLoader#addURL(URL)", e);
                    }
                }
                throw new RuntimeException("Cannot set accessible URLClassLoader#addURL(URL)", exception);
            }
            this.addURLMethodHandle = MethodHandles.lookup().unreflect(addURLMethod).bindTo(classLoader);
        }
        catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToClasspath(URL url) {
        try {
            this.addURLMethodHandle.invokeWithArguments(Objects.requireNonNull(url, "url"));
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void addToClasspath(Path path) {
        try {
            this.addToClasspath(Objects.requireNonNull(path, "path").toUri().toURL());
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void openUrlClassLoaderModule() throws Exception {
        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule", new Class[0]);
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);
        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class, new Object[0]);
        Object thisModule = getModuleMethod.invoke(URLClassLoaderHelper.class, new Object[0]);
        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }

    private MethodHandle getPrivilegedMethodHandle(Method method) throws Exception {
        for (Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
            if (trustedLookup.getType() != MethodHandles.Lookup.class || !Modifier.isStatic(trustedLookup.getModifiers()) || trustedLookup.isSynthetic()) continue;
            try {
                MethodHandles.Lookup lookup = (MethodHandles.Lookup)theUnsafe.getObject(theUnsafe.staticFieldBase(trustedLookup), theUnsafe.staticFieldOffset(trustedLookup));
                return lookup.unreflect(method);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        throw new RuntimeException("Cannot get privileged method handle.");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void addOpensWithAgent(LibraryManager libraryManager) throws Exception {
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader(new URL[0]);
        try {
            isolatedClassLoader.addPath(libraryManager.downloadLibrary(Library.builder().groupId("net.bytebuddy").artifactId("byte-buddy-agent").version("1.12.1").checksum("mcCtBT9cljUEniB5ESpPDYZMfVxEs1JRPllOiWTP+bM=").repository("https://repo1.maven.org/maven2/").build()));
            Class<?> byteBuddyAgent = isolatedClassLoader.loadClass("net.bytebuddy.agent.ByteBuddyAgent");
            Object instrumentation = byteBuddyAgent.getDeclaredMethod("install", new Class[0]).invoke(null, new Object[0]);
            Class<?> instrumentationClass = Class.forName("java.lang.instrument.Instrumentation");
            Method redefineModule = instrumentationClass.getDeclaredMethod("redefineModule", Class.forName("java.lang.Module"), Set.class, Map.class, Map.class, Set.class, Map.class);
            Method getModule = Class.class.getDeclaredMethod("getModule", new Class[0]);
            Map<String, Set<Object>> toOpen = Collections.singletonMap("java.net", Collections.singleton(getModule.invoke(this.getClass(), new Object[0])));
            redefineModule.invoke(instrumentation, getModule.invoke(URLClassLoader.class, new Object[0]), Collections.emptySet(), Collections.emptyMap(), toOpen, Collections.emptySet(), Collections.emptyMap());
        }
        finally {
            try {
                isolatedClassLoader.close();
            }
            catch (Exception exception) {}
        }
    }

    static {
        Unsafe unsafe = null;
        for (Field f : Unsafe.class.getDeclaredFields()) {
            try {
                if (f.getType() != Unsafe.class || !Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                unsafe = (Unsafe)f.get(null);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        theUnsafe = unsafe;
    }
}

