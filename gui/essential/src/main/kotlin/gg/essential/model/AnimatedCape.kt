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

import gg.essential.mod.EssentialAsset
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.Client
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UIdentifier
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.fromOrThrow
import gg.essential.util.image.bitmap.toTexture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.CompletableFuture

object AnimatedCape {
    fun createFrames(cosmetic: Cosmetic, texture: EssentialAsset, textureBytes: ByteArray): CompletableFuture<List<UIdentifier>> {
        return CompletableFuture.completedFuture(textureBytes).thenApplyAsync { bytes ->
            val fullImage = Bitmap.fromOrThrow(bytes.inputStream())
            val frameWidth = 64
            val frameHeight = 32
            val frameCount = fullImage.height / frameHeight

            if (fullImage.width != frameWidth || fullImage.height != frameHeight * frameCount) {
                throw IllegalArgumentException("Texture of ${cosmetic.id} has invalid size: ${fullImage.width}x${fullImage.height}")
            }

            (0 until frameCount).map { frameIdx ->
                val frame = Bitmap.ofSize(frameWidth, frameHeight)
                frame.set(0, 0, frameWidth, frameHeight, fullImage, srcY = frameIdx * frameHeight)
                Pair("${texture.checksum}/$frameIdx", frame.toTexture())
            }
        }.thenApplyAsync({ frames ->
            // Retrieving the identifier causes the texture to be registered with MC, so we need to do this last step
            // on the client main thread
            frames.map { platform.registerCosmeticTexture(it.first, it.second) }
        }, Dispatchers.Client.asExecutor())
    }
}
