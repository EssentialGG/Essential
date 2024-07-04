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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.or
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.util.hoveredState
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * The share button inside the ScreenshotBrowser that contains a dropdown on click
 */
class ShareButton(val screenshotBrowser: ScreenshotBrowser) : UIContainer() {


    private val shareHovered = hoveredState()
    private val shouldMenuExist = BasicState(false)

    private val image by IconButton(EssentialPalette.UPLOAD_9X, tooltipText = "Share")
        .rebindIconColor(EssentialPalette.getTextColor(shareHovered or shouldMenuExist))
        .setColor(EssentialPalette.getButtonColor(shareHovered or shouldMenuExist).toConstraint()) childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        }
        image.onLeftClick {
            shouldMenuExist.set { !it }
        }
        shouldMenuExist.onSetValue {
            if (it) {
                openMenu()
            }
        }
    }

    private fun openMenu() {
        val options = mutableListOf<ContextOptionMenu.Item>()
        val connectionManager = Essential.getInstance().connectionManager
        val screenshotManager = connectionManager.screenshotManager

        val editComponent = screenshotBrowser.focusEditComponent

        if (OnboardingData.hasAcceptedTos() && connectionManager.isAuthenticated) {
            options.add(ContextOptionMenu.Option("Send to Friends", image = EssentialPalette.SOCIAL_10X) {
                checkForUnsavedEditsAndRun(
                    withUnsavedEdits = { file, metadata ->
                        val future = CompletableFuture<Unit>()

                        GuiUtil.pushModal { manager -> 
                            createShareScreenshotModal(
                                manager,
                                screenshot = LocalScreenshot(file.toPath()),
                                metadata = metadata,
                                onModalCancelled = { future.complete(Unit) },
                                onComplete = { it.whenComplete { _, _ -> future.complete(Unit) } }
                            )
                        }

                        return@checkForUnsavedEditsAndRun future
                    },
                    withoutUnsavedEdits = { id ->
                        GuiUtil.pushModal { createShareScreenshotModal(it, id) }
                    }
                )
            })
        }

        options.add(ContextOptionMenu.Option("Copy Picture", image = EssentialPalette.COPY_10X7) {

            if (editComponent.hasEdits()) {
                editComponent.exportEditImageToTempFile()?.thenAcceptAsync({
                    screenshotManager.copyScreenshotToClipboard(it)

                    // Cleanup temp file
                    FileUtils.deleteQuietly(it)
                }, UMinecraft.getMinecraft().executor) ?: Notifications.error("Picture export failed", "")
            } else {
                val id = screenshotBrowser.focusing.get()?.id ?: return@Option
                screenshotManager.copyScreenshotToClipboard(id)
            }
        })
        if (OnboardingData.hasAcceptedTos() && connectionManager.isAuthenticated) {
            options.add(ContextOptionMenu.Option("Copy Link", image = EssentialPalette.LINK_10X7) {
                checkForUnsavedEditsAndRun(
                    withUnsavedEdits = { file, metadata -> screenshotManager.uploadAndCopyLinkToClipboard(file.toPath(), metadata) },
                    withoutUnsavedEdits = { id ->
                        when (id) {
                            is LocalScreenshot -> screenshotManager.uploadAndCopyLinkToClipboard(id.path)
                            is RemoteScreenshot -> screenshotManager.copyLinkToClipboard(id.media)
                        }
                    }
                )
            })
        }

        val menu = ContextOptionMenu(
            0f,
            0f,
            *options.toTypedArray(),
        )

        menu.onClose {
            shouldMenuExist.set(false)
        }

        Window.enqueueRenderOperation {
            // Align to left when in edit mode
            if (screenshotBrowser.focusType.get() == FocusType.EDIT) {
                val position = ContextOptionMenu.Position(this, true)
                menu.reposition(position.xConstraint, position.yConstraint)
            } else {
                menu.reposition(
                    CopyConstraintFloat() boundTo this@ShareButton,
                    SiblingConstraint(2f) boundTo this@ShareButton
                )
            }

            menu childOf Window.of(this@ShareButton)
            menu.init()
        }
    }

    fun setDimension(dimension: IconButton.Dimension): ShareButton {
        (image as IconButton).setDimension(dimension)
        return this
    }

    private fun checkForUnsavedEditsAndRun(
        withUnsavedEdits: (File, ClientScreenshotMetadata) -> CompletableFuture<*>,
        withoutUnsavedEdits: (ScreenshotId) -> Unit
    ) {
        val connectionManager = Essential.getInstance().connectionManager
        val screenshotManager = connectionManager.screenshotManager

        val editComponent = screenshotBrowser.focusEditComponent

        val focus = screenshotBrowser.focusing.get()
        if (focus != null) {
            if (editComponent.hasEdits()) {
                editComponent.exportEditImageToTempFile()?.thenAcceptAsync({
                    val checksum = screenshotManager.getChecksum(it)
                    if (checksum == null) {
                        Notifications.error("Picture export failed", "")
                        Essential.logger.debug("Unable to read checksum for ${it.absolutePath}")
                        FileUtils.deleteQuietly(it)
                        return@thenAcceptAsync
                    }
                    val metadata = focus.metadata?.cloneWithNewChecksum(checksum)
                        ?: screenshotManager.screenshotMetadataManager.createMetadata(
                            getImageTime(focus, true),
                            checksum
                        )

                    withUnsavedEdits(it, metadata).thenAcceptAsync({ _ ->
                        // Cleanup the temp file
                        FileUtils.deleteQuietly(it)
                    }, UMinecraft.getMinecraft().executor)


                }, UMinecraft.getMinecraft().executor) ?: Notifications.error("Picture export failed", "")

            } else {
                withoutUnsavedEdits(focus.id)
            }
        }
    }
}
