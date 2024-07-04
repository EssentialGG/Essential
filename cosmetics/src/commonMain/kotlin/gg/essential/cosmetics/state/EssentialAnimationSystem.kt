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
package gg.essential.cosmetics.state

import gg.essential.cosmetics.events.AnimationEvent
import gg.essential.cosmetics.events.AnimationEventType
import gg.essential.cosmetics.events.AnimationTarget
import gg.essential.model.BedrockModel
import gg.essential.model.ModelAnimationState
import gg.essential.model.molang.MolangQueryEntity
import kotlin.random.Random

class EssentialAnimationSystem(
    private val bedrockModel: BedrockModel,
    private val entity: MolangQueryEntity,
    private val animationState: ModelAnimationState,
    private val textureAnimationSync: TextureAnimationSync,
    private val animationTargets: Set<AnimationTarget>,
    private val onAnimation: (String) -> Unit,
) {
    private val ongoingAnimations = mutableSetOf<AnimationEvent>()
    private val animationStates = AnimationEffectStates()

    private class AnimationEffectStates {
        var skips = HashMap<AnimationEvent, Int>()
    }

    private var lastFrame = 0f

    init {
        processEvent(AnimationEventType.IDLE)
        processEvent(AnimationEventType.EQUIP)
        processEvent(AnimationEventType.EMOTE)
    }

    fun updateAnimationState() {
        val onComplete = mutableListOf<AnimationEvent>()
        ongoingAnimations.removeAll { ongoingAnimation: AnimationEvent ->
            for (animationState in animationState.active) {
                if (animationState.animation.name == ongoingAnimation.name && ongoingAnimation.loops > 0) {
                    val remove = animationState.animTime > animationState.animation.animationLength * ongoingAnimation.loops
                    if (remove && ongoingAnimation.onComplete != null) {
                        onComplete.add(ongoingAnimation.onComplete)
                    }
                    return@removeAll remove
                }
            }
            false
        }
        ongoingAnimations.addAll(onComplete)

        val highestPriority = highestPriority
        animationState.active.removeAll { animationState: ModelAnimationState.AnimationState ->
            val animationByName = getAnimationByName(animationState.animation.name)
            animationByName == null || animationByName !== highestPriority
        }
        if (animationState.active.isEmpty() && highestPriority != null) {
            val animation = bedrockModel.getAnimationByName(highestPriority.name)
            if (animation != null) {
                animationState.startAnimation(animation)
            }
        }
    }

    private val highestPriority: AnimationEvent?
        get() = ongoingAnimations.maxByOrNull { obj: AnimationEvent -> obj.priority }

    private fun getAnimationByName(name: String): AnimationEvent? {
        for (ongoingAnimation in ongoingAnimations) {
            if (ongoingAnimation.name == name) {
                return ongoingAnimation
            }
        }
        return null
    }

    fun processEvent(type: AnimationEventType) {
        val animationEvents = bedrockModel.animationEvents
        val highestPriority = highestPriority
        val priority = highestPriority?.priority ?: 0
        var needsUpdate = false
        for (event in animationEvents) {
            if (priority > event.priority || event.type != type) continue
            if (event.target != AnimationTarget.ALL && event.target !in animationTargets) {
                continue
            }
            if (event.skips != 0) {
                val i = (animationStates.skips[event] ?: 0) + 1
                animationStates.skips[event] = i
                if (i % event.skips != 0) {
                    continue
                }
            }
            if (!handleProbability(event)) continue
            if (event.target != AnimationTarget.SELF) {
                onAnimation(event.name)
            }
            ongoingAnimations.add(event)
            needsUpdate = true
        }
        if (needsUpdate) {
            updateAnimationState()
        }
    }

    fun fireTriggerFromAnimation(animationName: String) {
        if (animationName == "texture_start") {
            textureAnimationSync.syncTextureStart()
            return
        }
        for (animationEvent in bedrockModel.animationEvents) {
            if (animationEvent.name == animationName) {
                ongoingAnimations.add(animationEvent)
                updateAnimationState()
                break
            }
        }
    }

    private fun handleProbability(event: AnimationEvent): Boolean {
        return event.probability > Random.nextDouble()
    }

    fun maybeFireTextureAnimationStartEvent() {
        val totalFrames = bedrockModel.textureFrameCount
        val frame: Int = (entity.lifeTime * BedrockModel.TEXTURE_ANIMATION_FPS).toInt()
        if (frame % totalFrames < lastFrame) {
            onAnimation("texture_start")
            processEvent(AnimationEventType.TEXTURE_ANIMATION_START)
        }
        lastFrame = (frame % totalFrames).toFloat()
    }
}