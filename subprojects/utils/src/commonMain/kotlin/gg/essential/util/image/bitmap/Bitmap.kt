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
import gg.essential.util.image.bitmap.impl.IntArrayBitmap

/**
 * A [Bitmap] is an interface that represents a bitmap image.
 * Its underlying data type is implementation-specific.
 */
interface Bitmap {
    val width: Int
    val height: Int

    operator fun get(x: Int, y: Int): Color

    fun mutableCopy(): MutableBitmap

    companion object {
        fun ofSize(width: Int, height: Int): MutableBitmap = IntArrayBitmap(width, height)
    }
}

interface MutableBitmap : Bitmap {
    operator fun set(x: Int, y: Int, color: Color)

    operator fun set(x: Int, y: Int, w: Int, h: Int, color: Color) {
        for (yLoop in y until y + h) {
            for (xLoop in x until x + w) {
                this[xLoop, yLoop] = color
            }
        }
    }

    fun set(x: Int, y: Int, w: Int, h: Int, src: Bitmap, srcX: Int = 0, srcY: Int = 0, mirrorX: Boolean = false, mirrorY: Boolean = false) {
        for (yLoop in 0 until h) {
            for (xLoop in 0 until w) {
                val processedX: Int = if (mirrorX) w - 1 - xLoop else xLoop
                val processedY: Int = if (mirrorY) h - 1 - yLoop else yLoop
                this[x + processedX, y + processedY] = src[srcX + xLoop, srcY + yLoop]
            }
        }
    }
}