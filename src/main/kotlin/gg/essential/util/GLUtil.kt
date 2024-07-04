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

import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat4
import dev.folomeev.kotgl.matrix.matrices.mutables.mutableMat4

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import dev.folomeev.kotgl.matrix.matrices.mutables.times
//$$ import dev.folomeev.kotgl.matrix.matrices.mutables.toMutable
//$$ import gg.essential.mixins.impl.util.math.Matrix4fExt
//#else
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
//#endif

//#if MC<=11202
import org.lwjgl.opengl.GLContext
//#else
//$$ import org.lwjgl.opengl.GL
//#endif

object GLUtil {

    val isGL30: Lazy<Boolean> = lazy {
        //#if MC<=11202
        GLContext.getCapabilities().OpenGL30
        //#else
        //$$ GL.getCapabilities().OpenGL30
        //#endif
    }

    //#if MC>=11600
    //$$ fun glGetMatrix(stack: MatrixStack, scale: Float): MutableMat4 {
    //$$     return stack.last.matrix.kotgl.toMutable().apply {
    //$$         m03 /= scale
    //$$         m13 /= scale
    //$$         m23 /= scale
    //$$     }
    //$$ }
    //$$
    //$$ fun glMultMatrix(stack: MatrixStack, mat: Mat4, scale: Float) {
    //$$     val scaledMat = mat.toMutable().apply {
    //$$         m03 *= scale
    //$$         m13 *= scale
    //$$         m23 *= scale
    //$$     }
    //$$     val stack = stack.last.matrix
    //$$     stack.kotgl = stack.kotgl.times(scaledMat)
    //$$ }
    //#else
    private val FLOAT16_BUFFER = BufferUtils.createFloatBuffer(16)

    fun glGetMatrix(scale: Float): MutableMat4 {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, FLOAT16_BUFFER)
        return mutableMat4(
            m00 = FLOAT16_BUFFER.get(),
            m10 = FLOAT16_BUFFER.get(),
            m20 = FLOAT16_BUFFER.get(),
            m30 = FLOAT16_BUFFER.get(),
            m01 = FLOAT16_BUFFER.get(),
            m11 = FLOAT16_BUFFER.get(),
            m21 = FLOAT16_BUFFER.get(),
            m31 = FLOAT16_BUFFER.get(),
            m02 = FLOAT16_BUFFER.get(),
            m12 = FLOAT16_BUFFER.get(),
            m22 = FLOAT16_BUFFER.get(),
            m32 = FLOAT16_BUFFER.get(),
            m03 = FLOAT16_BUFFER.get() / scale,
            m13 = FLOAT16_BUFFER.get() / scale,
            m23 = FLOAT16_BUFFER.get() / scale,
            m33 = FLOAT16_BUFFER.get(),
        ).also {
            FLOAT16_BUFFER.rewind()
        }
    }

    fun glMultMatrix(mat: Mat4, scale: Float) {
        FLOAT16_BUFFER.clear()
        FLOAT16_BUFFER.put(mat.m00)
        FLOAT16_BUFFER.put(mat.m10)
        FLOAT16_BUFFER.put(mat.m20)
        FLOAT16_BUFFER.put(mat.m30)
        FLOAT16_BUFFER.put(mat.m01)
        FLOAT16_BUFFER.put(mat.m11)
        FLOAT16_BUFFER.put(mat.m21)
        FLOAT16_BUFFER.put(mat.m31)
        FLOAT16_BUFFER.put(mat.m02)
        FLOAT16_BUFFER.put(mat.m12)
        FLOAT16_BUFFER.put(mat.m22)
        FLOAT16_BUFFER.put(mat.m32)
        FLOAT16_BUFFER.put(mat.m03 * scale)
        FLOAT16_BUFFER.put(mat.m13 * scale)
        FLOAT16_BUFFER.put(mat.m23 * scale)
        FLOAT16_BUFFER.put(mat.m33)
        FLOAT16_BUFFER.rewind()
        GlStateManager.multMatrix(FLOAT16_BUFFER)
    }
    //#endif

}