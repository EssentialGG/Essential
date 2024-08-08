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

import dev.caoimhe.jnapple.appkit.NSData
import dev.caoimhe.jnapple.appkit.NSPasteboard
import java.io.File

/**
 * Uses macOS' NSPasteboard to copy raw data of files to the clipboard.
 * Related linear issues: EM-1869, EM-1607
 */
class MacOSClipboard : Clipboard {
    /**
     * Reads the bytes from a [file], and copies it to the clipboard as a PNG.
     */
    override fun copyPNG(file: File): Boolean {
        return try {
            val data = file.readBytes()
            val pasteboard = NSPasteboard.generalPasteboard()

            // Apple recommends clearing the existing clipboard's contents before "providing" your own data.
            pasteboard.clearContents()
            pasteboard.setData(NSData.initWithBytes(data), NSPasteboard.TypePNG)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}