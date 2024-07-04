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

import com.google.common.collect.ImmutableMap
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticPlayerSettingsPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserEquippedPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitSelectedRequestPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitSelectedResponsePacket
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.handler.PacketHandler
import gg.essential.network.connectionmanager.handler.cosmetics.ServerCosmeticOutfitSelectedResponsePacketHandler
import gg.essential.network.connectionmanager.subscription.SubscriptionManager
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.network.cosmetics.cape.CapeCosmeticsManager
import gg.essential.network.cosmetics.toMod
import gg.essential.network.cosmetics.toModSetting
import gg.essential.util.UUIDUtil
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EnumPlayerModelParts
import java.util.*

class EquippedCosmeticsManager(
    private val connectionManager: ConnectionManager,
    private val capeManager: CapeCosmeticsManager,
    private val cosmeticsData: CosmeticsData,
    private val infraCosmeticsData: InfraCosmeticsData
) : NetworkedManager, SubscriptionManager.Listener {
    private val refHolder: ReferenceHolder = ReferenceHolderImpl()
    private val subscriptionManager: SubscriptionManager = connectionManager.subscriptionManager
    private val equippedCosmetics: MutableMap<UUID, Map<CosmeticSlot, String>> = mutableMapOf()
    private val visibleCosmetics: MutableMap<UUID, ImmutableMap<CosmeticSlot, EquippedCosmetic>> = mutableMapOf()
    private val cosmeticSettings: MutableMap<UUID, Map<String, List<CosmeticSetting>>> = mutableMapOf()
    private var ownCosmeticsVisible = true

    init {
        subscriptionManager.addListener(this)

        cosmeticsData.onNewCosmetic(refHolder) { cosmetic: Cosmetic ->
            for ((key, value) in equippedCosmetics) {
                if (cosmetic.id == value[cosmetic.type.slot]) {
                    updateVisibleCosmetics(key)
                }
            }
        }

        EssentialConfig.disableCosmeticsState.onSetValue(refHolder) { cosmeticsDisabled: Boolean ->
            for (playerId in equippedCosmetics.keys) {
                updateVisibleCosmetics(playerId)
            }
            if (cosmeticsDisabled) {
                return@onSetValue
            }
            val capeHash = equippedCosmetics[UUIDUtil.getClientUUID()]?.get(CosmeticSlot.CAPE)
            // Configure MC's cape visibility setting accordingly
            setModelPartEnabled(EnumPlayerModelParts.CAPE, CAPE_DISABLED_COSMETIC_ID != capeHash)
        }

        connectionManager.registerPacketHandler(object : PacketHandler<ServerCosmeticsUserEquippedPacket>() {
            override fun onHandle(connectionManager: ConnectionManager, packet: ServerCosmeticsUserEquippedPacket) {
                update(packet.uuid, packet.equipped.toMod(), cosmeticSettings[packet.uuid] ?: emptyMap())
            }
        })
        connectionManager.registerPacketHandler(object : PacketHandler<ServerCosmeticPlayerSettingsPacket>() {
            override fun onHandle(connectionManager: ConnectionManager, packet: ServerCosmeticPlayerSettingsPacket) {
                update(packet.uuid, equippedCosmetics[packet.uuid] ?: emptyMap(), packet.settings.toModSetting())
            }
        })
        connectionManager.registerPacketHandler(ServerCosmeticOutfitSelectedResponsePacket::class.java, ServerCosmeticOutfitSelectedResponsePacketHandler())
    }

    fun getOwnCosmeticsVisible(): Boolean {
        return ownCosmeticsVisible
    }

    fun setOwnCosmeticsVisible(state: Boolean) {
        ownCosmeticsVisible = state

        updateVisibleCosmetics(UUIDUtil.getClientUUID())

        // Ensure config value matches current visibility
        if (EssentialConfig.ownCosmeticsHidden == state) {
            EssentialConfig.ownCosmeticsHidden = !state
        }
    }

    fun getEquippedCosmetics(): Map<CosmeticSlot, String> {
        return getEquippedCosmetics(UUIDUtil.getClientUUID()) ?: emptyMap()
    }

    fun getEquippedCosmetics(playerId: UUID): Map<CosmeticSlot, String>? {
        return equippedCosmetics[playerId]
    }

    fun update(
        playerId: UUID,
        equippedCosmetics: Map<CosmeticSlot, String>,
        settings: Map<String, List<CosmeticSetting>>, // may include settings for cosmetics not presently equipped
    ) {
        if (subscriptionManager.isSubscribedOrSelf(playerId)) {
            infraCosmeticsData.requestCosmeticsIfMissing(equippedCosmetics.values)

            this.equippedCosmetics[playerId] = equippedCosmetics
            this.cosmeticSettings[playerId] = settings

            updateVisibleCosmetics(playerId)

            if (playerId == UUIDUtil.getClientUUID()) {
                val capeHash = equippedCosmetics[CosmeticSlot.CAPE]
                val capeDisabled = CAPE_DISABLED_COSMETIC_ID == capeHash
                if (!EssentialConfig.disableCosmetics) {
                    // Configure MC's cape visibility setting accordingly
                    setModelPartEnabled(EnumPlayerModelParts.CAPE, !capeDisabled)
                }

                // And queue the cape to be updated at Mojang
                if (!capeDisabled && capeHash != null) {
                    capeManager.queueCape(capeHash)
                }
            }
        }
    }

    private fun updateVisibleCosmetics(playerId: UUID) {
        val newValue = computeVisibleCosmetics(playerId)
        // Keep old instance if unchanged, so external comparisons against it can continue to take the fast path
        if (visibleCosmetics[playerId] != newValue) {
            visibleCosmetics[playerId] = newValue
        }
    }

    private fun computeVisibleCosmetics(playerId: UUID): ImmutableMap<CosmeticSlot, EquippedCosmetic> {
        val cosmeticIds = equippedCosmetics[playerId] ?: return ImmutableMap.of()
        val settings = cosmeticSettings[playerId] ?: emptyMap()

        val cosmeticsHidden = !ownCosmeticsVisible && playerId == UUIDUtil.getClientUUID()

        fun isVisible(slot: CosmeticSlot): Boolean {
            if (slot == CosmeticSlot.ICON) {
                return true
            }

            if (cosmeticsHidden && slot != CosmeticSlot.EMOTE) {
                return false
            }

            if (slot == CosmeticSlot.EMOTE && EssentialConfig.disableEmotes) {
                return false
            }

            return true
        }

        val builder = ImmutableMap.Builder<CosmeticSlot, EquippedCosmetic>()

        for ((slot, value) in cosmeticIds) {
            if (!isVisible(slot)) continue
            val cosmetic = cosmeticsData.getCosmetic(value) ?: continue
            builder.put(slot, EquippedCosmetic(cosmetic, settings[value] ?: emptyList()))
        }

        return builder.build()
    }

    fun getVisibleCosmetics(playerId: UUID): ImmutableMap<CosmeticSlot, EquippedCosmetic> {
        return visibleCosmetics[playerId] ?: ImmutableMap.of()
    }

    override fun resetState() {
        equippedCosmetics.clear()
        visibleCosmetics.clear()
        cosmeticSettings.clear()
    }

    override fun onSubscriptionAdded(uuids: Set<UUID>) {
        for (uuid in uuids) {
            connectionManager.send(ClientCosmeticOutfitSelectedRequestPacket(uuid))
        }
    }

    override fun onSubscriptionRemoved(uuids: Set<UUID>) {
        for (uuid in uuids) {
            equippedCosmetics.remove(uuid)
            visibleCosmetics.remove(uuid)
            cosmeticSettings.remove(uuid)
        }
    }

    companion object {
        private fun setModelPartEnabled(part: EnumPlayerModelParts, enabled: Boolean) {
            val gameSettings = Minecraft.getMinecraft().gameSettings
            //#if MC>=11700
            //$$ if (gameSettings.isPlayerModelPartEnabled(part) != enabled) {
            //#else
            if (gameSettings.modelParts.contains(part) != enabled) {
            //#endif
                gameSettings.setModelPartEnabled(part, enabled)
            }
        }
    }
}
