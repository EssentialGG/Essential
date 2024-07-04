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
package gg.essential.gui.modals

import gg.essential.Essential
import gg.essential.elementa.dsl.childOf
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager
import gg.essential.util.Multithreading
import java.util.concurrent.TimeUnit

/**
 * Displays when the user attempts to open the Wardrobe, but cosmetics are not fully loaded yet.
 * This modal constructs a new instance of either [CosmeticStudio] or [Wardrobe] when complete.
 */
class CosmeticsLoadingModal(modalManager: ModalManager, callback: () -> Unit) : EssentialModal(modalManager) {

    init {
        configure {
            titleText = "Loading cosmetics"
            contentText = "Cosmetics are currently loading. This may take a moment..."
        }

        // `customContent` has a padding of 4f on its y position
        Spacer(height = 17f - 4f) childOf customContent

        val cosmeticsLoadedFuture = Essential.getInstance().connectionManager.cosmeticsManager.cosmeticsLoadedFuture
        Multithreading.runAsync {
            cosmeticsLoadedFuture.get(CosmeticsManager.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            callback()
        }
    }
}