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
package gg.essential.model.backend.minecraft

import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec4
import gg.essential.model.ParticleEffect
import gg.essential.model.ParticleSystem
import gg.essential.model.backend.RenderBackend
import gg.essential.model.file.ParticlesFile.Material.*
import gg.essential.model.light.Light
import gg.essential.model.util.Color
import gg.essential.model.util.timesSelf
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft.getMinecraft
import gg.essential.universal.shader.BlendState
import gg.essential.universal.utils.ReleasedDynamicTexture
import gg.essential.universal.vertex.UVertexConsumer
import gg.essential.util.identifier
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.io.ByteArrayInputStream
import gg.essential.model.util.UMatrixStack as CMatrixStack
import gg.essential.model.util.UVertexConsumer as CVertexConsumer

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

//#if MC>=11600
//$$ import net.minecraft.client.renderer.RenderType
//$$ import net.minecraft.client.renderer.texture.OverlayTexture
//#else
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.vertex.VertexFormat
//#endif

object MinecraftRenderBackend : RenderBackend {
    override fun createTexture(name: String, width: Int, height: Int): RenderBackend.Texture {
        return DynamicTexture(identifier("essential", name), ReleasedDynamicTexture(width, height))
    }

    override fun deleteTexture(texture: RenderBackend.Texture) {
        val identifier = (texture as DynamicTexture).identifier

        texture.texture.deleteGlTexture()

        val textureManager = getMinecraft().textureManager
        //#if MC>=11700
        //$$ val registeredTexture = textureManager.getOrDefault(identifier, null) as? ReleasedDynamicTexture
        //#else
        val registeredTexture = textureManager.getTexture(identifier) as? ReleasedDynamicTexture
        //#endif
        if (registeredTexture == texture.texture) {
            textureManager.deleteTexture(identifier)
        }
    }

    override fun blitTexture(dst: RenderBackend.Texture, ops: Iterable<RenderBackend.BlitOp>) {
        val textureManager = getMinecraft().textureManager
        fun RenderBackend.Texture.glId() =
            textureManager.getTexture((this as MinecraftTexture).identifier)!!.glTextureId

        val prevScissor = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST)
        if (prevScissor) GL11.glDisable(GL11.GL_SCISSOR_TEST)

