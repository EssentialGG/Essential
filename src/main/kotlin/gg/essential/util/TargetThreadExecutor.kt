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

import gg.essential.Essential
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

/**
 * Executor which runs tasks only on the thread which it was constructed on.
 */
class TargetThreadExecutor : Executor {
    private val targetThread = Thread.currentThread()
    private val queue: Queue<Runnable> = ConcurrentLinkedQueue()

    override fun execute(task: Runnable) {
        if (Thread.currentThread() == targetThread) {
            task.run()
        } else {
            queue.offer(task)
        }
    }

    fun run() {
        loop {
            val task = queue.poll() ?: return
            try {
                task.run()
            } catch (e: Exception) {
                Essential.logger.fatal("Error executing task", e)
            }
        }
    }
}
