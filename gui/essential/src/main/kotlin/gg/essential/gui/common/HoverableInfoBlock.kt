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

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.gui.util.hoveredState

class HoverableInfoBlock(tooltip: State<String>) : UIBlock() {

    private val hovered = hoveredState()
    private val iColor = hovered.map {
        if (it) EssentialPalette.TEXT_HIGHLIGHT else EssentialPalette.INFO_ELEMENT_UNHOVERED
    }

    init {
        constrain {
            width = 9.pixels
            height = AspectConstraint()
            color = EssentialPalette.getButtonColor(hovered).toConstraint()
        }

        bindHoverEssentialTooltip(tooltip)
    }

    private val iDot by UIBlock(iColor).constrain {
        x = CenterConstraint()
        y = 2.pixels
        width = 1.pixel
        height = AspectConstraint()
    } childOf this

    private val iBody by UIBlock(iColor).constrain {
        x = CenterConstraint()
        y = SiblingConstraint(1f)
        width = 1.pixel
        height = AspectConstraint(3f)
    } childOf this
}