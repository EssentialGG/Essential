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

import kotlin.jvm.JvmOverloads
import kotlin.math.max
import kotlin.math.min

//Taken from https://github.com/markaren/three.kt
data class Box3 @JvmOverloads constructor(
    var min: Vector3 = Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    var max: Vector3 = Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
) {

    private val intersectsTriangleHelper by lazy { IntersectsTriangleHelper() }

    fun set(min: Vector3, max: Vector3): Box3 {
        this.min.copy(min)
        this.max.copy(max)

        return this
    }

    fun setFromPoints(points: List<Vector3>): Box3 {
        this.makeEmpty()

        points.forEach {
            this.expandByPoint(it)
        }

        return this
    }

    fun makeEmpty(): Box3 {
        this.min.x = Float.POSITIVE_INFINITY
        this.min.y = Float.POSITIVE_INFINITY
        this.min.z = Float.POSITIVE_INFINITY

        this.max.x = Float.NEGATIVE_INFINITY
        this.max.y = Float.NEGATIVE_INFINITY
        this.max.z = Float.NEGATIVE_INFINITY

        return this
    }

    fun isEmpty(): Boolean {
        // this is a more robust check for empty than ( volume <= 0 ) because volume can get positive with two negative axes
        return (this.max.x < this.min.x) || (this.max.y < this.min.y) || (this.max.z < this.min.z)
    }

    @JvmOverloads
    fun getCenter(target: Vector3 = Vector3()): Vector3 {
        return if (this.isEmpty()) {
            target.set(0f, 0f, 0f)
        } else {
            target.addVectors(this.min, this.max).multiplyScalar(0.5f)
        }
    }

    @JvmOverloads
    fun getSize(target: Vector3 = Vector3()): Vector3 {
        return if (this.isEmpty()) {
            target.set(0f, 0f, 0f)
        } else {
            target.subVectors(this.max, this.min)
        }
    }

    fun expandByPoint(point: Vector3): Box3 {
        this.min.min(point)
        this.max.max(point)

        return this
    }

    fun expandByScalar(scalar: Float): Box3 {
        this.min.addScalar(-scalar)
        this.max.addScalar(scalar)

        return this
    }

    @JvmOverloads
    fun getParameter(point: Vector3, target: Vector3 = Vector3()): Vector3 {
        return target.set(
            (point.x - this.min.x) / (this.max.x - this.min.x),
            (point.y - this.min.y) / (this.max.y - this.min.y),
            (point.z - this.min.z) / (this.max.z - this.min.z)
        )
    }



    fun intersect(box: Box3): Box3 {
        this.min.max(box.min)
        this.max.min(box.max)

        // ensure that if there is no overlap, the result is fully empty, not slightly empty with non-inf/+inf values that will cause subsequence intersects to erroneously return valid values.
        if (this.isEmpty()) {
            this.makeEmpty()
        }

        return this
    }


    fun translate(offset: Vector3): Box3 {
        this.min.add(offset)
        this.max.add(offset)

        return this
    }



    fun clone(): Box3 {
        return Box3().copy(this)
    }

    fun copy(box: Box3): Box3 {
        this.min.copy(box.min)
        this.max.copy(box.max)

        return this
    }

    companion object {

        private val points by lazy {
            List(8) { Vector3() }
        }

    }

    private inner class IntersectsTriangleHelper {

        // triangle centered vertices
        var v0 = Vector3()
        var v1 = Vector3()
        var v2 = Vector3()

        // triangle edge vectors
        var f0 = Vector3()
        var f1 = Vector3()
        var f2 = Vector3()

        var testAxis = Vector3()

        var center = Vector3()
        var extents = Vector3()

        var triangleNormal = Vector3()

    }

}
