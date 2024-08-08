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

import gg.essential.data.VersionInfo
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

data class StunMessage(
    val type: StunType,
    val cls: StunClass,
    val transactionId: TransactionId,
    val attributes: List<StunAttribute>,
) {
    init {
        if (!type.isCompatible(cls)) {
            throw IllegalArgumentException("Message type $type cannot be used with $cls.")
        }
    }

    inline fun <reified T : StunAttribute> attribute(): T? =
        attributes.firstNotNullOfOrNull { it as? T }

    val isIntegrityProtected: Boolean
        get() = integrityProtectionKey != null

    val integrityProtectionKey: ByteArray?
        get() = attribute<StunAttribute.MessageIntegrity>()?.key

    fun encode(): ByteArray {
        fun encodeXorAddress(address: InetSocketAddress): ByteArray {
            val family = when (address.address) {
                is Inet4Address -> 1
                is Inet6Address -> 2
                else -> throw IOException("Unknown address family ${address.address}")
            }
            val port = address.port
            val addressBytes = address.address.address
            val addressMask = MAGIC_COOKIE + transactionId.bytes
            for (i in addressBytes.indices) {
                addressBytes[i] = addressBytes[i].xor(addressMask[i])
            }
            val bytes = ByteArray(4 + addressBytes.size)
            bytes[1] = family.toByte()
            bytes[2] = port.shr(8).toByte().xor(MAGIC_COOKIE[0])
            bytes[3] = port.toByte().xor(MAGIC_COOKIE[1])
            addressBytes.copyInto(bytes, 4)
            return bytes
        }
        fun encodeInt(value: Int): ByteArray {
            return byteArrayOf(
                value.shr(24).toByte(),
                value.shr(16).toByte(),
                value.shr(8).toByte(),
                value.shr(0).toByte(),
            )
        }
        fun encodeLong(value: Long): ByteArray {
            return byteArrayOf(
                value.shr(56).toByte(),
                value.shr(48).toByte(),
                value.shr(40).toByte(),
                value.shr(32).toByte(),
                value.shr(24).toByte(),
                value.shr(16).toByte(),
                value.shr(8).toByte(),
                value.shr(0).toByte(),
            )
        }

        val attributes = attributes.toMutableList()

        if (type != StunType.Send) {
            attributes.add(0, StunAttribute.Software(SOFTWARE))
        }

        val encodedAttrs = attributes.map { attr ->
            when (attr) {
                is StunAttribute.MappedAddress -> throw IllegalArgumentException("MappedAddress encoding not implemented.")
                is StunAttribute.XorMappedAddress -> Pair(StunAttributeType.XorMappedAddress, encodeXorAddress(attr.address))
                is StunAttribute.Username -> Pair(StunAttributeType.Username, attr.username.encodeToByteArray())
                is StunAttribute.MessageIntegrity -> Pair(StunAttributeType.MessageIntegrity, ByteArray(20))
                is StunAttribute.ErrorCode -> throw IllegalArgumentException("ErrorCode encoding not implemented.")
                is StunAttribute.UnknownAttributes -> throw IllegalArgumentException("UnknownAttributes encoding not implemented.")
                is StunAttribute.ChannelNumber -> Pair(StunAttributeType.ChannelNumber, encodeInt(attr.number.toInt().shl(16)))
                is StunAttribute.Lifetime -> Pair(StunAttributeType.Lifetime, encodeInt(attr.seconds.toInt()))
                is StunAttribute.XorPeerAddress -> Pair(StunAttributeType.XorPeerAddress, encodeXorAddress(attr.address))
                is StunAttribute.Data -> Pair(StunAttributeType.Data, attr.bytes)
                is StunAttribute.XorRelayedAddress -> Pair(StunAttributeType.XorRelayedAddress, encodeXorAddress(attr.address))
                is StunAttribute.RequestedTransport -> Pair(StunAttributeType.RequestedTransport, byteArrayOf(17/*UDP*/, 0, 0, 0))
                is StunAttribute.Priority -> Pair(StunAttributeType.Priority, encodeInt(attr.value))
                is StunAttribute.UseCandidate -> Pair(StunAttributeType.UseCandidate, ByteArray(0))
                is StunAttribute.Software -> Pair(StunAttributeType.Software, attr.value.encodeToByteArray())
                is StunAttribute.TransactionTransmitCounter -> Pair(
                    StunAttributeType.TransactionTransmitCounter,
                    byteArrayOf(0, 0, attr.request.toByte(), attr.response.toByte()))
                is StunAttribute.IceControlling -> Pair(StunAttributeType.IceControlling, encodeLong(attr.tiebreaker.toLong()))
                is StunAttribute.IceControlled -> Pair(StunAttributeType.IceControlled, encodeLong(attr.tiebreaker.toLong()))
            }
        }

        val typeAndClass = run {
            val t = type.id
            val c = cls.ordinal
            t.and(0b111110000000).shl(2)
                .or(t.and(0b1110000).shl(1))
                .or(t.and(0b1111))
                .or(c.and(0b10).shl(7))
                .or(c.and(0b1).shl(4))
        }
        val attrsLength = encodedAttrs.sumOf { 4 + it.second.size.padToMultipleOf(4) }

        val bytes = ByteArray(HEADER_SIZE + attrsLength)
        bytes[0] = typeAndClass.shr(8).toByte()
        bytes[1] = typeAndClass.toByte()
        // Length written later because MessageIntegrity handling might overwrite it
        MAGIC_COOKIE.copyInto(bytes, 4)
        transactionId.bytes.copyInto(bytes, 8)

        var offset = 20
        for ((type, encodedAttr) in encodedAttrs) {
            val attrLength = encodedAttr.size
            bytes[offset] = type.id.shr(8).toByte()
            bytes[offset + 1] = type.id.toByte()
            bytes[offset + 2] = attrLength.shr(8).toByte()
            bytes[offset + 3] = attrLength.toByte()
            offset += 4
            encodedAttr.copyInto(bytes, offset)
            offset += attrLength.padToMultipleOf(4)

            if (type == StunAttributeType.MessageIntegrity) {
                // The input to the HMAC is the content up to and including the **preceding** attribute
                val verifiedLength = offset - 24
                // The stored message length however includes the MessageIntegrity attribute
                val storedLength = offset
                bytes[2] = (storedLength - HEADER_SIZE).shr(8).toByte()
                bytes[3] = (storedLength - HEADER_SIZE).toByte()
                val key: ByteArray = attributes.firstNotNullOf { it as? StunAttribute.MessageIntegrity }.key
                    ?: throw IOException("Cannot encode MessageIntegrity without password.")
                val mac = Mac.getInstance("HmacSHA1")
                mac.init(SecretKeySpec(key, "HmacSHA1"))
                mac.update(bytes, 0, verifiedLength)
                val hmac = mac.doFinal()
                hmac.copyInto(bytes, offset - 20)
            }
        }

        // And finally, the true length
        bytes[2] = attrsLength.shr(8).toByte()
        bytes[3] = attrsLength.toByte()

        return bytes
    }

    companion object {
        private val SOFTWARE = "essential.gg v${VersionInfo().essentialVersion}"
        private const val HEADER_SIZE = 20
        private val MAGIC_COOKIE = byteArrayOf(0x21, 0x12, 0xA4.toByte(), 0x42)

        fun looksLikeStun(bytes: ByteArray): Boolean {
            if (bytes.size < HEADER_SIZE) return false
            // First two bits are zero by spec
            if (bytes[0].toInt() and 0b11000000 != 0) return false
            // Message length is always aligned to multiples of 4 bytes
            if (bytes[3].toInt() and 0b00000011 != 0) return false
            // Magic cookie
            if (!bytes.sliceArray(4 until 8).contentEquals(MAGIC_COOKIE)) return false
            // Certainly looks like STUN
            return true
        }

        fun decode(
            bytes: ByteArray,
            getServerKey: (user: String) -> ByteArray?,
            getClientKey: (tId: TransactionId) -> ByteArray?,
        ): StunMessage {
            val typeAndClass = bytes[0].toUByte().toInt()
                .shl(8).or(bytes[1].toUByte().toInt())
            if (typeAndClass and 0b1100000000000000 != 0) {
                throw IOException("First two bits of STUN message must be 0.")
            }
            val msgTypeId = (typeAndClass and 0b11111000000000 shr 2) or (typeAndClass and 0b11100000 shr 1) or (typeAndClass and 0b1111)
            val msgClassId = (typeAndClass and 0b100000000 shr 7) or (typeAndClass and 0b10000 shr 4)

            val msgType = StunType.byId[msgTypeId]
                ?: throw IOException("Unknown message type $msgTypeId.")
            val msgClass = StunClass.entries[msgClassId]
            if (!msgType.isCompatible(msgClass)) {
                throw IOException("Message type $msgType cannot be used with $msgClass.")
            }

            val length = bytes[2].toUByte().toInt()
                .shl(8).or(bytes[3].toUByte().toInt())
            if (length != length.padToMultipleOf(4)) {
                throw IOException("Invalid message length, must be multiple of 4 but was $length.")
            }
            if (length > bytes.size - HEADER_SIZE) {
                throw IOException("Invalid message length, was $length but only ${bytes.size - HEADER_SIZE} bytes remain.")
            }

            val cookie = bytes.sliceArray(4 until 8)
            if (!cookie.contentEquals(MAGIC_COOKIE)) {
                throw IOException("Invalid magic cookie value ${cookie.contentToString()}")
            }

            val transactionId = bytes.sliceArray(8 until 20)

            val attributes = mutableListOf<StunAttribute>()

            var offset = HEADER_SIZE

            fun decodeXorAddress(xorMask: ByteArray = MAGIC_COOKIE + transactionId): InetSocketAddress {
                val addressBytes = when (val family = bytes[offset + 1].toUByte().toInt()) {
                    1 -> 4
                    2 -> 16
                    else -> throw IOException("Unknown address family $family")
                }
                val port = bytes[offset + 2].xor(xorMask[0]).toUByte().toInt()
                    .shl(8).or(bytes[offset + 3].xor(xorMask[1]).toUByte().toInt())
                val address = bytes.sliceArray(offset + 4 until offset + 4 + addressBytes)
                for (i in address.indices) {
                    address[i] = address[i].xor(xorMask[i])
                }
                return InetSocketAddress(InetAddress.getByAddress(address), port)
            }
            fun decodeInt(): Int {
                return bytes[offset + 0].toInt()
                    .shl(8).or(bytes[offset + 1].toUByte().toInt())
                    .shl(8).or(bytes[offset + 2].toUByte().toInt())
                    .shl(8).or(bytes[offset + 3].toUByte().toInt())
            }
            fun decodeLong(): Long {
                return bytes[offset + 0].toLong()
                    .shl(8).or(bytes[offset + 1].toUByte().toLong())
                    .shl(8).or(bytes[offset + 2].toUByte().toLong())
                    .shl(8).or(bytes[offset + 3].toUByte().toLong())
                    .shl(8).or(bytes[offset + 4].toUByte().toLong())
                    .shl(8).or(bytes[offset + 5].toUByte().toLong())
                    .shl(8).or(bytes[offset + 6].toUByte().toLong())
                    .shl(8).or(bytes[offset + 7].toUByte().toLong())
            }

            while (offset < length + HEADER_SIZE) {
                val attrType = bytes[offset].toUByte().toInt()
                    .shl(8).or(bytes[offset + 1].toUByte().toInt())
                val attrLength = bytes[offset + 2].toUByte().toInt()
                    .shl(8).or(bytes[offset + 3].toUByte().toInt())

                offset += 4

                attributes.add(when (StunAttributeType.byId[attrType]) {
                    StunAttributeType.MappedAddress -> StunAttribute.MappedAddress(
                        decodeXorAddress(
                            xorMask = ByteArray(
                                16
                            )
                        )
                    )
                    StunAttributeType.XorMappedAddress -> StunAttribute.XorMappedAddress(decodeXorAddress())
                    StunAttributeType.Username -> {
                        if (attrLength > 512) {
                            throw IOException("Invalid username, too long: $attrLength")
                        }
                        StunAttribute.Username(bytes.sliceArray(offset until offset + attrLength).decodeToString())
                    }
                    StunAttributeType.MessageIntegrity -> {
                        // The input to the HMAC is the content up to and including the **preceding** attribute
                        val verifiedLength = offset - 4
                        val verifiedBytes = bytes.sliceArray(0 until verifiedLength)
                        // The stored message length however includes the MessageIntegrity attribute
                        val storedLength = offset + 20
                        verifiedBytes[2] = (storedLength - HEADER_SIZE).shr(8).toByte()
                        verifiedBytes[3] = (storedLength - HEADER_SIZE).toByte()

                        val key = if (msgClass.isResponse) {
                            getClientKey(TransactionId(transactionId))
                        } else {
                            val username = attributes.firstNotNullOfOrNull { (it as? StunAttribute.Username)?.username }
                                ?: throw IOException("Integrity protected requests must include a username.")
                            getServerKey(username)
                        }
                        if (key == null) {
                            attributes.add(StunAttribute.MessageIntegrity(null))
                            break
                        }
                        val mac = Mac.getInstance("HmacSHA1")
                        mac.init(SecretKeySpec(key, "HmacSHA1"))
                        mac.update(verifiedBytes)
                        val hmac = mac.doFinal()
                        if (!hmac.contentEquals(bytes.sliceArray(offset until offset + 20))) {
                            throw IOException("Invalid HMAC")
                        }
                        attributes.add(StunAttribute.MessageIntegrity(key))
                        break // ignore everything after this attribute (we don't support FINGERPRINT)
                    }
                    StunAttributeType.ErrorCode -> {
                        val cls = bytes[offset + 2].toUByte().toInt() and 0b111
                        val num = bytes[offset + 3].toUByte().toInt()
                        val code = cls * 100 + num
                        val message = bytes.decodeToString(offset + 4, offset + attrLength)
                            .takeWhile { it != 0.toChar() }
                        StunAttribute.ErrorCode(code, message)
                    }
                    StunAttributeType.UnknownAttributes -> {
                        StunAttribute.UnknownAttributes((0 until attrLength step 2).map { i ->
                            val id = bytes[offset + i].toUByte().toInt()
                                .shl(8).or(bytes[offset + i + 1].toUByte().toInt())
                            StunAttributeType.byId[id] ?: throw IOException("Unknown attribute type $id")
                        })
                    }
                    StunAttributeType.ChannelNumber -> throw IOException("CHANNEL-NUMBER decoding not supported")
                    StunAttributeType.Lifetime -> StunAttribute.Lifetime(decodeInt().toUInt())
                    StunAttributeType.XorPeerAddress -> StunAttribute.XorPeerAddress(decodeXorAddress())
                    StunAttributeType.Data -> StunAttribute.Data(bytes.sliceArray(offset until offset + attrLength))
                    StunAttributeType.XorRelayedAddress -> StunAttribute.XorRelayedAddress(decodeXorAddress())
                    StunAttributeType.RequestedTransport -> throw IOException("REQUESTED-TRANSPORT decoding not supported")
                    StunAttributeType.Priority -> StunAttribute.Priority(decodeInt())
                    StunAttributeType.UseCandidate -> StunAttribute.UseCandidate
                    StunAttributeType.Software -> StunAttribute.Software(
                        bytes.decodeToString(
                            offset,
                            offset + attrLength
                        )
                    )
                    StunAttributeType.TransactionTransmitCounter -> StunAttribute.TransactionTransmitCounter(
                        bytes[offset + 2].toInt(),
                        bytes[offset + 3].toInt(),
                    )
                    StunAttributeType.IceControlled -> StunAttribute.IceControlled(decodeLong().toULong())
                    StunAttributeType.IceControlling -> StunAttribute.IceControlling(decodeLong().toULong())
                    null -> {
                        if (StunAttributeType.isComprehensionRequired(attrType)) {
                            // TODO Not what we should do for requests, but we can always just pretend packet loss :P
                            throw IOException("Unknown attribute type $attrType")
                        }
                        offset += attrLength.padToMultipleOf(4)
                        continue
                    }
                })

                offset += attrLength.padToMultipleOf(4)
            }

            return StunMessage(msgType, msgClass, TransactionId(transactionId), attributes)
        }

        private fun Int.padToMultipleOf(multipleOf: Int): Int =
            (this + multipleOf - 1) / multipleOf * multipleOf
    }
}
