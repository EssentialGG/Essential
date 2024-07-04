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
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.isNotEmpty
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.Category

fun LayoutScope.settingsCategory(
    categories: ListState<Category>,
    currentCategoryName: State<String>,
    searchState: MutableState<String>
): ScrollComponent {
    // if we are searching, then we need to show all subcategories
    val isSearching = searchState.isNotEmpty()
    val subcategories = memo {
        val allCategories = categories()
        val categoryName = currentCategoryName()

        if (isSearching()) {
            allCategories.flatMap { it.subcategories }
        } else {
            allCategories.first { it.name == categoryName }.subcategories
        }
    }.toListState()

    val sectionsEmpty = subcategories.mapEach { it.settingsFilteredBy(searchState).map(List<*>::isEmpty) }
    val isEmpty = stateBy { sectionsEmpty().all { it() } }

    return scrollable(Modifier.fillParent(), vertical = true) {
        column(
            Modifier.fillWidth(padding = 10f).childBasedHeight(10f).alignVertical(Alignment.Start),
            Arrangement.spacedBy(10f, FloatPosition.CENTER)
        ) {
            if_(isEmpty.and(searchState.isNotEmpty())) {
                text("No matching settings found :(")
            } `else` {
                forEach(subcategories) {
                    subcategory(it, searchState)
                }
            }
        }
    }
}
