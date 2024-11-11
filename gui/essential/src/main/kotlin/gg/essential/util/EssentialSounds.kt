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
package gg.essential.util

import gg.essential.config.LoadsResources
import gg.essential.util.GuiEssentialPlatform.Companion.platform

object EssentialSounds {
    // Note: Sounds must be registered in `assets/essential/sounds.json` before they can be played.
    fun playSound(identifier: UIdentifier) = platform.playSound(identifier)

    @LoadsResources("/assets/essential/sounds/screenshot.ogg")
    fun playScreenshotSound() = playSound(UIdentifier("essential", "screenshot"))

    @LoadsResources("/assets/essential/sounds/coin_fill_up.ogg")
    fun playCoinsSound() = playSound(UIdentifier("essential", "coin_fill_up"))

    @LoadsResources("/assets/essential/sounds/purchase_confirmation.ogg")
    fun playPurchaseConfirmationSound() = playSound(UIdentifier("essential", "purchase_confirmation"))
}
