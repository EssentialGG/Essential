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

class ChannelData(val channelId: UShort, val data: ByteArray) {
    fun encode(): ByteArray = encode(channelId, data)

    companion object {
        private const val HEADER_SIZE = 4

        fun tryDecode(bytes: ByteArray): ChannelData? {
            if (bytes.size < HEADER_SIZE) return null

            val channelId = bytes[0].toUByte().toUInt().shl(8).or(bytes[1].toUByte().toUInt()).toUShort()
            if (channelId !in 0x4000u until 0x5000u) return null

            val len = bytes[2].toUByte().toInt().shl(8).or(bytes[3].toUByte().toInt())
            if (HEADER_SIZE + len > bytes.size) return null

            return ChannelData(channelId, bytes.sliceArray(HEADER_SIZE until HEADER_SIZE + len))
        }

        fun encode(channelId: UShort, data: ByteArray, dataOffset: Int = 0, dataLen: Int = data.size): ByteArray {
            val bytes = ByteArray(HEADER_SIZE + dataLen)
            bytes[0] = channelId.toUInt().shr(8).toByte()
            bytes[1] = channelId.toByte()
            bytes[2] = dataLen.shr(8).toByte()
            bytes[3] = dataLen.toByte()
            data.copyInto(bytes, HEADER_SIZE, dataOffset, dataOffset + dataLen)
            return bytes
        }
    }
}
