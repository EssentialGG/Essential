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
package gg.essential.gui.screenshot.image

import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.awt.Color

open class ScreenshotImage(val texture: State<PixelBufferTexture?>) : UIComponent() {

    constructor(texture: PixelBufferTexture? = null) : this(BasicState(texture))

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)
        val textureInstance = texture.get()
        if (textureInstance != null) {
            val x = this.getLeft().toDouble()
            val y = this.getTop().toDouble()
            val width = this.getWidth().toDouble()
            val height = this.getHeight().toDouble()
            val color = this.getColor()

            if (color.alpha == 0) {
                return super.draw(matrixStack)
            }
            matrixStack.push()
            matrixStack.translate(x, y, 0.0)
            renderImage(matrixStack, color, width, height)
            matrixStack.pop()

        }

        super.draw(matrixStack)
    }

    fun renderImage(
        matrixStack: UMatrixStack,
        color: Color,
        width: Double,
        height: Double
    ) {
        val textureInstance = texture.get() ?: return

        UGraphics.enableBlend()
        UGraphics.enableAlpha()
        //#if MC<=11202
        val glId = textureInstance.glTextureId
        //#else
        //$$ val glId = textureInstance.getGlTextureId()
        //#endif
        UGraphics.bindTexture(0, glId)
        val red = color.red.toFloat() / 255f
        val green = color.green.toFloat() / 255f
        val blue = color.blue.toFloat() / 255f
        val alpha = color.alpha.toFloat() / 255f
        val worldRenderer = UGraphics.getFromTessellator()
        UGraphics.configureTexture(glId) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        }

        worldRenderer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)

        worldRenderer.pos(matrixStack, 0.0, height, 0.0).tex(0.0, 1.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, width, height, 0.0).tex(1.0, 1.0).color(red, green, blue, alpha)
            .endVertex()
        worldRenderer.pos(matrixStack, width, 0.0, 0.0).tex(1.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, 0.0, 0.0, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.drawDirect()

    }
}