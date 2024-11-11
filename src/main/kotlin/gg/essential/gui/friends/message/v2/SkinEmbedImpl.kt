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

import com.mojang.authlib.GameProfile
import gg.essential.Essential
import gg.essential.api.profile.wrapped
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EmulatedUI3DPlayer
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.util.hoveredState
import gg.essential.gui.wardrobe.modals.SkinModal
import gg.essential.handlers.GameProfileManager
import gg.essential.mod.Skin
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.util.*

class SkinEmbedImpl(
    skin: Skin,
    messageWrapper: MessageWrapper,
) : SkinEmbed(skin, messageWrapper) {

    init {
        colorState.rebind(hoveredState().map { if (it) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON })

        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        val tempUUID = UUID.randomUUID()
        val profile = GameProfileManager.Overwrites(skin.hash, skin.model.type, null).apply(GameProfile(tempUUID, "EssentialBot")).wrapped()
        val ui3DPlayer = EmulatedUI3DPlayer(draggable = BasicState(false), profile = BasicState(profile))

        bubble.layoutAsBox(Modifier.width(103f).height(106f).hoverScope()) {
            ui3DPlayer(Modifier.height(73f))
            icon(EssentialPalette.EXPAND_6X, Modifier.alignBoth(Alignment.End(6f)).color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT))
        }

        val cosmeticsManager = Essential.getInstance().connectionManager.cosmeticsManager
        val skinsManager = Essential.getInstance().connectionManager.skinsManager

        bubble.onLeftClick {
            if (skinsManager.skins.get().size >= cosmeticsManager.wardrobeSettings.skinsLimit.get()) {
                Notifications.push("Error adding skin", "You have the maximum number of skins!")
                return@onLeftClick
            }
            USound.playButtonPress()
            GuiUtil.pushModal {
                SkinModal.add(it, skin, initialName = skinsManager.getNextIncrementalSkinName())
            }
        }

        bubble.onRightClick {
            messageWrapper.openOptionMenu(it, this@SkinEmbedImpl)
        }
    }

    override fun beginHighlight() {}
    override fun releaseHighlight() {}

}
