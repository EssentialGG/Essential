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
package gg.essential.gui.common

import gg.essential.Essential
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import gg.essential.util.lwjgl3.api.nanovg.NanoVG
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.glGetBoolean

/**
 * Component which displays a frame buffer texture generated using NanoVG in [renderVG].
 *
 * This extends [UIFrameBuffer] and all its behavior applies here as well.
 */
abstract class UINanoVG : UIFrameBuffer() {
    private var nanoVG: NanoVG? = null

    override fun delete() {
        nanoVG?.delete()
        nanoVG = null

        super.delete()
    }

    override fun render(matrixStack: UMatrixStack, width: Float, height: Float) {
        // Take snapshot of relevant GL state
        val blendState = BlendState.active()
        val depthState = glGetBoolean(GL_DEPTH_TEST)

        val activeTexture = UGraphics.getActiveTexture()
        UGraphics.setActiveTexture(activeTexture)
        val textureState = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)

        // Render
        val nanoVG = nanoVG ?: Essential.getInstance().lwjgl3.get<NanoVG>().also { nanoVG = it }
        nanoVG.beginFrame(width, height, 1.0f)
        renderVG(matrixStack, nanoVG, width, height)
        nanoVG.endFrame()

        // NanoVG will have modified the GL state directly, so MC's state tracker are out of date and will potentially
        // skip calls because they incorrectly see them to be redundant. To fix that, we explicitly tell MC what
        // values may be set by NanoVG (and even if it did not set them, MC's state tracker will be back in sync).
        UGraphics.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        UGraphics.enableBlend()
        UGraphics.disableDepth()
        //#if MC>=11700
        //$$ net.minecraft.client.render.BufferRenderer.unbindAll()
        //#endif

        // Restore the original state
        blendState.activate()
        if (depthState) UGraphics.enableDepth() else UGraphics.disableDepth()

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureState)
        UGraphics.setActiveTexture(activeTexture)
    }

    /**
     * Uses the given [NanoVG] instance to render into the frame buffer.
     *
     * All calls to this method are wrapped in [NanoVG.beginFrame]/[NanoVG.endFrame].
     *
     * The [matrixStack], [width] and [height] follow the specifications in [render].
     */
    protected abstract fun renderVG(matrixStack: UMatrixStack, vg: NanoVG, width: Float, height: Float)
}