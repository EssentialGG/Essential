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
package gg.essential.gui.screenshot.action

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.util.Multithreading
import java.io.File

/**
 * Used to execute an action after a screenshot has been taken.
 * @see gg.essential.network.connectionmanager.media.ScreenshotManager.handleNewScreenshot
 */
sealed class PostScreenshotAction {
    companion object {
        @JvmStatic
        fun current(): PostScreenshotAction {
            return when (EssentialConfig.postScreenshotAction) {
                0 -> Nothing
                1 -> CopyImage
                2 -> CopyURL
                else -> Nothing
            }
        }
    }

    abstract fun run(screenshot: File)

    object Nothing: PostScreenshotAction() {
        override fun run(screenshot: File) {}
    }

    object CopyImage: PostScreenshotAction() {
        override fun run(screenshot: File) {
            Multithreading.runAsync {
                Essential.getInstance().connectionManager.screenshotManager.copyScreenshotToClipboard(screenshot)
            }
        }
    }

    object CopyURL: PostScreenshotAction() {
        override fun run(screenshot: File) {
            Essential.getInstance().connectionManager.screenshotManager.uploadAndCopyLinkToClipboard(screenshot.toPath())
        }
    }
}