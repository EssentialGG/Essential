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
import gg.essential.ice.stun.StunSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.INFINITE

/**
 * This class implements ICE candidate gathering (host, server-reflexive, and relay candidates) in a way that it may be
 * shared between multiple [IceAgent]s.
 */
class CandidateManager(
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val stunManager: StunManager,
    private val stunServers: List<InetSocketAddress>,
    private val turnServers: List<InetSocketAddress>,
) {
    /**
     * Set to `true` once any of its candidates have been shut down, at which point this CandidateManager should no
     * longer be used for new connections and instead a new one should be created for those.
     */
    var anyShutDown = false
        private set

    private var doneCollecting = false
    private val candidates = mutableListOf<ReusableCandidate>()
    private val consumers = mutableListOf<Pair<Job, SendChannel<LocalCandidate>>>()

    private val socketRefCounts = mutableMapOf<StunSocket, Int>()
    private val bindingRefCounts = mutableMapOf<StunSocket.StunBinding, Int>()
    private val relayRefCounts = mutableMapOf<StunSocket.RelayAllocation, Int>()

    private var nextServerReflexivePreference = MAX_LOCAL_PREFERENCE
    private var nextRelayedPreference = MAX_LOCAL_PREFERENCE

    init {
        scope.launch {
            gatherCandidates()
            logger.debug("End of candidate gathering.")
            doneCollecting = true
            consumers.forEach { it.second.close() }
            consumers.clear()
        }
    }

    fun getCandidates(parentJob: Job): ReceiveChannel<LocalCandidate> {
        val channel = Channel<LocalCandidate>(Channel.UNLIMITED) { it.close() }

        for (candidate in candidates) {
            channel.trySend(candidate.use(parentJob))
        }

        if (doneCollecting) {
            channel.close()
        } else {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    consumers.add(Pair(parentJob, channel))
                    delay(INFINITE)
                } finally {
                    consumers.remove(Pair(parentJob, channel))
                }
            }
        }

        return channel
    }

    private fun addCandidate(candidate: ReusableCandidate) {
        candidates.add(candidate)

        consumers
            // Note: Need to allocate all candidates before sending any of them into their channel, otherwise if the
            // first one is immediately closed again, the entire ReusableCandidate might be closed even though there
            // might be others that still would have wanted to use it.
            .map { (job, channel) -> channel to candidate.use(job) }
            .forEach { (channel, bound) ->
                val result = channel.trySend(bound)
                if (!result.isSuccess) {
                    bound.close()
                }
            }
    }

    private suspend fun gatherCandidates() = coroutineScope {
        gatherHostCandidates()
            .onEach { addCandidate(it) }
            .onEach { hostCandidate ->
                gatherServerCandidates(hostCandidate.socket)
                    .onEach { addCandidate(it) }
                    .launchIn(this)
            }
            .launchIn(this)
    }

    private fun gatherHostCandidates(): Flow<ReusableCandidate> = flow {
        var nextNormalPreference = MAX_LOCAL_PREFERENCE
        var nextVPNPreference = MAX_LOCAL_PREFERENCE / 2
        for (iface in NetworkInterface.getNetworkInterfaces()) {
            if (iface.isLoopback) {
                logger.trace("Skipping network interface because it is a loopback interface: {}", iface)
                continue
            }
            if (!iface.isUp) {
                logger.trace("Skipping network interface because it is not up: {}", iface)
                continue
            }

            val isVPN = iface.isVirtual

            val unfilteredAddresses = iface.inetAddresses.toList()
            logger.trace("Network interface {} has addresses: {}", iface, unfilteredAddresses)

            val addresses = unfilteredAddresses.filter { address ->
                when {
                    address.isLoopbackAddress -> {
                        logger.trace("Skipping address because it is a loopback address: {}", address)
                        false
                    }
                    address is Inet6Address && address.isSiteLocalAddress -> {
                        logger.trace("Skipping address because it is a deprecated site-local IPv6 address: {}", address)
                        false
                    }
                    address is Inet6Address && address.isIPv4CompatibleAddress -> {
                        logger.trace("Skipping address because it is a deprecated IPv4-compatible IPv6 address: {}", address)
                        false
                    }
                    address is Inet6Address && address.isIPv4MappedAddress -> {
                        logger.trace("Skipping address because it is a IPv4-mapped IPv6 address: {}", address)
                        false
                    }
                    else -> true
                }
            }

            // Sort addresses as recommended by https://www.rfc-editor.org/rfc/rfc8421 for dual-stack agents
            val sortedAddresses = mutableListOf<InetAddress>()
            val v4Addresses = addresses.filterIsInstance<Inet4Address>().toMutableList()
            val v6Addresses = addresses.filterIsInstance<Inet6Address>().toMutableList()
            val v6HeadStart = if (v4Addresses.isNotEmpty()) addresses.size / v4Addresses.size else 0
            for (i in 0 until v6HeadStart) {
                v6Addresses.removeFirstOrNull()?.let { sortedAddresses.add(it) }
            }
            while (v4Addresses.isNotEmpty() || v6Addresses.isNotEmpty()) {
                v4Addresses.removeFirstOrNull()?.let { sortedAddresses.add(it) }
                v6Addresses.removeFirstOrNull()?.let { sortedAddresses.add(it) }
            }

            for (address in sortedAddresses) {
                val socket = try {
                    DatagramSocket(0, address)
                } catch (e: Exception) {
                    logger.warn("Failed to bind to $address, skipping:", e)
                    continue
                }
                val socketAddress = InetSocketAddress(address, socket.localPort)
                val stunSocket = StunSocket(logger, scope, stunManager, socket, socketAddress)
                val preference = if (isVPN) nextVPNPreference-- else nextNormalPreference--
                emit(ReusableCandidate(CandidateType.Host, stunSocket, null, null, socketAddress, preference))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun gatherServerCandidates(socket: StunSocket): Flow<ReusableCandidate> = channelFlow {
        val logger = socket.logger

        for (stunServer in stunServers) {
            if (socket.hostAddress.address is Inet6Address != stunServer.address is Inet6Address) {
                continue
            }

            stunManager.bindingPacer.await(true)
            logger.debug("Contacting STUN server {}", stunServer)

            val binding = socket.allocateBinding(stunServer)
            launch {
                send(ReusableCandidate(CandidateType.ServerReflexive, socket, binding, null, binding.mappedAddress.await(), nextServerReflexivePreference--))
            }
        }

        for (turnServer in turnServers) {
            if (socket.hostAddress.address is Inet6Address != turnServer.address is Inet6Address) {
                continue
            }

            stunManager.bindingPacer.await(true)
            logger.debug("Contacting TURN server {}", turnServer)

            val allocation = socket.allocateRelay(turnServer)
            launch {
                // TODO we could also get a server-reflexive candidate from the turn server, but then lifetime
                //  management gets complicated because we do still want to release the relay if we don't use it, but
                //  we may still have to keep the server-reflexive candidate alive at the same time.
                //  Given we have dedicated STUN server bindings above already, we'll simply skip these for now.
                // send(ReusableCandidate(CandidateType.ServerReflexive, socket, ???, ???, allocation.mappedAddress.await(), nextServerReflexivePreference--))
                send(ReusableCandidate(CandidateType.Relayed, socket, null, allocation, allocation.relayedAddress.await(), nextRelayedPreference--))
            }
        }
    }

    private inner class ReusableCandidate(
        val type: CandidateType,
        val socket: StunSocket,
        val binding: StunSocket.StunBinding?,
        val relay: StunSocket.RelayAllocation?,
        val address: InetSocketAddress,
        val preference: Int,
    ) {
        fun use(parentJob: Job): LocalCandidate {
            var closed = false
            val candidate = LocalCandidateImpl(type, socket, relay, address, preference) {
                if (closed) return@LocalCandidateImpl
                closed = true

                if (relay != null) {
                    if (relayRefCounts.compute(relay) { _, count -> (count ?: 0) - 1} == 0) {
                        relay.scope.cancel()
                        anyShutDown = true
                    }
                }
                if (binding != null) {
                    if (bindingRefCounts.compute(binding) { _, count -> (count ?: 0) - 1} == 0) {
                        binding.scope.cancel()
                        anyShutDown = true
                    }
                }
                if (socketRefCounts.compute(socket) { _, count -> (count ?: 0) - 1} == 0) {
                    socket.scope.cancel()
                    anyShutDown = true
                }
            }

            socketRefCounts.compute(socket) { _, count -> (count ?: 0) + 1}
            if (binding != null) {
                bindingRefCounts.compute(binding) { _, count -> (count ?: 0) + 1}
            }
            if (relay != null) {
                relayRefCounts.compute(relay) { _, count -> (count ?: 0) + 1}
            }

            scope.launch(parentJob, start = CoroutineStart.UNDISPATCHED) {
                try {
                    delay(INFINITE)
                } finally {
                    candidate.close()
                }
            }

            return candidate
        }
    }

    companion object {
        private const val MAX_LOCAL_PREFERENCE = 65535

        // https://www.rfc-editor.org/rfc/rfc4291#section-2.5.5.2
        private val Inet6Address.isIPv4MappedAddress: Boolean
            get() {
                val bytes = address
                return (0..9).all { bytes[it] == 0.toByte() } && bytes[10] == 255.toByte() && bytes[11] == 255.toByte()
            }
    }
}

