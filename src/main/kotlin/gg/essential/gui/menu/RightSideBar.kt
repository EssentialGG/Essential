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
import gg.essential.config.EssentialConfig
import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.AboutMenu
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.modals.UpdateAvailableModal
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.sps.WorldShareSettingsGui
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.pollingState
import gg.essential.gui.util.stateBy
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.network.connectionmanager.sps.SPSSessionSource
import gg.essential.universal.UDesktop
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

open class RightSideBar(menuType: PauseMenuDisplay.MenuType, menuVisible: State<Boolean>) : UIContainer() {

    val connectionManager = Essential.getInstance().connectionManager
    val collapsed = BasicState(false).map { it }
    private val hostable = BasicState(menuType == PauseMenuDisplay.MenuType.MAIN)
    private val showHostWorldInsteadOfInvite = stateBy { hostable() }
    private val isHostingWorld = pollingState { connectionManager.spsManager.localSession != null }

    val essentialTooltip = BasicState("About Essential")
    val inviteTooltip = stateBy {
        when {
            !collapsed() -> ""
            showHostWorldInsteadOfInvite() -> "Host World"
            else -> "Invite Friends"
        }
    }
    val socialTooltip = collapsed.map { if (it) "Social" else "" }
    val picturesTooltip = collapsed.map { if (it) "Pictures" else "" }
    val settingsTooltip = collapsed.map { if (it) "Settings" else "" }
    val folderTooltip = BasicState("Minecraft Folder")
    val worldSettingsTooltip = BasicState("World Host Settings")
    val updateTooltip = BasicState("Update Available!")

    val worldSettingsVisible = isHostingWorld

    private val inviteIcon = showHostWorldInsteadOfInvite.map {
        if (it) EssentialPalette.WORLD_8X
        else EssentialPalette.ENVELOPE_9X7
    }

    private val messageCount = connectionManager.chatManager.unreadMessageCount
        .zip(connectionManager.noticesManager.socialMenuNewFriendRequestNoticeManager.unseenFriendRequestCount().toV2())
        .map { (messages, friends) -> messages + friends }
        .map {
            if (it > 98) "99+" else it.toString()
        }
    val hasNotices = messageCount.map { it != "0" }

    val messageFlag by TextFlag(
        stateOf(MenuButton.LIGHT_RED),
        MenuButton.Alignment.CENTER,
        messageCount,
    )

    val essentialButton by MenuButton(BasicState("Essential"), textAlignment = MenuButton.Alignment.LEFT) {
        GuiUtil.openScreen { AboutMenu() }
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.setIcon(
        BasicState(EssentialPalette.ESSENTIAL_7X),
        rightAligned = true,
        color = BasicState(EssentialPalette.MESSAGE_SENT),
        xOffset = -1f,
    ).bindCollapsed(collapsed, 20f)

    val betaFlag by TextFlag(
        stateOf(MenuButton.LIGHT_RED),
        MenuButton.Alignment.CENTER,
        stateOf("BETA"),
    ).apply {
        bindEssentialTooltip(
            hoveredState() and menuVisible,
            BasicState("Branch: ${VersionData.essentialBranch}"),
            EssentialTooltip.Position.ABOVE,
        )
    }.onLeftClick {
        essentialButton.runAction()
    }

    val updateFlag by IconFlag(stateOf(MenuButton.NOTICE_GREEN), stateOf(EssentialPalette.DOWNLOAD_7X8)).onLeftClick {
        GuiUtil.pushModal { UpdateAvailableModal(it) }
    }

    val worldSettings by MenuButton {
        GuiUtil.openScreen { WorldShareSettingsGui() }
    }.constrain {
        width = 20.pixels
        height = AspectConstraint()
    }.setIcon(BasicState(EssentialPalette.WORLD_8X))

    val inviteButton by MenuButton(
        showHostWorldInsteadOfInvite.map { if (it) "Host World" else "Invite" },
        textAlignment = MenuButton.Alignment.LEFT,
    ) {
        PauseMenuDisplay.showInviteOrHostModal(SPSSessionSource.PAUSE_MENU)
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.setIcon(
        inviteIcon,
        rightAligned = true,
        xOffset = -2f,
        yOffset = if (hostable.get()) 1f else 0f,
    ).bindCollapsed(collapsed, 20f)

    val social by MenuButton(BasicState("Social"), textAlignment = MenuButton.Alignment.LEFT) {
        GuiUtil.openScreen { SocialMenu() }
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.setIcon(BasicState(EssentialPalette.SOCIAL_10X), rightAligned = true, xOffset = -1f).bindCollapsed(collapsed, 20f)

    val pictures by MenuButton(BasicState("Pictures"), textAlignment = MenuButton.Alignment.LEFT) {
        GuiUtil.openScreen { ScreenshotBrowser() }
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.setIcon(BasicState(EssentialPalette.PICTURES_10X10), rightAligned = true, xOffset = -1f, yOffset = 2f)
        .bindCollapsed(collapsed, 20f)

    val settings by MenuButton(BasicState("Settings"), textAlignment = MenuButton.Alignment.LEFT) {
        GuiUtil.openScreen { EssentialConfig.gui() }
    }.constrain {
        width = 80.pixels
        height = 20.pixels
    }.setIcon(BasicState(EssentialPalette.SETTINGS_9X8), rightAligned = true, xOffset = -1f, yOffset = 1f)
        .bindCollapsed(collapsed, 20f)

    val folder by MenuButton {
        UDesktop.open(UMinecraft.getMinecraft().mcDataDir)
    }.constrain {
        width = 20.pixels
        height = AspectConstraint()
    }.setIcon(BasicState(EssentialPalette.MC_FOLDER_8X7))

    companion object {

    }

}
