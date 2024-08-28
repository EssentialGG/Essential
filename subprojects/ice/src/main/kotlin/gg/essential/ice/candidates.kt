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

import gg.essential.ice.stun.StunSocket
import java.net.DatagramPacket
import java.net.Inet6Address
import java.net.InetSocketAddress

sealed interface Candidate {
    val type: CandidateType
    val address: InetSocketAddress
    val priority: Int

    val isRelay: Boolean
        get() = type == CandidateType.Relayed

    val isIPv6: Boolean
        get() = address.address is Inet6Address

    val isLinkLocal: Boolean
        get() = (address.address as? Inet6Address)?.isLinkLocalAddress == true

    val isSiteLocal: Boolean
        get() = address.address.isSiteLocalAddress
}

sealed interface LocalCandidate : Candidate {
    val base: InetSocketAddress
    val preference: Int

    override val priority: Int
        get() = computePriority(type, preference)

    val socket: StunSocket
    val relay: StunSocket.RelayAllocation?

    /**
     * Sends the given packet via this candidate.
     *
     * On success (just means the sending succeeded, not that it arrived, it's still UDP) returns `true`,
     * otherwise (i.e. on unrecoverable failure, e.g. no route to host) returns `false`.
     *
     * Use [sendUnchecked] if you don't care and don't want to wait.
     */
    suspend fun send(packet: DatagramPacket): Boolean
    fun sendUnchecked(packet: DatagramPacket)

    fun close()

    companion object {
        fun computePriority(type: CandidateType, localPreference: Int) =
            // https://www.rfc-editor.org/rfc/rfc8445#section-5.1.2.1
            (type.preference shl 24) or (localPreference shl 8) or 255
    }
}

interface RemoteCandidate : Candidate {
    override var type: CandidateType // mutable only for peer-reflexive candidates if we later learn they are relays
}

open class LocalCandidateImpl(
    final override val type: CandidateType,
    final override val socket: StunSocket,
    final override val relay: StunSocket.RelayAllocation?,
    final override val address: InetSocketAddress,
    final override val preference: Int,
    private val onClose: () -> Unit,
) : LocalCandidate {
    override val base: InetSocketAddress = when (type) {
        CandidateType.Host, CandidateType.Relayed -> address
        CandidateType.PeerReflexive, CandidateType.ServerReflexive -> socket.hostAddress
    }

    override fun toString(): String {
        return when (type) {
            CandidateType.Host -> "$address"
            CandidateType.PeerReflexive -> "$address (peer-reflexive for ${socket.hostAddress})"
            CandidateType.ServerReflexive -> "$address (server-reflexive for ${socket.hostAddress})"
            CandidateType.Relayed -> "$address (relayed for ${socket.hostAddress})"
        }
    }

    override suspend fun send(packet: DatagramPacket): Boolean {
        if (relay != null) {
            relay.sendChannel.trySend(packet)
            return true // relay doesn't give feedback
        } else {
            return socket.send(packet)
        }
    }

    override fun sendUnchecked(packet: DatagramPacket) {
        if (relay != null) {
            relay.sendChannel.trySend(packet)
        } else {
            socket.sendUnchecked(packet)
        }
    }

    override fun close() {
        onClose()
    }
}

class LocalPeerReflexiveCandidate(
    val baseCandidate: LocalCandidate,
    address: InetSocketAddress,
) : LocalCandidateImpl(CandidateType.PeerReflexive, baseCandidate.socket, baseCandidate.relay, address, baseCandidate.preference, {})

class RemoteCandidateImpl(
    override var type: CandidateType,
    override val address: InetSocketAddress,
    override val priority: Int,
) : RemoteCandidate {
    override fun toString(): String {
        return "$address (${type.shortName})"
    }
}
