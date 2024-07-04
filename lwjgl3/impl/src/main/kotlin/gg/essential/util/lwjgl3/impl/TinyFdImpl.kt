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
import gg.essential.util.lwjgl3.api.TinyFd
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs

@AccessedViaReflection("TinyFd")
@Suppress("unused") // called via reflection from Lwjgl3Loader
class TinyFdImpl : TinyFd {
    override fun openFileDialog(
        aTitle: CharSequence?,
        aDefaultPathAndFile: CharSequence?,
        aFilterPatterns: List<String>?,
        aSingleFilterDescription: CharSequence?,
        aAllowMultipleSelects: Boolean
    ): String? {
        val stack = MemoryStack.stackGet()
        val stackPointer = stack.pointer
        try {
            return TinyFileDialogs.tinyfd_openFileDialog(
                aTitle,
                aDefaultPathAndFile,
                aFilterPatterns?.let { patterns ->
                    stack.pointers(*patterns.map {
                        stack.nUTF8Safe(it, true)
                        stack.pointerAddress
                    }.toLongArray())
                },
                aSingleFilterDescription,
                aAllowMultipleSelects,
            )
        } finally {
            stack.pointer = stackPointer
        }
    }
}