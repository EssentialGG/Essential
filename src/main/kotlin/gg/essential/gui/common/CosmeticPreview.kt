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
package gg.essential.gui.common

import com.mojang.authlib.GameProfile
import gg.essential.api.profile.wrapped
import gg.essential.cosmetics.EquippedCosmetic
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.handlers.GameProfileManager
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.network.cosmetics.Cosmetic
import java.util.UUID

class CosmeticPreview(val cosmetic: Cosmetic, val settings: State<List<CosmeticSetting>> = mutableListStateOf()) : UIContainer() {

    val uuid = UUID.randomUUID()

    private var loading = true
    private var loadingIcon = LoadingIcon(2.0)
    private val emulatedUI3DPlayer: EmulatedUI3DPlayer
    private val emoteScheduler: EmoteScheduler?

    init {
        val profile = if (cosmetic.type.slot == CosmeticSlot.EMOTE) {
            null // A null profile will copy the player's profile so it will adapt to skin changes
        } else {
            GameProfileManager.Overwrites(
                "f91f0820500c414d308c5678594631b917e51e06a31fedaacac5dad1a44a49d8",
                "default",
                null,
            ).apply(GameProfile(uuid, "EssentialBot"))
        }?.wrapped()
        val slot = cosmetic.type.slot
        emoteScheduler = if (slot == CosmeticSlot.EMOTE) EmoteScheduler(this, stateOf(cosmetic), settings) else null
        emulatedUI3DPlayer = EmulatedUI3DPlayer(
            draggable = BasicState(false),
            profile = BasicState(profile),
        ).apply {
            setRotations(0f, 0f)
            constrain {
                width = 100.percent
                height = 100.percent
            }
            perspectiveCamera = PerspectiveCamera.forCosmeticSlot(slot)
            cosmeticsSource =
                memo {
                    buildMap {
                        if (emoteScheduler == null || emoteScheduler.emoteEquipped()) {
                            put(slot, EquippedCosmetic(cosmetic, settings()))
                        }
                        if (slot == CosmeticSlot.EMOTE) {
                            // hide third party capes for emote previews, which use the player's profile
                            put(CosmeticSlot.CAPE, EquippedCosmetic(CAPE_DISABLED_COSMETIC, emptyList()))
                        }
                    }
                }
        }
        addChild(emulatedUI3DPlayer)

        // Hide player while loading (we do need to add it as a child so it actually loads)
        emulatedUI3DPlayer.enableEffect(ScissorEffect(0f, 0f, 0f, 0f))
        addChild(loadingIcon)
    }

    override fun animationFrame() {
        if (loading) {
            val wearablesManager = emulatedUI3DPlayer.wearablesManager
            if (wearablesManager != null && wearablesManager.state.cosmetics.isNotEmpty()) {
                loading = false
                removeChild(loadingIcon)
                // Replace the zero-size scissor effect we used while loading with a regularly sized one
                emulatedUI3DPlayer.removeEffect<ScissorEffect>()
                emulatedUI3DPlayer.enableEffect(ScissorEffect())

            }
        }

        super.animationFrame()
    }
}
