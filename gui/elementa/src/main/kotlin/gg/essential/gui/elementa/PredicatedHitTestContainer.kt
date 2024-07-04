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
package gg.essential.gui.elementa

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer

class PredicatedHitTestContainer : UIContainer() {
    var shouldIgnore: (UIComponent) -> Boolean = { false }

    override fun hitTest(x: Float, y: Float): UIComponent {
        for (i in children.lastIndex downTo 0) {
            val child = children[i]

            if (child.isPointInside(x, y)) {
                val value = child.hitTest(x, y)

                if (shouldIgnore(value)) {
                    continue
                }

                return value
            }
        }

        return this
    }
}