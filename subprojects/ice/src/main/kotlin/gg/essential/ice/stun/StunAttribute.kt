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

import gg.essential.ice.toHexString
import java.net.InetSocketAddress

sealed interface StunAttribute {
    data class MappedAddress(val address: InetSocketAddress) : StunAttribute
    data class Username(val username: String) : StunAttribute
    class MessageIntegrity(val key: ByteArray?) : StunAttribute
    data class ErrorCode(val code: Int, val message: String) : StunAttribute
    data class UnknownAttributes(val attributes: List<StunAttributeType>) : StunAttribute
    data class XorMappedAddress(val address: InetSocketAddress) : StunAttribute
    data class ChannelNumber(val number: UShort) : StunAttribute
    data class Lifetime(val seconds: UInt) : StunAttribute
    data class XorPeerAddress(val address: InetSocketAddress) : StunAttribute
    class Data(val bytes: ByteArray) : StunAttribute {
        override fun toString(): String {
            return "Data(${bytes.toHexString()})"
        }
    }
    data class XorRelayedAddress(val address: InetSocketAddress) : StunAttribute
    data object RequestedTransport : StunAttribute
    data class Priority(val value: Int) : StunAttribute
    data object UseCandidate : StunAttribute
    data class Software(val value: String) : StunAttribute
    data class TransactionTransmitCounter(val request: Int, val response: Int) : StunAttribute
    data class IceControlling(val tiebreaker: ULong) : StunAttribute
    data class IceControlled(val tiebreaker: ULong) : StunAttribute
}
