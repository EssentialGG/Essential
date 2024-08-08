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
package gg.essential.util.lwjgl3.api

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.DefaultByteBufHolder
import java.nio.file.Path

/**
 * Reads image data using LWJGL STBImage lib
 */
interface NativeImageReader {

    /**
     * Returns ImageData with the data provided
     */
    fun getImageData(path: Path, allocator: ByteBufAllocator): ImageData

    /**
     * Returns ImageData with the data provided
     */
    fun getImageData(buf: ByteBuf, allocator: ByteBufAllocator): ImageData

    /**
     * Saves the image stored in the provided ImageData at the designated path
     */
    fun saveImage(path: Path, imageData: ImageData)
}

/**
 * Interface providing access to the data about an image
 */
data class ImageData(
    val data: ByteBuf,
    val width: Int,
    val height: Int,
    val fileChannels: Int,
) : DefaultByteBufHolder(data)
