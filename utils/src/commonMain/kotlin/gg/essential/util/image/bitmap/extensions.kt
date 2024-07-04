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
package gg.essential.util.image.bitmap

import gg.essential.model.util.Color

/**
 * Crops a [Bitmap] to a certain position and size.
 * This creates a new Bitmap, and won't modify the original.
 */
fun Bitmap.cropped(x: Int, y: Int, width: Int, height: Int): Bitmap {
    return Bitmap.ofSize(width, height).apply {
        forEachPixel { _, cropX, cropY ->
            val realX = cropX + x
            val realY = cropY + y

            this@apply[cropX, cropY] = this@cropped[realX, realY]
        }
    }
}

/**
 * Iterates over all the pixels in a [Bitmap].
 */
inline fun Bitmap.forEachPixel(action: (color: Color, x: Int, y: Int) -> Unit) {
    for (y in 0 until this.height) {
        for (x in 0 until this.width) {
            action(this[x, y], x, y)
        }
    }
}
