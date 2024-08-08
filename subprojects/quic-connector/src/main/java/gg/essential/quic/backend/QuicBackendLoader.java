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
package gg.essential.quic.backend;

import gg.essential.util.classloader.RelaunchClassLoader;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads our QUIC backend and bundled netty in a (mostly) isolated class loader as not to conflict with the system netty.
 */
public class QuicBackendLoader {

    public static final QuicBackendLoader INSTANCE = new QuicBackendLoader();

    private static final String IMPL_CLASS_NAME = "gg.essential.quic.backend.QuicBackendImpl";

    private static URL extractedBundleJar;

    private static URL findExtractedBundleJar() {
        if (extractedBundleJar == null) {
            try {
                URL bundledJar = QuicBackendLoader.class.getResource("/gg/essential/sps/quic/jvm/netty.jar");
                if (bundledJar == null) throw new IllegalStateException("Failed to find netty bundle jar");
                Path tmpPath = Files.createTempFile("essential-netty", ".jar");
                tmpPath.toFile().deleteOnExit();
                try (InputStream is = bundledJar.openStream()) {
                    Files.copy(is, tmpPath, StandardCopyOption.REPLACE_EXISTING);
                }
                extractedBundleJar = tmpPath.toUri().toURL();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load netty bundle jar");
            }
        }
        return extractedBundleJar;
    }

    private final RelaunchClassLoader loader = new RelaunchClassLoader(new URL[]{findExtractedBundleJar()}, QuicBackendLoader.class.getClassLoader());

    {
        // This will be automatically relocated by shadow on versions where we use relocated SLF4j
        loader.addPackageExclusion("org.slf4j.");
        // This will not be relocated by shadow, which is important because netty itself (being in a nested jar) won't
        // be processed by shadow either, and so will still refer to the original slf4j package (which is fine, because
        // it was designed to work with slf4j v1 and v2 at the same time).
        // If we do not exclude it, then with slf4j 1.8+ (MC 1.17+), where slf4j started using ServiceProvider, it'll
        // discover and try to load implementations which are outside of this class loader, but those will see different
        // slf4j classes than inside the class loader, resulting in a ServiceConfigurationError.
        loader.addPackageExclusion("org~slf4j~".replace('~', '.'));

        loader.addClassExclusion(QuicBackend.class.getName());
        loader.addClassExclusion(QuicListener.class.getName());
    }

    public QuicBackend createImpl(Logger logger, QuicListener listener) {
        try {
            Class<?> quicImplClass = loader.loadClass(IMPL_CLASS_NAME);
            Constructor<?> constructor = quicImplClass.getDeclaredConstructor(Logger.class, QuicListener.class);
            return (QuicBackend) constructor.newInstance(logger, listener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
