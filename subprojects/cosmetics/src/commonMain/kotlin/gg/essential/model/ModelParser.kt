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

import gg.essential.mod.cosmetics.CapeModel
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.file.ModelFile
import gg.essential.network.cosmetics.Cosmetic
import kotlin.math.PI

class ModelParser(
    private val cosmetic: Cosmetic,
    private val textureWidth: Int,
    private val textureHeight: Int,
) {
    private val boneByName = mutableMapOf<String, Bone>()

    val boundingBoxes = mutableListOf<Pair<Box3, Side?>>()
    val rootBone = makeBone("_root")
    var textureFrameCount = 1
    var translucent = false

    private fun makeBone(name: String): Bone {
        val bone = Bone(name)
        bone.textureWidth = textureWidth
        bone.textureHeight = textureHeight

        boneByName[name] = bone

        return bone
    }

    fun parse(file: ModelFile) {
        val geometry = file.geometries.firstOrNull() ?: return

        textureFrameCount = (textureHeight / geometry.description.textureHeight).coerceAtLeast(1)
        translucent = geometry.description.textureTranslucent

        val extraInflate = when {
            cosmetic.type.id == "PLAYER" -> 0f
            else -> ((EXTRA_INFLATE_GROUPS.find { it.value.contains(cosmetic.type.slot) }?.index ?: 0) * 0.01f) + 0.01f
        }

        for (bone in geometry.bones) {
            if (bone.name.startsWith("bbox_")) {
                for (cube in bone.cubes) {
                    val origin = cube.origin
                    val size = cube.size
                    val box = Box3()
                    box.expandByPoint(origin.copy().negateY())
                    box.expandByPoint((origin + size).negateY())
                    box.expandByScalar(cube.inflate + 0.025f)
                    boundingBoxes.add(box to bone.side)
                }
                continue // only for data purposes, do not render
            }
            val boneModel = makeBone(bone.name)
            boneModel.pivotX = bone.pivot.x
            boneModel.pivotY = -bone.pivot.y
            boneModel.pivotZ = bone.pivot.z
            boneModel.rotateAngleX = bone.rotation.x.toRadians()
            boneModel.rotateAngleY = bone.rotation.y.toRadians()
            boneModel.rotateAngleZ = bone.rotation.z.toRadians()
            boneModel.mirror = bone.mirror
            boneModel.side = bone.side

            for (cube in bone.cubes) {
                val (x, y, z) = cube.origin.copy().negateY()
                val (dx, dy, dz) = cube.size
                val mirror = cube.mirror ?: bone.mirror

                val inflate = cube.inflate + extraInflate
                val cubeModel = when (val uv = cube.uv) {
                    is ModelFile.Uvs.PerFace -> {
                        val uvData = CubeUvData(
                            uv.north.toFloatArray(),
                            uv.east.toFloatArray(),
                            uv.south.toFloatArray(),
                            uv.west.toFloatArray(),
                            uv.up.toFloatArray(),
                            uv.down.toFloatArray()
                        )
                        Cube(boneModel, x, y - dy, z, dx, dy, dz, inflate, mirror, uvData)
                    }
                    is ModelFile.Uvs.Box -> {
                        val (u, v) = uv.uv
                        Cube(boneModel, u, v, x, y - dy, z, dx, dy, dz, inflate, mirror)
                    }
                }
                boneModel.cubeList.add(cubeModel)
            }

            // For capes, we render the actual cape separately (so conceptually, the model only includes *extra*
            // geometry). However, for backwards compatibility, we still include the cape cube in the cosmetic file, so
            // we need to remove it from the model.
            // (except for the internal CapeModel which we use in place of the vanilla cape renderer in certain cases)
            if (EnumPart.fromBoneName(bone.name) == EnumPart.CAPE && geometry.description.identifier != CapeModel.GEOMETRY_ID) {
                boneModel.cubeList.removeFirstOrNull()
            }

            (boneByName[bone.parent] ?: rootBone).addChild(boneModel)
        }

        fun Bone.setAffectsPoseParts() {
            childModels.forEach { it.setAffectsPoseParts() }

            val affectedParts = mutableSetOf<EnumPart>()
            EnumPart.fromBoneName(boxName)?.let { affectedParts.add(it) }
            childModels.forEach { affectedParts.addAll(it.affectsPoseParts) }

            affectsPose = affectedParts.isNotEmpty()
            affectsPoseParts = affectedParts
        }
        rootBone.setAffectsPoseParts()
    }

    private fun Float.toRadians() = (this / 180.0 * PI).toFloat()

    private fun ModelFile.UvFace?.toFloatArray(): FloatArray {
        this ?: return floatArrayOf(0f, 0f, 0f, 0f)
        val (u, v) = uv
        val (du, dv) = size
        return floatArrayOf(u, v, u + du, v + dv)
    }

    companion object {
        // Ascending priority -> higher inflate
        private val EXTRA_INFLATE_GROUPS = listOf(
            setOf(
                CosmeticSlot.PANTS,
            ),
            setOf(
                CosmeticSlot.TOP,
                CosmeticSlot.HEAD,
                CosmeticSlot.FACE,
                CosmeticSlot.BACK,
            ),
            setOf(
                CosmeticSlot.HAT,
                CosmeticSlot.FULL_BODY,
            ),
            setOf(
                CosmeticSlot.ACCESSORY,
                CosmeticSlot.ARMS,
                CosmeticSlot.SHOES,
            )
        ).withIndex()
    }
}
