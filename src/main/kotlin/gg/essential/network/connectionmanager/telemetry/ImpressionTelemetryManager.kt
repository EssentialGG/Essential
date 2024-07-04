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
package gg.essential.network.connectionmanager.telemetry

import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.cosmetics.CosmeticId
import gg.essential.event.network.server.ServerLeaveEvent
import gg.essential.util.UUIDUtil
import me.kbrewster.eventbus.Subscribe
import java.util.*

object ImpressionTelemetryManager {
    private val telemetryManager = Essential.getInstance().connectionManager.telemetryManager
    private val impressions = mutableMapOf<String, MutableSet<UUID>>()

    fun initialize() {
        Essential.EVENT_BUS.register(this)
    }

    fun addImpression(cosmetic: CosmeticId, uuid: UUID) {
        if (uuid == UUIDUtil.getClientUUID()) return
        impressions.getOrPut(cosmetic) { mutableSetOf() }.add(uuid)
    }

    @Subscribe
    fun onServerLeave(event: ServerLeaveEvent) {
        if (impressions.isEmpty()) return

        telemetryManager.enqueue(ClientTelemetryPacket(
            "COSMETIC_IMPRESSIONS",
            mapOf("impressions" to impressions.mapValues { it.value.size })
        ))
        
        impressions.clear()
    }
}