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

import dev.folomeev.kotgl.matrix.matrices.Mat4
import dev.folomeev.kotgl.matrix.matrices.mutables.timesSelf
import dev.folomeev.kotgl.matrix.matrices.mutables.toMutable
import dev.folomeev.kotgl.matrix.vectors.vecUnitX
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import dev.folomeev.kotgl.matrix.vectors.vecUnitZ
import gg.essential.model.util.Quaternion
import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.UVertexConsumer
import kotlin.jvm.JvmField

// TODO clean up
class Bone(
    @JvmField
    val boxName: String
) {
    var textureWidth = 64
    var textureHeight = 32
    @JvmField
    var pivotX = 0f
    @JvmField
    var pivotY = 0f
    @JvmField
    var pivotZ = 0f
    @JvmField
    var rotateAngleX = 0f
    @JvmField
    var rotateAngleY = 0f
    @JvmField
    var rotateAngleZ = 0f
    var extra: Mat4? = null
    var mirror = false
    var showModel = true
    @JvmField
    var isHidden = false
    @JvmField
    var cubeList = mutableListOf<Cube>()
    @JvmField
    var childModels = mutableListOf<Bone>()
    @JvmField
    var animOffsetX = 0f
    @JvmField
    var animOffsetY = 0f
    @JvmField
    var animOffsetZ = 0f
    var animRotX = 0f
    var animRotY = 0f
    var animRotZ = 0f
    var animScaleX = 0f
    var animScaleY = 0f
    var animScaleZ = 0f
    @JvmField
    var userOffsetX = 0f
    @JvmField
    var userOffsetY = 0f
    @JvmField
    var userOffsetZ = 0f
    @JvmField
    var childScale = 1f
    var side: Side? = null
    @JvmField
    var visible: Boolean? = null // determines visibility for all bones in this tree unless overwritten in a child
    private var isVisible = true // actual visibility for this specific bone, set in propagateVisibility
    private var fullyInvisible = false // propagateVisibility has determined that we can skip this entire tree

    /** Whether an animation targeting this bone will have an effect on the player pose. */
    var affectsPose = false // initialized by ModelParser
    /** Which parts of the player pose will be affected by an animation targeting this bone. */
    var affectsPoseParts = emptySet<EnumPart>() // initialized by ModelParser

    /** Whether this bone should undo all parent rotation, as if it was stabilized by three gimbals. */
    var gimbal = false
    /** Quaternion representing the rotation of all parents to be undone if [gimbal] is `true` */
    var parentRotation: Quaternion = Quaternion.Identity
    /** Whether this bone should also undo the rotation of the entity in addition to [gimbal]. */
    var worldGimbal = false

    init {
        resetAnimationOffsets(false)
    }

    fun addChild(child: Bone) {
        childModels.add(child)
    }

    fun propagateVisibility(parentVisible: Boolean, side: Side?) {
        if (this.side != null && side != null && this.side !== side) {
            isVisible = false
            fullyInvisible = true
            return
        }
        val isVisible = if (visible == null) parentVisible else visible!!
        var fullyInvisible = !isVisible
        for (child in childModels) {
            child.propagateVisibility(isVisible, side)
            fullyInvisible = fullyInvisible and child.fullyInvisible
        }
        this.isVisible = isVisible
        this.fullyInvisible = fullyInvisible
    }

    fun resetAnimationOffsets(recursive: Boolean) {
        animOffsetZ = 0f
        animOffsetY = animOffsetZ
        animOffsetX = animOffsetY
        animRotZ = 0f
        animRotY = animRotZ
        animRotX = animRotY
        animScaleZ = 1f
        animScaleY = animScaleZ
        animScaleX = animScaleY
        gimbal = false
        if (recursive) {
            for (childModel in childModels) {
                childModel.resetAnimationOffsets(true)
            }
        }
    }

    fun render(
        matrixStack: UMatrixStack,
        renderer: UVertexConsumer,
        light: Int,
        scale: Float,
        verticalUVOffset: Float
    ) {
        if (!isHidden && showModel && !fullyInvisible) {
            matrixStack.push()
            matrixStack.scale(childScale, childScale, childScale)
            val translateX = pivotX * scale + animOffsetX * scale
            val translateY = pivotY * scale - animOffsetY * scale
            val translateZ = pivotZ * scale + animOffsetZ * scale
            matrixStack.translate(translateX, translateY, translateZ)
            if (gimbal) {
                matrixStack.rotate(parentRotation.conjugate())
            }
            matrixStack.rotate(rotateAngleZ + animRotZ, 0.0f, 0.0f, 1.0f, false)
            matrixStack.rotate(rotateAngleY + animRotY, 0.0f, 1.0f, 0.0f, false)
            matrixStack.rotate(rotateAngleX + animRotX, 1.0f, 0.0f, 0.0f, false)
            extra?.let {
                matrixStack.peek().model.timesSelf(it.toMutable().apply {
                    m03 *= scale
                    m13 *= scale
                    m23 *= scale
                })
            }
            matrixStack.scale(animScaleX, animScaleY, animScaleZ)
            matrixStack.translate(
                -pivotX * scale - userOffsetX * scale,
                -pivotY * scale - userOffsetY * scale,
                -pivotZ * scale - userOffsetZ * scale
            )
            if (isVisible) {
                for (cube in cubeList) {
                    cube.render(matrixStack, renderer, light, scale, verticalUVOffset)
                }
            }
            for (childModel in childModels) {
                childModel.render(matrixStack, renderer, light, scale, verticalUVOffset)
            }
            matrixStack.pop()
        }
    }

    fun setTextureSize(p_setTextureSize_1_: Int, p_setTextureSize_2_: Int) {
        textureWidth = p_setTextureSize_1_
        textureHeight = p_setTextureSize_2_
    }

    fun deepCopy(): Bone {
        val bone = Bone(boxName)
        bone.textureWidth = textureWidth
        bone.textureHeight = textureHeight
        bone.pivotX = pivotX
        bone.pivotY = pivotY
        bone.pivotZ = pivotZ
        bone.rotateAngleX = rotateAngleX
        bone.rotateAngleY = rotateAngleY
        bone.rotateAngleZ = rotateAngleZ
        bone.cubeList = ArrayList()
        for (cube in cubeList) {
            bone.cubeList.add(Cube(cube.getQuadList().map { face ->
                Face(face.vertexPositions.map { it.copy() }.toTypedArray())
            }, cube.mirror))
        }
        for (childModel in childModels) {
            bone.addChild(childModel.deepCopy())
        }
        bone.affectsPose = affectsPose
        bone.affectsPoseParts = affectsPoseParts
        bone.side = side
        return bone
    }

    /**
     * Returns true if this bone or any of its children contain visible boxes
     */
    fun containsVisibleBoxes(): Boolean {
        return !fullyInvisible && ((this.cubeList.isNotEmpty() && isVisible) || this.childModels.any { it.containsVisibleBoxes() })
    }

    fun propagateGimbal(parentRotation: Quaternion, entityRotation: Quaternion) {
        if (gimbal) {
            // If this is a gimbal, ignore parent and pose rotation, only keep animation rotation
            this.rotateAngleX = 0f
            this.rotateAngleY = 0f
            this.rotateAngleZ = 0f
            this.parentRotation = if (worldGimbal) entityRotation * parentRotation else parentRotation
        }

        var ownRotation = if (gimbal) Quaternion.Identity else parentRotation
        ownRotation *= Quaternion.fromAxisAngle(vecUnitZ(), rotateAngleZ + animRotZ)
        ownRotation *= Quaternion.fromAxisAngle(vecUnitY(), rotateAngleY + animRotY)
        ownRotation *= Quaternion.fromAxisAngle(vecUnitX(), rotateAngleX + animRotX)
        for (child in childModels) {
            child.propagateGimbal(ownRotation, if (worldGimbal) Quaternion.Identity else entityRotation)
        }
    }
}
