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
package gg.essential.model.collision

import dev.folomeev.kotgl.matrix.vectors.Vec3

interface CollisionProvider {
    /**
     * Tries to move a particle with radius [size] at [pos] by [offset].
     * Returns the actual offset it can move until a collision would occur as well as the normal of the surface it
     * collided with.
     */
    fun query(pos: Vec3, size: Float, offset: Vec3): Pair<Vec3, Vec3>?

    object None : CollisionProvider {
        override fun query(pos: Vec3, size: Float, offset: Vec3): Pair<Vec3, Vec3>? = null
    }
}
