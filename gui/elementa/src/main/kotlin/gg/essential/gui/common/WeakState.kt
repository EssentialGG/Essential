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
package gg.essential.gui.common

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.*
import java.util.function.Consumer

/**
 * A state which allows subscribing to an inner state without creating a strong reference from it.
 *
 * This allows short-lived states (e.g. elements in a dynamic list) to subscribe to longer-lived states (e.g. stored in
 * the screen or globally) while still being garbage-collectible, even before the longer-lived state is not.
 *
 * Note that the other way round, this is not true. This instance does keep a strong reference to its inner state, so
 * it can safely depend on other weak states.
 */
@Deprecated("Using StateV1 is discouraged, use StateV2 instead which is weak by default")
class WeakState<T>(private val inner: State<T>) : BasicState<T>(inner.get()) {
    init {
        // Get the reference queue for the given inner state, so we can clean up any stale listeners registered on it
        val referenceQueue = referenceQueues.getOrPut(inner, ::ReferenceQueue)
        while (true) {
            val reference = referenceQueue.poll() ?: break
            (reference as WeakListener<*>).unregister()
        }

        // Create a listener which only carries a weak reference to this state
        val listener = WeakListener(this, referenceQueue)
        // and register it on the inner state
        listener.unregister = inner.onSetValue(listener)
    }

    override fun get(): T = inner.get()

    /**
     * A Consumer which forwards calls to the given state while that is still alive. The listener itself only keeps a
     * weak reference to the outer state, and as such does not keep it alive just by being registered.
     */
    private class WeakListener<T>(
        weakState: WeakState<T>,
        referenceQueue: ReferenceQueue<State<*>>,
    ) : WeakReference<State<T>>(weakState, referenceQueue), Consumer<T> {
        lateinit var unregister: () -> Unit

        override fun accept(value: T) {
            get()?.set(value)
        }
    }

    private companion object {
        /**
         * To clean up stale listeners from the inner state's listeners list, we maintain a reference queue for each
         * inner state.
         * Whenever we get asked to add a new listener to an inner state, we can then poll the queue for stale
         * references and clean them up. This way, stale references cannot accumulate on inner states.
         * Ideally this would happen in the onSetValue method of the inner state but since we can't change that, a weak
         * hash map is the next best thing.
         *
         * We need to keep one reference queue per inner state because the unregister method is not thread safe and the
         * safe thread to call it may be different for each inner state.
         */
        val referenceQueues = WeakHashMap<State<*>, ReferenceQueue<State<*>>>()
    }
}
