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
package gg.essential.util

import java.util.*

sealed interface ServerType {
    object Singleplayer : ServerType
    object Realms : ServerType

    data class Multiplayer(val name: String, val address: String) : ServerType, SupportsInvites

    sealed interface SPS : ServerType {
        val hostUuid: UUID

        data class Host(override val hostUuid: UUID) : SPS, SupportsInvites
        data class Guest(override val hostUuid: UUID) : SPS
    }

    val supportsInvites: Boolean
        get() = this is SupportsInvites

    sealed interface SupportsInvites : ServerType

    companion object {
        fun current(): ServerType? = GuiEssentialPlatform.platform.currentServerType()
    }
}
