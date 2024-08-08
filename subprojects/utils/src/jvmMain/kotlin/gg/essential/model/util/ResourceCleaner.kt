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
package gg.essential.model.util

import java.io.Closeable
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ResourceCleaner<T> {
    private val referenceQueue = ReferenceQueue<T>()
    private val toBeCleanedUp: MutableSet<Resource> = Collections.newSetFromMap(ConcurrentHashMap())

    fun register(owner: T, cleanup: Runnable) {
        toBeCleanedUp.add(Resource(owner, cleanup))
    }

    fun runCleanups() {
        while (true) {
            ((referenceQueue.poll() ?: break) as Resource).close()
        }
    }

    private inner class Resource(owner: T, cleanup: Runnable) : PhantomReference<T>(owner, referenceQueue), Closeable {
        var cleanup: Runnable? = cleanup

        override fun close() {
            toBeCleanedUp.remove(this)

            cleanup?.run()
            cleanup = null
        }
    }
}