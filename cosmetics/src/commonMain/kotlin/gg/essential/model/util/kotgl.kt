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
import dev.folomeev.kotgl.matrix.matrices.mat3
import dev.folomeev.kotgl.matrix.matrices.mat4
import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.Vec4
import dev.folomeev.kotgl.matrix.vectors.mutables.MutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.MutableVec4
import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.set
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vec4
import kotlin.math.asin
import kotlin.math.atan2

inline fun <T> Vec3.times(mat: Mat3, out: (Float, Float, Float) -> T) =
    out(
        x * mat.m00 + y * mat.m01 + z * mat.m02,
        x * mat.m10 + y * mat.m11 + z * mat.m12,
        x * mat.m20 + y * mat.m21 + z * mat.m22,
    )

inline fun <T> Vec4.times(mat: Mat4, out: (Float, Float, Float, Float) -> T) =
    out(
        x * mat.m00 + y * mat.m01 + z * mat.m02 + w * mat.m03,
        x * mat.m10 + y * mat.m11 + z * mat.m12 + w * mat.m13,
        x * mat.m20 + y * mat.m21 + z * mat.m22 + w * mat.m23,
        x * mat.m30 + y * mat.m31 + z * mat.m32 + w * mat.m33,
    )

/**
 * Computes the matrix-vector product `mat * this`.
 */
fun Vec3.times(mat: Mat3) = times(mat, ::vec3)

/**
 * Computes the matrix-vector product `mat * this`.
 */
fun Vec4.times(mat: Mat4) = times(mat, ::vec4)

/**
 * Computes the matrix-vector product `this * vec` where `vec` is assumed to be a position (i.e. its w will be 1).
 */
fun Mat4.transformPosition(vec: Vec3): Vec3 = vec4(vec, 1f).times(this) { x, y, z, _ -> vec3(x, y, z) }

/**
 * Computes the matrix-vector product `mat * this`, storing the result in `this`.
 */
fun MutableVec3.timesSelf(mat: Mat3) = times(mat, ::set)

/**
 * Computes the matrix-vector product `mat * this`, storing the result in `this`.
 */
fun MutableVec4.timesSelf(mat: Mat4) = times(mat, ::set)

inline fun <T> Vec3.rotateBy(q: Quaternion, out: (Float, Float, Float) -> T): T =
    with(q * Quaternion(x, y, z, 0f) * q.conjugate()) { out(x, y, z) }

/**
 * Applies the given rotation represented as a [Quaternion] to this vector, returning the resulting vector.
 */
fun Vec3.rotateBy(q: Quaternion) = rotateBy(q, ::mutableVec3)

/**
 * Applies the given rotation represented as a [Quaternion] to this vector, storing the result in `this`.
 */
fun MutableVec3.rotateSelfBy(q: Quaternion) = rotateBy(q, ::set)

fun Mat4.getRotationEulerZYX() =
    vec3(
        //
        //   Z * Y * X
        //
        //   cz -sz  0       cy  0   sy       1   0   0
        //   sz  cz  0   *   0   1   0    *   0   cx -sx
        //   0   0   1      -sy  0   cy       0   sx  cx
        //
        //   czcy -sz  cysy       1   0   0
        //   szcy  cz  szsy   *   0   cx -sx
        //   -sy   0   cy         0   sx  cx
        //
        //   czcy   -cxsz+sxcysy   sxsz+cxcysy
        //   szcy    czcx+sxszsy   -sxcz+cxszsy
        //   -sy     sxcy          cxcy
        //
        x = atan2(m21, m22),
        y = asin(-m20.coerceIn(-1f, 1f)),
        z = atan2(m10, m00),
    )

fun Mat4.toMat3() = mat3(m00, m01, m02, m10, m11, m12, m20, m21, m22)
fun Mat3.toMat4() = mat4(m00, m01, m02, 0f, m10, m11, m12, 0f, m20, m21, m22, 0f, 0f, 0f, 0f, 1f)

fun FloatArray.toMat4() = mat4 { row, col -> this[row * 4 + col] }

fun Mat4.toFloatArray() =
    toRowMajor()

fun Mat4.toRowMajor() =
    floatArrayOf(
        m00,
        m01,
        m02,
        m03,
        m10,
        m11,
        m12,
        m13,
        m20,
        m21,
        m22,
        m23,
        m30,
        m31,
        m32,
        m33,
    )

