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
package gg.essential.gui.friends.state

import gg.essential.connectionmanager.common.enums.ProfileStatus
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.common.ReadOnlyState
import gg.essential.network.connectionmanager.profile.ProfileManager
import gg.essential.network.connectionmanager.sps.SPSManager
import gg.essential.util.AddressUtil
import gg.essential.util.MinecraftUtils
import gg.essential.util.UUIDUtil
import gg.essential.util.thenAcceptOnMainThread
import java.util.*


class StatusStateManagerImpl(
    private val profileManager: ProfileManager,
    private val spsManager: SPSManager
) : IStatusStates, IStatusManager {

    private val statesMap = mutableMapOf<UUID, State<PlayerActivity>>()

    init {
        profileManager.registerStateManager(this)
        spsManager.registerStateManager(this)
    }

    override fun getActivityState(uuid: UUID): ReadOnlyState<PlayerActivity> {
        return ReadOnlyState(getWritableState(uuid))
    }

    override fun getActivity(uuid: UUID): PlayerActivity {
        val status = profileManager.getStatus(uuid)
        val activity = profileManager.getActivity(uuid).orElse(null)
        return when (status) {
            ProfileStatus.OFFLINE -> PlayerActivity.Offline(
                null
            )
            ProfileStatus.ONLINE -> when (activity) {
                null -> {
                    if (spsManager.remoteSessions.any { it.hostUUID == uuid }) {
                        PlayerActivity.SPSSession(uuid, true)
                    } else {
                        PlayerActivity.Online
                    }
                }
                else -> {
                    val address = activity.second
                    if (AddressUtil.isSpecialFormattedAddress(address)) {
                        return PlayerActivity.OnlineWithDescription(address)
                    }
                    spsManager.getHostFromSpsAddress(address)?.let { host ->
                        return PlayerActivity.SPSSession(
                            host,
                            spsManager.remoteSessions.any { it.hostUUID == host }
                        )
                    }
                    PlayerActivity.Multiplayer(address)
                }
            }
        }
    }

    override fun joinSession(uuid: UUID): Boolean {
        val activity = getActivity(uuid)
        val address = when {
            activity is PlayerActivity.Multiplayer -> activity.serverAddress
            activity is PlayerActivity.SPSSession && activity.invited -> spsManager.getSpsAddress(uuid)
            else -> return false
        }
        UUIDUtil.getName(uuid).thenAcceptOnMainThread {
            MinecraftUtils.connectToServer(it, address)
        }
        return true
    }

    private fun getWritableState(uuid: UUID): State<PlayerActivity> {
        return statesMap.computeIfAbsent(uuid) {
            BasicState(getActivity(uuid))
        }
    }

    override fun refreshActivity(uuid: UUID) {
        getWritableState(uuid).set(getActivity(uuid))

        // If player B is on player A's SPS session and player A revokes the invite,
        // we must also update the session for player B, and all other players on that session
        statesMap.entries.forEach {
            if (it.key == uuid) {
                return
            }
            val activity = it.value.get()
            if (activity is PlayerActivity.SPSSession && activity.host == uuid) {
                refreshActivity(it.key)
            }
        }
    }

}

