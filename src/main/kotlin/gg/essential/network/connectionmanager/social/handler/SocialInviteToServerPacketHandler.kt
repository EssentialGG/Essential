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
package gg.essential.network.connectionmanager.social.handler

import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.social.SocialInviteToServerPacket
import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.layoutAsColumn
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.toastButton
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.handler.PacketHandler
import gg.essential.universal.UMinecraft
import gg.essential.util.CachedAvatarImage
import gg.essential.util.MinecraftUtils
import gg.essential.util.Multithreading
import gg.essential.util.UUIDUtil
import gg.essential.util.executor
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

class SocialInviteToServerPacketHandler : PacketHandler<SocialInviteToServerPacket>() {
    private val cooldowns = mutableSetOf<UUID>()

    override fun onHandle(connectionManager: ConnectionManager, packet: SocialInviteToServerPacket) {
        if (!EssentialConfig.essentialEnabled) return

        val hostUUID = packet.uuid
        val address = packet.address
        connectionManager.socialManager.addIncomingServerInvite(hostUUID, address)

        if (cooldowns.contains(hostUUID)) return
        cooldowns.add(hostUUID)
        Multithreading.scheduleOnMainThread({ cooldowns.remove(hostUUID) }, NOTIFICATION_COOLDOWN_DURATION, TimeUnit.SECONDS)

        UUIDUtil.getName(hostUUID).thenAcceptAsync(
            { username ->
                Notifications.pushPersistentToast(username, "", {}, {}) {
                    withCustomComponent(Slot.ICON, CachedAvatarImage.create(hostUUID))

                    val textContainer = UIContainer()

                    textContainer.layoutAsColumn(Modifier.fillWidth().childBasedHeight(), Arrangement.spacedBy(2f), Alignment.Start) {
                        text("Sent you an invite to", Modifier.color(EssentialPalette.TEXT))
                        text("$address.", truncateIfTooSmall = true,
                            modifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT))
                    }

                    withCustomComponent(Slot.LARGE_PREVIEW, textContainer)

                    val button = toastButton(
                        "Join",
                        backgroundModifier = Modifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(
                            EssentialPalette.BLUE_BUTTON_HOVER
                        ).shadow(Color.BLACK),
                        textModifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT)
                            .shadow(EssentialPalette.TEXT_SHADOW)
                    ) {
                        MinecraftUtils.connectToServer(username, address)
                        connectionManager.socialManager.removeIncomingServerInvite(hostUUID)
                    }
                    withCustomComponent(Slot.ACTION, button)
                }
            },
            UMinecraft.getMinecraft().executor
        )

    }

    companion object {
        private const val NOTIFICATION_COOLDOWN_DURATION = 11L
    }
}