        val prevDrawFrameBufferBinding = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val prevReadFrameBufferBinding = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)

        val dstBuffer = glGenFramebuffers()
        val srcBuffer = glGenFramebuffers()

        glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstBuffer)
        glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcBuffer)
        glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, dst.glId(), 0)

        for ((src, srcX, srcY, destX, destY, width, height) in ops) {
            glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, src.glId(), 0)
            GL30.glBlitFramebuffer(
                srcX, srcY, srcX + width, srcY + height,
                destX, destY, destX + width, destY + height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
            )
        }

        glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFrameBufferBinding)
        glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFrameBufferBinding)

        glDeleteFramebuffers(dstBuffer)
        glDeleteFramebuffers(srcBuffer)

        if (prevScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST)
    }

    override suspend fun readTexture(name: String, bytes: ByteArray): RenderBackend.Texture {
        return CosmeticTexture(name, UGraphics.getTexture(ByteArrayInputStream(bytes)))
    }

    interface MinecraftTexture : RenderBackend.Texture {
        val identifier: ResourceLocation
    }

    data class SkinTexture(override val identifier: ResourceLocation) : MinecraftTexture {
        override val width: Int
            get() = 64
        override val height: Int
            get() = 64
    }

    data class CapeTexture(override val identifier: ResourceLocation) : MinecraftTexture {
        override val width: Int
            get() = 64
        override val height: Int
            get() = 32
    }

    open class DynamicTexture(identifier: ResourceLocation, val texture: ReleasedDynamicTexture) : MinecraftTexture {
        override val width: Int = texture.width
        override val height: Int = texture.height

        override val identifier: ResourceLocation by lazy {
            val textureManager = getMinecraft().textureManager
            //#if MC>=11700
            //$$ (textureManager.getOrDefault(identifier, null) as? ReleasedDynamicTexture)?.clearGlId()
            //#else
            (textureManager.getTexture(identifier) as? ReleasedDynamicTexture)?.deleteGlTexture()
            //#endif
            textureManager.loadTexture(identifier, texture)
            identifier
        }
    }

    class CosmeticTexture(
        val name: String,
        texture: ReleasedDynamicTexture,
    ) : DynamicTexture(identifier("essential", "textures/cosmetics/${name.lowercase()}"), texture)

    //#if MC>=11600
    //$$ class VertexConsumerProvider(val provider: net.minecraft.client.renderer.IRenderTypeBuffer, val light: Int) : RenderBackend.VertexConsumerProvider {
    //#else
    class VertexConsumerProvider : RenderBackend.VertexConsumerProvider {
    //#endif

        override fun provide(texture: RenderBackend.Texture, block: (CVertexConsumer) -> Unit) {
            require(texture is MinecraftTexture)

            class VertexConsumerAdapter(private val inner: UVertexConsumer) : CVertexConsumer {
                override fun pos(stack: CMatrixStack, x: Double, y: Double, z: Double) = apply {
                    val vec = mutableVec4(x.toFloat(), y.toFloat(), z.toFloat(), 1f)
                    vec.timesSelf(stack.peek().model)
                    inner.pos(UMatrixStack.UNIT, vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
                    //#if MC>=11600
                    //$$ inner.color(1f, 1f, 1f, 1f)
                    //#endif
                }
                override fun tex(u: Double, v: Double) = apply {
                    inner.tex(u, v)
                    //#if MC>=11600
                    //$$ inner.overlay(OverlayTexture.getU(0f), OverlayTexture.getV(false))
                    //$$ inner.light(light and 0xffff, (light shr 16) and 0xffff)
                    //#else
                    // Note: X and Y are flipped because `BufferBuilder.lightmap` flips them in the SHORT case
                    inner.light(OpenGlHelper.lastBrightnessY.toInt(), OpenGlHelper.lastBrightnessX.toInt())
                    //#endif
                }
                override fun norm(stack: CMatrixStack, x: Float, y: Float, z: Float) = apply {
                    val vec = mutableVec3(x, y, z)
                    vec.timesSelf(stack.peek().normal)
                    inner.norm(UMatrixStack.UNIT, vec.x, vec.y, vec.z)
                }
                override fun color(color: Color): CVertexConsumer = this
                override fun light(light: Light): CVertexConsumer = this
                override fun endVertex() = apply { inner.endVertex() }
            }
            //#if MC>=11600
            //$$ val buffer = provider.getBuffer(RenderType.getEntityTranslucentCull(texture.identifier))
            //$$ block(VertexConsumerAdapter(UVertexConsumer.of(buffer)))
            //#else
            val renderer = UGraphics.getFromTessellator()
            GlStateManager.enableCull()
            UGraphics.enableAlpha()
            UGraphics.enableBlend()
            UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
            UGraphics.color4f(1f, 1f, 1f, 1f)
            val prevTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            UGraphics.bindTexture(0, texture.identifier)
            renderer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VERTEX_FORMAT)

            block(VertexConsumerAdapter(renderer.asUVertexConsumer()))

            renderer.drawSorted(0, 0, 0)

            GlStateManager.disableCull()
            UGraphics.bindTexture(0, prevTextureId)
            //#endif
        }

        //#if MC<11400
        companion object {
            private val VERTEX_FORMAT = VertexFormat().apply {
                addElement(DefaultVertexFormats.POSITION_3F)
                addElement(DefaultVertexFormats.TEX_2F) // texture
                addElement(DefaultVertexFormats.TEX_2S) // lightmap
                addElement(DefaultVertexFormats.NORMAL_3B)
                addElement(DefaultVertexFormats.PADDING_1B)
            }
        }
        //#endif
    }

    class ParticleVertexConsumerProvider : ParticleSystem.VertexConsumerProvider {
        override fun provide(renderPass: ParticleEffect.RenderPass, block: (CVertexConsumer) -> Unit) {
            val texture = renderPass.texture
            require(texture is MinecraftTexture)

            class VertexConsumerAdapter(private val inner: UVertexConsumer) : CVertexConsumer {
                override fun pos(stack: CMatrixStack, x: Double, y: Double, z: Double) = apply {
                    val vec = mutableVec4(x.toFloat(), y.toFloat(), z.toFloat(), 1f)
                    vec.timesSelf(stack.peek().model)
                    inner.pos(UMatrixStack.UNIT, vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
                }
                override fun tex(u: Double, v: Double) = apply {
                    inner.tex(u, v)
                }
                override fun norm(stack: CMatrixStack, x: Float, y: Float, z: Float) = this
                override fun color(color: Color): CVertexConsumer = apply {
                    with(color) {
                        inner.color(r.toInt(), g.toInt(), b.toInt(), a.toInt())
                    }
                }
                override fun light(light: Light): CVertexConsumer = apply {
                    inner.light(light.blockLight.toInt(), light.skyLight.toInt())
                }
                override fun endVertex() = apply { inner.endVertex() }
            }
            //#if MC<11700
            val prevAlphaTest = GL11.glGetBoolean(GL11.GL_ALPHA_TEST)
            val prevAlphaTestFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC)
            val prevAlphaTestRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF)
            //#endif
            val prevBlend = BlendState.active()
            val prevTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            val prevCull = GL11.glIsEnabled(GL11.GL_CULL_FACE)

            when (renderPass.material) {
                Add -> BlendState(BlendState.Equation.ADD, BlendState.Param.SRC_ALPHA, BlendState.Param.ONE)
                Cutout -> BlendState.DISABLED
                Blend -> BlendState.NORMAL
            }.activate()
            //#if MC<11700
            if (renderPass.material == Cutout) {
                GlStateManager.enableAlpha()
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.5f)
            }
            //#endif
            UGraphics.bindTexture(0, texture.identifier)
            if (!prevCull) GlStateManager.enableCull()

            val renderer = UGraphics.getFromTessellator()
            renderer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP)
            block(VertexConsumerAdapter(renderer.asUVertexConsumer()))
            renderer.drawDirect()

            if (!prevCull) GlStateManager.disableCull()
            UGraphics.bindTexture(0, prevTextureId)
            prevBlend.activate()
            //#if MC<11700
            if (renderPass.material == Cutout) {
                if (!prevAlphaTest) GlStateManager.disableAlpha()
                GlStateManager.alphaFunc(prevAlphaTestFunc, prevAlphaTestRef)
            }
            //#endif
        }
    }
}