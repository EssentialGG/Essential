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

import dev.folomeev.kotgl.matrix.matrices.mutables.inverse
import dev.folomeev.kotgl.matrix.matrices.mutables.times
import dev.folomeev.kotgl.matrix.matrices.mutables.timesSelf
import dev.folomeev.kotgl.matrix.vectors.vec4
import gg.essential.cosmetics.events.AnimationEvent
import gg.essential.cosmetics.skinmask.SkinMask
import gg.essential.model.EnumPart.Companion.fromBoneName
import gg.essential.model.backend.PlayerPose
import gg.essential.model.backend.RenderBackend
import gg.essential.model.file.AnimationFile
import gg.essential.model.file.ModelFile
import gg.essential.model.file.ParticlesFile
import gg.essential.model.file.SoundDefinitionsFile
import gg.essential.model.util.Quaternion
import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.getRotationEulerZYX
import gg.essential.model.util.times
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.network.cosmetics.Cosmetic.Diagnostic
import kotlin.jvm.JvmField

// TODO clean up
class BedrockModel(
    val cosmetic: Cosmetic,
    val variant: String,
    data: ModelFile?,
    val animationData: AnimationFile?,
    val particleData: Map<String, ParticlesFile>,
    val soundData: SoundDefinitionsFile?,
    var texture: RenderBackend.Texture?,
    val skinMasks: Map<Side?, SkinMask>,
) {
    val diagnostics: List<Diagnostic>

    @JvmField
    var boundingBoxes: List<Pair<Box3, Side?>>
    var rootBone: Bone
    var textureFrameCount = 1
    var translucent = false
    var animations: List<Animation>
    var animationEvents: List<AnimationEvent>

    // Stores all the different bone sides that are configured in this model
    val sideOptions: Set<Side>
    // Stores whether this model contains bones that hide on a specific side
    val isContainsSideOption: Boolean
        get() = sideOptions.isNotEmpty()

    init {
        val diagnostics = mutableListOf<Diagnostic>()

        val texture = texture
        if (data != null) {
            val parser = ModelParser(cosmetic, texture?.width ?: 64, texture?.height ?: 64)
            parser.parse(data)
            rootBone = parser.rootBone
            boundingBoxes = parser.boundingBoxes
            textureFrameCount = parser.textureFrameCount
            translucent = parser.translucent
        } else {
            rootBone = Bone("_root")
            boundingBoxes = emptyList()
        }

        sideOptions = getBones(rootBone).mapNotNull { it.side }.toSet()

        val particleEffects = mutableMapOf<String, ParticleEffect>()
        val soundEffects = mutableMapOf<String, SoundEffect>()

        if (data != null) {
            for ((path, file) in particleData) {
                val config = file.particleEffect
                val identifier = config.description.identifier

                val existingParticle = particleEffects[identifier]
                if (existingParticle != null) {
                    val msg = "Particle with id `$identifier` is already defined in `${existingParticle.file}`."
                    diagnostics.add(Diagnostic.error(msg, file = path))
                    continue
                }

                val material = config.description.basicRenderParameters.material
                particleEffects[identifier] =
                    ParticleEffect(
                        path,
                        identifier,
                        material,
                        config.components,
                        config.curves,
                        config.events,
                        texture,
                        particleEffects,
                        soundEffects,
                    )
            }
        }

        if (soundData != null) {
            for ((identifier, definition) in soundData.definitions) {
                val sounds = definition.sounds.mapNotNull { sound ->
                    val path = sound.name + ".ogg"
                    val asset = cosmetic.assets(variant).allFiles[path]
                        ?: return@mapNotNull null.also {
                            val msg = "File `$path` not found."
                            diagnostics.add(Diagnostic.error(msg, file = "sounds/sound_definitions.json"))
                        }
                    SoundEffect.Entry(
                        asset,
                        sound.stream,
                        sound.interruptible,
                        sound.volume,
                        sound.pitch,
                        sound.looping,
                        sound.directional,
                        sound.weight,
                    )
                }
                soundEffects[identifier] =
                    SoundEffect(
                        identifier,
                        definition.category,
                        definition.minDistance,
                        definition.maxDistance,
                        definition.fixedPosition,
                        sounds,
                    )
            }
        }

        if (animationData != null) {
            val referencedAnimations = animationData.triggers.flatMapTo(mutableSetOf()) { trigger ->
                generateSequence(trigger) { it.onComplete }.map { it.name }
            }

            animations = animationData.animations.map { Animation(it.key, it.value, getBones(rootBone), particleEffects, soundEffects) }
                .filter { animation ->
                    when {
                        animation.name !in referencedAnimations -> false
                        animation.animationLength <= 0f -> {
                            val msg = "Animation `${animation.name}` has zero or negative duration."
                            diagnostics.add(Diagnostic.error(msg, file = "animations.json"))
                            false
                        }
                        else -> true
                    }
                }
            animationEvents = animationData.triggers
        } else {
            animations = emptyList()
            animationEvents = emptyList()
        }

        this.diagnostics = diagnostics
    }

    fun getAnimationByName(name: String): Animation? {
        for (animation in animations) {
            if (animation.name == name) {
                return animation
            }
        }
        return null
    }

    fun computePose(basePose: PlayerPose, animationState: ModelAnimationState): PlayerPose {
        if (animationState.active.none { it.animation.affectsPose }) {
            return basePose
        }
        animationState.apply(rootBone, true)
        applyPose(rootBone, basePose)
        return retrievePose(rootBone, basePose)
    }

    fun applyPose(rootBone: Bone, pose: PlayerPose) {
        var anyGimbal = false
        for (bone in getBones(rootBone)) {
            if (bone.gimbal) {
                anyGimbal = true
            }
            val part = fromBoneName(bone.boxName) ?: continue
            copy(pose[part], bone, OFFSETS.getValue(part))
            if (pose.child) {
                if (part == EnumPart.HEAD) {
                    bone.childScale = 0.75f
                    bone.animOffsetY -= 8f
                } else {
                    bone.childScale = 0.5f
                }
            }
        }
        // TODO maybe optimize traversal, don't need to compute subtrees that do not even have any gimbal parts
        if (anyGimbal) {
            rootBone.propagateGimbal(Quaternion.Identity)
        }
    }

    fun retrievePose(rootBone: Bone, basePose: PlayerPose): PlayerPose {
        val parts = basePose.toMap(mutableMapOf())

        fun Bone.visit(matrixStack: UMatrixStack, parentHasScaling: Boolean) {
            val part = fromBoneName(boxName)

            if (part == null && childModels.isEmpty()) {
                return
            }

            val hasScaling = parentHasScaling || animScaleX != 1f || animScaleY != 1f || animScaleZ != 1f

            matrixStack.push()
            matrixStack.translate(pivotX + animOffsetX, pivotY - animOffsetY, pivotZ + animOffsetZ)
            if (gimbal) {
                matrixStack.rotate(parentRotation.conjugate())
            }
            matrixStack.rotate(rotateAngleZ + animRotZ, 0.0f, 0.0f, 1.0f, false)
            matrixStack.rotate(rotateAngleY + animRotY, 0.0f, 1.0f, 0.0f, false)
            matrixStack.rotate(rotateAngleX + animRotX, 1.0f, 0.0f, 0.0f, false)
            extra?.let {
                matrixStack.peek().model.timesSelf(it)
            }
            matrixStack.scale(animScaleX, animScaleY, animScaleZ)
            matrixStack.translate(-pivotX - userOffsetX, -pivotY - userOffsetY, -pivotZ - userOffsetZ)

            if (part != null) {
                val offset = OFFSETS.getValue(part)
                val matrix = matrixStack.peek().model

                // We can easily get the local pivot point by simply undoing the last `matrixStack.translate` call
                // (ignoring user offset for now because that is unused for emotes)
                val localPivot = vec4(pivotX, pivotY, pivotZ, 1f)
                // We can transform that into global space by simply passing it through the matrix
                val globalPivot = localPivot.times(matrix)

                // Local rotation is even simpler because there is no residual local rotation "pivot", so our global
                // rotation is simply the rotation of the matrix stack.
                val globalRotation = matrix.getRotationEulerZYX()

                // We only need to compute the scale/shear matrix if there was some scaling, otherwise we'll just end
                // up with an identity (within rounding errors) matrix and do a bunch of extra work (here and when
                // applying it to other cosmetics) which we don't really need to do.
                val extra = if (!hasScaling) {
                    null
                } else {
                    // To compute the scale/shear matrix, we need to convert the global pivot and rotation back into a
                    // matrix so we can then compute the difference between that and what we actually want to have
                    val resultStack = UMatrixStack()
                    resultStack.translate(globalPivot.x, globalPivot.y, globalPivot.z)
                    resultStack.rotate(globalRotation.z, 0.0f, 0.0f, 1.0f, false)
                    resultStack.rotate(globalRotation.y, 0.0f, 1.0f, 0.0f, false)
                    resultStack.rotate(globalRotation.x, 1.0f, 0.0f, 0.0f, false)
                    // To compute the difference, we also need to undo the final translate on the current stack because
                    // the extra matrix is applied before that, right after rotation (because the MC renderer doesn't do
                    // that final translate).
                    val expectedStack = matrixStack.fork()
                    expectedStack.translate(pivotX, pivotY, pivotZ)
                    // The final transform for a given bone in other cosmetics will end up being
                    //   M = R * X
                    // where
                    //   M is the final transform, this should end up matching the `expectedStack` computed above
                    //   R is the combination of translation and rotation, this will match `resultStack` computed above
                    //   X is a remainder of scale/shear transformations, this is what we need to compute and store
                    // To do so, we simply multiply by the inverse of R (denoted by R') from the left on both sides.
                    // The Rs on the right side will cancel out and we're left with:
                    //   R' * M = X
                    // or
                    //   X = R' * M
                    // which we can easily compute as follows:
                    resultStack.peek().model.inverse().times(expectedStack.peek().model)
                }

                parts[part] = PlayerPose.Part(
                    // As per the matrix stack transformations above, if we assume that this point doesn't have any
                    // parent (as would be the case for regular cosmetics), the global pivot point can be computed as:
                    //   (a) globalPivot = bone.pivot + bone.animOffset
                    // As per above [copy] method, the right side variables are set as:
                    //   (b) bone.pivot = offset.pivot
                    //   (c) bone.animOffset = pose.pivot + offset.offset
                    // We're trying to compute the `pose.pivot` value we need to store to replicate in other cosmetics
                    // the `globalPivot` value observed above.
                    // We can first rearrange the above equations as:
                    //   (a) bone.animOffset = globalPivot - offset.pivot
                    //   (b) bone.offset = bone.pivot
                    //   (c) pose.pivot = bone.animOffset - bone.offset
                    // Substituting (a) and (b) into (c) gives us the result we're looking for:
                    //   pose.pivot = globalPivot - offset.pivot - offset.offset
                    // A few of the signs get flipped for Y, the details of that are left as an exercise to the reader.
                    pivotX = (globalPivot.x - offset.pivotX - offset.offsetX),
                    pivotY = (globalPivot.y - offset.pivotY + offset.offsetY),
                    pivotZ = (globalPivot.z - offset.pivotZ - offset.offsetZ),
                    // Global rotation, again, is easier because we don't have to deal with any offsets
                    rotateAngleX = globalRotation.x,
                    rotateAngleY = globalRotation.y,
                    rotateAngleZ = globalRotation.z,
                    extra = extra,
                )
            }

            for (childModel in childModels) {
                childModel.visit(matrixStack, hasScaling)
            }

            matrixStack.pop()
        }

        rootBone.visit(UMatrixStack(), false)

        return PlayerPose.fromMap(parts, basePose.child)
    }

    /**
     * Renders the model
     *
     * Note: [Bone.resetAnimationOffsets] or equivalent must be called before calling this method.
     *
     * @param matrixStack
     */
    fun render(
        matrixStack: UMatrixStack,
        vertexConsumerProvider: RenderBackend.VertexConsumerProvider,
        rootBone: Bone,
        metadata: RenderMetadata,
        lifetime: Float,
    ) {
        val textureLocation = texture ?: metadata.skin

        val totalFrames = textureFrameCount.toFloat()
        val frame = (lifetime * TEXTURE_ANIMATION_FPS).toInt()
        val offset = frame % totalFrames / totalFrames

        val pose = metadata.pose
        if (pose != null) {
            applyPose(rootBone, pose)
        }

        propagateVisibilityToRootBone(metadata.side,
            rootBone,
            metadata.hiddenBones,
            metadata.parts,
        )

        vertexConsumerProvider.provide(textureLocation) { vertexConsumer ->
            rootBone.render(matrixStack, vertexConsumer, metadata.light, metadata.scale, offset)
        }
    }

    fun getBones(bone: Bone): List<Bone> {
        val bones = mutableListOf(bone)
        for (childModel in bone.childModels) {
            bones.addAll(getBones(childModel))
        }
        return bones
    }

    /**
     * Propagates visibility to the root bone with the given [side] or default side if null and required
     */
    fun propagateVisibilityToRootBone(
        side: Side?,
        rootBone: Bone,
        hiddenBones: Set<String>,
        parts: Set<EnumPart>?,
    ) {
        // If this cosmetic has bones with the side option, we want to force our default side
        // otherwise both sides will show
        val updatedSide = side ?: cosmetic.defaultSide ?: Side.getDefaultSideOrNull(sideOptions)

        for (bone in getBones(rootBone)) {
            val part = fromBoneName(bone.boxName)
            if (part == null) {
                bone.visible = if (bone.boxName in hiddenBones) false else null
                continue
            }
            bone.visible = (parts == null || part in parts) && bone.boxName !in hiddenBones
        }
        rootBone.propagateVisibility(true, updatedSide)
    }

    private fun copy(pose: PlayerPose.Part, bone: Bone, offset: Offset) {
        bone.rotateAngleX = pose.rotateAngleX
        bone.rotateAngleY = pose.rotateAngleY
        bone.rotateAngleZ = pose.rotateAngleZ
        bone.pivotX = offset.pivotX
        bone.pivotY = offset.pivotY
        bone.pivotZ = offset.pivotZ
        bone.animOffsetX += pose.pivotX + offset.offsetX
        bone.animOffsetY += -pose.pivotY + offset.offsetY
        bone.animOffsetZ += pose.pivotZ + offset.offsetZ
        bone.extra = pose.extra
        bone.isHidden = false
        bone.childScale = 1f
    }

    private class Offset(
        val pivotX: Float,
        val pivotY: Float,
        val pivotZ: Float,
        val offsetX: Float,
        val offsetY: Float,
        val offsetZ: Float
    )

    companion object {
        private val BASE = Offset(0f, -24f, 0f, 0f, 0f, 0f)
        private val RIGHT_ARM = Offset(-5f, -22f, 0f, 5f, 2f, 0f)
        private val LEFT_ARM = Offset(5f, -22f, 0f, -5f, 2f, 0f)
        private val LEFT_LEG = Offset(1.9f, -12f, 0f, -1.9f, 12f, 0f)
        private val RIGHT_LEG = Offset(-1.9f, -12f, 0f, 1.9f, 12f, 0f)
        private val CAPE = Offset(0f, -24f, 2f, 0f, 0f, -2f)
        private val OFFSETS =
            mapOf(
                EnumPart.HEAD to BASE,
                EnumPart.BODY to BASE,
                EnumPart.LEFT_ARM to LEFT_ARM,
                EnumPart.RIGHT_ARM to RIGHT_ARM,
                EnumPart.LEFT_LEG to LEFT_LEG,
                EnumPart.RIGHT_LEG to RIGHT_LEG,
                EnumPart.LEFT_SHOULDER_ENTITY to BASE,
                EnumPart.RIGHT_SHOULDER_ENTITY to BASE,
                EnumPart.LEFT_WING to Offset(5f, -24f, 2f, -5f, 0f, -2f),
                EnumPart.RIGHT_WING to Offset(-5f, -24f, 2f, 5f, 0f, -2f),
                EnumPart.CAPE to CAPE,
            )
        const val TEXTURE_ANIMATION_FPS = 7f
    }
}
