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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.UIConstraints
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.effect
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.ObservableAddEvent
import gg.essential.elementa.utils.ObservableClearEvent
import gg.essential.elementa.utils.ObservableList
import gg.essential.elementa.utils.ObservableRemoveEvent
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.util.hasWindow
import kotlin.reflect.KProperty

@Deprecated("Using StateV1 is discouraged, use StateV2 instead")
fun <T> State<T>.weak() = WeakState(this)

fun <T : UIComponent, S> T.bindConstraints(state: State<S>, config: UIConstraints.(S) -> Unit) = apply {
    state.onSetValueAndNow {
        constraints.config(it)
    }
}

fun <T : UIComponent, S> T.bindConstraints(state: gg.essential.gui.elementa.state.v2.State<S>, config: UIConstraints.(S) -> Unit) = apply {
    constraints.config(state.get())
    state.onSetValue(this) {
        constraints.config(it)
    }
}

fun <T : UIComponent> T.bindParent(
    parent: UIComponent,
    state: State<Boolean>,
    delayed: Boolean = false,
    index: Int? = null
) =
    bindParent(state.map {
        if (it) parent else null
    }, delayed, index)

fun <T : UIComponent> T.bindParent(
    parent: UIComponent,
    state: gg.essential.gui.elementa.state.v2.State<Boolean>,
    delayed: Boolean = false,
    index: Int? = null
) = bindParent(parent, state.toV1(parent), delayed, index)

fun <T : UIComponent> T.bindFloating(state: State<Boolean>) = apply {
    state.onSetValueAndNow {
        if (hasWindow) {
            this.setFloating(it)
        }
    }
}

fun <T : UIComponent> T.bindEffect(effect: Effect, state: State<Boolean>, delayed: Boolean = true) = apply {
    state.onSetValueAndNow {
        val update = {
            if (it) {
                this.effect(effect)
            } else {
                this.removeEffect(effect)
            }
        }
        if (delayed) {
            Window.enqueueRenderOperation {
                update()
            }
        } else {
            update()
        }
    }
}

fun <T : UIComponent> T.bindEffect(effect: Effect, state: gg.essential.gui.elementa.state.v2.State<Boolean>, delayed: Boolean = true) =
    bindEffect(effect, state.toV1(this), delayed)

fun <T : UIComponent> T.bindParent(state: State<UIComponent?>, delayed: Boolean = false, index: Int? = null) = apply {
    state.onSetValueAndNow { parent ->
        val handleStateUpdate = {
            if (this.hasParent && this.parent != parent) {
                this.parent.removeChild(this)
            }
            if (parent != null && this !in parent.children) {
                if (index != null) {
                    parent.insertChildAt(this, index)
                } else {
                    parent.addChild(this)
                }
            }
        }
        if (delayed) {
            Window.enqueueRenderOperation {
                handleStateUpdate()
            }
        } else {
            handleStateUpdate()
        }
    }
}

fun State<String>.empty() = map { it.isBlank() }
infix fun State<Boolean>.and(other: State<Boolean>) = zip(other).map { (a, b) -> a && b }
infix fun State<Boolean>.or(other: State<Boolean>) = zip(other).map { (a, b) -> a || b }

class ReadOnlyState<T>(private val internalState: State<T>) : State<T>() {

    init {
        internalState.onSetValueAndNow {
            super.set(it)
        }
    }

    override fun get(): T {
        return internalState.get()
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This state is read-only", level = DeprecationLevel.ERROR)
    override fun set(value: T) {
        throw IllegalStateException("Cannot set read only value")
    }
}

operator fun <T> State<T>.getValue(obj: Any, property: KProperty<*>): T = get()
operator fun <T> State<T>.setValue(obj: Any, property: KProperty<*>, value: T) = set(value)

fun <T> State<T>.mapToString() = this.map { it.toString() }

fun <T> T.state() = BasicState(this)

fun <T> State<T>.layoutSafe(): State<T> {
    val safeState = BasicState(get())
    onSetValue { Window.enqueueRenderOperation { safeState.set(it) } }
    return safeState
}


fun <T : UIComponent, E> T.bindChildren(
    list: ObservableList<E>,
    filter: (E) -> Boolean = { true },
    comparator: Comparator<UIComponent>? = null,
    mapper: (E) -> UIComponent,
): T {

    val components = mutableListOf<UIComponent?>()

    fun sort() {
        if (comparator != null) {
            if (this is ScrollComponent) {
                this.sortChildren(comparator)
            } else {
                children.sortWith(comparator)
            }
        }
    }

    fun handleNewItem(item: E, index: Int, delayed: Boolean) {
        if (!filter(item)) {
            components.add(index, null)
            return
        }
        val element = mapper(item)
        components.add(index, element)

        val addAndSort = {
            addChild(element)
            sort()
        }
        if (delayed) {
            Window.enqueueRenderOperation(addAndSort)
        } else {
            addAndSort()
        }
    }

    fun removeItem(index: Int) {
        components.removeAt(index)?.let {
            Window.enqueueRenderOperation {
                removeChild(it)
                sort()
            }
        }
    }

    list.addObserver { _, arg ->
        when (arg) {
            is ObservableRemoveEvent<*> -> {
                removeItem(arg.element.index)
            }
            is ObservableAddEvent<*> -> {
                handleNewItem(arg.element.value as E, arg.element.index, true)
            }
            is ObservableClearEvent<*> -> {
                components.indices.reversed().forEach {
                    removeItem(it)
                }
            }
        }
    }
    list.forEachIndexed { index, e -> handleNewItem(e, index, false) }

    return this
}

/**
 * Maps an observable list to a new observable list
 * of type [V] using the given [mapper] function.
 *
 */
fun <E, V> ObservableList<E>.map(
    mapper: (E) -> V,
): ObservableList<V> {

    val result = ObservableList<V>(mutableListOf())

    addObserver { _, arg ->
        when (arg) {
            is ObservableRemoveEvent<*> -> {
                result.removeAt(arg.element.index)
            }

            is ObservableAddEvent<*> -> {
                result.add(arg.element.index, mapper(arg.element.value as E))
            }

            is ObservableClearEvent<*> -> {
                result.clear()
            }
        }
    }
    forEach {
        result.add(mapper(it))
    }

    return result
}
private val updateCount = { a: Int, b: Int -> (a + b).takeIf { it > 0 } }
