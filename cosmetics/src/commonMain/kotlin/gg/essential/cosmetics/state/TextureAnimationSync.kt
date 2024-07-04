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

import gg.essential.model.BedrockModel
import gg.essential.model.util.now
import kotlin.math.abs

class TextureAnimationSync(private val textureFrameCount: Int) {

    private var mostRecentLifetime = 0f
    private var mostRecentSync: Long = 0
    private val animationOffsets = mutableListOf<Float>()
    private var currentLifetimeOffset = 0f
    private var lastTextureAdjustment = 0f

    /**
     * Interpolate lifetimeOffset based on the last 3 syncs
     */
    fun getAdjustedLifetime(lifetime: Float): Float {
        mostRecentSync = now().toEpochMilli()
        mostRecentLifetime = lifetime
        if (animationOffsets.isEmpty()) {
            return lifetime
        }
        var totalValues = 0f
        var totalEntries = 0f
        for (animationOffset in animationOffsets) {
            totalValues += animationOffset
            totalEntries++
        }
        val targetOffsetTime = totalValues / totalEntries
        val totalAnimationTime = textureFrameCount / BedrockModel.TEXTURE_ANIMATION_FPS

        // See if its better to speed up or slow down to get in sync
        val deltaTimeBackwards = targetOffsetTime - (currentLifetimeOffset + totalAnimationTime)
        val deltaTimeForwards = targetOffsetTime - currentLifetimeOffset

        // Make an adjustment every tick instead of every frame
        if (lifetime - lastTextureAdjustment > .05) {
            val timeAdjustment = if (abs(deltaTimeBackwards) < abs(deltaTimeForwards)) {
                deltaTimeBackwards.coerceIn(-0.01f, 0.01f)
            } else {
                deltaTimeForwards.coerceIn(-0.01f, 0.01f)
            }
            currentLifetimeOffset += timeAdjustment
            lastTextureAdjustment = lifetime
        }
        return lifetime + currentLifetimeOffset
    }

    fun syncTextureStart() {
        val totalFrames = textureFrameCount
        val lifeTime = (now().toEpochMilli() - mostRecentSync) / 1000f + mostRecentLifetime
        val frame = (lifeTime * BedrockModel.TEXTURE_ANIMATION_FPS).toInt()
        val completedFrames = frame % totalFrames
        val targetLifetimeOffset = (totalFrames - completedFrames) / BedrockModel.TEXTURE_ANIMATION_FPS
        if (animationOffsets.size > 3) {
            animationOffsets.removeFirst()
        }
        animationOffsets.add(targetLifetimeOffset)
    }
}