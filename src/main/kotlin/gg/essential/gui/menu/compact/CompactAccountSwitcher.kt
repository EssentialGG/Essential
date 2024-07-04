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
package gg.essential.gui.menu.compact

import gg.essential.Essential
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.EssentialPalette
import gg.essential.gui.account.factory.InitialSessionFactory
import gg.essential.gui.common.*
import gg.essential.gui.elementa.effects.ZIndexEffect
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.modals.AddAccountModal
import gg.essential.gui.util.hoveredState
import gg.essential.handlers.PauseMenuDisplay.Companion.window
import gg.essential.util.*
import kotlin.math.floor

class CompactAccountSwitcher(
    accountsList: ObservableList<AccountManager.AccountInfo>,
    sidebarContainer: UIContainer,
    private val accountManager: AccountManager,
) : UIContainer() {

    private val switcherExpanded = BasicState(false)

    private val mainAccount by MenuButton {
        if (!this@CompactAccountSwitcher.hasFocus()) {
            this@CompactAccountSwitcher.grabWindowFocus()
        } else {
            this@CompactAccountSwitcher.releaseWindowFocus()
        }
    }.constrain {
        x = 0.pixels(alignOpposite = true)
        width = AspectConstraint()
        height = 20.pixels
    }.setIcon(
        USession.active.map { ImageFactory { CachedAvatarImage.ofUUID(it.uuid) } }.toV1(this),
        iconWidth = 8f,
        iconHeight = 8f,
    ).apply {
        bindEffect(ZIndexEffect(1, parent = sidebarContainer), hoveredState(layoutSafe = false))
    } childOf this

    private val switcherContainer by UIContainer().constrain {
        x = SiblingConstraint(-1f, alignOpposite = true) boundTo mainAccount
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    }.bindParent(sidebarContainer, switcherExpanded)

    private val accountScroller by ScrollComponent(
        horizontalScrollEnabled = true,
        verticalScrollEnabled = false,
        horizontalScrollOpposite = true,
        pixelsPerScroll = 19f,
    ).constrain {
        x = 0.pixels(alignOpposite = true)
        width = width.coerceAtMost(basicWidthConstraint { 19f * floor((window.getWidth() - 65f) / 19f) } + 1.pixel)
        height = 35.pixels
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    } childOf switcherContainer

    private val addAccount by MenuButton(
        defaultStyle = BasicState(MenuButton.BLUE),
        hoverStyle = BasicState(MenuButton.LIGHT_BLUE),
    ) { GuiUtil.pushModal { AddAccountModal(it) } }.constrain {
        // x set in init
        width = AspectConstraint()
        height = 20.pixels
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    }.setIcon(BasicState(EssentialPalette.PLUS_5X)).setTooltip("Add Account") childOf switcherContainer

    private val collapseButton by MenuButton(
        defaultStyle = BasicState(MenuButton.DARK_GRAY)
    ).constrain {
        x = SiblingConstraint(-1f, alignOpposite = true)
        width = 13.pixels
        height = 20.pixels
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    }.setIcon(BasicState(EssentialPalette.ARROW_RIGHT_3X5)) childOf switcherContainer

    init {
        constrain {
            x = 0.pixels(alignOpposite = true)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        val activeUser = USession.active.map { it.username }.toV1(this)
        mainAccount.setTooltip(activeUser, visibleState = mainAccount.hoveredState() and switcherExpanded, xAlignment = MenuButton.Alignment.RIGHT)
        mainAccount.bindEssentialTooltip(mainAccount.hoveredState() and !switcherExpanded, activeUser, EssentialTooltip.Position.LEFT)

        accountScroller.bindChildren(accountsList) { AccountButton(it, accountManager) }

        // Adjust addAccount button x constraint to avoid extra overlap if there are no extra accounts in the switcher
        fun adjustAddAccount() = addAccount.constrain {
            x = SiblingConstraint(if (accountsList.size > 0) -1f else 0f, alignOpposite = true)
        }
        accountsList.addObserver { _, _ -> adjustAddAccount() }
        adjustAddAccount()

        // Collapse switcher when clicking in or outside it
        onFocus { switcherExpanded.set(true) }
        onFocusLost { switcherExpanded.set(false) }
    }

    private class AccountButton(accountInfo: AccountManager.AccountInfo, accountManager: AccountManager) : UIContainer() {

        private val username = UUIDUtil.getNameAsState(accountInfo.uuid, accountInfo.name)

        private val accountButton by MenuButton(
            defaultStyle = BasicState(MenuButton.DARK_GRAY),
        ) { accountManager.login(accountInfo.uuid) }.constrain {
            width = 20.pixels
            height = AspectConstraint()
        }.apply {
            bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
        }.setIcon(BasicState(ImageFactory { CachedAvatarImage.ofUUID(accountInfo.uuid) }), iconHeight = 8f, iconWidth = 8f)
            .setTooltip(username) childOf this

        init {
            constrain {
                x = SiblingConstraint(-1f, alignOpposite = true)
                width = ChildBasedMaxSizeConstraint()
                height = ChildBasedSizeConstraint()
            }

            bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))

            // Check if the UUID belongs to the initial session, so we know if the delete button should be displayed
            val isInitialSession = Essential.getInstance().sessionFactories
                .filterIsInstance<InitialSessionFactory>()
                .any { accountInfo.uuid in it.sessions.keys }

            if (!isInitialSession) {
                val delete by MenuButton(
                    defaultStyle = BasicState(MenuButton.RED),
                    hoverStyle = BasicState(MenuButton.LIGHT_RED),
                ) { accountManager.promptRemove(accountInfo.uuid, username.get()) }.constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint(-1f) boundTo accountButton
                    width = 16.pixels
                    height = AspectConstraint()
                }.setIcon(BasicState(EssentialPalette.TRASH_9X))

                delete.bindParent(this, accountButton.hoveredState() or delete.hoveredState())

                // Highlight accountButton when delete button is hovered
                delete.hoveredState(layoutSafe = false).onSetValueAndNow { accountButton.hoveredStyleOverrides.set(it) }
            }
        }
    }
}
