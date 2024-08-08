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

import java.net.InetSocketAddress
import java.util.Base64
import kotlin.time.Duration

fun DatagramPacket(buf: ByteArray, address: InetSocketAddress) = java.net.DatagramPacket(buf, buf.size, address)

fun min(a: Duration, b: Duration): Duration = if (a < b) a else b

private const val HEX_DIGITS = "0123456789abcdef"
fun ByteArray.toHexString(): String =
    joinToString("") { b ->
        val high = HEX_DIGITS[b.toUByte().toInt().shr(4)]
        val low = HEX_DIGITS[b.toUByte().toInt().and(0x0f)]
        "$high$low"
    }

fun ByteArray.toBase64String(): String =
    Base64.getEncoder().encodeToString(this)
