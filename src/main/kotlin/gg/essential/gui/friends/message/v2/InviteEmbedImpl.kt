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
package gg.essential.gui.friends.message.v2

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.network.connectionmanager.ConnectionManager
import java.awt.image.BufferedImage

class ServerInfo(private val address: String, private val connectionManager: ConnectionManager) {
    val playerCount: MutableState<Int?> = mutableStateOf(null)
    val maxPlayers: MutableState<Int?> = mutableStateOf(null)
    val version: MutableState<String?> = mutableStateOf(null)
    val icon: MutableState<BufferedImage?> = mutableStateOf(null)

}
