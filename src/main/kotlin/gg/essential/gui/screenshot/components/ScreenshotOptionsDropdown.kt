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

import gg.essential.Essential
import gg.essential.data.OnboardingData
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.universal.USound
import gg.essential.util.*

class ScreenshotOptionsDropdown(
    private val screenshotBrowser: ScreenshotBrowser,
    private val menuDialogOwner: State<ScreenshotProperties?>
) {

    /**
     * Opens a right click options menu
     */
    fun handleRightClick(
        screenshotProperties: ScreenshotProperties,
        event: UIClickEvent,
        errored: Boolean,
        delete: () -> Unit = {}
    ) {
        if (menuDialogOwner.get() != null) {
            return
        }
        USound.playButtonPress()
        menuDialogOwner.set { screenshotProperties }
        val posX = event.absoluteX
        val posY = event.absoluteY
        val options = mutableListOf<ContextOptionMenu.Item>()
        val connectionManager = Essential.getInstance().connectionManager
        val screenshotManager = connectionManager.screenshotManager

        val id = screenshotProperties.id

        if (!errored) {
            options.add(ContextOptionMenu.Option("Edit", image = EssentialPalette.EDIT_SHORT_10X7) {
                screenshotBrowser.openEditor(screenshotProperties)
            })

            options.add(ContextOptionMenu.Divider)

            options.add(ContextOptionMenu.Option("Send to Friends", image = EssentialPalette.SOCIAL_10X) {
                GuiUtil.pushModal { createShareScreenshotModal(it, id) }
            })

            options.add(ContextOptionMenu.Option("Copy Picture", image = EssentialPalette.COPY_10X7) {
                screenshotManager.copyScreenshotToClipboard(id)
            })

            if (OnboardingData.hasAcceptedTos() && connectionManager.isAuthenticated) {
                options.add(ContextOptionMenu.Option("Copy Link", image = EssentialPalette.LINK_10X7) {
                    when (id) {
                        is LocalScreenshot -> screenshotManager.uploadAndCopyLinkToClipboard(id.path)
                        is RemoteScreenshot -> screenshotManager.copyLinkToClipboard(id.media)
                    }
                })
            }

            options.add(ContextOptionMenu.Divider)
        }

        options.add(ContextOptionMenu.Option("Properties", image = EssentialPalette.PROPERTIES_10X5) {
            screenshotBrowser.displayPropertiesModal(screenshotProperties)
        })

        if (id is LocalScreenshot) {
            options.add(ContextOptionMenu.Option("File Location", image = EssentialPalette.FOLDER_10X7) {
                openFileInDirectory(id.path)
            })
        }

        options.add(ContextOptionMenu.Divider)

        val mediaId = screenshotProperties.metadata?.mediaId
        if (mediaId != null) {
            options.add(
                ContextOptionMenu.Option(
                    "Remove Upload",
                    image = EssentialPalette.CANCEL_5X,
                    hoveredColor = EssentialPalette.TEXT_WARNING,
                    // New default is text, so remove entirely when removing feature flag
                    hoveredShadowColor = EssentialPalette.BLACK,
                ) {
                    screenshotBrowser.providerManager.handleDelete(
                        screenshotProperties,
                        delete = delete,
                        uploadedOnly = true
                    )
                })
        }

        options.add(
            ContextOptionMenu.Option(
                "Delete",
                image = EssentialPalette.TRASH_9X,
                hoveredColor = EssentialPalette.TEXT_WARNING,
                // New default is text, so remove entirely when removing feature flag
                hoveredShadowColor = EssentialPalette.BLACK,
            ) {
                screenshotBrowser.providerManager.handleDelete(screenshotProperties, delete = delete)
            })
        val menu = ContextOptionMenu(
            posX,
            posY,
            *options.toTypedArray(),
        ) childOf screenshotBrowser.window
        menu.init()

        menu.onClose {
            menuDialogOwner.set(null)
        }
    }
}
