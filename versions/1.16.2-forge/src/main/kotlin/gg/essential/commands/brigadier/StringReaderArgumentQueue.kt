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
package gg.essential.commands.brigadier

import com.mojang.brigadier.StringReader
import gg.essential.commands.api.WhitespaceSensitiveArgumentQueue

class StringReaderArgumentQueue(private val reader: StringReader) : WhitespaceSensitiveArgumentQueue {
    override fun poll(): String {
        return readRaw()
    }

    override fun peek(): String? {
        val cursor = reader.cursor
        return readRaw().ifEmpty { null }.also { reader.cursor = cursor }
    }

    override fun isEmpty(): Boolean {
        return peek() == null
    }

    override fun markEndOfArgument() {
        // Brigadier uses the whitespace to decide when an argument ends, so we need to roll back.
        if (reader.read.endsWith(' ')) {
            reader.cursor--
        }
    }

    private fun readRaw(): String {
        return buildString {
            while (reader.canRead()) {
                val char = reader.read()
                if (char == ' ') break
                append(char)
            }
        }
    }
}