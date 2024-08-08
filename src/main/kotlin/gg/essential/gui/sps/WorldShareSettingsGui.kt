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
package gg.essential.gui.sps

import gg.essential.api.gui.GuiRequiresTOS
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.CopyConstraintColor
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.sps.categories.GameRulesCategory
import gg.essential.gui.sps.categories.PlayersAndPermissionsCategory
import gg.essential.gui.sps.categories.SPSSettingsCategory
import gg.essential.universal.GuiScale
import gg.essential.util.GuiUtil

class WorldShareSettingsGui : InternalEssentialGUI(
    ElementaVersion.V6,
    "World Host Settings",
    GuiScale.scaleForScreenSize().ordinal,
), GuiRequiresTOS {

    private val spsSettingsCategory by SPSSettingsCategory() childOf content

    private val firstDivider = createDivider() childOf content

    private val gameRulesCategory by GameRulesCategory() childOf content

    private val secondDivider = createDivider() childOf content

    private val playersAndPermissionsCategory by PlayersAndPermissionsCategory(spsSettingsCategory.cheatsEnabled) childOf content

    private val sections = listOf(spsSettingsCategory, gameRulesCategory, playersAndPermissionsCategory)


    init {
        val search by EssentialCollapsibleSearchbar().constrain {
            y = CenterConstraint()
            x = 10.pixels(alignOpposite = true)
        } childOf titleBar

        search.textContent.onSetValue { searchString ->
            sections.forEach {
                it.filter(searchString)
            }
        }

        // Setup scrollbars
        spsSettingsCategory.scroller.setVerticalScrollBarComponent(createScrollbar(firstDivider), hideWhenUseless = true)
        gameRulesCategory.scroller.setVerticalScrollBarComponent(createScrollbar(secondDivider), hideWhenUseless = true)
        playersAndPermissionsCategory.scroller.setVerticalScrollBarComponent(createScrollbar(rightDivider), hideWhenUseless = true)
    }

    private fun createScrollbar(boundWithin: UIComponent): UIBlock {
       return UIBlock(EssentialPalette.SCROLLBAR).constrain {
           width = CopyConstraintFloat() boundTo rightDivider
       } childOf boundWithin
    }

    override fun updateGuiScale() {
        newGuiScale = GuiUtil.getGuiScale()
        super.updateGuiScale()
    }

    private fun createDivider(): UIBlock {
        val divider by UIBlock().constrain {
            x = SiblingConstraint()
            color = CopyConstraintColor() boundTo rightDivider
            width = CopyConstraintFloat() boundTo rightDivider
            height = 100.percent
        }
        return divider
    }


}