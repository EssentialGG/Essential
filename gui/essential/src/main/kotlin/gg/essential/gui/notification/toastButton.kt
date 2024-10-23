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
package gg.essential.gui.notification

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedSize
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.util.onLeftClick

fun toastButton(
    text: String,
    isCompact: Boolean = false,
    // TODO: It would be better to use `Modifier.then` instead of using default values for these, but that would break
    //       hover properties.
    //       https://discord.com/channels/887304453500325900/887708890127536128/1214112758786826270
    backgroundModifier: Modifier = Modifier.color(EssentialPalette.GRAY_BUTTON).shadow(EssentialPalette.BLACK).hoverColor(EssentialPalette.GRAY_BUTTON_HOVER),
    textModifier: Modifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.TEXT_SHADOW).hoverColor(EssentialPalette.TEXT_HIGHLIGHT),
    action: () -> Unit,
): UIComponent {
    val sizeModifier = if (isCompact) {
        Modifier.childBasedSize(5f)
    } else {
        Modifier.childBasedWidth(10f).childBasedHeight(5f)
    }

    return UIContainer().apply {
        layout(Modifier.childBasedSize()) {
            box(backgroundModifier.hoverScope().then(sizeModifier)) {
                text(text, textModifier)
            }.onLeftClick { action() }
            spacer(width = 1f, height = 1f) // Extra pixel for button shadow
        }
    }
}