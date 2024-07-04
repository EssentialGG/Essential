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
package gg.essential.handlers.discord.party

data class PartyInformation(
    /**
     * This is a secret which contains unique information on joining a user's world or server.
     * This string CAN NOT BE EMPTY (i.e. "")! If setting it to null, the user will not be join-able.
     */
    val joinSecret: String? = null,
    val data: Data
) {
    data class Data(
        val id: String,
        val members: Int,
        val maximumMembers: Int
    )
}
