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

import dev.folomeev.kotgl.matrix.matrices.mutables.timesSelf
import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.mutables.minus
import dev.folomeev.kotgl.matrix.vectors.mutables.timesSelf
import dev.folomeev.kotgl.matrix.vectors.vec3
import dev.folomeev.kotgl.matrix.vectors.vec4
import dev.folomeev.kotgl.matrix.vectors.vecZero
import gg.essential.model.file.AnimationFile
import gg.essential.model.file.KeyframeSerializer
import gg.essential.model.file.KeyframesSerializer
import gg.essential.model.molang.*
import gg.essential.model.util.Quaternion
import gg.essential.model.util.TreeMap
import gg.essential.model.util.UMatrixStack
import gg.essential.model.util.times
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI

class ModelAnimationState(
    val entity: MolangQueryEntity,
    val parentLocator: ParticleSystem.Locator,
) {
    val active: MutableList<AnimationState> = mutableListOf()
    val pendingEvents: MutableList<Event> = mutableListOf()

    private val locators = mutableMapOf<String, BoneLocator>()
    private var lastLocatorUpdateTime = 0f // velocity will not be computed on the first update anyway

    fun startAnimation(animation: Animation) {
        if (active.any { it.animation == animation }) {
            return
        }
        active.add(AnimationState(animation))

        animation.effects.values.forEach { list ->
            list.forEach { event ->
                val name = (event as? Animation.LocatableEvent)?.locator?.boxName
                if (name != null && name !in locators) {
                    locators[name] = BoneLocator(vec3(), Quaternion.Identity, vec3())
                }
            }
        }
    }

    private fun findAndResetBones(bone: Bone, map: MutableMap<String, Bone>) {
        map[bone.boxName] = bone
        bone.resetAnimationOffsets(false)
        if (bone.childModels != null) {
            for (childModel in bone.childModels) {
                findAndResetBones(childModel, map)
            }
        }
    }

    fun apply(model: Bone, affectPose: Boolean) {
        val bones = mutableMapOf<String, Bone>()
        findAndResetBones(model, bones)


        for (state in active) {
            for ((boneName, channels) in state.animation.bones) {
                val bone = bones[boneName] ?: continue
                if (bone.affectsPose != affectPose) continue
                channels.relativeTo.rotation?.let { relativeTo ->
                    bone.gimbal = true
                    bone.worldGimbal = relativeTo == "world"
                }
                channels.position?.eval(state.context)?.let { (x, y, z) ->
                    bone.animOffsetX += x
                    bone.animOffsetY += y
                    bone.animOffsetZ += z
                }
                channels.rotation?.eval(state.context)?.let { (x, y, z) ->
                    bone.animRotX = (x / 180 * PI).toFloat()
                    bone.animRotY = (y / 180 * PI).toFloat()
                    bone.animRotZ = (z / 180 * PI).toFloat()
                }
                channels.scale?.eval(state.context)?.let { (x, y, z) ->
                    bone.animScaleX *= x
                    bone.animScaleY *= y
                    bone.animScaleZ *= z
                }
            }
        }
    }

    /** Whether there are any locators that still need to be updated via [updateLocators] for this frame. */
    fun locatorsNeedUpdating() =
        locators.isNotEmpty() && lastLocatorUpdateTime < entity.lifeTime

    /** Updates the position/rotation/velocity of all locators based on the current transform of bones in [rootBone]. */
    fun updateLocators(rootBone: Bone, scale: Float) {
        if (locators.isEmpty()) {
            return // nothing to do
        }

        val now = entity.lifeTime
        val dt = now - lastLocatorUpdateTime
        if (dt <= 0) {
            return
        }
        lastLocatorUpdateTime = now

        // TODO maybe optimize traversal, don't need to compute subtree with only dead ends (same for retrievePose)
        fun Bone.visit(matrixStack: UMatrixStack, parentHasScaling: Boolean) {
            val locator = locators[boxName]

            if (locator == null && childModels.isEmpty()) {
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

            if (locator != null) {
                val matrix = matrixStack.peek().model
                val lastPosition = locator.position
                val nextPosition = with(vec4(0f, 0f, 0f, 1f).times(matrix)) { vec3(x, y, z) }
                locator.position = nextPosition

                // LookAt is towards -1 because as per OpenGL convention the camera is looking towards negative Z.
                val lookAt = with(vec4(0f, 0f, -1f, 1f).times(matrix)) { vec3(x, y, z) }.minus(nextPosition)
                // Up is towards -1 because Mojang renders models upside down, and our cosmetics have been built around that
                val up = with(vec4(0f, -1f, 0f, 1f).times(matrix)) { vec3(x, y, z) }.minus(nextPosition)
                locator.rotation = Quaternion.fromLookAt(lookAt, up)

                // Only update if we have a valid previous value (we cannot compute velocity from just the first frame)
                // and if some time has passed since the last update (otherwise velocity is ill-defined)
                if (lastPosition != vecZero() && dt > 0f) {
                    locator.velocity = nextPosition.minus(lastPosition).timesSelf(1 / dt)
                }
            }

            extra?.let {
                matrixStack.peek().model.timesSelf(it)
            }
            matrixStack.scale(animScaleX, animScaleY, animScaleZ)
            matrixStack.translate(-pivotX - userOffsetX, -pivotY - userOffsetY, -pivotZ - userOffsetZ)

            for (childModel in childModels) {
                childModel.visit(matrixStack, hasScaling)
            }

            matrixStack.pop()
        }

        val matrixStack = UMatrixStack()
        val (position, rotation) = parentLocator.positionAndRotation
        matrixStack.translate(position)
        matrixStack.rotate(rotation)
        matrixStack.scale(scale)
        matrixStack.scale(-1f, -1f, 1f) // see RenderLivingBase.prepareScale
        matrixStack.scale(0.9375f) // see RenderPlayer.preRenderCallback
        rootBone.visit(matrixStack, false)
    }

    /** Emits effect keyframes into [pendingEvents]. */
    fun updateEffects(untilLifeTime: Float = entity.lifeTime) {
        for (state in active) {
            if (state.animation.effects.isEmpty()) {
                continue
            }
            while (true) {
                val (nextTime, effects) = state.animation.effects.higherEntry(state.lastEffectTime)
                    ?: if (state.animation.loop == AnimationFile.Loop.True) {
                        state.effectLoops++
                        state.lastEffectTime = Float.NEGATIVE_INFINITY
                        continue
                    } else {
                        break
                    }
                val nextLifeTime = state.animStartTime + state.effectLoopsDuration + nextTime
                if (nextLifeTime > untilLifeTime) {
                    break
                }
                state.lastEffectTime = nextTime
                effects.forEach { event ->
                    pendingEvents.add(when (event) {
                        is Animation.ParticleEvent -> ParticleEvent(
                            entity,
                            nextLifeTime,
                            entity,
                            event.effect,
                            event.locator?.boxName?.let { locators[it] } ?: parentLocator,
                            event.preEffectScript,
                        )
                        is Animation.SoundEvent -> SoundEvent(
                            entity,
                            nextLifeTime,
                            entity,
                            event.effect,
                            event.locator?.boxName?.let { locators[it] } ?: parentLocator,
                        )
                    })
                }
            }
        }
    }

    inner class AnimationState(
        val animation: Animation
    ) : MolangQueryAnimation, MolangQueryEntity by entity {
        val context = MolangContext(this)
        val animStartTime = entity.lifeTime
        override val animTime: Float
            get() = entity.lifeTime - animStartTime
        override val animLoopTime: Float
            get() =
                if (animation.loop == AnimationFile.Loop.HoldOnLastFrame) animTime.coerceAtMost(animation.animationLength)
                else animTime % animation.animationLength

        /** Effects up to and including this time have already been emitted. */
        internal var lastEffectTime = Float.NEGATIVE_INFINITY
        internal var effectLoops = 0
        internal val effectLoopsDuration: Float
            get() = if (animation.loop == AnimationFile.Loop.True) effectLoops * animation.animationLength else 0f
    }

    sealed interface Event {
        val timeSource: MolangQueryTime
        val time: Float
        val sourceEntity: MolangQueryEntity
    }

    data class ParticleEvent(
        override val timeSource: MolangQueryTime,
        override val time: Float,
        override val sourceEntity: MolangQueryEntity,
        val effect: ParticleEffect,
        val locator: ParticleSystem.Locator,
        val preEffectScript: MolangExpression?,
    ) : Event

    data class SoundEvent(
        override val timeSource: MolangQueryTime,
        override val time: Float,
        override val sourceEntity: MolangQueryEntity,
        val effect: SoundEffect,
        val locator: ParticleSystem.Locator,
    ) : Event

    private inner class BoneLocator(
        override var position: Vec3,
        override var rotation: Quaternion,
        override var velocity: Vec3
    ) : ParticleSystem.Locator {
        override val parent: ParticleSystem.Locator?
            get() = parentLocator
        override val isValid: Boolean
            get() = parentLocator.isValid
    }
}

data class Animation(
    val name: String,
    val animationLength: Float,
    val loop: AnimationFile.Loop,
    val bones: Map<String, Channels>,
    val effects: TreeMap<Float, List<Event>>,
    val affectsPoseParts: Set<EnumPart>,
) {
    val affectsPose: Boolean = affectsPoseParts.isNotEmpty()


    constructor(
        name: String,
        file: AnimationFile.Animation,
        bones: List<Bone>,
        particleEffects: Map<String, ParticleEffect>,
        soundEffects: Map<String, SoundEffect>,
    ) : this(
        name,
        file.animationLength ?: file.bones.calcAnimationLength(),
        file.loop,
        file.bones,
        TreeMap(mutableMapOf<Float, MutableList<Event>>().also { events ->
            file.particleEffects.forEach { (time, effects) ->
                val eventsAtTime = events.getOrPut(time, ::mutableListOf)
                for (config in effects) {
                    eventsAtTime.add(ParticleEvent(
                        particleEffects[config.effect] ?: continue,
                        config.locator?.let { locatorName -> bones.find { it.boxName == locatorName } },
                        config.preEffectScript,
                    ))
                }
            }
            file.soundEffects.forEach { (time, effects) ->
                val eventsAtTime = events.getOrPut(time, ::mutableListOf)
                for (config in effects) {
                    eventsAtTime.add(SoundEvent(
                        soundEffects[config.effect] ?: continue,
                        config.locator?.let { locatorName -> bones.find { it.boxName == locatorName } },
                    ))
                }
            }
        }),
        bones.flatMapTo(mutableSetOf()) {
            if (it.boxName in file.bones) it.affectsPoseParts else emptyList()
        },
    )

    sealed interface Event

    sealed interface LocatableEvent : Event {
        val locator: Bone?
    }

    data class ParticleEvent(
        val effect: ParticleEffect,
        override val locator: Bone?,
        val preEffectScript: MolangExpression?,
    ) : Event, LocatableEvent

    data class SoundEvent(
        val effect: SoundEffect,
        override val locator: Bone?,
    ) : Event, LocatableEvent

    companion object {
        private fun Map<String, Channels>.calcAnimationLength(): Float {
            return maxOfOrNull { (_, channels) ->
                listOf(channels.position, channels.rotation, channels.scale).maxOfOrNull {
                    it?.frames?.lastKey() ?: 0f
                } ?: 0f
            } ?: 0f
        }
    }
}

@Serializable
data class Channels(
    val position: Keyframes? = null,
    val rotation: Keyframes? = null,
    val scale: Keyframes? = null,
    @SerialName("relative_to")
    val relativeTo: RelativeTo = RelativeTo(),
)

@Serializable
data class RelativeTo(
    val rotation: String? = null,
)

@Serializable(with = KeyframesSerializer::class)
data class Keyframes(
    val frames: TreeMap<Float, Keyframe>
) {
    fun eval(context: MolangContext): Vec3 {
        val animTime = (context.query as? MolangQueryAnimation)?.animLoopTime ?: 0f
        val floor = frames.floorEntry(animTime)
        val ceil = frames.ceilingEntry(animTime)
        val floorValue = floor?.value?.post?.eval(context)
        val ceilValue = ceil?.value?.pre?.eval(context)
        return when {
            floorValue == null -> ceilValue!!
            ceilValue == null -> floorValue
            floor == ceil -> floorValue
            floor.value.smooth || ceil.value.smooth -> {
                val beforeFloor = frames.lowerEntry(floor.key)
                val afterCeil = frames.higherEntry(ceil.key)
                val beforeFloorValue = beforeFloor?.value?.post?.eval(context) ?: floorValue
                val afterCeilValue = afterCeil?.value?.post?.eval(context) ?: ceilValue
                val t = (animTime - floor.key) / (ceil.key - floor.key)
                catmullRom(t, beforeFloorValue, floorValue, ceilValue, afterCeilValue)
            }
            floorValue == ceilValue -> floorValue
            else -> floorValue.lerp(ceilValue, (animTime - floor.key) / (ceil.key - floor.key))
        }
    }
}

fun Vec3.lerp(other: Vec3, alpha: Float): Vec3 =
    vec3(x.lerp(other.x, alpha), y.lerp(other.y, alpha), z.lerp(other.z, alpha))

fun Float.lerp(other: Float, alpha: Float) = this + (other - this) * alpha

fun catmullRom(
    t: Float,
    a: Vec3,
    b: Vec3,
    c: Vec3,
    d: Vec3,
): Vec3 {
    return vec3(
        catmullRom(t, a.x, b.x, c.x, d.x),
        catmullRom(t, a.y, b.y, c.y, d.y),
        catmullRom(t, a.z, b.z, c.z, d.z),
    )
}

fun catmullRom(
    t: Float,
    a: Float,
    b: Float,
    c: Float,
    d: Float,
): Float {
    val v0 = -0.5f * a + 1.5f * b - 1.5f * c + 0.5f * d
    val v1 = a - 2.5f * b + 2 * c - 0.5f * d
    val v2 = -0.5f * a + 0.5f * c
    val tt = t * t
    return v0 * t * tt + v1 * tt + v2 * t + b
}

fun bezier(
    t: Float,
    a: Float,
    b: Float,
    c: Float,
    d: Float,
): Float {
    val ab = a.lerp(b, t)
    val bc = b.lerp(c, t)
    val cd = c.lerp(d, t)
    val abc = ab.lerp(bc, t)
    val bcd = bc.lerp(cd, t)
    return abc.lerp(bcd, t)
}

@Serializable(with = KeyframeSerializer::class)
data class Keyframe(
    val pre: MolangVec3,
    val post: MolangVec3,
    /** Sections around the keyframe are interpolated using Catmull-Rom splines instead of linear interpolation. */
    val smooth: Boolean,
)
