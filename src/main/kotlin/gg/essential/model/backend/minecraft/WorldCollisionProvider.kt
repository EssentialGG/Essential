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

import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.mutables.minus
import dev.folomeev.kotgl.matrix.vectors.mutables.times
import dev.folomeev.kotgl.matrix.vectors.vecUnitX
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecUnitZ
import gg.essential.model.collision.CollisionProvider
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.util.math.AxisAlignedBB
import kotlin.math.abs

//#if MC>=11600
//$$ import net.minecraft.util.Direction
//$$ import kotlin.streams.toList
//#endif

class WorldCollisionProvider(private val world: WorldClient) : CollisionProvider {
    override fun query(pos: Vec3, size: Float, offset: Vec3): Pair<Vec3, Vec3>? {
        val objectBB =
            AxisAlignedBB(
                (pos.x - size).toDouble(),
                (pos.y - size).toDouble(),
                (pos.z - size).toDouble(),
                (pos.x + size).toDouble(),
                (pos.y + size).toDouble(),
                (pos.z + size).toDouble(),
            )
        return query(objectBB, offset)
    }

    private fun query(inputBB: AxisAlignedBB, offset: Vec3): Pair<Vec3, Vec3>? {
        // First find all boxes we could possibly be colliding with
        val maxBB = with(offset) { inputBB.expand(x.toDouble(), y.toDouble(), z.toDouble()) }
        //#if MC>=11600
        //$$ val collisions = world.getCollisionShapes(null, maxBB).toList()
        //#elseif MC>=11200
        val collisions = world.getCollisionBoxes(null, maxBB)
        //#else
        //$$ val collisions = world.getCollisionBoxes(maxBB)
        //#endif
        if (collisions.isEmpty()) {
            return null
        }

        // Then check how far we can go before we hit something
        // Doing this is quite tricky however because we can only really check one axis at a time:
        //
        //   O  |
        //    \ |_  <- A
        //     \  |
        //      \ |
        //       \|  <- B
        //        |\
        //        | \
        //        |__\__
        //            \|
        //             |\
        // __C_________| \   <- D
        //                \
        //
        // O is the object and the backslashes indicate the queried offset
        // The expected result would be at B.
        //
        // If we naively check one axis after the other (like Minecraft itself does), starting with
        // the Y axis, we first
        // move all the way down to point C and then over to the wall at D, giving us the wrong
        // result (and we can't
        // just roll with it because that point isn't even on the path).
        //
        // So instead of moving the object after the axis check, we expand it along the axis,
        // similar to how we
        // expanded it to get all possible collision boxes in the first place, and then compute the
        // possible offset for
        // that expanded box along the next axis. This won't get us all the way to point B though,
        // because the vertical
        // wall at A gets in the way. That's not too big of an issue though because now we do at
        // least have a distance
        // we can safely step (landing us on the path, roughly between A and B) and then we can just
        // repeat the process
        // from there which in this case gets us to our real destination.
        // For this to work under all circumstances, we however also need to switch which axis we
        // check first on each
        // iteration, otherwise if there's a block to the right of C, we might not be able to make
        // any rightwards
        // progress even though the path itself isn't blocked.

        var iteration = 0
        var primaryAxis = when {
            abs(offset.x) > abs(offset.y) && abs(offset.x) > abs(offset.z) -> Axis.X
            abs(offset.y) > abs(offset.z) -> Axis.Y
            else -> Axis.Z
        }
        var remainingOffset = offset
        var objectBB = inputBB
        while (true) {
            var testBB = objectBB
            var x = remainingOffset.x.toDouble()
            var y = remainingOffset.y.toDouble()
            var z = remainingOffset.z.toDouble()

            fun checkXAxis() {
                if (x == 0.0) return
                for (bb in collisions) {
                    //#if MC>=11600
                    //$$ x = bb.getAllowedOffset(Direction.Axis.X, testBB, x)
                    //#else
                    x = bb.calculateXOffset(testBB, x)
                    //#endif
                }
                if (x == 0.0) return
                testBB = testBB.expand(x, 0.0, 0.0)
            }
            fun checkYAxis() {
                if (y == 0.0) return
                for (bb in collisions) {
                    //#if MC>=11600
                    //$$ y = bb.getAllowedOffset(Direction.Axis.Y, testBB, y)
                    //#else
                    y = bb.calculateYOffset(testBB, y)
                    //#endif
                }
                if (y == 0.0) return
                testBB = testBB.expand(0.0, y, 0.0)
            }
            fun checkZAxis() {
                if (z == 0.0) return
                for (bb in collisions) {
                    //#if MC>=11600
                    //$$ z = bb.getAllowedOffset(Direction.Axis.Z, testBB, z)
                    //#else
                    z = bb.calculateZOffset(testBB, z)
                    //#endif
                }
                if (z == 0.0) return
                testBB = testBB.expand(0.0, 0.0, z)
            }

            when(primaryAxis) {
                Axis.X -> {
                    checkXAxis()
                    checkYAxis()
                    checkZAxis()
                }
                Axis.Y -> {
                    checkYAxis()
                    checkXAxis()
                    checkZAxis()
                }
                Axis.Z -> {
                    checkZAxis()
                    checkYAxis()
                    checkXAxis()
                }
            }

            // did we actually even end up hitting anything?
            if (x == remainingOffset.x.toDouble() && y == remainingOffset.y.toDouble() && z == remainingOffset.z.toDouble()) {
                return null
            }

            // Compute how far we can go along the path before we exit the safe box
            infix fun Float.safeDiv(other: Float) = if (other == 0f) 1f else this / other
            val xFraction = x.toFloat() safeDiv remainingOffset.x
            val yFraction = y.toFloat() safeDiv remainingOffset.y
            val zFraction = z.toFloat() safeDiv remainingOffset.z
            val minFraction: Float
            val minAxis: Axis
            when {
                xFraction < yFraction && xFraction < zFraction -> {
                    minFraction = xFraction
                    minAxis = Axis.X
                }
                yFraction < zFraction -> {
                    minFraction = yFraction
                    minAxis = Axis.Y
                }
                else -> {
                    minFraction = zFraction
                    minAxis = Axis.Z
                }
            }

            // If we did not make any progress, we literally hit a wall
            // If we have more than 3 iterations, we figuratively hit a wall
            if (minFraction <= 0.001 || iteration > 3) {
                // In either case we'll stop here and return a wall on the axis where we made the least progress
                val axisVec = minAxis.vec
                val normal = if (minAxis.get(offset) > 0) axisVec.times(-1f) else axisVec
                return Pair(offset.minus(remainingOffset), normal)
            }

            // Otherwise, move our box to the new safe location and repeat
            // Though we don't want to move it exactly as far as computed because then we risk overshooting due to float
            // inaccuracies, which can cause things to fall through the block. So we move just ever so slightly less
            // than we're allowed to (notably a smaller fraction than the termination condition, so it's still enough
            // to terminate the loop next round if we hit a wall).
            val safeOffset = remainingOffset.times(minFraction - 0.0001f)
            remainingOffset = remainingOffset.minus(safeOffset)
            objectBB = objectBB.offset(safeOffset.x.toDouble(), safeOffset.y.toDouble(), safeOffset.z.toDouble())
            primaryAxis = minAxis
            iteration++
        }
    }

    private enum class Axis {
        X,
        Y,
        Z;

        val vec: Vec3
            get() =
                when (this) {
                    X -> vecUnitX()
                    Y -> vecUnitY()
                    Z -> vecUnitZ()
                }

        fun get(vec: Vec3): Float =
            get(vec.x, vec.y, vec.z)

        fun get(x: Float, y: Float, z: Float): Float =
            when (this) {
                X -> x
                Y -> y
                Z -> z
            }
    }
}
