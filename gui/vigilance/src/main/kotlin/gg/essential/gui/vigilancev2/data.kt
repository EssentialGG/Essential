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
package gg.essential.gui.vigilancev2

import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.vigilancev2.builder.VisibleDependencyPredicate
import gg.essential.gui.vigilancev2.utils.containsSearchTerm
import gg.essential.vigilance.data.PropertyData

class Category(val name: String, val subcategories: List<SubCategory>)

class SubCategory(val name: String, val settings: List<PropertyData>) {
    val visibleSettings = stateBy {
        settings.filter {
            val predicate = it.dependencyPredicate as VisibleDependencyPredicate
            predicate.visible()
        }
    }.toListState()

    fun settingsFilteredBy(searchTerm: State<String>) =
        stateBy {
            val needle = searchTerm()
            visibleSettings().filter {
                it.containsSearchTerm(needle)
            }
        }.toListState()
}
