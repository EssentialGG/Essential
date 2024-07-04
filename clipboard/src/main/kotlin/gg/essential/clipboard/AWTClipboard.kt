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
package gg.essential.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO

class AWTClipboard: Clipboard, ClipboardOwner {
    val lostOwnership = Semaphore(0)

    override fun copyPNG(file: File): Boolean {
        return try {
            val image = this.ensureRGBImage(ImageIO.read(file))
            Toolkit.getDefaultToolkit().systemClipboard.setContents(TransferableImage(image), this)

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun lostOwnership(p0: java.awt.datatransfer.Clipboard?, p1: Transferable?) {
        // On Linux, we want to keep serving paste requests until another application takes ownership of the clipboard.
        lostOwnership.release()
    }

    private fun ensureRGBImage(bufferedImage: BufferedImage): BufferedImage {
        if (bufferedImage.type == BufferedImage.TYPE_INT_RGB) {
            return bufferedImage
        }

        return BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_INT_RGB).apply {
            val graphics = createGraphics()
            graphics.drawImage(bufferedImage, 0, 0, null)
            graphics.dispose()
        }
    }

    /**
     * Utility class for setting the system clipboard to an image
     */
    private class TransferableImage(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return DataFlavor.imageFlavor.equals(flavor)
        }

        @Throws(UnsupportedFlavorException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
            if (DataFlavor.imageFlavor.equals(flavor)) {
                return image
            }
            throw UnsupportedFlavorException(flavor)
        }
    }
}