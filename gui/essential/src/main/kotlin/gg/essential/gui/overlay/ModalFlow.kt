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
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.overlay.ModalFlow.ModalContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

/**
 * Manages a dynamic sequence of modals.
 * @see launchModalFlow
 */
class ModalFlow(val modalManager: ModalManager) {
    var replacePreviousModalWith: CompletableDeferred<Modal?> = CompletableDeferred(null)

    suspend fun <T> awaitModal(block: (continuation: ModalContinuation<T>) -> Modal): T {
        val (deferred, result) = suspendCancellableCoroutine { continuation ->
            val modal = block(ModalContinuation(modalManager, continuation))
            continuation.invokeOnCancellation {
                modal.close()
            }
            replacePreviousModalWith.complete(modal)
        }
        replacePreviousModalWith = deferred
        return result
    }

    class ModalContinuation<T>(
        private val modalManager: ModalManager,
        private val coroutineContinuation: Continuation<Pair<CompletableDeferred<Modal?>, T>>,
    ) {
        /**
         * Resumes the [ModalFlow] coroutine and suspends until the next modal is queued via [awaitModal].
         *
         * If you cannot suspend, you may instead use [resumeImmediately] which will return an empty temporary [Modal]
         * immediately and then later replace it with the real one once that has been determined.
         */
        suspend fun resume(result: T): Modal? {
            val job = CompletableDeferred<Modal?>(parent = coroutineContext.job)
            coroutineContinuation.resume(Pair(job, result))
            return job.await()
        }

        fun resumeImmediately(result: T): Modal {
            return object : Modal(modalManager) {
                override fun onOpen() {
                    super.onOpen()

                    coroutineScope.launch {
                        replaceWith(resume(result))
                    }
                }
                override fun LayoutScope.layoutModal() {}
                override fun handleEscapeKeyPress() {}
            }
        }
    }
}

