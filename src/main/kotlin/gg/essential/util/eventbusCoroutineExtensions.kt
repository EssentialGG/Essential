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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Waits for and returns the next event of the appropriate type. */
suspend inline fun <reified T> EventBus.await(priority: Int = 0): T = await(T::class.java, priority)

/** Waits for and returns the next event of the appropriate type. */
suspend fun <T> EventBus.await(cls: Class<T>, priority: Int = 0): T {
    return suspendCancellableCoroutine { continuation ->
        lateinit var listener: (T) -> Unit
        listener = { event ->
            unregister(cls, listener)
            continuation.resume(event)
        }
        register(cls, listener, priority)
        continuation.invokeOnCancellation { unregister(cls, listener) }
    }
}
