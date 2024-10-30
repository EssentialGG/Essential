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
package gg.essential.mixins.ext.client.gui

import gg.essential.gui.multiplayer.FriendsIndicator
import gg.essential.network.connectionmanager.serverdiscovery.NewServerDiscoveryManager
import net.minecraft.client.gui.ServerListEntryNormal

interface ServerListEntryNormalExt {
    fun `essential$getFriends`(): FriendsIndicator
    fun `essential$setImpressionConsumer`(consumer: NewServerDiscoveryManager.ImpressionConsumer)
}

val ServerListEntryNormalExt.friends get() = `essential$getFriends`()
fun ServerListEntryNormalExt.setImpressionConsumer(consumer: NewServerDiscoveryManager.ImpressionConsumer)
    = `essential$setImpressionConsumer`(consumer)
val ServerListEntryNormal.ext get() = this as ServerListEntryNormalExt
