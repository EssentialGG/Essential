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

import gg.essential.ice.toBase64String
import java.security.SecureRandom

class TransactionId(val bytes: ByteArray) {
    init {
        assert(bytes.size == 12)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is TransactionId) return false
        return other.bytes.contentEquals(this.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String {
        return bytes.toBase64String()
    }

    companion object {
        private val secureRandom = SecureRandom()

        @Synchronized
        fun create(): TransactionId = TransactionId(ByteArray(12).apply { secureRandom.nextBytes(this) })
    }
}
