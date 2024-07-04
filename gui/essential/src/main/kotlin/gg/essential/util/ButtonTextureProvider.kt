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
package gg.essential.util

import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.model.util.Color
import gg.essential.universal.utils.ReleasedDynamicTexture
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.forEachPixel
import gg.essential.util.image.bitmap.toTexture
import java.util.*
import java.awt.Color as JavaColor

/**
 * Manages tinting textures, also maintains a cache to improve performance.
 */
object ButtonTextureProvider {
    private val averageColorCache = WeakHashMap<Bitmap, Color>()
    private val tintedImageCache = WeakHashMap<Bitmap, HashMap<JavaColor?, ReleasedDynamicTexture>>()
    private val referenceHolder = ReferenceHolderImpl()

    init {
        platform.onResourceManagerReload {
            clearCache()
        }
        platform.config.useVanillaButtonForRetexturing.onSetValue(referenceHolder) { enabled ->
            if (!enabled) {
                clearCache() // no longer need it, may as well free it
            }
        }
    }

    /**
     * Tints an [source] to a certain [color].
     * If the [source] has already been tinted to that [color], it will return the cached texture.
     *
     * @return [ReleasedDynamicTexture]
     */
    fun provide(source: Bitmap, color: JavaColor? = null): ReleasedDynamicTexture {
        val variantsForImage = tintedImageCache.computeIfAbsent(source) { HashMap() }
        val tintedTexture = variantsForImage.computeIfAbsent(color) {
            val tint = color ?: return@computeIfAbsent source.toTexture()

            val average = averageColor(source)
            val destination = Bitmap.ofSize(source.width, source.height)

            // These are how much we need to tint each pixel by
            val redDifference = average.r.toInt() - tint.red
            val greenDifference = average.g.toInt() - tint.green
            val blueDifference = average.b.toInt() - tint.blue

            source.forEachPixel { color, x, y ->
                val tintedColor = color.tinted(redDifference, greenDifference, blueDifference)
                destination[x, y] = tintedColor
            }

            destination.toTexture()
        }

        return tintedTexture
    }

    /**
     * Clears the tinted image and average color caches.
     */
    fun clearCache() {
        tintedImageCache.clear()
        averageColorCache.clear()
    }

    /**
     * A very simple sum-based average color algorithm.
     * Adds all the red values, green values and blue values together, and then gets the average from those 3 values.
     * There are other more complex algorithms, but this is fine for now.
     */
    private fun averageColor(image: Bitmap) =
        averageColorCache.computeIfAbsent(image) {
            var r = 0u
            var g = 0u
            var b = 0u

            image.forEachPixel { color, _, _ ->
                r += color.r
                g += color.g
                b += color.b
            }

            val pixels = (image.width * image.height).toUInt()
            Color((r / pixels).toUByte(), (g / pixels).toUByte(), (b / pixels).toUByte(), 255u)
        }

    private fun Color.tinted(red: Int, green: Int, blue: Int): Color {
        // If the color is white, we don't want to tint it.
        // (See the vanilla hovered button texture for an example of this)
        if (r == 255.toUByte() && g == 255.toUByte() && b == 255.toUByte()) return this

        val newRed = (this.r.toInt() - red).coerceIn(0..255)
        val newGreen = (this.g.toInt() - green).coerceIn(0..255)
        val newBlue = (this.b.toInt() - blue).coerceIn(0..255)

        return Color(newRed.toUByte(), newGreen.toUByte(), newBlue.toUByte(), this.a)
    }
}