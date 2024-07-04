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
package club.sk1er.mods.core.tweaker;

import gg.essential.api.tweaker.EssentialTweaker;

import java.io.File;

@Deprecated
public class ModCoreTweaker {

    /**
     * Please use new package. This exists to provide compatibility to those mods which implemented the original Essential loading specification.
     * @see EssentialTweaker
     * @param gameDir current Minecraft home
     */
    @Deprecated
    public static void initialize(File gameDir) {
       EssentialTweaker.initialize(gameDir);
    }
}
