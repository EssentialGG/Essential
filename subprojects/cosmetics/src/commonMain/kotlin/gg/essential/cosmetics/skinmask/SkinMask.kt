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
package gg.essential.cosmetics.skinmask

import gg.essential.model.EnumPart
import gg.essential.model.util.Color
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.MutableBitmap
import gg.essential.util.image.mask.Mask
import gg.essential.util.image.mask.MutableMask
import kotlin.math.max
import kotlin.math.min

/**
 * A black/white bitmap which is used to mask out certain parts of a player skin.
 * Pixels which are black in the mask get removed from the skin, pixels which are white in the mask are unaffected.
 */
class SkinMask(val parts: Map<EnumPart, Mask>) {
    fun apply(skin: Bitmap): Bitmap {
        return skin.mutableCopy().apply { applyTo(this) }
    }

    fun applyTo(skin: MutableBitmap) {
        for ((part, mask) in parts) {
            val box = SKIN_PARTS[part] ?: continue
            for (y in 0 until box.height) {
                for (x in 0 until box.width) {
                    if (!mask[x, y]) {
                        skin[box.x + x, box.y + y] = Color(0u)
                    }
                }
            }
        }
    }

    fun offset(x: Int, y: Int, z: Int): SkinMask = SkinMask(parts.mapValues { (part, mask) ->
        val cubeMaps = CUBE_MAPS[part] ?: return@mapValues mask
        val result = Mask.ofSize(mask.width, mask.height)
        result[0, 0, mask.width, mask.height] = true
        for (cubeMap in cubeMaps) {
            result.copyFrom(mask, cubeMap.front, x, y)
            result.copyFrom(mask, cubeMap.back, -x, y)
            result.copyFrom(mask, cubeMap.right, -z, y)
            result.copyFrom(mask, cubeMap.left, z, y)
            result.copyFrom(mask, cubeMap.top, x, -z)
            result.copyFrom(mask, cubeMap.bottom, -x, -z)
        }
        result
    })

    private fun MutableMask.copyFrom(source: Mask, box: Box, offX: Int, offY: Int) {
        val minX = max(box.x + offX, box.x)
        val maxX = min(box.x + box.width + offX, box.x + box.width)
        val minY = max(box.y + offY, box.y)
        val maxY = min(box.y + box.height + offY, box.y + box.height)
        for (y in minY until maxY) {
            for (x in minX until maxX) {
                this[x, y] = source[x - offX, y - offY]
            }
        }
    }

    private data class Box(val x: Int, val y: Int, val width: Int, val height: Int)

    private data class CubeMap(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val depth: Int,
    ) {
        val top = Box(x + depth, y, width, depth)
        val bottom = Box(x + depth + width, y, width, depth)
        val right = Box(x, y + depth, depth, height)
        val front = Box(x + depth, y + depth, width, height)
        val left = Box(x + depth + width, y + depth, depth, height)
        val back = Box(x + depth + width + depth, y + depth, width, height)
    }

    companion object {
        // Positions are within the overall skin file
        private val SKIN_PARTS = mapOf(
            EnumPart.HEAD to Box(0, 0, 64, 16),
            EnumPart.BODY to Box(16, 16, 24, 32),
            EnumPart.LEFT_ARM to Box(32, 48, 32, 16),
            EnumPart.RIGHT_ARM to Box(40, 16, 16, 32),
            EnumPart.LEFT_LEG to Box(0, 48, 32, 16),
            EnumPart.RIGHT_LEG to Box(0, 16, 16, 32),
        )

        // Positions are within the respective part as per SKIN_PARTS above
        private val CUBE_MAPS = mapOf(
            EnumPart.HEAD to listOf(CubeMap(0, 0, 8, 8, 8), CubeMap(32, 0, 8, 8, 8))
        )

        fun read(bitmap: Bitmap): SkinMask {
            val parts = mutableMapOf<EnumPart, Mask>()
            for ((part, box) in SKIN_PARTS) {
                val mask = Mask.copyOf(bitmap, box.x, box.y, box.width, box.height)
                if (mask.count() == mask.width * mask.height) continue // part is all white and therefore not affected
                parts[part] = mask
            }
            return SkinMask(parts)
        }

        fun merge(skinMasks: List<SkinMask>): SkinMask {
            return skinMasks.asSequence()
                .flatMap { it.parts.asSequence() }
                .groupBy({ it.key }, { it.value })
                .mapNotNull { (part, masks) ->
                    when {
                        masks.isEmpty() -> null
                        masks.size == 1 -> part to masks.first()
                        else -> {
                            val combined = masks.first().mutableCopy()
                            for (i in 1..masks.lastIndex) {
                                combined.setOr(masks[i])
                            }
                            part to combined
                        }
                    }
                }
                .toMap()
                .let { SkinMask(it) }
        }
    }
}