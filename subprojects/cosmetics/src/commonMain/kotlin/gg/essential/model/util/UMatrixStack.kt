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
package gg.essential.model.util

import dev.folomeev.kotgl.matrix.matrices.Mat3
import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.matrices.identityMat3
import dev.folomeev.kotgl.matrix.matrices.identityMat4
import dev.folomeev.kotgl.matrix.matrices.mat3
import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat3
import dev.folomeev.kotgl.matrix.matrices.mutables.MutableMat4
import dev.folomeev.kotgl.matrix.matrices.mutables.timesSelf
import dev.folomeev.kotgl.matrix.matrices.mutables.toMutable
import dev.folomeev.kotgl.matrix.vectors.Vec3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class UMatrixStack(
    private val stack: MutableList<Entry>,
) {
    constructor(
        model: Mat4 = identityMat4(),
        normal: Mat3 = identityMat3(),
    ) : this(mutableListOf(Entry(model.toMutable(), normal.toMutable())))

    fun translate(x: Float, y: Float, z: Float) {
        if (x == 0f && y == 0f && z == 0f) return
        stack.last().run {
            // kotgl's builtin translate functions put the translation in the wrong place (last row
            // instead of column)
            model.timesSelf(
                identityMat4().toMutable().apply {
                    m03 = x
                    m13 = y
                    m23 = z
                }
            )
        }
    }

    fun translate(vec: Vec3) = translate(vec.x, vec.y, vec.z)

    fun scale(value: Float) {
        scale(value, value, value)
    }

    fun scale(x: Float, y: Float, z: Float) {
        if (x == 1f && y == 1f && z == 1f) return
        return stack.last().run {
            // kotgl's builtin scale functions also scale the translate values
            model.timesSelf(
                identityMat4().toMutable().apply {
                    m00 = x
                    m11 = y
                    m22 = z
                }
            )
            if (x == y && y == z) {
                if (x < 0f) {
                    normal.timesSelf(-1f)
                }
            } else {
                val ix = 1f / x
                val iy = 1f / y
                val iz = 1f / z
                val rt = cbrt(ix * iy * iz)
                normal.timesSelf(
                    identityMat3().toMutable().apply {
                        m00 = rt * ix
                        m11 = rt * iy
                        m22 = rt * iz
                    }
                )
            }
        }
    }

    fun rotate(angle: Float, x: Float, y: Float, z: Float, degrees: Boolean) {
        if (angle == 0f) return
        stack.last().run {
            val angleRadians = if (degrees) (angle / 180 * PI).toFloat() else angle
            val c = cos(angleRadians)
            val s = sin(angleRadians)
            val oneMinusC = 1 - c
            val xx = x * x
            val xy = x * y
            val xz = x * z
            val yy = y * y
            val yz = y * z
            val zz = z * z
            val xs = x * s
            val ys = y * s
            val zs = z * s
            val rotation = mat3(
                xx * oneMinusC + c,
                xy * oneMinusC - zs,
                xz * oneMinusC + ys,
                xy * oneMinusC + zs,
                yy * oneMinusC + c,
                yz * oneMinusC - xs,
                xz * oneMinusC - ys,
                yz * oneMinusC + xs,
                zz * oneMinusC + c,
            )
            model.timesSelf(rotation.toMat4())
            normal.timesSelf(rotation)
        }
    }

    fun rotate(q: Quaternion) {
        val n = 1 / sqrt(1 - q.w * q.w)
        rotate(2 * acos(q.w), q.x * n, q.y * n, q.z * n, degrees = false)
    }

    fun multiply(other: UMatrixStack) {
        val thisEntry = this.stack.last()
        val otherEntry = other.stack.last()
        thisEntry.model.timesSelf(otherEntry.model)
        thisEntry.normal.timesSelf(otherEntry.normal)
    }

    fun fork() = UMatrixStack(mutableListOf(stack.last().deepCopy()))

    fun push() {
        stack.add(stack.last().deepCopy())
    }

    fun pop() {
        stack.removeLast()
    }

    fun peek(): Entry = stack.last()

    data class Entry(val model: MutableMat4, val normal: MutableMat3) {
        fun deepCopy() = Entry(model.copyOf(), normal.copyOf())
    }
}
