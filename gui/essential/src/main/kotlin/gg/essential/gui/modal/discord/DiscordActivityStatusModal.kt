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
package gg.essential.gui.modal.discord

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.EssentialModal2
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager

class DiscordActivityStatusModal(modalManager: ModalManager) : EssentialModal2(modalManager) {
    override fun LayoutScope.layoutBody() {
        wrappedText(
            "To enable activity status on Discord,\ngo to {settings}\nand enable {setting-name-first}\n{setting-name-second}",
            textModifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK)
        ) {
            val textModifier = Modifier.color(EssentialPalette.WHITE).shadow(EssentialPalette.BLACK)

            "settings" { text("User Settings > Activity Privacy", textModifier) }
            "setting-name-first" { text("\"Share your activity", textModifier) }
            "setting-name-second" { text("with others\".", textModifier) }
        }
    }

    override fun LayoutScope.layoutButtons() {
        primaryButton("Done") { close() }
    }
}