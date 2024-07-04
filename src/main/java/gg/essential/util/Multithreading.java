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
package gg.essential.util;

import net.minecraft.client.Minecraft;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static gg.essential.util.ExtensionsKt.getExecutor;


public class Multithreading {
    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final ScheduledExecutorService RUNNABLE_POOL = Executors.newScheduledThreadPool(10, r ->
        new Thread(r, "Essential Thread " + counter.incrementAndGet()));

    public static ThreadPoolExecutor POOL = new ThreadPoolExecutor(10, 30,
            0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "Essential Thread " + counter.incrementAndGet()));

    /**
     * @deprecated This method executes the passed runnable on a background thread.
     * Most of the time however, you want {@link #scheduleOnMainThread} instead.
     * If you really do want a background thread, use {@link #scheduleOnBackgroundThread}.
     * Or use Kotlin Coroutines, which makes thread selection correct by default, instead.
     */
    @Deprecated
    public static ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
        return scheduleOnBackgroundThread(r, delay, unit);
    }

    public static ScheduledFuture<?> scheduleOnBackgroundThread(Runnable r, long delay, TimeUnit unit) {
        return RUNNABLE_POOL.schedule(r, delay, unit);
    }

    public static ScheduledFuture<?> scheduleOnMainThread(Runnable r, long delay, TimeUnit unit) {
        return RUNNABLE_POOL.schedule(() -> getExecutor(Minecraft.getMinecraft()).execute(r), delay, unit);
    }

    public static void runAsync(Runnable runnable) {
        POOL.execute(runnable);
    }

    public static Future<?> submit(Runnable runnable) {
        return POOL.submit(runnable);
    }

    public static ThreadPoolExecutor pool = POOL;
    public static ThreadPoolExecutor getPool() {
        return pool;
    }

    public static final ScheduledExecutorService scheduledPool = RUNNABLE_POOL;
    public static ScheduledExecutorService getScheduledPool() {
        return scheduledPool;
    }
}


