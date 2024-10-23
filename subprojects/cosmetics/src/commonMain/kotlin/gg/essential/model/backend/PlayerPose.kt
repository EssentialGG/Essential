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
package gg.essential.model.backend

import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vecUnitX
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecUnitZ
import gg.essential.model.EnumPart
import gg.essential.model.util.Quaternion
import kotlin.math.PI

data class PlayerPose(
    val head: Part,
    val body: Part,
    val rightArm: Part,
    val leftArm: Part,
    val rightLeg: Part,
    val leftLeg: Part,
    val rightShoulderEntity: Part,
    val leftShoulderEntity: Part,
    val rightWing: Part,
    val leftWing: Part,
    val cape: Part,
    val child: Boolean,
) : Map<EnumPart, PlayerPose.Part> {

    override val entries: Set<Map.Entry<EnumPart, Part>>
        get() = keys.mapTo(mutableSetOf()) { MapEntry(it, get(it)) }
    override val keys: Set<EnumPart>
        get() = EnumPart.values().toSet()
    override val size: Int
        get() = EnumPart.values().size
    override val values: Collection<Part>
        get() = entries.map { it.value }

    override fun containsKey(key: EnumPart): Boolean = true

    override fun containsValue(value: Part): Boolean = value in values

    override fun get(key: EnumPart): Part = when(key) {
        EnumPart.HEAD -> head
        EnumPart.BODY -> body
        EnumPart.RIGHT_ARM -> rightArm
        EnumPart.LEFT_ARM -> leftArm
        EnumPart.RIGHT_LEG -> rightLeg
        EnumPart.LEFT_LEG -> leftLeg
        EnumPart.RIGHT_SHOULDER_ENTITY -> rightShoulderEntity
        EnumPart.LEFT_SHOULDER_ENTITY -> leftShoulderEntity
        EnumPart.RIGHT_WING -> rightWing
        EnumPart.LEFT_WING -> leftWing
        EnumPart.CAPE -> cape
    }

    override fun isEmpty(): Boolean = false

    data class Part(
        val pivotX: Float = 0f,
        val pivotY: Float = 0f,
        val pivotZ: Float = 0f,
        val rotateAngleX: Float = 0f,
        val rotateAngleY: Float = 0f,
        val rotateAngleZ: Float = 0f,
        val extra: Mat4? = null,
    ) {
        fun offset(pivotOffset: Vec3) = copy(
            pivotX = pivotX + pivotOffset.x,
            pivotY = pivotY + pivotOffset.y,
            pivotZ = pivotZ + pivotOffset.z,
        )

        val pivot: Vec3
            get() = vec3(pivotX, pivotY, pivotZ)

        val rotation: Quaternion
            get() = Quaternion.fromAxisAngle(vecUnitZ(), rotateAngleZ) *
                    Quaternion.fromAxisAngle(vecUnitY(), rotateAngleY) *
                    Quaternion.fromAxisAngle(vecUnitX(), rotateAngleX)

        companion object {
            // Parts that weren't rendered, we'll just draw far away so they'll appear is if they weren't there
            val MISSING = Part(pivotY = -10000f)
        }
    }

    private data class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

    companion object {
        fun fromMap(map: Map<EnumPart, Part>, child: Boolean) =
            PlayerPose(
                head = map.getValue(EnumPart.HEAD),
                body = map.getValue(EnumPart.BODY),
                rightArm = map.getValue(EnumPart.RIGHT_ARM),
                leftArm = map.getValue(EnumPart.LEFT_ARM),
                rightLeg = map.getValue(EnumPart.RIGHT_LEG),
                leftLeg = map.getValue(EnumPart.LEFT_LEG),
                rightShoulderEntity = map.getValue(EnumPart.RIGHT_SHOULDER_ENTITY),
                leftShoulderEntity = map.getValue(EnumPart.LEFT_SHOULDER_ENTITY),
                rightWing = map.getValue(EnumPart.RIGHT_WING),
                leftWing = map.getValue(EnumPart.LEFT_WING),
                cape = map.getValue(EnumPart.CAPE),
                child = child,
            )

        fun neutral() = PlayerPose(
            head = Part(),
            body = Part(),
            rightArm = Part(-5f, 2f, 0f),
            leftArm = Part(5f, 2f, 0f),
            rightLeg = Part(-1.9f, 12f, 0.1f),
            leftLeg = Part(1.9f, 12f, 0.1f),
            rightShoulderEntity = Part(),
            leftShoulderEntity = Part(),
            // The pivotZ is done separately by MC in LayerCape
            rightWing = Part(-5f, 0f, 2f, 15f.degrees, 0f, 15f.degrees),
            leftWing = Part(5f, 0f, 2f, 15f.degrees, 0f, (-15f).degrees),
            // Values determined experimentally because MC doesn't use the regular ModelPart variables for the cape
            cape = Part(0f, 0f, 2f, PI.toFloat() - 0.1f, 0f, -PI.toFloat()),
            child = false,
        )

        private val Float.degrees
            get() = this / 180f * PI.toFloat()
    }
}