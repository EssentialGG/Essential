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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.universal.USound
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class Checkbox(
    initialValue: Boolean = false,
    boxColor: State<Color> = EssentialPalette.BUTTON.state(),
    checkmarkColor: State<Color> = EssentialPalette.TEXT_HIGHLIGHT.state(),
    checkmarkScaleOffset: Float = 0f,
    private val playClickSound: Boolean = true,
    private val callback: ((Boolean) -> Unit)? = null,
) : UIBlock(EssentialPalette.BUTTON) {

    val isChecked = initialValue.state()
    val boxColorState = boxColor.map { it }
    val checkmarkColorState = checkmarkColor.map { it }

    init {
        constrain {
            width = 9.pixels
            height = AspectConstraint()
            color = hoveredState().zip(boxColorState).map { (hovered, color) ->
                if (hovered) color.brighter() else color
            }.toConstraint()
        }

        onLeftClick {click ->
            click.stopPropagation()
            toggle()
        }
    }

    private val checkmark by Checkmark(checkmarkScaleOffset, checkmarkColorState).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }.bindParent(this, isChecked)

    fun toggle() {
        isChecked.set { !it }

        if (playClickSound) {
            USound.playButtonPress()
        }

        callback?.invoke(isChecked.get())
    }
}

private class Checkmark(scaleOffset: Float, color: State<Color>) : UIContainer() {
    init {
        repeat(5) {
            UIBlock(color).constrain {
                x = SiblingConstraint(alignOpposite = true)
                y = SiblingConstraint()
                width = AspectConstraint()
                height = GuiScaleOffsetConstraint(scaleOffset)
            } childOf this
        }

        repeat(2) {
            UIBlock(color).constrain {
                x = SiblingConstraint(alignOpposite = true)
                y = SiblingConstraint(alignOpposite = true)
                width = AspectConstraint()
                height = GuiScaleOffsetConstraint(scaleOffset)
            } childOf this
        }

        constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint() * 5
        }
    }
}
