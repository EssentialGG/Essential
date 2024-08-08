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
package gg.essential.mixincompat;

import gg.essential.CompatMixin;
import gg.essential.CompatShadow;
import org.spongepowered.asm.launch.GlobalProperties;

@CompatMixin(GlobalProperties.class)
public abstract class GlobalPropertiesCompat {
    @CompatShadow
    public static <T> T get(GlobalProperties.Keys key) { throw new LinkageError(); }

    @CompatShadow
    public static <T> T get(GlobalProperties.Keys key, T defaultValue) { throw new LinkageError(); }

    @CompatShadow
    public static String getString(GlobalProperties.Keys key, String defaultValue) { throw new LinkageError(); }

    @CompatShadow
    public static void put(GlobalProperties.Keys key, Object value) { throw new LinkageError(); }

    public static Object get(String key) {
        return get(GlobalProperties.Keys.of(key));
    }

    public static Object get(String key, Object defaultValue) {
        return get(GlobalProperties.Keys.of(key), defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return getString(GlobalProperties.Keys.of(key), defaultValue);
    }

    public static void put(String key, Object value) {
        put(GlobalProperties.Keys.of(key), value);
    }
}
