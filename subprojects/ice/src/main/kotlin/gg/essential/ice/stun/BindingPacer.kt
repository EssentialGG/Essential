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
package gg.essential.ice.stun

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Hands out transmit slots at regular intervals.
 *
 * This is necessary because some NATs may not allow too many new bindings to be opened in too short of a time, so we
 * need to space out our requests to avoid hitting those limits.
 * See [RFC 8445 Appendix B.1](https://www.rfc-editor.org/rfc/rfc8445#appendix-B.1).
 *
 * However, unlike the RFC, we implement some slightly more complex rules than just a single fixed-rate timer:
 * - We prioritize requests sent to STUN/TURN servers so we can discover all candidates asap and don't end up stalling
 *   because we need to wait for their responses. Most of our users will need to know their public address before they
 *   have any chance of connecting, so prioritizing that make sense for us.
 * - We use the default 50ms interval because Ice4J does too, and we need to stay in sync with it for hole punching
 *   to be most effective.
 * - For requests sent to STUN/TURN servers however we use the lowest interval the RFC allows (5ms), so we can get
 *   all those out asap. Since these do not affect the other client, we should be free to choose a different value here.
 *   Bandwidth should also not be a concern because these packets are only on the order of 100 bytes and we have a
 *   fairly limited number of servers we need to contact.
 */
class BindingPacer(
    private val coroutineScope: CoroutineScope,
    private val interval: Duration = 50.milliseconds,
) {
    private val highPrioRequests = Channel<CompletableDeferred<Unit>>(Channel.UNLIMITED)
    private val regularRequests = Channel<CompletableDeferred<Unit>>(Channel.UNLIMITED)

    init {
        coroutineScope.launch(Dispatchers.Unconfined) {
            while (true) {
                val (request, delay) = select {
                    highPrioRequests.onReceive { it to MIN_INTERVAL }
                    regularRequests.onReceive { it to interval }
                }

                if (!request.isCompleted) {
                    request.complete(Unit)
                    delay(delay)
                }
            }
        }
    }

    suspend fun await(highPrio: Boolean) {
        val queue = if (highPrio) highPrioRequests else regularRequests

        val request = CompletableDeferred<Unit>(parent = coroutineScope.coroutineContext.job)
        try {
            queue.send(request)
            request.await()
        } finally {
            // If we are cancelled while waiting, complete our request ourselves, so we don't waste a slot for nothing
            request.complete(Unit)
        }
    }

    companion object {
        private val MIN_INTERVAL = 5.milliseconds // as per RFC
    }
}