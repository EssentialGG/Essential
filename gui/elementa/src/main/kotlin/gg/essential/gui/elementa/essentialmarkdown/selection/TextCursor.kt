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
package gg.essential.gui.elementa.essentialmarkdown.selection

import gg.essential.elementa.dsl.width
import gg.essential.gui.elementa.essentialmarkdown.drawables.TextDrawable

/**
 * A simple class which points to a position in a TextDrawable.
 */
class TextCursor(target: TextDrawable, val offset: Int) : Cursor<TextDrawable>(target) {
    override val xBase = target.x +
        target.formattedText.substring(0, offset + target.style.numFormattingChars).width(target.scaleModifier)
    override val yBase = target.y

    override operator fun compareTo(other: Cursor<*>): Int {
        if (other !is TextCursor) {
            return target.y.compareTo(other.target.y).let {
                if (it == 0) target.x.compareTo(other.target.x) else it
            }
        }
        
        if (target == other.target)
            return offset.compareTo(other.offset)

        if (target.y == other.target.y)
            return target.x.compareTo(other.target.x)

        return target.y.compareTo(other.target.y)
    }
}
