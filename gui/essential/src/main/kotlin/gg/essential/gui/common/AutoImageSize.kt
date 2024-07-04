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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.image.CacheableImage
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.constrain
import gg.essential.universal.utils.ReleasedDynamicTexture

/**
 * Supply an instance of this class to a [UIImage] to automatically set the size of
 * the component supplied in the constructor to the size of the image's texture.
 *
 * @param component The component to adjust the size of
 * @param alwaysOverrideSize When true, the size of the component will always update to the size of the texture. When false, it will only update the size of the component if the width and height of the component appear to be unchanged form their default values
 *
 */
class AutoImageSize(
    private val component: UIComponent,
    private val alwaysOverrideSize: Boolean = false,
) : CacheableImage {

    override fun applyTexture(texture: ReleasedDynamicTexture?) {
        if (texture == null) return
        val constraints = component.constraints

        if (alwaysOverrideSize || (appearsDefaultOrOverridable(constraints.width) && appearsDefaultOrOverridable(constraints.height))) {
            component.constrain {
                width = AutomaticImageSizeConstraint(texture.width)
                height = AutomaticImageSizeConstraint(texture.height)
            }
        }
    }

    /**
     * Returns true if the supplied [constraint] appears to be unedited from its default state
     */
    private fun appearsDefault(constraint: SuperConstraint<Float>): Boolean {
        if (constraint !is PixelConstraint) {
            return false
        }
        return constraint.value == 0f && !constraint.alignOpposite && !constraint.alignOutside
    }

    private fun appearsDefaultOrOverridable(constraint: SuperConstraint<Float>): Boolean {
        return constraint is AutomaticImageSizeConstraint || appearsDefault(constraint)
    }

    override fun supply(image: CacheableImage) {
        // Not implemented
    }

    /**
     * Effectively a PixelConstraint for Width and Height that does not have any of the extra options.
     * This class exists so that we can set the width and height of an image to a fixed value while being
     * able to detect the type and mark the constraint as overrideable if the image changes.
     */
    class AutomaticImageSizeConstraint(
        private val value: Int
    ):  WidthConstraint, HeightConstraint  {
        override var cachedValue = 0f
        override var recalculate = true
        override var constrainTo: UIComponent? = null

        override fun getHeightImpl(component: UIComponent): Float {
            return value.toFloat()
        }

        override fun getWidthImpl(component: UIComponent): Float {
            return value.toFloat()
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
        }
    }

}