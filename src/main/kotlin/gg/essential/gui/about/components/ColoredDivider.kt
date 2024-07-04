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
package gg.essential.gui.about.components

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.not
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import java.awt.Color

class ColoredDivider(
    text: State<String?> = BasicState(null),
    textColor: State<Color> = BasicState(EssentialPalette.TEXT),
    hasShadow: Boolean = true,
    shadowColor: Color = EssentialPalette.TEXT_SHADOW,
    dividerColor: State<Color> = BasicState(EssentialPalette.LIGHT_DIVIDER),
    textPadding: Float = 6f,
    scaleOffset: Float = 0f,
) : UIContainer() {

    constructor(
        text: String? = null,
        textColor: Color = EssentialPalette.TEXT,
        hasShadow: Boolean = true,
        shadowColor: Color = EssentialPalette.TEXT_SHADOW,
        dividerColor: Color = EssentialPalette.LIGHT_DIVIDER,
        textPadding: Float = 6f,
        scaleOffset: Float = 0f,
    ) : this(
        BasicState(text),
        BasicState(textColor),
        hasShadow,
        shadowColor,
        BasicState(dividerColor),
        textPadding,
        scaleOffset,
    )

    init {
        constrain {
            height = ChildBasedMaxSizeConstraint()
            width = 100.percent
        }

        val hasText = text.map { it != null }

        val textContainer by UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = ChildBasedMaxSizeConstraint() + textPadding.pixels * 2
            height = ChildBasedMaxSizeConstraint()
        } childOf this

        EssentialUIText(shadow = hasShadow, shadowColor = shadowColor).bindText(text.map { it ?: "" }).constrain {
            x = CenterConstraint()
            color = textColor.toConstraint()
            textScale = GuiScaleOffsetConstraint(scaleOffset)
        } childOf textContainer

        // Divider line left
        UIBlock(dividerColor).constrain {
            y = CenterConstraint()
            width = 50.percent - (100.percent boundTo textContainer) / 2
            height = GuiScaleOffsetConstraint(scaleOffset)
        } childOf this

        // Divider line middle
        UIBlock(dividerColor).constrain {
            x = 0.pixels boundTo textContainer
            y = CenterConstraint()
            width = 100.percent
            height = GuiScaleOffsetConstraint(scaleOffset)
        }.bindParent(textContainer, !hasText)

        // Divider line right
        UIBlock(dividerColor).constrain {
            x = SiblingConstraint() boundTo textContainer
            y = CenterConstraint()
            width = FillConstraint()
            height = GuiScaleOffsetConstraint(scaleOffset)
        } childOf this
    }
}
