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
package gg.essential.util

import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
//#if MC>=11700
//$$ import org.lwjgl.opengl.GL30.glBindFramebuffer
//$$ import org.lwjgl.opengl.GL30.glDeleteFramebuffers
//$$ import org.lwjgl.opengl.GL30.glFramebufferTexture2D
//$$ import org.lwjgl.opengl.GL30.glGenFramebuffers
//#elseif MC>=11400
//$$ import com.mojang.blaze3d.platform.GlStateManager.bindFramebuffer as glBindFramebuffer
//$$ import com.mojang.blaze3d.platform.GlStateManager.deleteFramebuffers as glDeleteFramebuffers
//$$ import com.mojang.blaze3d.platform.GlStateManager.framebufferTexture2D as glFramebufferTexture2D
//$$ import com.mojang.blaze3d.platform.GlStateManager.genFramebuffers as glGenFramebuffers
//#else
import net.minecraft.client.renderer.OpenGlHelper.glBindFramebuffer
import net.minecraft.client.renderer.OpenGlHelper.glDeleteFramebuffers
import net.minecraft.client.renderer.OpenGlHelper.glFramebufferTexture2D
import net.minecraft.client.renderer.OpenGlHelper.glGenFramebuffers
//#endif
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8
import org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL
import org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT
import org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.GL_UNSIGNED_INT_24_8
import java.awt.Color
import java.nio.ByteBuffer

class GlFrameBuffer(width: Int, height: Int) {
    var width: Int = width
        private set
    var height: Int = height
        private set

    var frameBuffer = -1
        private set
    var texture = -1
        private set
    var depthStencil = -1
        private set

    fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height && this.frameBuffer != -1) {
            return
        }
        this.width = width
        this.height = height

        delete()

        frameBuffer = glGenFramebuffers()
        texture = glGenTextures()
        depthStencil = glGenTextures()

        UGraphics.configureTexture(texture) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA8,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                null as ByteBuffer?
            )
        }

        UGraphics.configureTexture(depthStencil) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_DEPTH24_STENCIL8,
                width,
                height,
                0,
                GL_DEPTH_STENCIL,
                GL_UNSIGNED_INT_24_8,
                null as ByteBuffer?
            )
        }

        withFrameBuffer(frameBuffer) {
            glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                texture,
                0
            )
            glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_DEPTH_STENCIL_ATTACHMENT,
                GL_TEXTURE_2D,
                depthStencil,
                0
            )
        }
    }

    fun delete() {
        if (depthStencil != -1) {
            glDeleteTextures(depthStencil)
            depthStencil = -1
        }

        if (texture != -1) {
            glDeleteTextures(texture)
            texture = -1
        }

        if (frameBuffer != -1) {
            glDeleteFramebuffers(frameBuffer)
            frameBuffer = -1
        }
    }

    fun <T> use(block: () -> T): T {
        if (frameBuffer == -1) {
            resize(width, height)
        }
        return withFrameBuffer(frameBuffer, block)
    }

    private fun <T> withFrameBuffer(glId: Int, block: () -> T): T {
        val unbind = bind(glId)
        try {
            return block()
        } finally {
            unbind()
        }
    }

    fun useAsRenderTarget(block: (UMatrixStack, Int, Int) -> Unit) {
        use {
            // Prepare frame buffer
            val scissorState = glGetBoolean(GL_SCISSOR_TEST)
            glDisable(GL_SCISSOR_TEST)
            glViewport(0, 0, width, height)

            // Undo MC's scaling and the distortion caused by different viewport size with same projection matrix
            val stack = UMatrixStack()
            val scale = 1.0 / UResolution.scaleFactor
            stack.scale(
                scale * UResolution.viewportWidth / width,
                scale * UResolution.viewportHeight / height,
                1.0,
            )

            // Rendering
            block(stack, width, height)

            // Restore the original state
            glViewport(0, 0, UResolution.viewportWidth, UResolution.viewportHeight)
            if (scissorState) glEnable(GL_SCISSOR_TEST)
        }
    }

    fun drawTexture(matrixStack: UMatrixStack, x: Double, y: Double, width: Double, height: Double, color: Color) {
        matrixStack.push()

        UGraphics.enableBlend()
        UGraphics.enableAlpha()
        matrixStack.scale(1f, 1f, 50f)

        UGraphics.bindTexture(0, texture)
        val red = color.red.toFloat() / 255f
        val green = color.green.toFloat() / 255f
        val blue = color.blue.toFloat() / 255f
        val alpha = color.alpha.toFloat() / 255f
        val worldRenderer = UGraphics.getFromTessellator()

        worldRenderer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)

        worldRenderer.pos(matrixStack, x, y + height, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, x + width, y + height, 0.0).tex(1.0, 0.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, x + width, y, 0.0).tex(1.0, 1.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.pos(matrixStack, x, y, 0.0).tex(0.0, 1.0).color(red, green, blue, alpha).endVertex()
        worldRenderer.drawDirect()

        UGraphics.bindTexture(0, 0)
        matrixStack.pop()
    }

    fun bind(): () -> Unit {
        if (frameBuffer == -1) {
            resize(width, height)
        }
        return bind(frameBuffer)
    }

    private fun bind(glId: Int): () -> Unit {
        val prevReadFrameBufferBinding = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val prevDrawFrameBufferBinding = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)

        glBindFramebuffer(GL_FRAMEBUFFER, glId)

        return {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFrameBufferBinding)
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFrameBufferBinding)
        }
    }

    @JvmOverloads
    fun clear(
        clearColor: Color = Color(0, 0, 0, 0),
        clearDepth: Double = 1.0,
        clearStencil: Int = 0,
    ) {
        use {
            with(clearColor) {
                GlStateManager.clearColor(red / 255f, green / 255f, blue / 255f, alpha / 255f)
            }
            GlStateManager.clearDepth(clearDepth)
            glClearStencil(clearStencil)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        }
    }
}
