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

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.editor.ScreenshotCanvas
import gg.essential.gui.screenshot.image.PixelBufferTexture
import gg.essential.gui.screenshot.providers.toSingleWindowRequest
import gg.essential.universal.USound
import gg.essential.util.GuiUtil
import gg.essential.util.centered
import net.minecraft.client.Minecraft
import java.io.File
import java.util.concurrent.CompletableFuture

class FocusEditComponent(
    private val screenshotBrowser: ScreenshotBrowser
) : FocusComponent(screenshotBrowser, FocusType.EDIT) {

    private val textureState = BasicState<PixelBufferTexture?>(null)
    private val aspectConstraint = textureState.map {
        if (it != null) {
            it.imageWidth / it.imageHeight.toFloat()
        } else {
            16 / 9f
        }
    }

    private val imageSizeContainer by UIContainer().centered().constrain {
        width = focusImageWidthPercent.percent
        height = 100.percent - (focusImageVerticalPadding * 2).pixels
    } childOf container

    private val imageSize by UIContainer().centered().constrain {
        width = AspectPreservingFillConstraint(aspectConstraint)
        height = AspectPreservingFillConstraint(aspectConstraint)
    } childOf imageSizeContainer

    private val canvas by ScreenshotCanvas(textureState).centered().constrain {
        width = (100.percent boundTo imageSize) + 4.pixels // Crop handles are 2px on each side
        height = (100.percent boundTo imageSize) + 4.pixels // Crop handles are 2px on each side
    }.apply {
        screenshotBrowser.closeOperation { vectorEditingOverlay.delete() }
    } childOf container

    private val toolbar by EditorToolbar(screenshotBrowser, canvas, active).constrain {
        y = 10.pixels(alignOpposite = true)
        x = 10.pixels
    } childOf container

    private val saveButton by IconButton(EssentialPalette.SAVE_9X, "Save", iconShadow = true)
        .setDimension(IconButton.Dimension.Fixed(47f, buttonSize))
        .rebindEnabled(canvas.getHasChanges())
        .onActiveClick {
            USound.playButtonPress()
            saveCurrentChanges()
        }.constrain {
            y = 10.pixels(alignOpposite = true)
            x = 10.pixels(alignOpposite = true)
        } childOf container

    private fun saveCurrentChanges() {
        val source = screenshotBrowser.focusing.get()
        if (source != null) {
            canvas.exportImage(
                source = source.id,
                screenshotManager = screenshotBrowser.screenshotManager,
                screenshotBrowser = screenshotBrowser,
                temp = false,
                viewAfter = true,
            )
        }
    }

    init {
        val focusing = screenshotBrowser.focusing

        time.bindText(canvas.getHasChanges().zip(focusing).map { (edits, focusing) ->
            if (focusing != null) {
                if (edits) {
                    "Copy of " + focusing.id.name
                } else {
                    focusing.id.name
                }
            } else {
                ""
            }
        })
    }

    override fun animationFrame() {
        super.animationFrame()

        val focused = screenshotBrowser.focusing.get()
        if (active.get() && focused != null) {

            val targetIndex = providerManager.currentPaths.indexOf(focused.id)
            if (targetIndex != -1) {

                val provideFocus = providerManager.provideFocus(targetIndex.toSingleWindowRequest())
                val resourceLocation = provideFocus[focused.id]

                if (resourceLocation != null) {
                    val textureManager = Minecraft.getMinecraft().textureManager
                    val texture = textureManager.getTexture(resourceLocation) as PixelBufferTexture?
                    if (texture != null) {
                        textureState.set(texture)
                        aspectConstraint.set(texture.imageWidth / texture.imageHeight.toFloat())
                        return
                    }
                }
            }
        }

        textureState.set(null)
    }

    /**
     * @return whether the current screenshot has any edits made to it
     */
    fun hasEdits(): Boolean {
        return canvas.getHasChanges().get()
    }

    /**
     * Exports the current edits to a temporary file
     */
    fun exportEditImageToTempFile(): CompletableFuture<File>? {
        val source = screenshotBrowser.focusing.get()
        return if (source != null) {
            canvas.exportImage(
                source.id,
                screenshotBrowser.screenshotManager,
                screenshotBrowser,
                temp = true,
                viewAfter = false
            )
        } else {
            null
        }
    }

    override fun onClose() {
        canvas.reset()
    }

    override fun onBackButtonPressed() {
        if (canvas.getHasChanges().get()) {
            GuiUtil.pushModal { manager -> 
                ConfirmDenyModal(manager, false).configure {
                    primaryButtonText = "Continue"
                    titleText = "Are you sure you want to quit without saving?"
                }.onPrimaryAction {
                    super.onBackButtonPressed()
                }
            }
        } else {
            super.onBackButtonPressed()
        }
    }
}
