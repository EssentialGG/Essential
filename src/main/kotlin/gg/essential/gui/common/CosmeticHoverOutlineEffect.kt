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

import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.UResolution.viewportHeight
import gg.essential.universal.UResolution.viewportWidth
import gg.essential.universal.shader.BlendState
import gg.essential.universal.shader.UShader
import gg.essential.util.GlFrameBuffer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.roundToInt

class CosmeticHoverOutlineEffect(
    private val backgroundColor: Color,
    private val outlineCosmetic: State<List<Cosmetic>>,
) : Effect() {

    private val mc = UMinecraft.getMinecraft()

    private var previousScissorState: Boolean = false
    private var previousFrameBuffer: () -> Unit = {}

    private val mutableHoveredCosmetic = mutableStateOf<Cosmetic?>(null)
    val hoveredCosmetic: State<Cosmetic?> = mutableHoveredCosmetic

    override fun beforeDraw(matrixStack: UMatrixStack) {
        check(active == null) { "Outline effects cannot be nested." }
        active = this

        previousScissorState = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        compositeFrameBuffer.clear(backgroundColor.withAlpha(0))
        previousFrameBuffer = compositeFrameBuffer.bind()
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        previousFrameBuffer()

        if (previousScissorState) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
        }

        UGraphics.enableDepth()

        compositeShader.bind()
        compositeColor?.setValue(compositeFrameBuffer.texture)
        compositeDepth?.setValue(compositeFrameBuffer.depthStencil)
        renderFullScreenQuad()
        compositeShader.unbind()

        mutableHoveredCosmetic.set(computeHoveredCosmetic())

        outlineCosmetic.get().forEach { cosmetic ->
            val hoveredFrameBuffer = frameBuffers[cosmetic]
            if (hoveredFrameBuffer != null) {
                doDrawOutline(hoveredFrameBuffer)
            }
        }

        UGraphics.disableDepth()

        cleanup()

        active = null
    }

    private val frameBuffers = mutableMapOf<Cosmetic, GlFrameBuffer>()

    fun allocOutlineBuffer(cosmetic: Cosmetic): GlFrameBuffer {
        val existingBuffer = frameBuffers[cosmetic]
        if (existingBuffer != null) {
            return existingBuffer
        }
        val buffer = unusedFrameBuffers.removeLastOrNull() ?: GlFrameBuffer(viewportWidth, viewportHeight)
        frameBuffers[cosmetic] = buffer
        buffer.clear()
        return buffer
    }

    private fun computeHoveredCosmetic(): Cosmetic? {
        val scissor = ScissorEffect.currentScissorState
        if (scissor != null && !scissor.contains(UMouse.Scaled.x, UMouse.Scaled.y)) {
            return null
        }

        val (hoveredCosmetic, hoveredDepth) = frameBuffers.entries.associate {
            it.key to it.value.use { readHoveredDepth() }
        }.minByOrNull { it.value } ?: return null

        val compositeDepth = compositeFrameBuffer.use { readHoveredDepth() }
        if (hoveredDepth - 0.0001f >= compositeDepth.coerceAtMost(0.999f)) {
            return null // player is obstructing the cosmetic
        }

        return hoveredCosmetic
    }

    private fun doDrawOutline(hoveredFrameBuffer: GlFrameBuffer) {
        GlStateManager.depthFunc(GL11.GL_ALWAYS)

        outlineShader.bind()
        configureOutlineShaderParams(compositeFrameBuffer.depthStencil, hoveredFrameBuffer.depthStencil)
        renderFullScreenQuad()
        outlineShader.unbind()

        GlStateManager.depthFunc(GL11.GL_LEQUAL)
    }

    private fun configureOutlineShaderParams(compositeStencil: Int, targetStencil: Int) {
        outlineComposite?.setValue(compositeStencil)
        outlineTarget?.setValue(targetStencil)
        outlineOneTexel?.setValue(1f / viewportWidth, 1f / viewportHeight)
        outlineWidth?.setValue(UMinecraft.guiScale * 2);
    }

    fun cleanup() {
        unusedFrameBuffers.addAll(frameBuffers.values)
        frameBuffers.clear()
        if (compositeFrameBuffer.width != viewportWidth || compositeFrameBuffer.height != viewportHeight) {
            compositeFrameBuffer.resize(viewportWidth, viewportHeight)
            unusedFrameBuffers.forEach { it.resize(viewportWidth, viewportHeight) }
        }
    }

    private fun renderFullScreenQuad() {
        UGraphics.getFromTessellator().apply {
            beginWithActiveShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
            pos(UMatrixStack.UNIT, 0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 0.0, 0.0).tex(1.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 1.0, 0.0).tex(1.0, 1.0).endVertex()
            pos(UMatrixStack.UNIT, 0.0, 1.0, 0.0).tex(0.0, 1.0).endVertex()
            drawDirect()
        }
    }

    private fun ScissorEffect.ScissorState.contains(testX: Double, testY: Double): Boolean {
        val scaleFactor = UResolution.scaleFactor.toInt()
        val tx = (testX * scaleFactor).roundToInt()
        val ty = viewportHeight - (testY * scaleFactor).roundToInt()
        return x <= tx && tx < x + width && y <= ty && ty < y + height
    }

    companion object {
        var active: CosmeticHoverOutlineEffect? = null
            private set

        private val compositeFrameBuffer by lazy { GlFrameBuffer(viewportWidth, viewportHeight) }
        private val unusedFrameBuffers = mutableListOf<GlFrameBuffer>()
        private val tmpFloatBuffer = BufferUtils.createFloatBuffer(1)

        private fun readHoveredDepth(): Float {
            GL11.glReadPixels(
                (UMouse.Scaled.x * UResolution.scaleFactor).toInt(),
                viewportHeight - (UMouse.Scaled.y * UResolution.scaleFactor).toInt(),
                1,
                1,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT,
                tmpFloatBuffer,
            )
            return tmpFloatBuffer.get(0)
        }

        private val vertexShaderSource = """
            #version 120
            varying vec2 texCoord;
            void main(){
                gl_Position = vec4(gl_Vertex.xy * 2.0 - vec2(1.0), 0.5, 1.0);
                texCoord = gl_Vertex.xy;
            }
        """.trimIndent()

        private val compositeShader: UShader = UShader.fromLegacyShader(vertexShaderSource, """
            #version 120
            uniform sampler2D ColorSampler;
            uniform sampler2D DepthSampler;
            varying vec2 texCoord;
            void main() {
                vec4 color = texture2D(ColorSampler, texCoord);
                if (color.a == 0.0) {
                    discard;
                }
                gl_FragColor = vec4(color.rgb, 1.0);
                gl_FragDepth = texture2D(DepthSampler, texCoord).r;
            }
        """.trimIndent(), BlendState.NORMAL)

        private val compositeColor = compositeShader.getSamplerUniformOrNull("ColorSampler")
        private val compositeDepth = compositeShader.getSamplerUniformOrNull("DepthSampler")

        private val outlineShader: UShader = UShader.fromLegacyShader(vertexShaderSource, """
            #version 120
            uniform sampler2D CompositeSampler;
            uniform sampler2D TargetSampler;
            uniform vec2 OneTexel;
            uniform int OutlineWidth;
            varying vec2 texCoord;
            
            vec4 query(vec2 offset) {
                float composite = texture2D(CompositeSampler, texCoord + offset).r;
                float depth = texture2D(TargetSampler, texCoord + offset).r;
                if (depth > 0.99 || composite < depth) {
                    return vec4(0, 0, 0, 0);
                } else {
                    return vec4(1, 1, 1, 1);
                }
            }
            void main() {
                vec4 fragColor;
                
                bool isInside = false;
                bool shouldRender = false;
                
                for (int x = -OutlineWidth; x < OutlineWidth; x++) {
                    for (int y = -OutlineWidth; y < OutlineWidth; y++) {
                        vec2 d = vec2(float(x) * OneTexel.x, float(y) * OneTexel.y);
                        float value = query(d).a;
                        if (x == 0 && y == 0 && value == 1) {
                            isInside = true;
                        }
                        if (value == 1) {
                            shouldRender = true;
                        }
                    }
                }
                if (shouldRender && !isInside) {
                    fragColor = vec4(1, 1, 1, 1);
                } else {
                    fragColor = vec4(0, 0, 0, 0);
                }
                
                float fragDepth;
                {
                    float center = texture2D(TargetSampler, texCoord).r;
                    float left = texture2D(TargetSampler, texCoord - vec2(OneTexel.x, 0.0)).r;
                    float right = texture2D(TargetSampler, texCoord + vec2(OneTexel.x, 0.0)).r;
                    float up = texture2D(TargetSampler, texCoord - vec2(0.0, OneTexel.y)).r;
                    float down = texture2D(TargetSampler, texCoord + vec2(0.0, OneTexel.y)).r;
                    fragDepth = min(center, min(min(left, right), min(up, down)));
                }
                
                gl_FragColor = fragColor;
                gl_FragDepth = fragDepth;
            }
        """.trimIndent(), BlendState.NORMAL)

        private val outlineOneTexel = outlineShader.getFloat2UniformOrNull("OneTexel")
        private val outlineComposite = outlineShader.getSamplerUniformOrNull("CompositeSampler")
        private val outlineTarget = outlineShader.getSamplerUniformOrNull("TargetSampler")
        private val outlineWidth = outlineShader.getIntUniformOrNull("OutlineWidth")
    }
}
