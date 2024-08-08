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
package gg.essential.gui.screenshot.components

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.Essential
import gg.essential.gui.modals.select.SelectModal
import gg.essential.gui.modals.select.friendsAndGroups
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import java.util.concurrent.CompletableFuture

fun createShareScreenshotModal(
    modalManager: ModalManager,
    screenshot: ScreenshotId,
    metadata: ClientScreenshotMetadata? = null,
    onModalCancelled: (Boolean) -> Unit = {},
    onComplete: (CompletableFuture<*>) -> Unit = {}
): SelectModal<Channel> {
    return selectModal<Channel>(modalManager, "Share Picture") {
        friendsAndGroups()

        modalSettings {
            primaryButtonText = "Share"
            cancelButtonText = "Cancel"
            onCancel(onModalCancelled)
        }

        selectTooltip = "Add"
        deselectTooltip = "Remove"

        requiresSelection = true
        requiresButtonPress = false
    }.onPrimaryAction { selectedChannels ->
        val screenshotManager = Essential.getInstance().connectionManager.screenshotManager
        val future = when (screenshot) {
            is LocalScreenshot -> {
                if (metadata != null) {
                    screenshotManager.uploadAndShareLinkToChannels(selectedChannels, screenshot.path, metadata)
                } else {
                    screenshotManager.uploadAndShareLinkToChannels(selectedChannels, screenshot.path)
                }
            }

            is RemoteScreenshot -> {
                screenshotManager.shareLinkToChannels(selectedChannels, screenshot.media)
                CompletableFuture.completedFuture(Unit)
            }
        }

        onComplete(future)
    }
}
