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

import gg.essential.gui.elementa.state.v2.collections.*

typealias SetState<T> = State<TrackedSet<T>>
typealias MutableSetState<T> = MutableState<MutableTrackedSet<T>>

fun <T> State<Set<T>>.toSetState(): SetState<T> {
    var oldSet = MutableTrackedSet<T>()
    return memo {
        val newSet = get()
        oldSet.applyChanges(TrackedSet.Change.estimate(oldSet, newSet)).also { oldSet = it }
    }
}

fun <T, U> SetState<T>.mapChanges(init: (TrackedSet<T>) -> U, update: (old: U, changes: Sequence<TrackedSet.Change<T>>) -> U): State<U> {
    var trackedSet: TrackedSet<T>? = null
    var trackedValue: U? = null
    return memo {
        val newSet = get()
        val oldSet = trackedSet
        val newValue =
            if (oldSet == null) {
                init(newSet)
            } else {
                @Suppress("UNCHECKED_CAST")
                update(trackedValue as U, newSet.getChangesSince(oldSet))
            }

        trackedSet = newSet
        trackedValue = newValue

        newValue
    }
}

fun <T, U> SetState<T>.mapChange(init: (TrackedSet<T>) -> U, update: (old: U, change: TrackedSet.Change<T>) -> U): State<U> =
    mapChanges(init) { old, changes -> changes.fold(old, update) }

fun <T> mutableSetState(vararg elements: T): MutableSetState<T> =
    mutableStateOf(MutableTrackedSet(mutableSetOf(*elements)))

fun <T> MutableSetState<T>.add(element: T) = set { it.add(element) }
fun <T> MutableSetState<T>.addAll(toAdd: Collection<T>) = set { it.addAll(toAdd) }
fun <T> MutableSetState<T>.setAll(newSet: Set<T>) = set { it.applyChanges(TrackedSet.Change.estimate(it, newSet)) }
fun <T> MutableSetState<T>.remove(element: T) = set { it.remove(element) }
fun <T> MutableSetState<T>.clear() = set { it.clear() }
