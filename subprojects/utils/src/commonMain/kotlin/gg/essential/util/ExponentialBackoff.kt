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

import kotlin.time.Duration

class ExponentialBackoff(
    private val start: Duration,
    private val max: Duration,
    private val base: Double,
) {
    private var delay = Duration.ZERO

    /**
     * Should be called after a failed attempt.
     * Will return the time to wait before the next attempt, and increase it for future calls.
     */
    fun increment(): Duration {
        return delay.also {
            delay = if (delay < start) start else delay * base
            if (delay > max) delay = max
        }
    }

    /** Should be called on a successful attempt. Resets the delay for future failed attempts to zero. */
    fun reset() {
        delay = Duration.ZERO
    }
}
