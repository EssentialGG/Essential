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
package gg.essential.gui.elementa

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.MasterConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.universal.UResolution

class GuiScaleOffsetConstraint(val offset: Float = -1f) : MasterConstraint {
    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    private fun getValue(): Float {
        val scaleFactor = UResolution.scaleFactor
        return ((scaleFactor + offset).coerceAtLeast(1.0) / scaleFactor).toFloat()
    }

    override fun getXPositionImpl(component: UIComponent): Float = component.parent.getLeft() + getValue()

    override fun getYPositionImpl(component: UIComponent): Float = component.parent.getTop() + getValue()

    override fun getWidthImpl(component: UIComponent): Float = getValue()

    override fun getHeightImpl(component: UIComponent): Float = getValue()

    override fun getRadiusImpl(component: UIComponent): Float = getValue()

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
    }
}