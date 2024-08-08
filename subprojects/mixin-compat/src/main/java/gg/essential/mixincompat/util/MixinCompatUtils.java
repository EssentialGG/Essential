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
package gg.essential.mixincompat.util;

import gg.essential.mixincompat.extensions.MixinConfigExt;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.VersionNumber;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class MixinCompatUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String STAGE0_TWEAKERS_KEY = "essential.loader.stage2.stage0tweakers";
    private static final String MIXIN_BOOTSTRAP_CLASS = MixinBootstrap.class.getName().replace('.', '/') + ".class";
    private static final String OUR_CLASS = MixinCompatUtils.class.getName().replace('.', '/') + ".class";

    private static final VersionNumber VERSION_0_8_4 = VersionNumber.parse("0.8.4"); // Local capture algorithm changed.
    private static final VersionNumber VERSION_LATEST = VersionNumber.parse(MixinEnvironment.getCurrentEnvironment().getVersion());

    private static final ThreadLocal<IMixinInfo> currentMixinInfo = new ThreadLocal<>();
    private static VersionNumber mixinVersionBeforeUs = null;

    public static void withCurrentMixinInfo(IMixinInfo info, Runnable block) {
        IMixinInfo old = currentMixinInfo.get();
        currentMixinInfo.set(info);
        try {
            block.run();
        } finally {
            currentMixinInfo.set(old);
        }
    }

    public static <T> T withCurrentMixinInfo(IMixinInfo info, Supplier<T> block) {
        IMixinInfo old = currentMixinInfo.get();
        currentMixinInfo.set(info);
        try {
            return block.get();
        } finally {
            currentMixinInfo.set(old);
        }
    }

    public static boolean canUseNewLocalsAlgorithm() {
        IMixinInfo info = currentMixinInfo.get();
        if (info == null) {
            // We're not being invoked from a known call-site, so we provide the new behaviour immediately.
            return true;
        }
        return canUseFeature(info, VERSION_0_8_4);
    }

    private static boolean canUseFeature(IMixinInfo mixin, VersionNumber introducedIn) {
        IMixinConfig config = mixin.getConfig();
        String decorationKey = "essential.can_use_feature_from_" + introducedIn;
        if (config.hasDecoration(decorationKey)) return config.getDecoration(decorationKey);
        boolean result = canUseFeatureImpl(mixin, introducedIn);
        config.decorate(decorationKey, result);
        return result;
    }

    private static boolean canUseFeatureImpl(IMixinInfo mixin, VersionNumber introducedIn) {
        IMixinConfig config = mixin.getConfig();

        // If they have set their `minVersion` to a version new enough, it was built against the new behaviour.
        if (config instanceof MixinConfigExt) {
            MixinConfigExt ext = ((MixinConfigExt) config);
            if (ext.getMinVersion().compareTo(introducedIn) >= 0) {
                return true;
            }
        }

        try (JarFile jar = getJar(mixin)) {
            if (jar != null) {
                // If they bundle a version of Mixin, they almost certainly want the behaviour for said version.
                VersionNumber bundled = getBundledMixinVersion(jar);
                if (bundled != null) {
                    return bundled.compareTo(introducedIn) >= 0;
                }
                // Otherwise, if they use Essential, we'll treat them as bundling 0.8.4,
                // unless they opt in to newer behaviour via their min version.
                if (dependsOnEssential(jar)) {
                    return VERSION_0_8_4.compareTo(introducedIn) >= 0;
                }
            }
        } catch (Throwable e) {
            LOGGER.error("An error occurred while trying to read the jar file for " + mixin + ": ", e);
        }

        // Finally, if none of the above ways worked, we'll fall back to whatever behaviour would've occurred had Essential not been installed.
        return getFallbackMixinVersion().compareTo(introducedIn) >= 0;
    }

    private static VersionNumber getBundledMixinVersion(JarFile jar) throws IOException {
        ZipEntry mixinBootstrap = jar.getEntry(MIXIN_BOOTSTRAP_CLASS);
        if (mixinBootstrap == null) return null;

        try (InputStream stream = jar.getInputStream(mixinBootstrap)) {
            return getMixinVersion(stream);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean dependsOnEssential(JarFile jar) {
        Set<String> tweakers = (Set<String>) Launch.blackboard.get(STAGE0_TWEAKERS_KEY);
        return tweakers.stream().anyMatch(name -> jar.getEntry(name.replace('.', '/') + ".class") != null);
    }

    private static VersionNumber getFallbackMixinVersion() {
        if (mixinVersionBeforeUs != null) return mixinVersionBeforeUs;

        try {
            for (URL source : Launch.classLoader.getSources()) {
                URI uri = source.toURI();
                if (!"file".equals(uri.getScheme())) continue;
                File file = Paths.get(uri).toFile();
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    try (JarFile jar = new JarFile(file)) {
                        if (jar.getEntry(OUR_CLASS) != null) {
                            // Don't consider our own version of Mixin.
                            continue;
                        }
                        ZipEntry entry = jar.getEntry(MIXIN_BOOTSTRAP_CLASS);
                        if (entry == null) continue;

                        try (InputStream stream = jar.getInputStream(entry)) {
                            return mixinVersionBeforeUs = getMixinVersion(stream);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to determine fallback Mixin version. Defaulting to latest functionality: ", e);
        }
        // We are the only version on the classpath, and we can use the latest functionality hopefully without issue.
        return mixinVersionBeforeUs = VERSION_LATEST;
    }

    private static VersionNumber getMixinVersion(InputStream mixinBootstrapStream) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(mixinBootstrapStream).accept(node, ClassReader.SKIP_CODE);
        for (FieldNode field : node.fields) {
            if (field.name.equals("VERSION") && field.value instanceof String) {
                return VersionNumber.parse((String) field.value);
            }
        }
        return null;
    }

    private static JarFile getJar(IMixinInfo mixin) throws IOException, URISyntaxException {
        String resourceName = mixin.getClassRef() + ".class";
        URL url = Launch.classLoader.findResource(resourceName);
        if (url == null) return null;

        // With LaunchWrapper, every class gets their own protection domain and urls are of the form
        // jar:file:/some/path/to/archive.jar!/package/of/javaClass.class
        // or for directories
        // file:/some/path/to/dir/package/of/javaClass.class
        String jarSuffix = "!/" + resourceName;
        String file = url.getFile();
        if ("jar".equals(url.getProtocol()) && file.endsWith(jarSuffix)) {
            URI uri = new URL(file.substring(0, file.lastIndexOf(jarSuffix))).toURI();
            return new JarFile(Paths.get(uri).toFile());
        } else {
            // Likely comes from a directory. We're probably in dev, let's not worry too much.
            return null;
        }
    }
}
