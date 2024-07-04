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

import java.util.Observable

/**
 * A simple ObservableMap wrapping [wrapped]. Only supports observing events for the following calls
 * 1. [put]
 * 2. [remove]
 * 3. [clear]
 */
class SimpleObservableMap<K, V: Any>(private val wrapped: MutableMap<K, V>) : MutableMap<K, V> by wrapped, Observable() {

    private fun update(event: ObservableMapEvent<K, V>) {
        setChanged()
        notifyObservers(event)
    }

    override fun clear() {
        wrapped.clear()
        update(ObservableMapEvent.Clear())
    }

    override fun put(key: K, value: V): V? {
        return wrapped.put(key, value).also {
            // Check if this actually did anything
            if (it !== value) {
                if (it != null) {
                    update(ObservableMapEvent.Remove(key to it))
                }
                update(ObservableMapEvent.Add(key to value))
            }
        }

    }

    override fun remove(key: K): V? {
        return wrapped.remove(key).also {
            if (it != null) {
                update(ObservableMapEvent.Remove(key to it))
            }
        }
    }
}
sealed class ObservableMapEvent<K, V> {

    class Add<K, V>(val element: Pair<K, V>) : ObservableMapEvent<K, V>()

    class Remove<K, V>(val element: Pair<K, V>) : ObservableMapEvent<K, V>()

    class Clear<K, V> : ObservableMapEvent<K, V>()

}
