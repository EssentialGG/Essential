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
package gg.essential.commands.api

import gg.essential.api.commands.ArgumentQueue
import java.util.*

class ArgumentQueueImpl(private val backingDeque: Deque<String>) : ArgumentQueue {
    private val changed = mutableListOf<String>()

    override fun poll(): String {
        changed.add(peek()!!)
        return backingDeque.pollFirst()
    }

    override fun peek(): String? {
        return backingDeque.peekFirst()
    }

    override fun isEmpty() = backingDeque.isEmpty()

    internal fun undo() {
        changed.forEach {
            backingDeque.addFirst(it)
        }

        sync()
    }

    internal fun sync() {
        changed.clear()
    }
}
