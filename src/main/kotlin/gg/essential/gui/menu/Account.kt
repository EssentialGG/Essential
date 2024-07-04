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
package gg.essential.gui.menu

import gg.essential.Essential
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.account.factory.InitialSessionFactory
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.bindEffect
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.effects.ZIndexEffect
import gg.essential.gui.image.ImageFactory
import gg.essential.util.CachedAvatarImage
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.gui.util.hoveredState

class Account(
    accountInfo: AccountManager.AccountInfo,
    collapsed: State<Boolean>,
    accountManager: AccountManager,
) : UIContainer() {

    private val nameState = BasicState(accountInfo.name)
    private val hovered = hoveredState()

    init {
        constrain {
            y = SiblingConstraint(-1f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    }

    private val accountButton by MenuButton(
        nameState,
        textAlignment = MenuButton.Alignment.LEFT,
        collapsedText = nameState.map { it },
        truncate = true,
    ) {
        accountManager.login(accountInfo.uuid)
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    }.setIcon(
        BasicState(ImageFactory { CachedAvatarImage.ofUUID(accountInfo.uuid) }),
        iconWidth = 8f,
        iconHeight = 8f,
    ).bindCollapsed(collapsed, 50f) childOf this

    init {
        accountButton.setTooltip(
            nameState,
            above = false,
            accountButton.isTruncated,
            followCursorX = true,
            followCursorY = true,
            xOffset = 4f,
            yOffset = 4f,
            notchSize = 0
        )

        // Check if the UUID belongs to the initial session
        val isInitialSession = Essential.getInstance().sessionFactories
            .filterIsInstance<InitialSessionFactory>()
            .any { accountInfo.uuid in it.sessions.keys }

        if (!isInitialSession) {
            MenuButton(
                defaultStyle = BasicState(MenuButton.RED),
                hoverStyle = BasicState(MenuButton.LIGHT_RED)
            ) { accountManager.promptRemove(accountInfo.uuid, nameState.get()) }.constrain {
                x = SiblingConstraint(-1f) boundTo accountButton
                width = 14.pixels
                height = 20.pixels
            }.apply {
                bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))

                // Highlight accountButton when delete button is hovered
                hoveredState(layoutSafe = false).onSetValueAndNow { accountButton.hoveredStyleOverrides.set(it) }
            }.setIcon(BasicState(EssentialPalette.TRASH_9X))
                .bindHoverEssentialTooltip(BasicState("Remove Account"))
                .bindParent(this, hovered)
        }
    }
}
