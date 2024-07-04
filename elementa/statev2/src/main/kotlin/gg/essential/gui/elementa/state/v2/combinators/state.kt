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
package gg.essential.gui.elementa.state.v2.combinators

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo

/** Maps this state into a new state */
fun <T, U> State<T>.map(mapper: (T) -> U): State<U> {
    return memo { mapper(get()) }
}

/** Maps this mutable state into a new mutable state. */
fun <T, U> MutableState<T>.bimap(map: (T) -> U, unmap: (U) -> T): MutableState<U> {
    return object : MutableState<U>, State<U> by this.map(map) {
        override fun set(mapper: (U) -> U) {
            this@bimap.set { unmap(mapper(map(it))) }
        }
    }
}

/** Zips this state with another state */
fun <T, U> State<T>.zip(other: State<U>): State<Pair<T, U>> = zip(other, ::Pair)

/** Zips this state with another state using [mapper] */
fun <T, U, V> State<T>.zip(other: State<U>, mapper: (T, U) -> V): State<V> {
    return memo { mapper(this@zip(), other()) }
}
