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
import dev.folomeev.kotgl.matrix.vectors.dot
import dev.folomeev.kotgl.matrix.vectors.mutables.minus
import dev.folomeev.kotgl.matrix.vectors.mutables.normalize
import dev.folomeev.kotgl.matrix.vectors.mutables.plusScaled
import dev.folomeev.kotgl.matrix.vectors.mutables.times
import dev.folomeev.kotgl.matrix.vectors.sqrLength
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecZero
import kotlin.math.absoluteValue


/**
 * Represents an infinite plane as defined by the given [normal] and [pointOnPlane].
 */
class PlaneCollisionProvider(
    private val pointOnPlane: Vec3,
    private val normal: Vec3,
) : CollisionProvider {
    override fun query(pos: Vec3, size: Float, offset: Vec3): Pair<Vec3, Vec3>? {
        // Whether we are currently "on top" of the plane (that is, on the side to which the normal is pointing)
        val topSide = pos.dot(normal) > pointOnPlane.dot(normal)

        // Where are we looking to go
        val direction = offset.normalize()
        val dirDotNorm = direction.dot(normal)
        if (dirDotNorm.absoluteValue < 0.001) {
            return null // parallel to plane, will never collide; don't care about points in plane
        } else if (dirDotNorm > 0 == topSide) {
            return null // moving away from the plane, will never collide
        }

        val offsetPlane = pointOnPlane.plusScaled(if (topSide) size else -size, normal)
        val distanceToPlane = offsetPlane.minus(pos).dot(normal) / dirDotNorm
        return if (offset.sqrLength() < distanceToPlane.sqr()) {
            null
        } else {
            Pair(direction.times(distanceToPlane), normal)
        }
    }

    companion object {
        val PlaneXZ = PlaneCollisionProvider(vecZero(), vecUnitY())
    }
}

private fun Float.sqr() = this * this
