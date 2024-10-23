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
package gg.essential.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * A dispatcher which will queue all tasks to run when [runTasks] is called.
 *
 * After [shutdown], when [runTasks] will no longer be called, it'll instead dispatch all tasks to [Dispatchers.IO]
 * with parallelism limited to 1.
 */
class SingleThreadDispatcher(private val name: String) : CoroutineDispatcher() {
    /**
     * Lock guarding all access to [queue] and [shutdownDelegate].
     * Note: This lock MUST NOT be held while executing task, only for polling the queue, because that would block
     *       other threads from submitting tasks for that entire duration.
     */
    private val lock = Any()
    private val queue = ArrayDeque<Runnable>()
    private var shutdownDelegate: CoroutineDispatcher? = null

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(lock) {
            val shutdownDelegate = shutdownDelegate
            if (shutdownDelegate == null) {
                queue.addLast(block)
            } else {
                shutdownDelegate.dispatch(context, block)
            }
        }
    }

    fun runTasks() {
        while (true) {
            val task = synchronized(lock) {
                check(shutdownDelegate == null) { "Dispatcher has been shut down" }
                queue.removeFirstOrNull()
            } ?: break
            task.run()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class) // FIXME becomes stable with an extra arg in 1.9
    fun shutdown() {
        while (true) {
            runTasks()
            synchronized(lock) {
                // We must only switch to the delegate if there are no more tasks queued at this point; someone could
                // have queued another one between this synchronized block and the one in runTasks.
                if (queue.isEmpty()) {
                    shutdownDelegate = Dispatchers.IO.limitedParallelism(1)
                    return
                }
            }
        }
    }

    override fun toString(): String {
        return name
    }
}
