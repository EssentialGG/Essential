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

import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.universal.UImage
import gg.essential.universal.utils.ReleasedDynamicTexture
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.UIdentifier
import org.slf4j.LoggerFactory
import java.io.InputStream


/**
 * Used for checking if the resource pack has been changed.
 */
private val resourceReloadState = mutableStateOf(0).apply {
    platform.onResourceManagerReload {
        set { it + 1 }
    }
}

/**
 * Returns a [MutableBitmap] derived from this [UIdentifier].
 * This is updated whenever the user changes their resource pack.
 */
fun UIdentifier.bitmapState(): State<MutableBitmap?> = resourceReloadState.map { Bitmap.from(this) }

/**
 * Returns a [MutableBitmap] derived from this [UIdentifier], only if [flag] is set.
 */
fun UIdentifier.bitmapStateIf(flag: State<Boolean>) = resourceReloadState.zip(flag).map { (_, flag) ->
    if (flag) Bitmap.from(this) else null
}

fun Bitmap.Companion.from(location: UIdentifier): MutableBitmap? {
    return try {
        platform.bitmapFromMinecraftResource(location)
    } catch (e: Exception) {
        LoggerFactory.getLogger(Bitmap::class.java).error("Failed to read bitmap from $location!", e)
        null
    }
}

fun Bitmap.Companion.from(inputStream: InputStream): MutableBitmap? {
    return try {
        fromOrThrow(inputStream)
    } catch (e: Exception) {
        LoggerFactory.getLogger(Bitmap::class.java).error("Failed to read bitmap from inputStream!", e)
        null
    }
}

fun Bitmap.Companion.fromOrThrow(inputStream: InputStream): MutableBitmap = platform.bitmapFromInputStream(inputStream)

fun Bitmap.toTexture(): ReleasedDynamicTexture {
    return platform.uImageIntoReleasedDynamicTexture(toUImage())
}

fun Bitmap.toUImage(): UImage {
    val image = UImage.ofSize(width, height, clear = false)

    forEachPixel { color, x, y ->
        image.setPixelRGBA(x, y, color.rgba.toInt())
    }

    return image
}
