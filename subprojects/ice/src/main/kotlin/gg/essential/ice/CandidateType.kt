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

enum class CandidateType(val shortName: String, val preference: Int) {
    // Note: We use a much reduced local preference for Host candidates because we expect most people to play with
    // friends that are in a different network and behind NAT, so trying host candidates first is a bit of a waste.
    // We'll still test the host candidates eventually, so they will end up with the best route at the end of the day,
    // we'd just rather optimize connect times for the typical case.
    Host("host", 80),
    PeerReflexive("prflx", 110),
    ServerReflexive("srflx", 100),
    Relayed("relay", 0),
    ;

    companion object {
        val byShortName = CandidateType.entries.associateBy { it.shortName }
    }
}
