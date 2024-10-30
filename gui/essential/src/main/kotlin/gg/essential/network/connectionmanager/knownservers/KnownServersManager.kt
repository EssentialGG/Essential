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
package gg.essential.network.connectionmanager.knownservers

import gg.essential.connectionmanager.common.model.knownserver.KnownServer
import gg.essential.connectionmanager.common.packet.knownservers.ClientKnownServersRequestPacket
import gg.essential.connectionmanager.common.packet.knownservers.ServerKnownServersResponsePacket
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class KnownServersManager(val connectionManager: CMConnection) : NetworkedManager {
    private val mutableKnownServers = mutableStateOf(listOf<KnownServer>())
    val knownServers: State<List<KnownServer>> = mutableKnownServers

    private val serversByAddress = knownServers.map { servers ->
        servers.flatMap { server -> server.addresses.filter { !isRegex(it) }.map { it to server } }.toMap()
    }

    private val serversByRegex = knownServers.map { servers ->
        servers.flatMap { server -> server.addresses.filter{ isRegex(it) }.map { Pattern.compile(it) to server } }.toMap()
    }

    override fun onConnected() {
        connectionManager.connectionScope.launch { refreshKnownServers() }
    }

    private suspend fun refreshKnownServers() {
        val response =
            connectionManager.call(ClientKnownServersRequestPacket())
                .exponentialBackoff()
                .await<ServerKnownServersResponsePacket>()
        mutableKnownServers.set(response.knownServers)
    }

    fun findServerByAddress(address: String): KnownServer? {
        serversByAddress.getUntracked()[address]?.let { return it }

        for ((pattern, server) in serversByRegex.getUntracked()) {
            if (pattern.matcher(address).matches()) {
                return server
            }
        }

        return null
    }

    fun normalizeAddress(address: String): String {
        findServerByAddress(address)?.let { return it.addresses[0] }
        return address
    }

    companion object {
        private fun isRegex(address: String) = address.startsWith("^") && address.endsWith("$")
    }
}
