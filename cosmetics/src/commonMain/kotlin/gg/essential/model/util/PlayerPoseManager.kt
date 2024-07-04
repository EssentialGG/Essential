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

import gg.essential.cosmetics.WearablesManager
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.ModelAnimationState
import gg.essential.model.ModelInstance
import gg.essential.model.ParticleSystem
import gg.essential.model.backend.PlayerPose
import gg.essential.model.molang.MolangQueryEntity

/**
 * A [ModelInstance] and its most recently active animation state. We need to explicitly track the
 * most recently active animation state because we want to hold that after the emote is done playing
 * while transitioning away from it.
 */
private typealias EmoteState = Pair<ModelInstance, ModelAnimationState.AnimationState?>

/** Manages a player pose to smoothly transition in of, out of and between emotes. */
class PlayerPoseManager(
    private val entity: MolangQueryEntity,
) {
    private var lastTime = entity.lifeTime

    /** Previously equipped emote */
    private var transitionFrom: EmoteState? = null
    /** Progress transitioning away from the previously equipped emote. */
    private var transitionFromProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Currently equipped emote */
    private var transitionTo: EmoteState? = null
    /** Progress transitioning to the currently equipped emote. */
    private var transitionToProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    fun update(wearablesManager: WearablesManager) =
        update(
            wearablesManager.models.values
                .find { it.cosmetic.type.slot == CosmeticSlot.EMOTE }
                ?.takeUnless { it.animationState.active.isEmpty() }
        )

    fun update(target: ModelInstance?) {
        val previous = transitionTo
        if (target == null) {
            if (previous != null) {
                // Switch from an emote to nothing
                transitionFrom = previous
                transitionFromProgress = 1f - transitionToProgress
                transitionTo = null
                transitionToProgress = 1f
            } else {
                // From nothing to nothing
            }
        } else {
            if (target != previous?.first) {
                // From one emote (or nothing) to another emote
                transitionFrom = previous
                transitionFromProgress = 1f - transitionToProgress
                transitionTo = Pair(target, target.animationState.active.firstOrNull())
                transitionToProgress = 0f
            } else {
                // Target unchanged
            }
        }

        /** Stores the most recently active animation state for use after the emote has finished. */
        fun EmoteState.updateAnimationState() =
            first.animationState.active.firstOrNull()?.let { Pair(first, it) } ?: this

        transitionTo = transitionTo?.updateAnimationState()
        transitionFrom = transitionFrom?.updateAnimationState()

        val now = entity.lifeTime
        val dt = now - lastTime.also { lastTime = now }
        val progress = dt / transitionTime
        transitionToProgress += progress
        transitionFromProgress += progress
    }

    /**
     * Computes the final pose for the player based on their vanilla [basePose], equipped cosmetics,
     * the current emote and, if not yet fully transitioned, the previous emote.
     */
    fun computePose(wearablesManager: WearablesManager, basePose: PlayerPose): PlayerPose {
        var transformedPose = basePose

        // Apply interpolated emote pose
        transformedPose = computePose(transformedPose)

        // Apply pose animations from all other cosmetics (if any)
        for ((cosmetic, model) in wearablesManager.models) {
            if (cosmetic.type.slot == CosmeticSlot.EMOTE) {
                continue // already handled separately before the loop
            }
            transformedPose = model.computePose(transformedPose)
        }

        return transformedPose
    }

    /**
     * Computes the final pose for the player based on their vanilla [basePose], the current emote
     * and, if not yet fully transitioned, the previous emote.
     */
    private fun computePose(basePose: PlayerPose): PlayerPose {
        var transformedPose = basePose

        fun EmoteState.computePoseForAffectedParts(basePose: PlayerPose): PlayerPose {
            val (instance, latestAnimation) = this
            // Construct a fake animation state so we can hold the most recently active animation
            // even after it is over
            // (so we can smoothly interpolate away from it)
            val animationState = ModelAnimationState(instance.animationState.entity, ParticleSystem.Locator.Zero)
            latestAnimation?.let { animationState.active.add(it) }
            // Compute the pose for this model based on the neutral pose (i.e. suppressing
            // input/base/vanilla pose)
            val modelPose = instance.model.computePose(PlayerPose.neutral(), animationState)
            // but keep the base pose for those parts that were not affected by the animation
            val affectedParts =
                animationState.active.flatMapTo(mutableSetOf()) { it.animation.affectsPoseParts }
            return PlayerPose.fromMap(
                basePose.keys.associateWith {
                    if (it in affectedParts) modelPose[it] else basePose[it]
                },
                basePose.child,
            )
        }

        val transitionFrom = transitionFrom
        if (transitionFrom != null) {
            val fromPose = transitionFrom.computePoseForAffectedParts(transformedPose)
            transformedPose = interpolatePose(fromPose, transformedPose, transitionFromProgress)
        }

        val transitionTo = transitionTo
        if (transitionTo != null) {
            val toPose = transitionTo.computePoseForAffectedParts(transformedPose)
            transformedPose = interpolatePose(transformedPose, toPose, transitionToProgress)
        }

        return transformedPose
    }

    private fun interpolatePose(a: PlayerPose, b: PlayerPose, alpha: Float): PlayerPose {
        return when {
            alpha == 0f -> a
            alpha == 1f -> b
            a == b -> a
            else ->
                PlayerPose.fromMap(
                    a.keys.associateWith { interpolatePosePart(a[it], b[it], alpha) },
                    a.child
                )
        }
    }

    private fun interpolatePosePart(
        a: PlayerPose.Part,
        b: PlayerPose.Part,
        alpha: Float
    ): PlayerPose.Part {
        if (a == b) {
            return a
        }
        val oneMinusAlpha = 1 - alpha
        fun interp(a: Float, b: Float) = a * oneMinusAlpha + b * alpha
        fun interpRot(a: Float, b: Float): Float {
            // Wrap angles around such that we'll always interpolate the short way round
            var aMod = a.mod(TAU)
            var bMod = b.mod(TAU)
            if (aMod - bMod > HALF_TAU) {
                aMod -= TAU
            } else if (bMod - aMod > HALF_TAU) {
                bMod -= TAU
            }
            return interp(aMod, bMod)
        }
        return PlayerPose.Part(
            pivotX = interp(a.pivotX, b.pivotX),
            pivotY = interp(a.pivotY, b.pivotY),
            pivotZ = interp(a.pivotZ, b.pivotZ),
            rotateAngleX = interpRot(a.rotateAngleX, b.rotateAngleX),
            rotateAngleY = interpRot(a.rotateAngleY, b.rotateAngleY),
            rotateAngleZ = interpRot(a.rotateAngleZ, b.rotateAngleZ),
            // we use this only for (mostly small) non-uniform scaling and vanilla doesn't use it at
            // all, doubt anyone will notice we're cheating and I'm not keen on doing matrix
            // interpolation
            extra =
                when {
                    a.extra == null -> b.extra
                    b.extra == null -> a.extra
                    else -> if (alpha < 0.5) a.extra else b.extra
                },
        )
    }

    companion object {
        private const val HALF_TAU = kotlin.math.PI.toFloat()
        private const val TAU = HALF_TAU * 2

        /**
         * Time (in seconds) we'll take to transition from one emote (or none) to another emote (or
         * none).
         */
        const val transitionTime = 0.3f
    }
}
