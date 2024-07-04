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
package gg.essential.handlers.discord.activity.provider.impl

import gg.essential.handlers.discord.DiscordIntegration
import gg.essential.handlers.discord.activity.ActivityState
import gg.essential.handlers.discord.activity.provider.ActivityStateProvider
import gg.essential.util.ServerType
import gg.essential.util.UUIDUtil

class GameActivityStateProvider : ActivityStateProvider {
    private var state: ServerType? = null
        set(value) {
            val shouldRegenerateKey = when (val castedField = field) {
                is ServerType.Multiplayer -> value !is ServerType.Multiplayer || castedField.address != value.address
                is ServerType.SPS.Host -> value !is ServerType.SPS.Host
                else -> false
            }

            if (shouldRegenerateKey) {
                DiscordIntegration.regenerateSpsJoinKey()
            }

            field = value
        }

    override fun provide(): ActivityState? {
        this.state = ServerType.current()

        return when (val state = this.state) {
            is ServerType.Singleplayer -> ActivityState.Singleplayer
            is ServerType.Multiplayer -> ActivityState.Multiplayer(state.address)

            is ServerType.Realms -> ActivityState.Realm

            is ServerType.SPS.Guest -> {
                val uuid = state.hostUuid
                val username = UUIDUtil.getName(uuid).join()

                ActivityState.SPSGuest(username)
            }

            is ServerType.SPS.Host -> ActivityState.SPSHost

            null -> null
        }
    }
}

