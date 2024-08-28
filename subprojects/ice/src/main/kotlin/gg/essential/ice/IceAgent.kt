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
package gg.essential.ice

import gg.essential.ice.stun.StunManager
import gg.essential.ice.stun.StunAttribute
import gg.essential.ice.stun.StunClass
import gg.essential.ice.stun.StunMessage
import gg.essential.ice.stun.StunRequest
import gg.essential.ice.stun.StunSocket
import gg.essential.ice.stun.StunType
import gg.essential.ice.stun.TransactionId
import gg.essential.ice.stun.UdpStunPacket
import gg.essential.slf4j.with
import gg.essential.slf4j.withKeyValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.slf4j.Logger
import org.slf4j.spi.LoggingEventBuilder
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * This class implements an ICE server/client (commonly called "agent").
 *
 * Specifically a subset of
 *  - STUN (https://www.rfc-editor.org/rfc/rfc8489)
 *  - TURN (https://www.rfc-editor.org/rfc/rfc8656)
 *  - ICE (https://www.rfc-editor.org/rfc/rfc8445)
 *  - Trickle ICE (https://www.rfc-editor.org/rfc/rfc8838)
 *  - Round-Trip Time (https://www.rfc-editor.org/rfc/rfc7982)
 * as required for our use case, not being afraid to take various shortcuts for things we're really not interested in.
 *
 * Be sure to read at least Section 2 of the ICE RFC before trying to understand this.
 *
 * This class is responsible for creating candidate pairs, running connectivity checks for them, sending and receiving
 * application data once we have a valid pair, and finally picking one of the pairs to settle on ("nomination").
 *
 * The discovery of local candidates is left to [CandidateManager], which can be shared between multiple [IceAgent]s.
 * Low-level STUN request/response handling and pretty much all of TURN is left to [StunManager]/[StunSocket], which
 * similarly may be shared (allowing re-use of a single relay for multiple connections).
 */
class IceAgent(
    private val logger: Logger,
    parentScope: CoroutineScope, // must have Job and a concurrency-limited "main" dispatcher
    private val stunManager: StunManager,
    private val candidateManager: CandidateManager,
    private val controlling: Boolean,
    private val localCreds: Pair<String, ByteArray>,
    private val remoteCreds: Deferred<Pair<String, ByteArray>>,
) {
    private val job = Job(parentScope.coroutineContext.job)
    private val coroutineScope = parentScope + job
    /** Scope for connectivity checks. Cancelled once connection has been established and a pair has been nominated. */
    private val checksScope = coroutineScope + Job(job)

    val localCandidateChannel = Channel<LocalCandidate>(Channel.UNLIMITED)
    val remoteCandidateChannel = Channel<RemoteCandidate>(Channel.UNLIMITED)
    /** Completes once we are ready to send data. Should be used `withTimeout` as it may never complete if ICE fails. */
    val readyForData = CompletableDeferred<Unit>(parent = job)
    val inboundDataChannel = Channel<Pair<LocalCandidate, ByteArray>>(1000, BufferOverflow.DROP_OLDEST) { pair ->
        logger.warn("IceAgent.inboundDataChannel overflow, dropping packet of {} bytes", pair.second.size)
    }
    val outboundDataChannel = Channel<ByteArray>(1000, BufferOverflow.DROP_OLDEST) { packet ->
        logger.warn("IceAgent.outboundDataChannel overflow, dropping packet of {} bytes", packet.size)
    }

    // The old Ice4J ICE implementation will only try to establish a connection once a candidate has nominated, so when
    // talking to one, we need to hurry up with nomination, and we can't start sending data until we have one.
    // This value will be set based on the SOFTWARE attribute in check responses we receive. It will therefore always be
    // set before the first valid pair is found, which is the earliest we need it.
    private var remoteIsIce4J = CompletableDeferred<Boolean>()

    private var highVolumeLogging = true
    init {
        coroutineScope.launch {
            delay(2.minutes)
            logger.debug("Ending logging of high volume events.")
            highVolumeLogging = false
        }
    }

    private val localCandidates = mutableListOf<LocalCandidate>()
    private val remoteCandidates = mutableListOf<RemoteCandidate>()
    private val triggeredCheckQueue = mutableListOf<CandidatePair>()
    private val checklist = mutableListOf<CandidatePair>()
    private val validList = mutableListOf<CandidatePair>()
    private var selectedPair: CandidatePair? = null

    /**
     * Contains the pair on which the controlled client has last received data while [selectedPair] is not yet set.
     * Undefined for controlling client and after [selectedPair] has been set.
     */
    private var lastReceivedDataPair: CandidatePair? = null

    init {
        // Starting UNDISPATCHED so we immediately add any already-collected candidates before processing any packets
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            for (candidate in candidateManager.getCandidates(job)) {
                addLocalCandidate(candidate)
            }
        }

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            for (candidate in remoteCandidateChannel) {
                addRemoteCandidate(candidate)
            }
        }

        val stunServer = stunManager.registerServer(job, localCreds.first, localCreds.second)
        coroutineScope.launch {
            for ((packet, message) in stunServer.stunRequestReceiveChannel) {
                processStunRequest(ReceivedPacket(getLocalCandidate(packet) ?: continue, packet.source, packet.data), message)
            }
        }
        coroutineScope.launch {
            for (packet in stunServer.dataReceiveChannel) {
                processPacket(ReceivedPacket(getLocalCandidate(packet) ?: continue, packet.source, packet.data))
            }
        }

        checksScope.launch { performConnectivityChecks() }

        if (controlling) {
            coroutineScope.launch { waitForGoodCandidateAndThenNominateIt() }
        }

        coroutineScope.launch {
            for (packet in outboundDataChannel) {
                dispatchDataPacket(packet)
            }
            inboundDataChannel.close()
            job.cancel()
        }
    }

    private fun getLocalCandidate(packet: StunSocket.ReceivedPacket): LocalCandidate? {
        val relay = packet.relay
        return if (relay != null) {
            localCandidates.find { it.type == CandidateType.Relayed && it.relay == relay }
        } else {
            val socket = packet.socket
            localCandidates.find { it.type == CandidateType.Host && it.socket == socket }
        }
    }

    private fun addLocalCandidate(candidate: LocalCandidate) {
        if (selectedPair != null) {
            logger.debug("Ignoring new local candidate {} because we already have a selected pair.", candidate)
            candidate.close()
            return
        }

        for (other in localCandidates) {
            if (other.address == candidate.address && other.base == candidate.base) {
                logger.debug("Ignoring new local candidate {} because we already have {}", candidate, other)
                candidate.close()
                return
            }
        }

        logger.debug("Got new local candidate: {}", candidate)

        localCandidates.add(candidate)

        if (candidate.type != CandidateType.PeerReflexive) {
            localCandidateChannel.trySend(candidate)

            for (remoteCandidate in remoteCandidates) {
                tryPair(candidate, remoteCandidate)
            }
        }
    }

    private fun addRemoteCandidate(candidate: RemoteCandidate) {
        if (selectedPair != null) {
            logger.debug("Ignoring new remote candidate {} because we already have a selected pair.", candidate)
            return
        }

        for (other in remoteCandidates) {
            if (other.address == candidate.address) {
                logger.debug("Ignoring new remote candidate {} because we already have {}", candidate, other)
                if (other.type == CandidateType.PeerReflexive && candidate.isRelay) {
                    other.type = CandidateType.Relayed
                }
                return
            }
        }

        logger.debug("Got new remote candidate: {}", candidate)

        remoteCandidates.add(candidate)

        for (localCandidate in localCandidates) {
            tryPair(localCandidate, candidate)
        }
    }

    private fun tryPair(local: LocalCandidate, remote: RemoteCandidate): CandidatePair? {
        if (local.isIPv6 != remote.isIPv6) {
            return null
        }
        if (local.isLinkLocal != remote.isLinkLocal) {
            return null
        }
        // Not the case for a general ICE implementation, but we know that our TURN servers will never be able to
        // connect to private address space, so we can immediately skip those pairs.
        if (local.isRelay && remote.isSiteLocal || remote.isRelay && local.isSiteLocal) {
            return null
        }

        val newPair = CandidatePair(local, remote, controlling)

        for (oldPair in checklist) {
            if (newPair.local.base == oldPair.local.base && newPair.remote.address == oldPair.remote.address) {
                if (newPair.priority > oldPair.priority && oldPair.state == CandidatePair.State.Waiting) {
                    logger.trace("Replacing lower priority candidate pair {} with new pair", oldPair)
                    checklist.remove(oldPair)
                    break
                } else {
                    return oldPair
                }
            }
        }
        logger.trace("New candidate pair: {}", newPair)

        val index = checklist.binarySearch(newPair, compareBy { -it.priority })
        checklist.add(if (index >= 0) index else -index - 1, newPair)

        if (checklist.size > MAX_CHECKLIST_SIZE) {
            // Remove failed pairs first
            for (entry in checklist) {
                if (entry.state == CandidatePair.State.Failed) {
                    checklist.remove(entry)
                    break
                }
            }
            // If that didn't do it, discard the lowest priority one
            if (checklist.size > MAX_CHECKLIST_SIZE) {
                checklist.removeLast()
            }
        }

        local.relay?.createPermission(remote.address.address)

        return newPair
    }

    private suspend fun performConnectivityChecks() {
        // Need to wait for remote credentials to arrive before we can do connectivity checks.
        remoteCreds.await()

        while (true) {
            // Wait until it's our turn to transmit something
            stunManager.bindingPacer.await(false)

            fun pollTriggeredCheck(): CandidatePair? {
                while (triggeredCheckQueue.isNotEmpty()) {
                    return triggeredCheckQueue.removeFirst()
                        // Only if it hasn't been removed or completed yet
                        .takeIf { it in checklist && it.state != CandidatePair.State.Succeeded }
                        ?: continue
                }
                return null
            }

            // Prefer triggered checks because they have a huge chance of success
            val pair = pollTriggeredCheck()
                ?: checklist.find { it.state == CandidatePair.State.Waiting }

            if (pair == null) {
                performRTTChecks()
                continue
            }

            pair.state = CandidatePair.State.InProgress
            pair.check?.cancel()
            pair.check = checksScope.launch {
                checkPair(pair)
            }
        }
    }

    private suspend fun performRTTChecks() {
        val pair = validList.minByOrNull { it.extraRttChecks } ?: return

        // If we have a valid pair, we must have already received a successful response and should therefore be aware
        // of which software the remote uses.
        if (remoteIsIce4J.await()) {
            return // Ice4J might react badly to repeated Binding requests, let's just not risk it
        }

        pair.extraRttChecks++
        checksScope.launch {
            val tId = TransactionId.create()
            val logger = logger.withKeyValue("tId", tId)
            logger.trace("Starting rtt check: {}", pair)

            val (request, response) = sendIceBindingRequest(tId, pair)
            if (response == null) {
                logger.warn("RTT check of previously valid pair failed, no response: {}", pair)
                return@launch
            }

            if (response.message.cls == StunClass.ResponseError) {
                // We never send error responses, so any we receive are unexpected
                logger.warn("Failed, got unexpected error response: {}", response.message)
                return@launch
            }

            // Success!
            val rtt = request.getRoundTripTime(response)
            logger.trace("Measured RTT of {} to be {}ms", pair, rtt.inWholeMilliseconds)
            pair.rtt = min(pair.rtt ?: INFINITE, rtt)
        }
    }

    private suspend fun checkPair(pair: CandidatePair) {
        val tId = TransactionId.create()
        val logger = logger.withKeyValue("tId", tId)
        logger.debug("Starting connectivity check: {}", pair)

        val (request, response) = sendIceBindingRequest(tId, pair)
        if (response == null) {
            logger.debug("Connectivity check failed, no response: {}", pair)
            pair.state = CandidatePair.State.Failed
            return
        }

        // Check for non-symmetry (7.2.5.2.1.)
        if (pair.remote.address != response.source) {
            logger.debug("Failed, request destination ({}) does not match response source ({}).",
                pair.remote.address, response.source)
            pair.state = CandidatePair.State.Failed
            return
        }

        if (response.message.cls == StunClass.ResponseError) {
            // We never send error responses, so any we receive are unexpected
            logger.warn("Failed, got unexpected error response: {}", response.message)
            pair.state = CandidatePair.State.Failed
            return
        }

        val mappedAddress = response.message.attribute<StunAttribute.XorMappedAddress>()?.address
        if (mappedAddress == null) {
            logger.warn("Failed, response was missing XOR-MAPPED-ADDRESS: {}", response.message)
            pair.state = CandidatePair.State.Failed
            return
        }

        // Success!
        val rtt = request.getRoundTripTime(response)
        logger.debug("Connectivity check succeeded: {} ({}ms)", pair, rtt.inWholeMilliseconds)

        remoteIsIce4J.complete(response.message.attribute<StunAttribute.Software>()?.value == "ice4j.org")

        // Potentially discover peer reflexive candidate
        if (localCandidates.none { it.address == mappedAddress }) {
            addLocalCandidate(LocalPeerReflexiveCandidate(pair.local, mappedAddress))
        }

        // Find or construct valid pair
        var validPair: CandidatePair? = null
        if (pair.local.address == mappedAddress) {
            validPair = pair
        }
        if (validPair == null) {
            validPair = checklist.find { it.local.address == mappedAddress && it.remote.address == pair.remote.address }
        }
        if (validPair == null) {
            // a local candidate must exist here; if it didn't, we would have created a new peer-reflexive one above
            validPair = CandidatePair(localCandidates.first { it.address == mappedAddress }, pair.remote, controlling)
            // also add it to checklist so we can find it again later; it'll already be Succeeded, so won't get checked
            checklist.add(validPair)
        }
        validList.add(validPair)

        pair.rtt = min(pair.rtt ?: INFINITE, rtt)
        validPair.rtt = min(validPair.rtt ?: INFINITE, rtt)

        pair.state = CandidatePair.State.Succeeded
        validPair.state = CandidatePair.State.Succeeded

        // Once we have a valid pair, we can send data via it (provided the remote supports this)
        if (!remoteIsIce4J.await()) {
            readyForData.complete(Unit)
        }

        if (selectedPair == null && (pair.nominateOnSuccess || validPair.nominateOnSuccess)) {
            logger.info("Nomination successful: {}", validPair)
            logValidList(validPair)
            selectPair(validPair)
        }
    }

    private suspend fun sendIceBindingRequest(tId: TransactionId, pair: CandidatePair, nominate: Boolean = false): Pair<StunRequest, UdpStunPacket?> {
        val (localUsername, _) = localCreds
        val (remoteUsername, remotePassword) = remoteCreds.await()
        val msg = StunMessage(StunType.Binding, StunClass.Request, tId, listOfNotNull(
            StunAttribute.Priority(LocalCandidate.computePriority(CandidateType.PeerReflexive, pair.local.preference)),
            if (nominate) StunAttribute.UseCandidate else null,
            // Required attribute for role conflict resolution, however we don't fully implement it because role
            // conflicts do not occur with our signaling mechanism.
            if (controlling) StunAttribute.IceControlling(0u) else StunAttribute.IceControlled(0u),
            // For accurate Round Trip Time measurements in face of packet loss
            StunAttribute.TransactionTransmitCounter(0, 0),
            // Authentication
            StunAttribute.Username("$remoteUsername:$localUsername"),
            StunAttribute.MessageIntegrity(remotePassword),
            // Note: spec says we MUST use FINGERPRINT, but we can reliably tell STUN by the first byte, so we'll pass
        ))
        return coroutineScope {
            val logger = logger.with { addKeyValues(pair.local) }
            val request = StunRequest(logger, coroutineScope, pair.local::send, pair.remote.address, msg)
            stunManager.sendRequest(request)
            request to request.await()
        }
    }

    private suspend fun waitForGoodCandidateAndThenNominateIt() {
        if (remoteIsIce4J.await()) {
            // This roughly mirrors NominateBestRTT.java
            // 1. Wait until we have some valid candidate
            while (validList.isEmpty()) {
                delay(0.1.seconds)
            }
            // 2. If we only have relay pairs, wait for any direct ones
            val relayWaitTimeout = TimeSource.Monotonic.markNow() + 10.seconds
            while (validList.all { it.local.isRelay || it.remote.isRelay }) {
                delay(0.1.seconds)
                if (relayWaitTimeout.hasPassedNow()) {
                    break
                }
            }
            // 3. Wait some more in the hope that more direct ones are discovered
            val directWaitTimeout = TimeSource.Monotonic.markNow() + 3.seconds
            while (checklist.any { it.state <= CandidatePair.State.InProgress }) {
                delay(0.1.seconds)
                if (directWaitTimeout.hasPassedNow()) {
                    break
                }
            }
            // We have waited enough, time to nominate someone
        } else {
            // We are in no hurry, only reason we need to pick eventually is so we can free up allocated relays,
            // so we can just wait until all checks are done (or timed out).
            // We just don't want to pick too quickly, because more peer reflexive candidates can be discovered
            // in the process. Similar to what's discussed in https://www.rfc-editor.org/rfc/rfc8863 but instead
            // of failure, we're concerned with prematurely picking a sub-optimal route.
            val minWaitTimeout = TimeSource.Monotonic.markNow() + 30.seconds
            while (checklist.isEmpty() || checklist.any { it.state <= CandidatePair.State.InProgress } || !minWaitTimeout.hasPassedNow()) {
                delay(0.1.seconds)
            }
        }

        val nominatePair = getBestValidPair() ?: throw IOException("No valid pairs could be found.")

        logger.info("Nominate: {} ({}ms RTT)", nominatePair, nominatePair.rtt?.inWholeMilliseconds)
        logValidList(nominatePair)

        nominate(nominatePair)
    }

    private suspend fun nominate(pair: CandidatePair) {
        check(controlling) { "Only the controlling agent may nominate a pair." }

        val tId = TransactionId.create()
        val logger = logger.withKeyValue("tId", tId)
        logger.debug("Nominating: {}", pair)

        val (_, response) = sendIceBindingRequest(tId, pair, nominate = true)
        if (response == null) {
            logger.error("Nomination failed, no response: {}", pair)
            throw IOException("Nomination timeout")
        }

        logger.info("Nomination successful: {}", pair)
        selectPair(pair)
    }

    private suspend fun selectPair(pair: CandidatePair) {
        check(selectedPair == null)

        selectedPair = pair

        if (remoteIsIce4J.await()) {
            readyForData.complete(Unit)
        }

        pair.local.relay?.createChannel(pair.remote.address)

        // We're not going to change our mind, so we can cancel any remaining checks
        checksScope.cancel()
        // We only need to keep alive this one pair now, so we can release all other candidates
        val local = (pair.local as? LocalPeerReflexiveCandidate)?.baseCandidate ?: pair.local
        for (other in localCandidates) {
            if (other != local) {
                other.close()
            }
        }
    }

    private fun getBestValidPair(): CandidatePair? {
        return validList.minByOrNull { pair ->
            // Prefer lowest latency and non-relay
            var score = pair.rtt?.inWholeMilliseconds ?: 9999
            if (pair.local.isRelay) score += RELAY_PENALTY
            if (pair.remote.isRelay) score += RELAY_PENALTY
            score
        }
    }

    private fun logValidList(nominatedPair: CandidatePair) {
        for (pair in validList) {
            logger.info(" {} {} ({}ms RTT)", if (pair === nominatedPair) "*" else " ", pair, pair.rtt?.inWholeMilliseconds)
        }
    }

    private fun dispatchDataPacket(bytes: ByteArray) {
        // Note: If this is the controlled client and no final pair has been selected yet, then we must prefer sending
        //       data on the pair we last received data on, because that's the one the controlling client uses.
        //       If we were to send data back on what we think is the best pair, which might be different to what the
        //       controlling client decided, the data flow may be asymmetric and NATs on the way which then only ever
        //       see data going one way may decide that the other side is no longer interested in the conversation and
        //       therefore drop their port mapping.
        val pair = selectedPair ?: (if (controlling) null else lastReceivedDataPair) ?: getBestValidPair() ?: return

        if (highVolumeLogging) {
            val checksum = sha256.digest(bytes).toBase64String()
            logger.atTrace()
                .addKeyValues(pair.local)
                .addKeyValue("remoteAddress", pair.remote.address)
                .log("Sending {} bytes of data with checksum {}", bytes.size, checksum)
        }
        pair.local.sendUnchecked(DatagramPacket(bytes, pair.remote.address))
    }

    private suspend fun processPacket(packet: ReceivedPacket) {
        if (highVolumeLogging) {
            val checksum = sha256.digest(packet.data).toBase64String()
            logger.atTrace()
                .addKeyValues(packet.candidate)
                .addKeyValue("remoteAddress", packet.source)
                .log("Received {} bytes of data with checksum {}", packet.data.size, checksum)
        }
        if (!controlling && selectedPair == null) {
            lastReceivedDataPair = validList.find { it.local.base == packet.candidate.base && it.remote.address == packet.source }
        }
        inboundDataChannel.send(Pair(packet.candidate, packet.data))
    }

    private suspend fun processStunRequest(packet: ReceivedPacket, message: StunMessage) {
        val logger = logger.with {
            addKeyValues(packet.candidate)
            addKeyValue("remoteAddress", packet.source)
            addKeyValue("tId", message.transactionId)
        }

        when (message.type) {
            StunType.Binding -> {
                logger.trace("Received STUN Binding request.")

                val response = message.copy(
                    cls = StunClass.ResponseSuccess,
                    attributes = listOfNotNull(
                        message.attribute<StunAttribute.TransactionTransmitCounter>(),
                        StunAttribute.XorMappedAddress(packet.source),
                        StunAttribute.MessageIntegrity(message.integrityProtectionKey),
                    )
                )
                packet.reply(response.encode())

                // Potentially discover new peer reflexive candidate
                var remoteCandidate = remoteCandidates.find { it.address == packet.source }
                if (remoteCandidate == null) {
                    val priority = message.attribute<StunAttribute.Priority>()?.value
                    if (priority == null) {
                        logger.warn("Received STUN Binding request which is missing PRIORITY attribute: {}", message)
                        return
                    }
                    remoteCandidate = RemoteCandidateImpl(CandidateType.PeerReflexive, packet.source, priority)
                    addRemoteCandidate(remoteCandidate)
                }

                // We received a request, this is very promising, schedule a triggered check for this pair asap
                val pair = validList.find { it.local.base == packet.candidate.base && it.remote.address == packet.source }
                    ?: checklist.find { it.local == packet.candidate && it.remote == remoteCandidate }
                    ?: tryPair(packet.candidate, remoteCandidate)
                    ?: return
                if (!pair.hadTriggeredCheck && pair !in triggeredCheckQueue) {
                    pair.hadTriggeredCheck = true
                    when (pair.state) {
                        CandidatePair.State.Succeeded -> {}
                        CandidatePair.State.Waiting, CandidatePair.State.InProgress -> triggeredCheckQueue.add(pair)
                        CandidatePair.State.Failed -> {
                            pair.state = CandidatePair.State.Waiting
                            triggeredCheckQueue.add(pair)
                        }
                    }
                }

                // If the remote is trying to nominate this pair,
                if (!controlling && message.attribute<StunAttribute.UseCandidate>() != null && selectedPair == null) {
                    if (pair.state == CandidatePair.State.Succeeded) {
                        logger.info("Nomination successful: {}", pair)
                        logValidList(pair)
                        selectPair(pair)
                    } else {
                        logger.info("Remote nominated a pair, now waiting for its check to succeed: {}", pair)
                        pair.nominateOnSuccess = true
                    }
                }
            }
            else -> logger.warn("Ignoring STUN request because it had unexpected type {}.", message.type)
        }
    }

    private class ReceivedPacket(
        val candidate: LocalCandidate,
        val source: InetSocketAddress,
        val data: ByteArray,
    ) {
        fun reply(data: ByteArray) {
            candidate.sendUnchecked(DatagramPacket(data, source))
        }
    }

    private class CandidatePair(val local: LocalCandidate, val remote: RemoteCandidate, isLocalControlling: Boolean) {
        var state: State = State.Waiting
        var check: Job? = null
        var rtt: Duration? = null
        var extraRttChecks = 0
        var hadTriggeredCheck = false

        /**
         * Set on the controlled side when this pair is nominated by the controlling agent but we don't yet know whether
         * it actually works.
         * A triggered check will have been scheduled, and if that succeeds, this pair should be selected and ICE
         * concluded.
         */
        var nominateOnSuccess = false

        val priority: Long = run {
            // priority of candidate of controllin**g** agent and of controlle**d** agent
            // https://www.rfc-editor.org/rfc/rfc8445#section-6.1.2.3
            val g = (if (isLocalControlling) local else remote).priority
            val d = (if (isLocalControlling) remote else local).priority
            min(g, d).toLong().shl(32) + max(g, d).toLong().shl(1) + if (g > d) 1 else 0
        }

        override fun toString(): String {
            return "$local -> $remote"
        }

        enum class State {
            Waiting,
            InProgress,
            Succeeded,
            Failed,
        }
    }

    companion object {
        private const val MAX_CHECKLIST_SIZE = 100
        private val RELAY_PENALTY = Integer.getInteger("essential.sps.relay_latency_threshold", 100)
        private val sha256 = MessageDigest.getInstance("SHA-256")

        private fun LoggingEventBuilder.addKeyValues(candidate: LocalCandidate): LoggingEventBuilder {
            if (candidate.type == CandidateType.Relayed) {
                addKeyValue("hostAddress", candidate.socket.hostAddress)
                addKeyValue("relayAddress", candidate.address)
            } else {
                addKeyValue("hostAddress", candidate.address)
            }
            return this
        }
    }
}
