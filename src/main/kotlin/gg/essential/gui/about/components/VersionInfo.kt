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
package gg.essential.gui.about.components

import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.shadow.EssentialUIWrappedText

class VersionInfo : UIContainer() {

    init {
        val versionText = listOf(
            "Essential Mod v${VersionData.essentialVersion}",
            "#${VersionData.essentialCommit}",
            VersionData.formatPlatform(VersionData.getEssentialPlatform())
        )
        EssentialUIWrappedText(versionText.joinToString("\n")).constrain {
            width = versionText.maxOf { it.width() }.pixels
            color = EssentialPalette.TEXT.toConstraint()
        } childOf this
    }
}
