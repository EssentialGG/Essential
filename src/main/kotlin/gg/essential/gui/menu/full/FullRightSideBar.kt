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

import gg.essential.config.EssentialConfig
import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.menu.RightSideBar
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.handlers.PauseMenuDisplay.Companion.window
import gg.essential.universal.UMinecraft.getMinecraft
import gg.essential.util.AutoUpdate
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.pollingState
import gg.essential.vigilance.utils.onLeftClick

class FullRightSideBar(
    menuType: PauseMenuDisplay.MenuType,
    topButton: UIContainer,
    bottomButton: UIContainer,
    isCollapsed: State<Boolean>,
    menuVisible: State<Boolean>,
) : RightSideBar(menuType, menuVisible) {

    init {
        collapsed.rebind(isCollapsed)

        essentialButton.constrain {
            x = 0.pixels(alignOpposite = true)
        }.setTooltip(
            essentialTooltip,
            xAlignment = MenuButton.Alignment.RIGHT
        ) childOf this

        // Superclass component constraints
        inviteButton.constrain {
            x = 0.pixels(alignOpposite = true)
            y = 0.pixels boundTo topButton
        }.setTooltip(inviteTooltip, xAlignment = MenuButton.Alignment.RIGHT) childOf this

        worldSettings.constrain {
            x = SiblingConstraint(4f, alignOpposite = true) boundTo inviteButton
            y = CenterConstraint() boundTo inviteButton
        }.setTooltip(worldSettingsTooltip).bindParent(this, worldSettingsVisible)

        social.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.setTooltip(socialTooltip, xAlignment = MenuButton.Alignment.RIGHT) childOf this

        pictures.constrain {
            y = SiblingConstraint(4f)
        }.bindConstraints(isCollapsed) { collapsed ->
            x = if (collapsed) CenterConstraint() else 0.pixels(alignOpposite = true)
        }.setTooltip(picturesTooltip) childOf this

        settings.constrain {
            x = 0.pixels(alignOpposite = true)
        }.bindConstraints(isCollapsed) { collapsed ->
            y = if (collapsed) SiblingConstraint(4f) boundTo social else 0.pixels boundTo bottomButton
        }.setTooltip(settingsTooltip, xAlignment = MenuButton.Alignment.RIGHT) childOf this
    }

    private val toolbarContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint()
        height = 20.pixels
    }.bindConstraints(isCollapsed) { collapsed ->
        y = if (collapsed) {
            0.pixels boundTo bottomButton
        } else {
            (4.pixels(alignOpposite = true) boundTo window).coerceAtMost(SiblingConstraint(30f) boundTo bottomButton)
        }
    } childOf this

    private val toolbar by UIContainer().constrain {
        width = ChildBasedSizeConstraint()
        height = 100.percent
    }.also { component ->
        if (EssentialConfig.showQuickActionBar) {
            component childOf toolbarContainer
        }
    }

    private val isFullScreen = pollingState {
        //#if MC<=11200
        getMinecraft().isFullScreen
        //#else
        //$$ getMinecraft().mainWindow.isFullscreen
        //#endif
    }
    private val fullscreenToggleButton by MenuButton {
        //#if MC<=11200
        getMinecraft().toggleFullscreen()
        //#else
        //$$ getMinecraft().mainWindow.toggleFullscreen()
        //#endif
    }.constrain {
        x = 0.pixels(alignOpposite = true)
        width = 20.pixels
        height = 20.pixels
    }.setIcon(isFullScreen.map { if (it) EssentialPalette.FULLSCREEN_10X_ON else EssentialPalette.FULLSCREEN_10X_OFF })
        .setTooltip(
            isFullScreen.map { if (it) "Disable Full Screen" else "Enable Full Screen" },
            xAlignment = MenuButton.Alignment.RIGHT,
        ) childOf toolbar

    private val isSilentMode = EssentialConfig.disableAllNotificationsState
    private val silentModeToggleButton by MenuButton {
        EssentialConfig.run {
            disableAllNotifications = !disableAllNotifications
        }
    }.constrain {
        x = SiblingConstraint(4f, alignOpposite = true)
        width = 20.pixels
        height = 20.pixels
    }.setIcon(isSilentMode.map {
        if (it) EssentialPalette.NOTIFICATIONS_10X_OFF else EssentialPalette.NOTIFICATIONS_10X_ON
    }.toV1(this)) childOf toolbar

    private val isOwnCosmeticsVisible = pollingState { connectionManager.cosmeticsManager.ownCosmeticsVisible }
    private val cosmeticVisibilityToggleButton by MenuButton {
        connectionManager.cosmeticsManager.toggleOwnCosmeticVisibility(false)
    }.constrain {
        x = SiblingConstraint(4f, alignOpposite = true)
        width = 20.pixels
        height = 20.pixels
    }.setIcon(isOwnCosmeticsVisible.map { if (it) EssentialPalette.COSMETICS_10X_ON else EssentialPalette.COSMETICS_10X_OFF })
        .setTooltip(isOwnCosmeticsVisible.map { if (it) "Hide My Cosmetics" else "Show My Cosmetics" }) childOf toolbar

    init {
        folder.constrain {
            x = 4.pixels(alignOutside = true) boundTo pictures
            y = CenterConstraint() boundTo settings
        }.setTooltip(folderTooltip) childOf this

        constrain {
            width = 100.percent
            height = ChildBasedRangeConstraint()
        }

        // Manually create silent toggle button tooltip, so it can be aligned without displaying past the sidebar boundaries
        val silentButtonTooltip =
            EssentialTooltip(
                silentModeToggleButton,
                position = EssentialTooltip.Position.ABOVE,
            ).constrain {
                x = 0.pixels(alignOpposite = true) boundTo fullscreenToggleButton
                y = SiblingConstraint(5f, alignOpposite = true) boundTo silentModeToggleButton
            }
                .bindLine(isSilentMode.map { if (it) "Disable Silent Mode" else "Enable Silent Mode" }.toV1(this))
                .bindVisibility(silentModeToggleButton.hoveredState())

        messageFlag.constrain {
            x = 3.pixels(alignOutside = true) boundTo social
            y = CenterConstraint() boundTo social
        }.onLeftClick { social.runAction() }.bindParent(this, hasNotices)

        betaFlag.constrain {
            x = 3.pixels(alignOutside = true) boundTo essentialButton
            y = CenterConstraint() boundTo essentialButton
        }.bindParent(
            window,
            menuVisible and BasicState(VersionData.essentialBranch != "stable"),
        )

        updateFlag.constrain {
            x = SiblingConstraint(3f, alignOpposite = true) boundTo essentialButton
            y = CenterConstraint() boundTo essentialButton
        }.apply {
            bindHoverEssentialTooltip(
                updateTooltip,
                EssentialTooltip.Position.ABOVE,
            )
        }.bindParent(window, menuVisible and AutoUpdate.updateAvailable.toV1(this))
    }
}
