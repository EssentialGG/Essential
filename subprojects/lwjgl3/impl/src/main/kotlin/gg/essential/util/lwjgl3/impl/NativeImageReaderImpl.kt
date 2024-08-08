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
package gg.essential.util.lwjgl3.impl

import gg.essential.config.AccessedViaReflection
import gg.essential.util.lwjgl3.api.ImageData
import gg.essential.util.lwjgl3.api.NativeImageReader
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.lwjgl.stb.STBIWriteCallback
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

@AccessedViaReflection("NativeImageReader")
@SuppressWarnings("unused")
class NativeImageReaderImpl : NativeImageReader {

    private val fileOptions =
        EnumSet.of(
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        )

    override fun getImageData(path: Path, allocator: ByteBufAllocator): ImageData {
        val fileData = Files.newInputStream(path).use { readResource(it) }
        fileData.rewind()

        // Incase the thread is interrupted during the next block of code
        // We will want to make sure any native memory allocated
        // Has been cleaned up
        try {
            return getImageData(fileData, allocator)
        } finally {
            MemoryUtil.memFree(fileData)
        }
    }

    override fun getImageData(buf: ByteBuf, allocator: ByteBufAllocator): ImageData {
        return if (buf.isDirect) {
            getImageData(buf.nioBuffer(), allocator)
        } else {
            val directBuf = Unpooled.directBuffer(buf.readableBytes())
            try {
                directBuf.writeBytes(buf, buf.readerIndex(), buf.readableBytes())
                return getImageData(directBuf.nioBuffer(), allocator)
            } finally {
                directBuf.release()
            }
        }
    }

    private fun getImageData(inputBuffer: ByteBuffer, allocator: ByteBufAllocator): ImageData {
        MemoryStack.stackPush().use { memoryStack ->
            val widthBuffer = memoryStack.mallocInt(1)
            val heightBuffer = memoryStack.mallocInt(1)
            val fileChannels = memoryStack.mallocInt(1)

            val nativeBuffer =
                STBImage.stbi_load_from_memory(inputBuffer, widthBuffer, heightBuffer, fileChannels, 0)
                    ?: throw IOException("Could not load image: " + STBImage.stbi_failure_reason())
            try {
                // Create a copy so we can immediately free the native memory using the appropriate function
                val buffer = allocator.directBuffer(nativeBuffer.remaining())
                buffer.writeBytes(nativeBuffer)
                return ImageData(buffer, widthBuffer.get(0), heightBuffer.get(0), fileChannels.get(0))
            } finally {
                nativeBuffer.rewind()
                STBImage.stbi_image_free(nativeBuffer)
            }
        }
    }

    override fun saveImage(path: Path, imageData: ImageData) {
        Files.newByteChannel(path, fileOptions).use {
            WriteCallback(it).use { writeCallback ->
                STBImageWrite.nstbi_write_png_to_func(
                    writeCallback.address(),
                    0L,
                    imageData.width,
                    imageData.height,
                    imageData.fileChannels,
                    MemoryUtil.memAddress(imageData.data.nioBuffer()),
                    0
                ) != 0
            }
        }
    }

    internal class WriteCallback(private val channel: WritableByteChannel) : STBIWriteCallback(), Closeable {
        private var exception: IOException? = null
        override fun invoke(context: Long, data: Long, size: Int) {
            val byteBuffer = getData(data, size)
            try {
                channel.write(byteBuffer)
            } catch (var8: IOException) {
                exception = var8
            }
        }

        override fun close() {
            free()
        }
    }

    @Throws(IOException::class)
    fun readResource(inputStream: InputStream): ByteBuffer {
        var byteBuffer: ByteBuffer? = null
        try {
            if (inputStream is FileInputStream) {
                val fileChannel = inputStream.channel
                byteBuffer = MemoryUtil.memAlloc(fileChannel.size().toInt() + 1)
                while (fileChannel.read(byteBuffer) != -1) {
                }
            } else {
                byteBuffer = MemoryUtil.memAlloc(8192)
                val fileInputStream = Channels.newChannel(inputStream)
                while (fileInputStream.read(byteBuffer) != -1) {
                    if (byteBuffer?.remaining() == 0) {
                        byteBuffer = MemoryUtil.memRealloc(byteBuffer, byteBuffer.capacity() * 2)
                    }
                }
            }
            return byteBuffer!!.also {
                byteBuffer = null
            }
        } finally {
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer)
            }
        }
    }
}
