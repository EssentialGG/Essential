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

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UMatrixStack

/**
 * Lazily initializes the inner scope by first only placing a [box] as described by [modifier] without any children and
 * only initializing the inner scope once that box has been rendered once.
 *
 * This should be a last reserve for initializing a large list of poorly optimized components, not a common shortcut to
 * "make it not lag". Properly profiling and fixing initialization performance issues should always be preferred.
 */
fun LayoutScope.lazyBox(modifier: Modifier = Modifier.fillParent(), block: LayoutScope.() -> Unit) {
    val initialized = BasicState(false)
    box(modifier) {
        if_(initialized, cache = false /** don't need it; once initialized, we are never going back */) {
            block()
        } `else` {
            LazyComponent(initialized)(Modifier.fillParent())
        }
    }
}

private class LazyComponent(private val initialized: State<Boolean>) : UIContainer() {
    override fun draw(matrixStack: UMatrixStack) {
        super.draw(matrixStack)

        Window.enqueueRenderOperation {
            initialized.set(true)
        }
    }
}
