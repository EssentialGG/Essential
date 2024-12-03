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
import dev.folomeev.kotgl.matrix.vectors.vecUnitY
import gg.essential.api.profile.wrapped
import gg.essential.cosmetics.CosmeticId
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.then
import gg.essential.gui.wardrobe.Item
import gg.essential.handlers.GameProfileManager
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.preview.PerspectiveCamera
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.gui.util.onAnimationFrame
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.model.util.Quaternion
import gg.essential.model.util.rotateBy
import java.util.*

fun LayoutScope.skinRenderPreview(skin: Item.SkinItem, modifier: Modifier = Modifier) {
    val profile = GameProfileManager.Overwrites(skin.skin.hash, skin.skin.model.type, null)
        .apply(GameProfile(UUID.randomUUID(), "EssentialBot")).wrapped()
    EmulatedUI3DPlayer(
        draggable = BasicState(false),
        profile = BasicState(profile)
    ).apply {
        setRotations(0f, 0f)
        perspectiveCamera = PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)
        cosmeticsSource = stateOf(emptyMap())
    }(Modifier.fillParent().effect { ScissorEffect() }.then(modifier))
}

fun LayoutScope.outfitRenderPreview(state: WardrobeState, outfit: Item.OutfitItem, modifier: Modifier = Modifier) {
    fullBodyRenderPreview(
        state, outfit.skin, outfit.cosmetics, outfit.settings,
        showEmotes = false, constantRotation = false, modifier = modifier
    )
}

fun LayoutScope.bundleRenderPreview(state: WardrobeState, bundle: Item.Bundle, modifier: Modifier = Modifier) {
    fullBodyRenderPreview(
        state, bundle.skin.toMod(), bundle.cosmetics, bundle.settings,
        showEmotes = true, constantRotation = bundle.rotateOnPreview, modifier = modifier
    )
}

fun LayoutScope.fullBodyRenderPreview(
    state: WardrobeState,
    skin: gg.essential.mod.Skin?,
    cosmeticsMap: Map<CosmeticSlot, CosmeticId>,
    settings: Map<CosmeticId, List<CosmeticSetting>>,
    showEmotes: Boolean,
    constantRotation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val loading = mutableStateOf(cosmeticsMap.isNotEmpty())
    val emulatedUI3DPlayerModifierState = loading.map {
        Modifier.fillParent().effect { if (it) ScissorEffect(0f, 0f, 0f, 0f) else ScissorEffect() }
    }
    val profile = if (skin != null) {
        GameProfileManager.Overwrites(
            skin.hash,
            skin.model.type,
            null,
        ).apply(GameProfile(UUID.randomUUID(), "EssentialBot")).wrapped()
    } else {
        null
    }
    box(Modifier.fillParent().then(modifier)) {
        val emulatedUI3DPlayer = EmulatedUI3DPlayer(
            draggable = BasicState(false),
            profile = BasicState(profile)
        ).apply {
            setRotations(0f, 0f)
            val visibleCosmeticIds = if (showEmotes) cosmeticsMap else cosmeticsMap - CosmeticSlot.EMOTE
            cosmeticsSource = memo { with(state) { resolveCosmeticIds(visibleCosmeticIds, settings) } }

            if (constantRotation) {
                val fullRotationMillis = 10000.0
                val angleStep = 360.0 / fullRotationMillis
                onAnimationFrame {
                    val angle = ((((System.currentTimeMillis() - state.wardrobeOpenTime) * angleStep) % 360.0) * Math.PI / 180.0).toFloat()
                    val rotation = Quaternion.fromAxisAngle(vecUnitY(), angle)
                    perspectiveCamera = with(PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)) {
                        copy(camera = camera.rotateBy(rotation), target = target.rotateBy(rotation))
                    }
                }
            } else {
                perspectiveCamera = PerspectiveCamera.forCosmeticSlot(CosmeticSlot.FULL_BODY)
            }
        }(Modifier.then(emulatedUI3DPlayerModifierState))

        if_(loading) {
            LoadingIcon(2.0)().onAnimationFrame {
                loading.set(emulatedUI3DPlayer.wearablesManager?.state?.cosmetics.isNullOrEmpty() && cosmeticsMap.isNotEmpty())
            }
        }
    }
}
