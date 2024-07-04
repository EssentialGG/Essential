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
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.CramSiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.screenshot.constraints.TileConstraint
import gg.essential.gui.screenshot.image.PixelBufferTexture
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.gui.screenshot.providers.WindowedProvider
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

class ScreenshotPreview(
    val properties: ScreenshotProperties,
    private val viewComponent: ListViewComponent,
    private val index: Int,
    private val numberOfItemsPerRow: State<Int>,
) : UIContainer() {
    val isScreenshotErrored = mutableStateOf(false)

    private val stateManager = viewComponent.screenshotBrowser.stateManager
    private val favorite = stateManager.getFavoriteState(properties)
    private val favoriteTextState = stateManager.getFavoriteTextState(properties)
    private val imageAspectState = stateManager.getAspectRatio(properties)

    private val viewAspectRatio = isScreenshotErrored.zip(imageAspectState.toV2()) { errored, aspectRatio ->
            if (errored) 16.0f/9.0f else aspectRatio
        }.toV1(this@ScreenshotPreview)

    private val container by UIContainer().centered().constrain {
        height = AspectPreservingFillConstraint(viewAspectRatio)
        width = AspectPreservingFillConstraint(viewAspectRatio)
    } childOf this

    private val img by ScreenshotImage()

    private val hovered =
        container.hoveredState(layoutSafe = true) or viewComponent.screenshotBrowser.menuDialogOwner.map { it == properties }

    private val favoriteContainer by UIContainer().constrain {
        y = 5.pixels
        x = 5.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint()
        height = ChildBasedSizeConstraint()
    }.bindHoverEssentialTooltip(favoriteTextState) childOf img

    private val solidHeart = ShadowIcon(EssentialPalette.HEART_FILLED_9X, true)
        .rebindPrimaryColor(BasicState(EssentialPalette.TEXT_RED))
        .rebindShadowColor(BasicState(EssentialPalette.BLACK))
        .bindParent(favoriteContainer, favorite)


    private val hollowHeart by ShadowIcon(EssentialPalette.HEART_EMPTY_9X, true)
        .rebindShadowColor(BasicState(EssentialPalette.BLACK))
        .bindParent(favoriteContainer, !favorite and hovered)
        .rebindPrimaryColor(favoriteContainer.hoveredState().map {
            if (it) {
                EssentialPalette.TEXT_RED
            } else {
                EssentialPalette.TEXT
            }
        })

    private val dotsContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = 0.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint() + 10.pixels
        height = ChildBasedSizeConstraint() + 10.pixels
    }.bindParent(img, hovered, delayed = true).bindHoverEssentialTooltip(BasicState("Options"), EssentialTooltip.Position.ABOVE, padding = 2f)

    private val dots by ShadowIcon(EssentialPalette.OPTIONS_8X2, true)
        .centered()
        .rebindShadowColor(BasicState(EssentialPalette.BLACK))
        .rebindPrimaryColor(EssentialPalette.getTextColor(dotsContainer.hoveredState())) childOf dotsContainer

    init {
        container.layout {
            if_(isScreenshotErrored) {
                invalidScreenshotView()
            } `else` {
                img(Modifier.fillParent())
            }
        }

        constrain {
            x = CramSiblingConstraint(horizontalScreenshotPadding)
            y = CramSiblingConstraint(horizontalScreenshotPadding)
            width = TileConstraint(numberOfItemsPerRow, horizontalScreenshotPadding)
            height = AspectConstraint(9 / 16f)
        }
        container.bindEffect(OutlineEffect(EssentialPalette.ACCENT_BLUE, 2f, false), hovered)

        favoriteContainer.onLeftClick {
            USound.playButtonPress()
            // Saving value is handled by ScreenshotStateManager
            favorite.set { !it }
            it.stopPropagation()
        }
        dotsContainer.onLeftClick {
            viewComponent.handleRightClick(this@ScreenshotPreview, it)
            it.stopPropagation()
        }

        container.onMouseClick {
            if (it.mouseButton == 1) {
                viewComponent.handleRightClick(this@ScreenshotPreview, it)
                it.stopPropagation()
            }
        }

        onLeftClick {
            viewComponent.focus(properties)
        }
    }

    /**
     * Receives [resourceLocation] from a WindowedProvider and applies its texture to this preview
     */
    fun updateTexture(resourceLocation: ResourceLocation?) {
        val texture = resourceLocation?.let {
            Minecraft.getMinecraft().textureManager.getTexture(it) as PixelBufferTexture?
        }

        img.texture.set(texture)

        if (texture != null) {
            imageAspectState.set(texture.imageWidth / texture.imageHeight.toFloat())
        }

        isScreenshotErrored.set(texture?.error ?: false)
    }

    override fun draw(matrixStack: UMatrixStack) {
        super.draw(matrixStack)
        val providerManager = viewComponent.screenshotBrowser.providerManager
        providerManager.renderedLastFrame =
            providerManager.renderedLastFrame?.expandToInclude(index) ?: WindowedProvider.Window(index..index, false)
    }
}