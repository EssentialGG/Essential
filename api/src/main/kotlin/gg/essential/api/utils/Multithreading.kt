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
package gg.essential.api.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility for running functions asynchronously.
 */
object Multithreading {
    private val counter = AtomicInteger(0)

    @JvmStatic
    val scheduledPool: ScheduledExecutorService = Executors.newScheduledThreadPool(10) { target: Runnable? ->
        Thread(target, "Thread " + counter.incrementAndGet())
    }

    @JvmStatic
    val pool: ThreadPoolExecutor
        get() = POOL

    var POOL = ThreadPoolExecutor(
        10, 30,
        0L, TimeUnit.SECONDS,
        LinkedBlockingQueue()
    ) { target: Runnable? -> Thread(target, "Thread ${counter.incrementAndGet()}") }

    fun schedule(r: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledPool.scheduleAtFixedRate(r, initialDelay, delay, unit)
    }

    @JvmStatic
    fun schedule(r: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        return scheduledPool.schedule(r, delay, unit)
    }

    @JvmStatic
    fun runAsync(runnable: Runnable) {
        POOL.execute(runnable)
    }

    fun submit(runnable: Runnable): Future<*> {
        return POOL.submit(runnable)
    }
}