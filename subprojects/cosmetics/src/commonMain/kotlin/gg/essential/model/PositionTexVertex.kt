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
package gg.essential.model

import kotlin.jvm.JvmField

// TODO clean up
data class PositionTexVertex(
    @JvmField
    var vector3: Vector3,
    var texturePositionX: Float,
    @JvmField
    var texturePositionY: Float,
) {

    constructor(x: Float, y: Float, z: Float, u: Float, v: Float) : this(Vector3(x, y, z), u, v)

    constructor(
        textureVertex: PositionTexVertex,
        texturePositionXIn: Float,
        texturePositionYIn: Float
    ) : this(textureVertex.vector3, texturePositionXIn, texturePositionYIn)

    fun setTexturePosition(u: Float, v: Float): PositionTexVertex {
        return PositionTexVertex(this, u, v)
    }

    fun copy(): PositionTexVertex {
        return PositionTexVertex(vector3.clone(), texturePositionX, texturePositionY)
    }
}
