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
package gg.essential.gui.elementa.state.v2

import gg.essential.elementa.state.v2.ReferenceHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Waits until this [State] has a value which [equals] the given [value]. */
suspend fun <T> State<T>.awaitValue(value: T): T = await { it == value }

/** Waits until this [State] has a value for which [accept] returns `true` and returns that value. */
suspend fun <T> State<T>.await(accept: (T) -> Boolean): T {
    // Fast-path
    get().let { if (accept(it)) return it }

    // Slow path
    return suspendCancellableCoroutine { continuation ->
        lateinit var unregister: () -> Unit
        var listener: ((T) -> Unit)?
        listener = { value ->
            if (accept(value)) {
                unregister()
                continuation.resume(value)
            }
        }
        unregister = onSetValue(ReferenceHolder.Weak, listener)
        listener(get())
        continuation.invokeOnCancellation {
            // Note: we cannot call `unregister` here because `invokeOnCancellation` makes no guarantee about which
            // thread we run on, and `unregister` isn't thread safe.
            // So we'll instead merely drop our reference to the listener and leave it to State's weakness properties
            // to clean up the registration.
            // This does mean our callback will continue to be invoked, but `CancellableCoroutine` is fine with that
            // because cancellation may race with `resume` in pretty much any code.
            listener = null
        }
    }
}
