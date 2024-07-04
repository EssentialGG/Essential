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
package gg.essential.gui.common.outline

import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.common.state
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import java.awt.Color

class GuiScaleOffsetOutline(val offset: Float, val color: State<Color>) : Effect() {

    constructor(offset: Float, color: Color) : this(offset, color.state())

    private val width = BasicState(1f)
    private val outline = OutlineEffect(color, width)

    private fun updateOutline() {
        outline.bindComponent(boundComponent)
        val scaleFactor = UResolution.scaleFactor
        width.set(((scaleFactor + offset).coerceAtLeast(1.0) / scaleFactor).toFloat())
    }

    override fun beforeChildrenDraw(matrixStack: UMatrixStack) {
        updateOutline()
        outline.beforeChildrenDraw(matrixStack)
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        outline.afterDraw(matrixStack)
    }
}
