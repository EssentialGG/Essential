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

import gg.essential.ice.DatagramPacket
import gg.essential.slf4j.withKeyValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.net.DatagramPacket
import java.net.InetSocketAddress
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class StunRequest(
    parentLogger: Logger,
    private val coroutineScope: CoroutineScope,
    private val send: suspend (DatagramPacket) -> Boolean,
    private val destination: InetSocketAddress,
    val request: StunMessage,
) {
    private val logger = parentLogger
        .withKeyValue("remoteAddress", destination)
        .withKeyValue("tId", request.transactionId)
    private val deferredResponse = CompletableDeferred<UdpStunPacket?>(parent = coroutineScope.coroutineContext.job)

    private var tries = 0
    private var retransmissionTimeout = INITIAL_RETRANSMISSION_TIMEOUT
    private val transmitTimes = mutableListOf<ComparableTimeMark>()

    private lateinit var job: Job

    fun start(): Job {
        logger.trace("Begin STUN request: {}", request)

        // Starting UNDISPATCHED, so the packet is sent (or at least queued to be sent) before the method returns,
        // so we can guarantee a more optimal ordering of packets.
        job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            while (tries < MAX_TRIES) {
                tries++
                if (!send()) {
                    logger.trace("Failed to send packet, aborting")
                    deferredResponse.complete(null)
                    return@launch
                }
                delay(retransmissionTimeout)
                retransmissionTimeout *= 2
            }
            logger.trace("No response after {} tries, giving up.", tries)
            deferredResponse.complete(null)
        }
        return job
    }

    private suspend fun send(): Boolean {
        logger.trace("Sending STUN request, attempt {}/{}", tries, MAX_TRIES)

        var request = request
        if (request.attribute<StunAttribute.TransactionTransmitCounter>() != null) {
            transmitTimes.add(TimeSource.Monotonic.markNow())
            val filledAttr = StunAttribute.TransactionTransmitCounter(transmitTimes.size, 0)
            request = request.copy(attributes = request.attributes.map { attr ->
                if (attr is StunAttribute.TransactionTransmitCounter) filledAttr else attr
            })
        }

        return send(DatagramPacket(request.encode(), destination))
    }

    fun cancel() {
        job.cancel()
    }

    fun onResponse(response: UdpStunPacket) {
        logger.trace("Received response: {}", response.message)

        deferredResponse.complete(response)
        job.cancel()
    }

    suspend fun await(): UdpStunPacket? = deferredResponse.await()

    fun getRoundTripTime(response: UdpStunPacket): Duration {
        val attr = response.message.attribute<StunAttribute.TransactionTransmitCounter>()
        if (attr == null) {
            logger.warn("Response did not include TRANSACTION-TRANSMIT-COUNTER: {}", response)
            return transmitTimes.first().elapsedNow()
        }
        val sentTime = transmitTimes.getOrNull(attr.request - 1)
        if (sentTime == null) {
            logger.warn("Response contained invalid TRANSACTION-TRANSMIT-COUNTER: {}", response)
            return transmitTimes.first().elapsedNow()
        }
        val recvTime = response.timestamp
        return recvTime - sentTime
    }

    companion object {
        private const val MAX_TRIES = 7
        private val INITIAL_RETRANSMISSION_TIMEOUT = 500.milliseconds
    }
}
