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

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

/**
 * An executor that allows for at most [parallelism] tasks to run at any one time.
 * Surplus tasks are buffered in a priority queue and processed in their natural order as active tasks complete.
 */
class LimitedExecutor(
    private val delegate: Executor,
    private val parallelism: Int,
    private val workQueue: Queue<Runnable> = ConcurrentLinkedQueue(),
) : Executor, Runnable {
    private val lock = Object()
    private var activeWorkers: Int = 0

    override fun execute(work: Runnable) {
        workQueue.offer(work)

        synchronized(lock) {
            if (activeWorkers < parallelism) {
                activeWorkers++
                delegate.execute(this)
            }
        }
    }

    override fun run() {
        try {
            while (true) {
                val work = workQueue.poll() ?: break
                work.run()
            }
        } finally {
            val moreWork = synchronized(lock) {
                if (!workQueue.isEmpty()) {
                    true
                } else {
                    activeWorkers--
                    false
                }
            }
            if (moreWork) {
                delegate.execute(this)
            }
        }
    }
}