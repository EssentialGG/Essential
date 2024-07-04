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
package gg.essential.gui.image

import gg.essential.Essential
import gg.essential.elementa.components.UIImage
import gg.essential.mod.EssentialAsset
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * An image factory that loads the image from the specified [asset]
 */
data class EssentialAssetImageFactory(
    private val asset: EssentialAsset,
    private val priority: AssetLoader.Priority = AssetLoader.Priority.Blocking,
) : ImageFactory() {
    private val assetLoader = Essential.getInstance().connectionManager.cosmeticsManager.assetLoader

    fun primeCache(cachePriority: AssetLoader.Priority) {
        getCachedImage(cachePriority)
    }

    private fun getCachedImage(computePriority: AssetLoader.Priority): UIImage {
        val byteArrayFuture = assetLoader.getAssetBytes(asset, computePriority)
        return cache.computeIfAbsent(asset.checksum) {
            UIImage(byteArrayFuture.thenApplyAsync {
                ImageIO.read(ByteArrayInputStream(it))
            })
        }
    }

    override fun generate(): UIImage {
        val cachedImage = getCachedImage(priority)
        return UIImage(CompletableFuture.completedFuture(null)).also {
            cachedImage.supply(it)
        }
    }

    companion object {
        private val cache = mutableMapOf<String, UIImage>()
    }
}
