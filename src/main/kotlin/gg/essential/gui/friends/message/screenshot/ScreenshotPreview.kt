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
package gg.essential.gui.friends.message.screenshot

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.image.PixelBufferTexture
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.gui.screenshot.providers.WindowedProvider
import gg.essential.universal.UMatrixStack
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

abstract class ScreenshotPreview(
    val screenshotId: ScreenshotId,
    private val provider: SimpleScreenshotProvider,
) : UIContainer() {

    private val imgIdentifier = mutableStateOf<ResourceLocation?>(null)
    protected val imgTexture =
        imgIdentifier.map { it?.let(Minecraft.getMinecraft().textureManager::getTexture) as PixelBufferTexture? }
    protected val imageAspectState =
        imgTexture.map {
            if (it == null || it.error) {
                16.0f / 9.0f
            } else {
                it.imageWidth / it.imageHeight.toFloat()
            }
        }
    protected val img by ScreenshotImage(imgTexture.toV1(this))

    override fun draw(matrixStack: UMatrixStack) {
        imgIdentifier.set(provider.imageMap[screenshotId])
        super.draw(matrixStack)
        val index = provider.currentPaths.indexOf(screenshotId)
        provider.renderedLastFrame =
            provider.renderedLastFrame?.expandToInclude(index) ?: WindowedProvider.Window(index..index, false)
    }

}