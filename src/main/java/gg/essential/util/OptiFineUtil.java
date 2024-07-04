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
package gg.essential.util;

import gg.essential.Essential;

import java.lang.reflect.Field;

public class OptiFineUtil {
    private static final String version;

    public static String getVersion() {
        return version;
    }

    public static boolean isLoaded() {
        return version != null;
    }

    static {
        String detectedVersion;
        try {
            //#if MC>=11400
            //$$ Class<?> configClass = Class.forName("net.optifine.Config");
            //#else
            Class<?> configClass = Class.forName("Config");
            //#endif
            Field versionField = configClass.getField("VERSION");
            detectedVersion = (String) versionField.get(null);
            Essential.logger.info("OptiFine version {} detected.", detectedVersion);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            detectedVersion = null;
            Essential.logger.info("OptiFine not detected.");
        }
        version = detectedVersion;
    }
}
