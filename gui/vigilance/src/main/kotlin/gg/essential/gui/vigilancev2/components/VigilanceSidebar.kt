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

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.Category
import gg.essential.gui.vigilancev2.palette.VigilancePalette
import gg.essential.util.scrollGradient

// The height for the scroll gradient at the top and bottom of the sidebar
const val scrollGradientHeight = 30f

/// The padding between the left and top side of the view and the first item
const val sidebarPadding = 10f

/// The spacing between two sidebar items
const val sidebarItemPadding = 7f

fun LayoutScope.vigilanceSidebar(
    modifier: Modifier = Modifier,
    categories: ListState<Category>,
    currentCategoryName: MutableState<String>,
    searchState: MutableState<String>,
    customSections: List<LayoutScope.() -> Unit> = emptyList(),
): ScrollComponent {
    val scroller = scrollable(modifier, vertical = true) {
        box(Modifier.fillWidth().childBasedHeight(sidebarPadding).alignVertical(Alignment.Start)) {
            column(Modifier.fillWidth(), Arrangement.spacedBy(sidebarPadding)) {
                sidebarSection {
                    forEach(categories, cache = true) { category ->
                        categoryLabel(
                            category,
                            currentCategoryName,
                            Modifier.onLeftClick { searchState.set("") }
                        )
                    }
                }

                customSections.forEach {
                    divider()
                    sidebarSection(it)
                }
            }
        }
    }

    scroller.scrollGradient(scrollGradientHeight.pixels)
    return scroller
}

private fun LayoutScope.divider() {
    box(Modifier.fillWidth().color(VigilancePalette.SIDEBAR_DIVIDER).height(1f))
}

private fun LayoutScope.sidebarSection(block: LayoutScope.() -> Unit) {
    column(
        Modifier.fillWidth(padding = sidebarPadding),
        Arrangement.spacedBy(sidebarItemPadding),
        Alignment.Start,
        block
    )
}