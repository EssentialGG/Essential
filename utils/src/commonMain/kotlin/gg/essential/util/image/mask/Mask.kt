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
package gg.essential.util.image.mask

import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.mask.impl.LongArrayMask

/**
 * A [Bitmap] where each pixel is only ever white or black.
 *
 * This type is generally much more storage efficient than a regular bitmap and provides functions for combining masks
 * that are multiple order of magnitudes faster than a naive implementation using regular bitmaps.
 */
interface Mask {
    val width: Int
    val height: Int

    /** Returns `true` if the given pixel is set (white), `false` otherwise. */
    operator fun get(x: Int, y: Int): Boolean

    /** Returns the number of set (white) pixels. */
    fun count(): Int

    fun mutableCopy(): MutableMask

    companion object {
        fun ofSize(width: Int, height: Int): MutableMask = LongArrayMask(width, height)

        fun copyOf(bitmap: Bitmap, x: Int = 0, y: Int = 0, width: Int = bitmap.width, height: Int = bitmap.height): MutableMask {
            val mask = ofSize(width, height)
            for (my in 0 until height) {
                for (mx in 0 until width) {
                    mask[mx, my] = (bitmap[x + mx, y + my].argb and 0xffffffu) != 0u
                }
            }
            return mask
        }
    }
}

interface MutableMask : Mask {
    /** Changes the given pixel to be set (white). */
    fun set(x: Int, y: Int)
    /** Changes the given pixel to be unset (black). */
    fun clear(x: Int, y: Int)

    /** Changes the given pixel to be set (white) if [value] is `true`, or unset (black) if it is `false`. */
    operator fun set(x: Int, y: Int, value: Boolean) {
        if (value) set(x, y) else clear(x, y)
    }

    fun set(x: Int, y: Int, w: Int, h: Int) {
        for (yLoop in y until y + h) {
            for (xLoop in x until x + w) {
                set(xLoop, yLoop)
            }
        }
    }

    fun clear(x: Int, y: Int, w: Int, h: Int) {
        for (yLoop in y until y + h) {
            for (xLoop in x until x + w) {
                clear(xLoop, yLoop)
            }
        }
    }

    operator fun set(x: Int, y: Int, w: Int, h: Int, value: Boolean) {
        if (value) set(x, y, w, h) else clear(x, y, w, h)
    }

    /** Inverts all pixels in this mask. */
    fun inv()

    /** Sets each pixel to `true` iff it either already is `true` or it is `true` in the given other mask. */
    fun setOr(other: Mask)
    /** Sets each pixel to `true` iff it is `true` in this and the given other mask. */
    fun setAnd(other: Mask)
}

