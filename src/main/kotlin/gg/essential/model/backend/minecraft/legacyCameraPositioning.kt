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
//#if MC<11400
package gg.essential.model.backend.minecraft

import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.vec3
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU

fun getRelativeCameraPosFromGlState(): Vec3 {
    val modelMatrix = BufferUtils.createFloatBuffer(16)
    val projectionMatrix = BufferUtils.createFloatBuffer(16)
    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix)
    GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix)

    val viewport = BufferUtils.createIntBuffer(16)
    GL11.glGetInteger(GL11.GL_VIEWPORT, viewport)
    val centerX = (viewport[0] + viewport[2]) / 2f
    val centerY = (viewport[1] + viewport[3]) / 2f

    val objPos = BufferUtils.createFloatBuffer(3)
    GLU.gluUnProject(centerX, centerY, 0.0f, modelMatrix, projectionMatrix, viewport, objPos)
    return vec3(objPos[0], objPos[1], objPos[2])
}
//#endif
