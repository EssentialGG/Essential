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
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.elementa.effects.AlphaEffect
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.image.PixelBufferTexture
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.gui.screenshot.providers.WindowedProvider
import gg.essential.universal.UKeyboard
import gg.essential.universal.UResolution
import gg.essential.universal.USound
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

class FocusListComponent(
    private val screenshotBrowser: ScreenshotBrowser,
) : FocusComponent(screenshotBrowser, FocusType.VIEW) {

    /**
     * The index of the current item the preview should be centered around in the current
     * tab
     */
    private var previewIndex = 0

    fun beginPreview(properties: ScreenshotProperties) {
        time.setText(properties.id.name)

        previewIndex = providerManager.currentPaths.indexOf(properties.id)

        container.clearChildren()

        FocusScreenshotRange(screenshotBrowser, properties).constrain {
            width = 100.percent
            height = 100.percent
        } childOf container

    }

    init {
        delete.onLeftClick {
            deleteCurrentFocus(false)
        }
        favorite.onLeftClick {
            val favState = screenshotBrowser.stateManager.getFavoriteState(previewing)
            favState.set { !it }
        }


        screenshotBrowser.focusing.onSetValue { properties ->
            if (properties != null) {
                val favState = screenshotBrowser.stateManager.getFavoriteState(properties)

                applyFavoriteState(favState)
            }
        }

        screenshotBrowser.window.onKeyType { _, keyCode ->
            if (active.get()) {
                if (keyCode == UKeyboard.KEY_LEFT && previewIndex > 0) {
                    focus(previewIndex - 1)
                } else if (keyCode == UKeyboard.KEY_RIGHT && previewIndex < providerManager.currentPaths.size - 1) {
                    focus(previewIndex + 1)
                }
            }
        }
    }


    inner class FocusScreenshotRange(
        private val screenshotBrowser: ScreenshotBrowser,
        centeredAround: ScreenshotProperties,
    ) : UIContainer() {

        private val providerManager = screenshotBrowser.providerManager
        private val centerIndex = providerManager.currentPaths.indexOf(centeredAround.id)

        // Based on the Figma designs (Screenshot Sharing > Share from Browser)
        private val padding = 23f / UResolution.scaleFactor.toFloat()

        private val center =
            FocusScreenshot(screenshotBrowser, centeredAround, Alignment.CENTER).centered() childOf this

        private var right: FocusScreenshot? = createView(centerIndex + 1, Alignment.RIGHT)?.also { focus ->
            focus.constrain {
                x = SiblingConstraint(padding)
                y = CenterConstraint()
            } childOf this@FocusScreenshotRange
        }
        private var left: FocusScreenshot? = createView(centerIndex - 1, Alignment.LEFT)?.also { focus ->
            focus.constrain {
                x = SiblingConstraint(padding , alignOpposite = true) boundTo center
                y = CenterConstraint()
            } childOf this@FocusScreenshotRange
        }

        init {
            enableEffect(ScissorEffect())

            isMainScreenshotErroredSource.set(center.isScreenshotErrored)
        }

        /**
         * Creates a focus screenshot at the specified index if it is in bound
         */
        private fun createView(index: Int, alignment: Alignment): FocusScreenshot? {
            if (index in providerManager.currentPaths.indices) {
                val properties = providerManager.propertyMap[providerManager.currentPaths[index]]
                if (properties != null) {
                    return FocusScreenshot(screenshotBrowser, properties, alignment)
                }
            }
            return null
        }

        override fun animationFrame() {
            val map = providerManager.provideFocus(
                // Preload one extra in each direction so switching feels better, multiple windows ordered by importance
                listOf(0, 1, -1, 2, -2)
                    .map { it + centerIndex }
                    .filter { it in providerManager.currentPaths.indices }
                    .map { WindowedProvider.Window(IntRange(it, it), false) }
            )
            readTexture(center, map)
            left?.let {
                readTexture(it, map)
            }
            right?.let {
                readTexture(it, map)
            }
            super.animationFrame()
        }

        /**
         * Applies the texture for the supplied focus screenshot if its present
         */
        private fun readTexture(focusScreenshot: FocusScreenshot, map: Map<ScreenshotId, ResourceLocation>) {
            val resourceLocation = map[focusScreenshot.properties.id]
            if (resourceLocation != null) {
                val textureManager = Minecraft.getMinecraft().textureManager
                val texture = textureManager.getTexture(resourceLocation) as PixelBufferTexture?
                if (texture != null) {
                    focusScreenshot.applyTexture(texture)
                }
            }
        }

        private inner class FocusScreenshot(
            private val screenshotBrowser: ScreenshotBrowser,
            val properties: ScreenshotProperties,
            val alignment: Alignment,
        ) : UIContainer() {
            val isScreenshotErrored = mutableStateOf(false)

            private val stateManager = screenshotBrowser.stateManager
            private val aspectRatio = stateManager.getAspectRatio(properties)
            private val isMainPreview = screenshotBrowser.focusing.map {
                it == properties
            }

            private val viewAspectRatio = isScreenshotErrored.zip(aspectRatio.toV2()) { errored, aspectRatio ->
                    if (errored) 16.0f/9.0f else aspectRatio
                }.toV1(this@FocusScreenshot)

            private val container = UIContainer().centered().constrain {
                width = AspectPreservingFillConstraint(viewAspectRatio)
                height = AspectPreservingFillConstraint(viewAspectRatio)
                if (alignment != Alignment.CENTER) {
                    x = 0.pixels(alignOpposite = alignment == Alignment.LEFT)
                }
            } childOf this

            private val image = ScreenshotImage()

            private val hovered = container.hoveredState()

            init {
                container.layout {
                    if_(isScreenshotErrored) {
                        invalidScreenshotView()
                    } `else` {
                        image(Modifier.fillParent())
                    }
                }

                container.effect(AlphaEffect(hovered.zip(isMainPreview).map { (hovered, main) ->
                    if (main) {
                        1.0f
                    } else {
                        if (hovered) {
                            0.5f
                        } else {
                            0.25f
                        }
                    }
                }))

                container.onMouseClick {
                    if (isMainPreview.get()) {
                        if (it.mouseButton == 1) {
                            val paths = screenshotBrowser.providerManager.currentPaths
                            val index = paths.indexOf(properties.id)

                            screenshotBrowser.optionsDropdown.handleRightClick(
                                properties,
                                it,
                                isScreenshotErrored.get()
                            ) {
                                val currentPaths = screenshotBrowser.providerManager.currentPaths
                                val nextIndex = if (index == currentPaths.size) index - 1 else index

                                focus(nextIndex)
                            }
                        }
                    } else if (it.mouseButton == 0) {
                        screenshotBrowser.openFocusView(properties)
                        USound.playButtonPress()
                    }
                }

                centered().constrain {
                    width = focusImageWidthPercent.percent
                    height = 100.percent - (focusImageVerticalPadding * 2).pixels
                }
            }


            fun applyTexture(texture: PixelBufferTexture) {
                image.texture.set(texture)
                aspectRatio.set(texture.imageWidth / texture.imageHeight.toFloat())

                isScreenshotErrored.set(texture.error)
            }
        }
    }

    private enum class Alignment {
        CENTER,
        LEFT,
        RIGHT;
    }
}


