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
package gg.essential.gui.vigilancev2.components

import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.isEmpty
import gg.essential.gui.elementa.state.v2.isNotEmpty
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.SubCategory
import gg.essential.gui.vigilancev2.palette.VigilancePalette

fun LayoutScope.subcategory(subcategory: SubCategory, searchState: State<String>) {
    val settings = subcategory.settingsFilteredBy(searchState)

    // Only show the header if there are settings to show, and the user is not currently searching.
    if_(settings.isNotEmpty() and searchState.isEmpty()) {
        subCategoryHeader(subcategory.name)
    }

    forEach(settings, cache = true) { setting ->
        settingContainer(setting)
    }
}

private fun LayoutScope.subCategoryHeader(name: String) {
    box(Modifier.fillWidth().childBasedMaxHeight()) {
        box(Modifier.color(VigilancePalette.SUBCATEGORY_DIVIDER).height(1f).fillWidth())

        box(
            Modifier
                .color(EssentialPalette.GUI_BACKGROUND)
                .childBasedWidth(8f)
                .alignHorizontal(Alignment.TrueCenter)
        ) {
            text(
                name,
                Modifier
                    .color(VigilancePalette.TEXT_LIGHT)
                    .shadow(VigilancePalette.TEXT_SHADOW)
            )
        }
    }
}
