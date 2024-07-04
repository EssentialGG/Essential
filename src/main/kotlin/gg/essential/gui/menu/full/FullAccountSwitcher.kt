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
package gg.essential.gui.menu.full

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.PredicatedHitTestContainer
import gg.essential.gui.elementa.effects.ZIndexEffect
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.menu.Account
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.modals.AddAccountModal
import gg.essential.gui.util.hoveredState
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

class FullAccountSwitcher(
    accountsList: ObservableList<AccountManager.AccountInfo>,
    collapsed: State<Boolean>,
    private val accountManager: AccountManager
) : UIContainer() {

    private val accountsExpanded = BasicState(false)
    private val showingScrollbarContainer = BasicState(false)
    private val scrollBoundingBoxOnTop = BasicState(false)
    private val pixelsPerScroll = 19f

    // Container for the main and collapse buttons
    private val buttonContainer by UIContainer().constrain {
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    }.onLeftClick {
        if (!this@FullAccountSwitcher.hasFocus()) {
            this@FullAccountSwitcher.grabWindowFocus()
        } else {
            this@FullAccountSwitcher.releaseWindowFocus()
        }
        it.stopPropagation()
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    } childOf this

    private val mainButton by MenuButton(
        USession.active.map { it.username }.toV1(this),
        textAlignment = MenuButton.Alignment.LEFT,
        collapsedText = USession.active.map { it.username }.toV1(this),
        truncate = true,
    ).constrain {
        width = 80.pixels
        height = 20.pixels
    }.apply {
        bindEffect(ZIndexEffect(1), hoveredState(layoutSafe = false))
    }.setIcon(
        USession.active.map { ImageFactory { CachedAvatarImage.ofUUID(it.uuid) } }.toV1(this),
        iconWidth = 8f,
        iconHeight = 8f
    ).bindCollapsed(collapsed, 50f) childOf buttonContainer

    private val collapseButton by MenuButton().constrain {
        x = SiblingConstraint(-1f)
        width = 14.pixels
        height = 20.pixels
    }.setIcon(
        accountsExpanded.map {
            if (it) EssentialPalette.ARROW_UP_7X5 else EssentialPalette.ARROW_DOWN_7X5
        },
    ) childOf buttonContainer

    private val scrollBoundingBox by PredicatedHitTestContainer().constrain {
        x = 0.pixels boundTo mainButton
        y = SiblingConstraint(-1f) boundTo mainButton
        width = 100.percent boundTo buttonContainer
        height = 115.pixels
    }
        .bindParent(this, accountsExpanded, index = 0)
        .bindEffect(ZIndexEffect(0), scrollBoundingBoxOnTop)

    private val scrollbarContainer by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        x = 11.pixels(alignOpposite = true)
        y = CenterConstraint()
        width = 2.pixels
        height = 100.percent - 2.pixels // Offsetting the outline effect
    }
        .onMouseClick { this@FullAccountSwitcher.grabWindowFocus() }
        .effect(OutlineEffect(
            EssentialPalette.BLACK,
            width = 1f,
            sides = setOf(OutlineEffect.Side.Right, OutlineEffect.Side.Bottom, OutlineEffect.Side.Left)
        ))
        .bindParent(parent = scrollBoundingBox, state = showingScrollbarContainer, index = 0)

    private val scrollbar by UIBlock(EssentialPalette.SCROLLBAR).constrain {
        width = 100.percent
    } hiddenChildOf scrollbarContainer

    private val accountsScroller by ScrollComponent(
        pixelsPerScroll = pixelsPerScroll,
    ).constrain {
        width = 100.percent
        height = 100.percent
    } childOf scrollBoundingBox

    init {
        scrollBoundingBox.shouldIgnore = { it == accountsScroller.children.firstOrNull() }
    }

    private val accountsContainer by UIContainer().constrain {
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    }.apply {
        effect(ZIndexEffect(1))
    } childOf accountsScroller

    private val addAccountButton by MenuButton(
        BasicState("Add Account"),
        BasicState(MenuButton.BLUE),
        BasicState(MenuButton.LIGHT_BLUE),
        textAlignment = MenuButton.Alignment.LEFT,
        textXOffset = BasicState(-1f),
        collapsedText = BasicState("+"),
    ) { GuiUtil.pushModal { AddAccountModal(it) } }
        .setIcon(BasicState(EssentialPalette.PLUS_5X), yOffset = -1f, visibleState = !collapsed).constrain {
            // y set in init
            width = 80.pixels
            height = 20.pixels
        }.bindCollapsed(collapsed, 50f)
        .apply {
            bindEssentialTooltip(hoveredState() and collapsed, BasicState("Add Account"))
            bindEffect(ZIndexEffect(2), hoveredState(layoutSafe = false))
        } childOf accountsScroller

    init {
        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedSizeConstraint()
        }

        // Highlight mainButton and the collapse button when the container is hovered
        buttonContainer.hoveredState(layoutSafe = false).onSetValueAndNow {
            mainButton.hoveredStyleOverrides.set(it)
            collapseButton.hoveredStyleOverrides.set(it)
        }

        // NOTE: This does not make `scrollbarContainer` visible! We just want it so that `scrollbar` has a Window parent.
        scrollbarContainer.parent = scrollBoundingBox
        scrollbarContainer.children.addObserver { _, _ ->
            showingScrollbarContainer.set(scrollbarContainer.children.isNotEmpty())
        }

        accountsScroller.addScrollAdjustEvent(false) { _, _ ->
            val remainder = accountsScroller.verticalOffset % pixelsPerScroll
            scrollBoundingBoxOnTop.set(remainder == 0.0f)
        }

        accountsScroller.setVerticalScrollBarComponent(scrollbar, hideWhenUseless = true)

        mainButton.setTooltip(
            USession.active.map { it.username }.toV1(this),
            above = false,
            mainButton.isTruncated,
            followCursorX = true,
            followCursorY = true,
            xOffset = 4f,
            yOffset = 4f,
            notchSize = 0,
        )

        accountsContainer.bindChildren(accountsList) { accountInfo ->
            Account(accountInfo, collapsed, accountManager)
        }

        // Adjust addAccount button x constraint to avoid extra overlap if there are no extra accounts in the switcher
        fun adjustAddAccount() {
            addAccountButton.constrain {
                y = SiblingConstraint(if (accountsList.size > 0) -1f else 0f)
            }
        }
        accountsList.addObserver { _, _ -> adjustAddAccount() }
        adjustAddAccount()

        // Collapse switcher when clicking in or outside it
        onFocus { accountsExpanded.set(true) }
        onFocusLost { accountsExpanded.set(false) }
    }
}
