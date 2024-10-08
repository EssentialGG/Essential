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

import gg.essential.gui.common.modal.Modal
import kotlinx.coroutines.CoroutineScope

/**
 * Queues [Modal]s to be displayed on a [Layer].
 */
interface ModalManager {
    /**
     * Coroutine scope which is cancelled once the last modal in the queue is closed and no followup modals are queued.
     *
     * Beware that if no modal is ever opened, this scope will never be cancelled.
     */
    val coroutineScope: CoroutineScope

    /**
     * True for the short amount of time where the modal is already visible but still fading in.
     * Modals should ideally ignore user input during this period, so they're not inadvertently dismissed before
     * even being properly visible.
     */
    val isCurrentlyFadingIn: Boolean

    /**
     * Queues the given modal to be displayed after existing modals (or immediately if there are no active modals).
     */
    fun queueModal(modal: Modal)

    /**
     * Attempts to push the next modal on the queue onto the layer.
     * This should NOT be called by anything other than a [Modal], or the [ModalManager] itself.
     */
    fun modalClosed()
}