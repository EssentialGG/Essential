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
package gg.essential.network.connectionmanager.skins

import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinCreatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinDeletePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateDataPacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateFavoriteStatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateLastUsedStatePacket
import gg.essential.connectionmanager.common.packet.skin.ClientSkinUpdateNamePacket
import gg.essential.connectionmanager.common.packet.skin.ServerSkinPopulatePacket
import gg.essential.cosmetics.SkinId
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.wardrobe.Item
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.handler.skins.ServerSkinPopulatePacketHandler
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue
import gg.essential.network.cosmetics.toInfra
import gg.essential.network.cosmetics.toMod
import gg.essential.util.*
import java.util.concurrent.CompletableFuture

class SkinsManager(val connectionManager: ConnectionManager) : NetworkedManager {

    private val packetQueue = SequentialPacketQueue.Builder(connectionManager).onTimeoutSkip().create()

    // Actual data
    private val mutableSkins = mutableStateOf<Map<SkinId, Item.SkinItem>>(mapOf())

    // Derived data
    val skins: State<Map<SkinId, Item.SkinItem>> = mutableSkins
    val skinsOrdered = mutableSkins.map { skins ->
        skins.values.sortedWith(
            compareBy<Item.SkinItem> { it.favoritedSince?.toEpochMilli() }
                .thenByDescending { (it.lastUsedAt ?: it.createdAt).toEpochMilli() }
        )
    }.toListState()

    init {
        connectionManager.registerPacketHandler(ServerSkinPopulatePacket::class.java, ServerSkinPopulatePacketHandler())
    }


    override fun onConnected() {
        // Infra sends us a packet with all the skins when we connect
        resetState()
    }

    override fun resetState() {
        mutableSkins.set { mapOf() }
    }

    fun getSkin(id: SkinId) = skins.map { it[id] }

    fun getNextIncrementalSkinName(): String {
        val skinNames = skins.get().map { it.value.name }.toSet()
        return (skinNames.size + 1 .. Int.MAX_VALUE).firstNotNullOf { i ->
            "Skin #$i".takeUnless { it in skinNames }
        }
    }

    fun addSkin(name: String, skin: Skin, selectSkin: Boolean = true, favorite: Boolean = false): CompletableFuture<Item.SkinItem> {
        val future = CompletableFuture<Item.SkinItem>()
        connectionManager.send(ClientSkinCreatePacket(name, skin.model.toInfra(), skin.hash, favorite)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            if (response is ServerSkinPopulatePacket) {
                val skinResponse = response.skins
                if (skinResponse.isEmpty()) {
                    future.completeExceptionally(IllegalStateException("Received empty reply when creating skin!"))
                    return@send
                }
                // We don't add the skin here, since the packet handler adds it already
                val skinItem = skinResponse.first().toMod()
                if (selectSkin) {
                    selectSkin(skinItem)
                }
                future.complete(skinItem)
            } else {
                future.completeExceptionally(IllegalStateException("Failed to add skin!"))
            }
        }
        return future
    }

    fun openDeleteSkinModal(skin: Item.SkinItem) {
        if (connectionManager.outfitManager.outfits.get().any { it.skinId == skin.id }) {
            Notifications.error("Can’t delete skin", "Skin is currently used on one or more outfits.")
            return
        }
        GuiUtil.pushModal { manager -> 
            DangerConfirmationEssentialModal(manager, "Delete", false)
                .configure { contentText = "Are you sure you want to delete\n${skin.name}?" }
                .onPrimaryAction { deleteSkin(skin) }
        }
    }

    private fun deleteSkin(skin: Item.SkinItem) {
        connectionManager.send(ClientSkinDeletePacket(skin.id)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            if (response is ResponseActionPacket && response.isSuccessful) {
                mutableSkins.set { it - skin.id }
            } else {
                Notifications.push("Error deleting skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun setFavoriteState(skin: Item.SkinItem, favorite: Boolean) {
        packetQueue.enqueue(ClientSkinUpdateFavoriteStatePacket(skin.id, favorite)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun renameSkin(skin: Item.SkinItem, name: String) {
        packetQueue.enqueue(ClientSkinUpdateNamePacket(skin.id, name)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun updateLastUsedAtState(skin: Item.SkinItem) {
        packetQueue.enqueue(ClientSkinUpdateLastUsedStatePacket(skin.id)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun setSkinModel(skin: Item.SkinItem, model: Model) {
        packetQueue.enqueue(ClientSkinUpdateDataPacket(skin.id, model.toInfra(), skin.skin.hash)) { maybeResponse ->
            val response = maybeResponse.orElse(null)
            // We don't replace the skin here, since the packet handler already replaces it
            if (response !is ServerSkinPopulatePacket) {
                Notifications.push("Error updating skin", "An unexpected error has occurred. Try again.")
            }
        }
    }

    fun selectSkin(skin: Item.SkinItem) {
        connectionManager.outfitManager.updateOutfitSkin(skin.id, false)
    }

    fun onSkinPopulate(skins: Map<SkinId, Item.SkinItem>) {
        mutableSkins.set { map -> map + skins }
    }

}
