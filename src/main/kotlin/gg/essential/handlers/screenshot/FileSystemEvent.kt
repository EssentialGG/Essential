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
package gg.essential.handlers.screenshot

import gg.essential.handlers.io.FileEventType
import gg.essential.handlers.io.FileSystemEvent

/**
 * Removes redundant screenshot folder operations.
 *
 * For example, create delete create will only have the last create processed
 * and delete, create, modify, delete will only have the last delete processed.
 */
fun List<FileSystemEvent>.filterRedundancy(): List<FileSystemEvent> =
    groupBy { it.path }.flatMap { (path, events) ->
        val first = events.first().eventType
        val last = events.last().eventType
        when {
            first == FileEventType.CREATE && last == FileEventType.DELETE -> emptyList()
            first == FileEventType.CREATE -> listOf(FileEventType.CREATE)
            last == FileEventType.DELETE -> listOf(FileEventType.DELETE)
            else -> listOf(FileEventType.DELETE, FileEventType.CREATE)
        }.map { FileSystemEvent(path, it) }
    }
