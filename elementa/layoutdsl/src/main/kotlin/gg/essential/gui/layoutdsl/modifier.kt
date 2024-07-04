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
package gg.essential.gui.layoutdsl

import gg.essential.elementa.UIComponent

interface Modifier {
    /**
     * Applies this modifier to the given component, and returns a function which can be called to undo the applied changes.
     */
    fun applyToComponent(component: UIComponent): () -> Unit

    infix fun then(other: Modifier) = if (other === Modifier) this else CombinedModifier(this, other)

    companion object : Modifier {
        override fun applyToComponent(component: UIComponent): () -> Unit = {}

        override infix fun then(other: Modifier) = other
    }
}

private class CombinedModifier(
    private val first: Modifier,
    private val second: Modifier
) : Modifier {
    override fun applyToComponent(component: UIComponent): () -> Unit {
        val undoFirst = first.applyToComponent(component)
        val undoSecond = second.applyToComponent(component)
        return {
            undoSecond()
            undoFirst()
        }
    }
}
