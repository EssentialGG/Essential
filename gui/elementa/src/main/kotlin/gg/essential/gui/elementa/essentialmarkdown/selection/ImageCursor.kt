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

import gg.essential.gui.elementa.essentialmarkdown.drawables.ImageDrawable

class ImageCursor(target: ImageDrawable) : Cursor<ImageDrawable>(target) {
    override fun compareTo(other: Cursor<*>): Int {
        if (other !is ImageCursor) {
            return target.y.compareTo(other.target.y).let {
                if (it == 0) target.x.compareTo(other.target.x) else it
            }
        }

        return if (target.url == other.target.url) return 0 else 1
    }
}
