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
package gg.essential.network.connectionmanager.serverdiscovery

import gg.essential.connectionmanager.common.packet.serverdiscovery.ClientServerDiscoveryRequestPacket
import gg.essential.connectionmanager.common.packet.serverdiscovery.ServerServerDiscoveryResponsePacket
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.connectionmanager.common.model.serverdiscovery.Server
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.knownservers.KnownServersManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.function.Consumer

class NewServerDiscoveryManager(
    val connectionManager: CMConnection,
    val knownServersManager: KnownServersManager,
    val telemetryPacketConsumer: Consumer<ClientTelemetryPacket>,
) : NetworkedManager {
    private var refreshServersJob: Job? = null

    private val mutableServers = mutableStateOf(Servers(listOf(), listOf()))
    val servers: State<Servers> = mutableServers

    override fun onConnected() {
        refreshServers()
    }

    fun refreshServers() {
        if (refreshServersJob == null || refreshServersJob?.isCompleted == true) {
            refreshServersJob = connectionManager.connectionScope.launch {
                val response =
                    connectionManager.call(ClientServerDiscoveryRequestPacket())
                        .exponentialBackoff()
                        .await<ServerServerDiscoveryResponsePacket>()
                mutableServers.set(Servers(response.featuredServers, response.recommendedServers))
            }
        }
    }

    fun interface ImpressionConsumer : Consumer<String>

    inner class ImpressionTracker {
        private val featured = mutableSetOf<String>()
        private val recommended = mutableSetOf<String>()

        val featuredConsumer = ImpressionConsumer { trackImpression(it, featured::add) }
        val recommendedConsumer = ImpressionConsumer { trackImpression(it, recommended::add) }

        private fun trackImpression(ip: String, idConsumer: Consumer<String>) {
            knownServersManager.findServerByAddress(ip)?.also { idConsumer.accept(it.id) }
        }

        fun submit() {
            if (featured.isEmpty() && recommended.isEmpty()) return
            telemetryPacketConsumer.accept(ClientTelemetryPacket("DISCOVER_SERVER_IMPRESSION_1", mapOf(
                "featured" to featured.toSet(),
                "recommended" to recommended.toSet(),
            )))
            featured.clear()
            recommended.clear()
        }
    }

    data class Servers(val featured: List<Server>, val recommended: List<Server>) {
        fun isEmpty() = featured.isEmpty() && recommended.isEmpty()
    }

    companion object {
        val NEW_TAG_END_DATE = Instant.parse("2024-12-01T00:00:00Z")
    }
}
