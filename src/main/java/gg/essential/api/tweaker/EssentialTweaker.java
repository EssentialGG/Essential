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
package gg.essential.api.tweaker;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

@Deprecated
public class EssentialTweaker {

    public static boolean initialized = false;

    /**
     * Called by loader stage2.
     *
     * @param gameDir current game dir
     * @deprecated Internal use only. Called by loader stage2.
     */
    @Deprecated
    public static void initialize(File gameDir) {
        initialized = true;

        try {
            Class.forName("gg.essential.main.Bootstrap", false, EssentialTweaker.class.getClassLoader())
                .getDeclaredMethod("initialize")
                .invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
