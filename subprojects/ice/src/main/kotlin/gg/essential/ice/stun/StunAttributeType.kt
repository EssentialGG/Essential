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

// This is subset of types as required for the functionality we implement.
// For a list of all registered types with links to relevant RFCs, see
// https://www.iana.org/assignments/stun-parameters/stun-parameters.xhtml
enum class StunAttributeType(val id: Int) {
    // Comprehension-required range
    MappedAddress(0x0001),
    Username(0x0006),
    MessageIntegrity(0x0008),
    ErrorCode(0x0009),
    UnknownAttributes(0x000a),
    // Realm(0x0014),
    // Nonce(0x0015),
    XorMappedAddress(0x0020),
    // TURN-specific
    ChannelNumber(0x000c),
    Lifetime(0x000d),
    XorPeerAddress(0x0012),
    Data(0x0013),
    XorRelayedAddress(0x0016),
    RequestedTransport(0x0019),
    // ICE-specific
    Priority(0x0024),
    UseCandidate(0x0025),

    // Comprehension-optional range
    Software(0x8022),
    // AlternateServer(0x8023),
    TransactionTransmitCounter(0x8025),
    // Fingerprint(0x8028),
    // ICE-specific
    IceControlled(0x8029),
    IceControlling(0x802a),
    ;

    companion object {
        val byId = entries.associateBy { it.id }

        fun isComprehensionRequired(id: Int) = id < 0x8000
    }
}
