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

import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels

/**
 * A simple UIContainer where you can specify [width], [height], or both.
 *
 * If only [width] is specified, X-axis will be constrained to [SiblingConstraint].
 *
 * If only [height] is specified, Y-axis will be constrained to [SiblingConstraint].
 */
class Spacer(width: WidthConstraint = 0.pixels, height: HeightConstraint = 0.pixels) : HollowUIContainer() {
    constructor(width: Float, _desc: Int = 0) : this(width = width.pixels) { setX(SiblingConstraint()) }
    constructor(height: Float, _desc: Short = 0) : this(height = height.pixels) { setY(SiblingConstraint()) }
    constructor(width: Float, height: Float) : this(width = width.pixels, height = height.pixels)

    init {
        constrain {
            this.width = width
            this.height = height
        }
    }
}
