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

import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.menu.RightSideBar
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

class CompactRightSideBar(
    menuType: PauseMenuDisplay.MenuType,
    menuVisible: State<Boolean>,
    parentContainer: UIContainer,
    accountManager: AccountManager,
) : RightSideBar(menuType, menuVisible) {

    private val accountSwitcher by accountManager.getCompactAccountSwitcher(parentContainer).constrain {
        x = 0.pixels(alignOpposite = true)
    }.bindParent(this, BasicState(menuType == PauseMenuDisplay.MenuType.MAIN))

    init {
        collapsed.rebind(BasicState(true))

        // Superclass component constraints
        val aboutContainer by UIContainer().constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        } childOf this

        essentialButton.constrain {
            x = 0.pixels(alignOpposite = true)
        }.bindHoverEssentialTooltip(essentialTooltip, EssentialTooltip.Position.LEFT) childOf aboutContainer

        betaFlag.constrain {
            x = SiblingConstraint(3f, alignOpposite = true)
            y = CenterConstraint()
        }.bindParent(aboutContainer, BasicState(VersionData.essentialBranch != "stable"))

        updateFlag.constrain {
            x = SiblingConstraint(3f, alignOpposite = true)
            y = CenterConstraint()
        }.apply {
            bindHoverEssentialTooltip(
                updateTooltip,
                EssentialTooltip.Position.LEFT,
            )
        }.bindParent(aboutContainer, AutoUpdate.updateAvailable.toV1(this))

        worldSettings.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.bindHoverEssentialTooltip(worldSettingsTooltip, EssentialTooltip.Position.LEFT)
            .bindParent(this, worldSettingsVisible, index = 0)

        inviteButton.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.bindHoverEssentialTooltip(inviteTooltip, EssentialTooltip.Position.LEFT) childOf this

        val socialContainer by UIContainer().constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        } childOf this

        social.constrain {
            x = 0.pixels(alignOpposite = true)
        }.bindHoverEssentialTooltip(socialTooltip, EssentialTooltip.Position.LEFT) childOf socialContainer

        messageFlag.constrain {
            x = SiblingConstraint(3f, alignOpposite = true)
            y = CenterConstraint()
        }.onLeftClick { social.runAction() }.bindParent(socialContainer, hasNotices)

        pictures.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.bindHoverEssentialTooltip(picturesTooltip, EssentialTooltip.Position.LEFT) childOf this

        settings.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.bindHoverEssentialTooltip(settingsTooltip, EssentialTooltip.Position.LEFT) childOf this

        folder.constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint(4f)
        }.bindHoverEssentialTooltip(folderTooltip, EssentialTooltip.Position.LEFT) childOf this

        constrain {
            x = 0.pixels(alignOpposite = true)
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedSizeConstraint()
        }
    }
}
