/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
// Note: This is from loader stage2, apply fixes there as well
package gg.essential.util.classloader;

import com.google.common.collect.Iterators;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class loader which strongly prefers loading its own instance of a class rather than using the one from its parent.
 * This allows us to re-order the class path such that more recent versions of libraries can be used even when the old
 * one has already been loaded into the system class loader before we get to run.
 * The only exception are JRE internal classes, lwjgl and logging as defined in {@link #packageExclusions}.
 */
public class IsolatedClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    // These should only contain things which need to be on the system class loader because the whole point of
    // relaunching is to get our versions of libraries loaded and anything in here, we cannot replace.
    private final List<String> packageExclusions = new ArrayList<>(Arrays.asList(
        "java.", // JRE cannot be loaded twice
        "javax.", // JRE cannot be loaded twice
        "sun.", // JRE internals cannot be loaded twice
        "jdk.", // JRE cannot be loaded twice
        "org.apache.logging." // Continue to use the logging set up by any pre-launch code
    ));

    private final List<String> classExclusions = new ArrayList<>();

    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    /**
     * The conceptual (but not actual) parent of this class loader.
     * <p>
     * It is not the actual class loader because there is no way in Java 8 to re-define packages if they are already
     * defined in your parent. To work around that, our actual parent is an empty class loader, which has no packages
     * loaded at all, and we manually delegate to the conceptual parent as required.
     */
    private final ClassLoader delegateParent;

    public IsolatedClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, new EmptyClassLoader());

        this.delegateParent = parent;
    }

    public void addPackageExclusion(String packagePrefix) {
        this.packageExclusions.add(packagePrefix);
    }

    public void addClassExclusion(String className) {
        this.classExclusions.add(className);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // Fast path
        Class<?> cls = classes.get(name);
        if (cls != null) {
            return cls;
        }

        // For excluded packages, use the parent class loader
        for (String exclusion : packageExclusions) {
            if (name.startsWith(exclusion)) {
                cls = delegateParent.loadClass(name);
                classes.put(name, cls);
                return cls;
            }
        }

        // For excluded classes, use the parent class loader
        for (String exclusion : classExclusions) {
            if (name.equals(exclusion)) {
                cls = delegateParent.loadClass(name);
                classes.put(name, cls);
                return cls;
            }
        }

        // Class is not excluded, so we define it in this loader regardless of whether it's already loaded in
        // the parent (cause that's the point of re-launching).
        synchronized (getClassLoadingLock(name)) {
            // Check if we have previously loaded this class. May be the case because we do not synchronize on
            // the lock for the fast path, so it may initiate loading multiple times.
            cls = findLoadedClass(name);

            // If the have not yet defined the class, let's do that
            if (cls == null) {
                cls = findClassImpl(name);
            }

            // Class loaded successfully, store it in our map so we can take the fast path in the future
            classes.put(name, cls);

            return cls;
        }
    }

    // We redirect this method to our loadClass (which checks the parent for exclusions) because our loadClass is not
    // getting called on OpenJ9 [1] when resolving references [2] from dynamically generated reflection accessor
    // classes [3].
    // Subclasses should override findClassImpl instead.
    //
    // [1]: https://github.com/ibmruntimes/openj9-openjdk-jdk8/blob/a1a7ea06e2244735697b8b9ae379de0d85ef4d47/jdk/src/share/classes/sun/reflect/package.html#L116-L132
    // [2]: https://github.com/eclipse-openj9/openj9/blob/b430644c83c2a19a2ecf60fa2eebb03e6976ce42/jcl/src/java.base/share/classes/java/lang/ClassLoader.java#L1347
    // [3]: https://github.com/ibmruntimes/openj9-openjdk-jdk8/blob/c74851c6f9218e365e3e74c5a01ebf794c3721d1/jdk/src/share/classes/sun/reflect/ClassDefiner.java#L70
    @Override
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        return loadClass(name);
    }

    /**
     * Like {@link #findClass(String)} but we need to override that one to re-check for exclusions.
     */
    protected Class<?> findClassImpl(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        // Try our classpath first because the order of our entries may be different from our parent.
        URL url = findResource(name);
        if (url != null) {
            return url;
        }

        return delegateParent.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return Iterators.asEnumeration(Iterators.concat(
            Iterators.forEnumeration(super.getResources(name)),
            Iterators.forEnumeration(delegateParent.getResources(name))
        ));
    }

    /**
     * We use an empty class loader as the actual parent because using null will use the system class loader and there
     * is plenty of stuff in there.
     */
    private static class EmptyClassLoader extends ClassLoader {
        @Override
        protected Package getPackage(String name) {
            return null;
        }

        @Override
        protected Package[] getPackages() {
            return null;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public URL getResource(String name) {
            return null;
        }

        @Override
        public Enumeration<URL> getResources(String name) {
            return Collections.emptyEnumeration();
        }
    }
}
