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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.Packet
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import java.util.Optional
import java.util.function.Consumer

class CosmeticEquipVisibilityResponse(
    private val nextState: Boolean,
    private val notification: Boolean,
) : Consumer<Optional<Packet>> {
    override fun accept(responseOptional: Optional<Packet>) {
        val packet = responseOptional.orElse(null) ?: return run {
            if (notification) {
                Notifications.error("Error", "Failed to toggle cosmetic visibility. Please try again.")
            }
        }

        if (packet is ResponseActionPacket && packet.isSuccessful) {
            Essential.getInstance().connectionManager.cosmeticsManager.ownCosmeticsVisible = nextState
            if (notification) {
                Notifications.push("Your cosmetics are ${if (nextState) "shown" else "hidden"}", "") {
                    if (nextState) {
                        withCustomComponent(Slot.ICON, EssentialPalette.COSMETICS_10X7.create())
                    } else {
                        withCustomComponent(Slot.ICON, EssentialPalette.COSMETICS_OFF_10X7.create())
                    }
                }
            }
        }
    }
}
