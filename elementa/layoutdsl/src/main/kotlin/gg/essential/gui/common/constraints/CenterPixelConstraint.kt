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
package gg.essential.gui.common.constraints

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.ConstraintType
import gg.essential.elementa.constraints.PositionConstraint
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import kotlin.math.ceil
import kotlin.math.floor

/** Centers the component to whole pixels, rounding down unless [roundUp] is true */
class CenterPixelConstraint(private val roundUp: Boolean = false) : PositionConstraint {

    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    override fun getXPositionImpl(component: UIComponent): Float {
        val parent = constrainTo ?: component.parent

        val center = if (component.isPositionCenter()) {
            parent.getWidth() / 2
        } else {
            parent.getWidth() / 2 - component.getWidth() / 2
        }

        return parent.getLeft() + if (roundUp) ceil(center) else floor(center)
    }

    override fun getYPositionImpl(component: UIComponent): Float {
        val parent = constrainTo ?: component.parent

        val center = if (component.isPositionCenter()) {
            parent.getHeight() / 2
        } else {
            parent.getHeight() / 2 - component.getHeight() / 2
        }

        return parent.getTop() + if (roundUp) ceil(center) else floor(center)
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {}
}
