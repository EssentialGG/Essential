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
import com.google.common.collect.MapMaker
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticPlayerSettingsPacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsUserEquippedPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ClientCosmeticOutfitSelectedRequestPacket
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitSelectedResponsePacket
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.mod.Model
import gg.essential.mod.Skin
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.connectionmanager.subscription.SubscriptionManager
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.network.cosmetics.toMod
import gg.essential.network.cosmetics.toModSetting
import gg.essential.network.registerPacketHandler
import gg.essential.util.USession
import java.util.*

class EquippedCosmeticsManager(
    private val connectionManager: CMConnection,
    private val subscriptionManager: SubscriptionManager,
    private val queueOwnMojangCape: (String) -> Unit,
    private val cosmeticsData: CosmeticsData,
    private val infraCosmeticsData: InfraCosmeticsData,
    private val applyPlayerSkin: (UUID, Skin) -> Unit,
    private val applyCapeModelPartEnabled: (Boolean) -> Unit,
) : NetworkedManager, SubscriptionManager.Listener {
    private val refHolder: ReferenceHolder = ReferenceHolderImpl()
    private val equippedCosmetics: MutableMap<UUID, Map<CosmeticSlot, String>> = mutableMapOf()
    private val visibleCosmetics: MutableMap<UUID, ImmutableMap<CosmeticSlot, EquippedCosmetic>> = mutableMapOf()
    private val visibleCosmeticsStates: MutableMap<UUID, MutableState<Map<CosmeticSlot, EquippedCosmetic>>> =
        MapMaker().weakValues().makeMap()
    private val cosmeticSettings: MutableMap<UUID, Map<String, List<CosmeticSetting>>> = mutableMapOf()
    private var ownCosmeticsVisible = true

    private val ownUuid: UUID
        get() = USession.activeNow().uuid

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
            val capeHash = equippedCosmetics[ownUuid]?.get(CosmeticSlot.CAPE)
            // Configure MC's cape visibility setting accordingly
            applyCapeModelPartEnabled(CAPE_DISABLED_COSMETIC_ID != capeHash)
        }

        connectionManager.registerPacketHandler<ServerCosmeticsUserEquippedPacket> { packet ->
            update(packet.uuid, packet.equipped.toMod(), cosmeticSettings[packet.uuid] ?: emptyMap())
        }
        connectionManager.registerPacketHandler<ServerCosmeticPlayerSettingsPacket> { packet ->
            update(packet.uuid, equippedCosmetics[packet.uuid] ?: emptyMap(), packet.settings.toModSetting())
        }
        connectionManager.registerPacketHandler<ServerCosmeticOutfitSelectedResponsePacket> { packet ->
            val skinTexture = packet.skinTexture
            if (skinTexture != null && skinTexture.contains(";")) {
                val (type, hash) = skinTexture.split(";")
                applyPlayerSkin(packet.uuid, Skin(hash, if (type == "1") Model.ALEX else Model.STEVE))
            }

            val equippedCosmetics = packet.equippedCosmetics ?: emptyMap()
            val cosmeticSettings = packet.cosmeticSettings ?: emptyMap()
            update(packet.uuid, equippedCosmetics.toMod(), cosmeticSettings.toModSetting())
        }
    }

    fun getOwnCosmeticsVisible(): Boolean {
        return ownCosmeticsVisible
    }

    fun setOwnCosmeticsVisible(state: Boolean) {
        ownCosmeticsVisible = state

        updateVisibleCosmetics(ownUuid)

        // Ensure config value matches current visibility
        if (EssentialConfig.ownCosmeticsHidden == state) {
            EssentialConfig.ownCosmeticsHidden = !state
        }
    }

    fun getEquippedCosmetics(): Map<CosmeticSlot, String> {
        return getEquippedCosmetics(ownUuid) ?: emptyMap()
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

            if (playerId == ownUuid) {
                val capeHash = equippedCosmetics[CosmeticSlot.CAPE]
                val capeDisabled = CAPE_DISABLED_COSMETIC_ID == capeHash
                if (!EssentialConfig.disableCosmetics) {
                    // Configure MC's cape visibility setting accordingly
                    applyCapeModelPartEnabled(!capeDisabled)
                }

                // And queue the cape to be updated at Mojang
                if (!capeDisabled && capeHash != null) {
                    queueOwnMojangCape(capeHash)
                }
            }
        }
    }

    private fun updateVisibleCosmetics(playerId: UUID) {
        val newValue = computeVisibleCosmetics(playerId)
        // Keep old instance if unchanged, so external comparisons against it can continue to take the fast path
        if (visibleCosmetics[playerId] != newValue) {
            visibleCosmetics[playerId] = newValue
            visibleCosmeticsStates[playerId]?.set(newValue)
        }
    }

    private fun computeVisibleCosmetics(playerId: UUID): ImmutableMap<CosmeticSlot, EquippedCosmetic> {
        val cosmeticIds = equippedCosmetics[playerId] ?: return ImmutableMap.of()
        val settings = cosmeticSettings[playerId] ?: emptyMap()

        val cosmeticsHidden = EssentialConfig.disableCosmetics || (!ownCosmeticsVisible && playerId == ownUuid)

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

    fun getVisibleCosmeticsState(playerId: UUID): State<Map<CosmeticSlot, EquippedCosmetic>> {
        return visibleCosmeticsStates.getOrPut(playerId) { mutableStateOf(visibleCosmetics[playerId] ?: emptyMap()) }
    }

    override fun resetState() {
        equippedCosmetics.clear()
        visibleCosmetics.clear()
        // Note: The visibleCosmeticsState map MUST NOT be cleared here because downstream states won't necessarily be
        //       re-created on reconnect but should still continue to receive future updates.
        //       It'll be cleaned up automatically as its entries become unused by virtue of having weak values.
        visibleCosmeticsStates.values.forEach { it.set(emptyMap()) }
        cosmeticSettings.clear()
    }

    override fun onSubscriptionAdded(uuids: Set<UUID>) {
        for (uuid in uuids) {
            connectionManager.call(ClientCosmeticOutfitSelectedRequestPacket(uuid))
                .fireAndForget()
        }
    }

    override fun onSubscriptionRemoved(uuids: Set<UUID>) {
        for (uuid in uuids) {
            equippedCosmetics.remove(uuid)
            visibleCosmetics.remove(uuid)
            cosmeticSettings.remove(uuid)
            // Note: The entry MUST NOT be removed from the visibleCosmeticsState map. See comment in [resetState].
            visibleCosmeticsStates[uuid]?.set(emptyMap())
        }
    }
}
