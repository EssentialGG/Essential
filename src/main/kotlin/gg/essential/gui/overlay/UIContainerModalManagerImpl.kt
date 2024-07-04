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
package gg.essential.gui.overlay

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percentOfWindow
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.defaultEssentialModalFadeTime
import gg.essential.gui.elementa.transitions.FadeInTransition
import java.awt.Color

/**
 * A [ModalManager] which is backed by a [UIContainer].
 */
class UIContainerModalManagerImpl(
    backgroundColor: Color = Color.BLACK.withAlpha(150)
) : UIContainer(), ModalManager {
    private val modalQueue = mutableListOf<Modal>()

    private val background by UIBlock(backgroundColor).constrain {
        x = 0.pixels
        y = 0.pixels
        width = 100.percentOfWindow()
        height = 100.percentOfWindow()
    } childOf this

    init {
        constrain {
            x = 0.pixels
            y = 0.pixels
            width = 100.percentOfWindow()
            height = 100.percentOfWindow()
        }
    }

    override fun queueModal(modal: Modal) {
        // We can't immediately show a modal if this component doesn't have a parent.
        if (hasParent && background.children.isEmpty()) {
            pushModalFromQueue()
            return
        }

        modalQueue.add(modal)
    }

    override fun modalClosed() {
        val pushedModal = pushModalFromQueue()
        if (pushedModal == null) {
            // If there are no modals left, we can remove the manager from its parent.
            parent.removeChild(this)
        }
    }

    override fun afterInitialization() {
        super.afterInitialization()

        // If a modal was queued before we had a parent, we need to make it a child now for it to be
        // displayed as expected.
        if (background.children.isEmpty()) {
            val modal = pushModalFromQueue() ?: return
            modal.isAnimating = true

            FadeInTransition(defaultEssentialModalFadeTime).transition(this) {
                modal.isAnimating = false
            }
        }
    }

    private fun pushModalFromQueue(): Modal? {
        val modal = modalQueue.removeFirstOrNull() ?: return null
        modal childOf background
        modal.onOpen()

        return modal
    }
}